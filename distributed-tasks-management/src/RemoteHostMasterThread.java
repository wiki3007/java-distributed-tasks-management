import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Remote Host Master Thread functions as a slave to Main Server.
 * Receives tasks from Main Server and executes them
 */
public class RemoteHostMasterThread implements Callable<String> {
    private static int globalHostId = 0;
    /**
     * ID of Remote Host Master Thread for identification by Main Server
     */
    private int hostId;

    /**
     * ArrayList of tasks to execute
     */
    private ArrayList<RemoteHostTask> taskArray = new ArrayList<>();
    /**
     * ArrayList of tasks to execute converted to Future class
     */
    private ArrayList<Future<String>> taskArrayFuture = new ArrayList<>();
    /**
     * Executor to start new tasks
     */
    ExecutorService exec = Executors.newCachedThreadPool();

    /**
     * Server requests an Exchanger session
     */
    public boolean commandRequest = false;
    /**
     * Socket to handle network communications
     */
    Socket socket;
    /**
     * Reader used to read messages coming over the network
     */
    BufferedReader receiveMsg;
    /**
     * Writer used to send messages over the network
     */
    PrintWriter sendMsg;

    /**
     * List of commands received from main server
     */
    ArrayList<String> commandsList = new ArrayList<>();
    /**
     * Index of next-to-execute command
     */
    int commandsListIndex = 0;

    /**
     * Creates a new remote host master thread object.
     * Responsible for network communications and handling the threads that later have their results returned to the server
     * @param serverAddress Address of the server the remote host communicates with
     * @param port Network port over which remote host communicates with
     * @throws IOException If stuff breaks
     */
    public RemoteHostMasterThread(InetAddress serverAddress, int port) throws IOException {
        this.hostId = globalHostId++;
        this.socket = new Socket(serverAddress, port);
        socket.setSoTimeout(5000);
        receiveMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        sendMsg = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Sorts ArrayList of tasks by their priority
     */
    private void sortTaskArray()
    {
        for (int i=0; i<taskArray.size()-1; i++)
        {
            for (int j=i+1; j<taskArray.size(); j++)
            {
                if (taskArray.get(i).getTaskPriority() < taskArray.get(j).getTaskPriority())
                {
                    RemoteHostTask temp = taskArray.get(i);
                    taskArray.set(i, taskArray.get(j));
                    taskArray.set(j, temp);
                }
            }
        }
    }

    /**
     * Sorts the taskArray and blocks all but the oldest, highest priority thread
     */
    public void doPriorityCheck(){
        sortTaskArray();

        // find the ID of the first not yet finished task
        int firstUndoneTask=0;
        for (int i=0; i<taskArrayFuture.size(); i++)
        {
            if (!taskArrayFuture.get(i).isDone())
            {
                firstUndoneTask = i;
                break;
            }
        }

        // then wake it up and put every task after it to sleep
        taskArray.get(firstUndoneTask).lowPriorityFlag = false;
        for (int i= firstUndoneTask+1; i<taskArray.size(); i++)
        {
            taskArray.get(i).lowPriorityFlag = true;
        }
        taskArray.get(firstUndoneTask).notify();
    }

    /**
     * Creates new task and puts it in the taskArray
     * @param taskPriority priority of the new task
     */
    public void startNewTask(int taskPriority){
        RemoteHostTask temp = new RemoteHostTask(taskPriority);
        taskArray.add(temp);

        doPriorityCheck();
        taskArrayFuture.add(exec.submit(temp));
    }

    /**
     * Get ArrayList of tasks being executed by the remote host
     * @return ArrayList of RemoteHostTask class
     */
    public ArrayList<RemoteHostTask> getRemoteHostTasks()
    {
        return taskArray;
    }

    /**
     * Sets priority to a task governed by the remote host
     * @param remoteHostTaskId ID of the task to change the priority of
     * @param priority the new priority
     */
    public void setRemoteHostTaskPriority(int remoteHostTaskId, int priority){
        for (RemoteHostTask task : taskArray)
        {
            if (task.getTaskId() == remoteHostTaskId)
            {
                task.setTaskPriority(priority);
                doPriorityCheck();
            }
        }
    }

    /**
     * Exchanges commands
     * @throws InterruptedException If thread interrupted while exchanging
     */
    private void readCommands() throws InterruptedException, IOException {
        try
        {
            while (true) // keep reading until .readLine throws a timeout
            {
                String command = receiveMsg.readLine();
                commandsList.add(command);
            }
        }
        catch (SocketTimeoutException waitTooLong)
        {
            // means no more messages to read, so just go on with your life
        }
        finally {
            commandRequest = false;
        }
    }

    /**
     * Main loop of the Remote Host Master
     * @return
     * @throws Exception
     */
    @Override
    public String call() throws Exception {
        // notify whichever server thread that's listening who he's talking with
        sendMsg.println(hostId);

        boolean keepAlive = true;
        while (keepAlive)
        {
            // if request for incoming commands, read them
            /**
            if (commandRequest)
            {
                readCommands();
            }
            **/
            // read all pending commands
            readCommands();

            // process all the still unfinished commands
            while (commandsListIndex < commandsList.size())
            {
                // process commands
                String command = commandsList.get(commandsListIndex);

                switch (command)
                {
                    case "EXIT_THREAD": // 1 string in buffer
                    case "HOST_EXIT_THREAD": // 1 string in buffer
                        keepAlive = false;
                        break;
                    case "HOST_START_TASK": // 1 string and 1 arg in buffer
                        startNewTask(Integer.parseInt(commandsList.get(commandsListIndex)));
                        commandsListIndex++;
                        break;
                    case "HOST_CANCEL_TASK": // 1 string and 2 arg in buffer
                        taskArrayFuture.get(Integer.parseInt(commandsList.get(commandsListIndex))).cancel(Boolean.parseBoolean(commandsList.get(commandsListIndex+1)));
                        commandsListIndex += 2;
                    case "HOST_CHANGE_PRIORITY": // 1 string and 2 arg in buffer
                        setRemoteHostTaskPriority(Integer.parseInt(commandsList.get(commandsListIndex)), Integer.parseInt(commandsList.get(commandsListIndex+1)));
                        commandsListIndex += 2;
                    case "HOST_RETURN_TASK": // 1 string, 1 arg in buffer, 1 string return
                        int taskId = Integer.parseInt(commandsList.get(commandsListIndex));
                        commandsListIndex++;
                        sendMsg.println(taskArray.get(taskId));
                        break;
                    case "HOST_RETURN_ALL_TASKS": // 1 string in buffer, 1 string return
                        sendMsg.println(taskArray);
                        break;

                }
                commandsListIndex++;
            }
        }
        return null;
    }
}
