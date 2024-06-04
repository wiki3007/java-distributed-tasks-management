import java.io.*;
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
    /**
     * ID of Remote Host Master Thread for identification by Main Server
     */
    private int hostId;
    private int taskId = 0;

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

    int currentWorkingThread = -1;

    /**
     * Creates a new remote host master thread object.
     * Responsible for network communications and handling the threads that later have their results returned to the server
     * @param serverAddress Address of the server the remote host communicates with
     * @param port Network port over which remote host communicates with
     * @throws IOException If stuff breaks
     */
    public RemoteHostMasterThread(InetAddress serverAddress, int port, int id) throws IOException {
        this.hostId = id;
        this.socket = new Socket(serverAddress, port);
        socket.setSoTimeout(1000);
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
                if (taskArray.get(i).getTaskPriority() > taskArray.get(j).getTaskPriority())
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
    private void doPriorityCheck(){
        //System.out.println("PRIORITY BEFORE SORT");
        sortTaskArray();
        //System.out.println("PRIORITY AFTER SORT");

        // find the ID of the first not yet finished task
        int firstUndoneTask=0;

        if (taskArray.size() >= 2)
        {
            for (int i=0; i<taskArray.size(); i++)
            {
                //System.out.println("FUTURE DONE TEST PARITY");
                String status = taskArray.get(i).getStatus();
                if (!status.equals("Done") && !status.equals("Interrupted") && !status.equals("Cancelled"))
                {
                    firstUndoneTask = i;
                    break;
                }
                //System.out.println("FUTURE AFTER PARITY");
            }
            currentWorkingThread = firstUndoneTask;

            // then wake it up and put every task after it to sleep
            //System.out.println("PRIORITY BEFORE FLAG");
            taskArray.get(firstUndoneTask).lowPriorityFlag = false;
            //System.out.println("PRIORITY AFTER FLAG");
            //System.out.println("HOST BEFORE WAKEUP");
            for (int i= firstUndoneTask+1; i<taskArray.size(); i++)
            {
                taskArray.get(i).lowPriorityFlag = true;
            }
        }
        //System.out.println("HOST AFTER WAKEUP");
        //taskArray.get(firstUndoneTask).notify(); // doing notify just locks up the entire thread indefinitely
    }

    /**
     * Creates new task and puts it in the taskArray
     * @param taskPriority priority of the new task
     */
    private void startNewTask(int taskPriority){
        //System.out.println("TASK CREATEAD");
        RemoteHostTask temp = new RemoteHostTask(taskId, taskPriority);
        taskId++;
        taskArray.add(temp);
        taskArrayFuture.add(exec.submit(temp));

        doPriorityCheck();
        //System.out.println("AFTER PRIORITY CHECK");

    }

    /**
     * Get ArrayList of tasks being executed by the remote host
     * @return ArrayList of RemoteHostTask class
     */
    private ArrayList<RemoteHostTask> getRemoteHostTasks()
    {
        return taskArray;
    }

    /**
     * Sets priority to a task governed by the remote host
     * @param remoteHostTaskId ID of the task to change the priority of
     * @param priority the new priority
     */
    private void setRemoteHostTaskPriority(int remoteHostTaskId, int priority){
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
        //System.out.println("HOST READING COMMANDS");
        try
        {
            while (true) // keep reading until .readLine throws a timeout
            {
                String command = receiveMsg.readLine();
                //System.out.println(" HOST READ " + command);
                commandsList.add(command);
            }
        }
        catch (SocketTimeoutException waitTooLong)
        {
            // means no more messages to read, so just go on with your life
            //System.out.println("HOST DIDNT READ COMMANDS");
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
        //sendMsg.println(hostId);


        boolean keepAlive = true;
        int delayCounter = 0;
        while (keepAlive)
        {
            doPriorityCheck();

            // read all pending commands
            readCommands();
            //System.out.println("HOST AFTER READING COMMANDS (" + commandsListIndex + " / " + commandsList.size() + ")");

            // process all the still unfinished commands
            while (commandsListIndex < commandsList.size())
            {
                // process commands
                System.out.println("host parsing command (" + commandsListIndex + " / " + (commandsList.size()-1) + ") " + commandsList.get(commandsListIndex));
                String command = commandsList.get(commandsListIndex);
                commandsListIndex++;
                ArrayList<String> components;
                switch (command)
                {
                    case "EXIT_THREAD": // 1 string in buffer
                    case "HOST_EXIT_THREAD": // 1 string in buffer
                        sendMsg.println("HOST_EXITING");
                        exec.shutdownNow();
                        keepAlive = false;
                        break;
                    case "HOST_START_TASK": // 1 string and 1 arg in buffer
                        startNewTask(Integer.parseInt(commandsList.get(commandsListIndex)));
                        commandsListIndex++;
                        break;
                    case "HOST_CANCEL_TASK": // 1 string and 2 arg in buffer
                        //System.out.println("CANCEL START");
                        int taskIdCancel = Integer.parseInt(commandsList.get(commandsListIndex));
                        commandsListIndex++;
                        //System.out.println("CANCEL ID " + taskIdCancel);
                        String cancelIfRunning = commandsList.get(commandsListIndex);
                        commandsListIndex++;
                        //System.out.println("\t\t " + taskIdCancel + " " + cancelIfRunning);
                        if (cancelIfRunning.equals("TRUE"))
                        {
                            //System.out.println("TASK CANCELLED HOST BEFORE");
                            taskArrayFuture.get(taskIdCancel).cancel(true);
                            //System.out.println("TASK CANCELLED HOST AFTER");
                        }
                        else if (cancelIfRunning.equals("FALSE"))
                        {
                            taskArrayFuture.get(taskIdCancel).cancel(false);
                        }
                        else
                        {
                            System.out.println("Cancel failed");
                        }
                        //doPriorityCheck();
                        break;
                    case "HOST_CHANGE_PRIORITY": // 1 string and 2 arg in buffer
                        setRemoteHostTaskPriority(Integer.parseInt(commandsList.get(commandsListIndex)), Integer.parseInt(commandsList.get(commandsListIndex+1)));
                        commandsListIndex += 2;
                        break;
                    case "HOST_RETURN_TASK": // 1 string, 1 arg in buffer, 1 string return
                        sendMsg.println("RESPONSE_GET_TASK");
                        int taskId = Integer.parseInt(commandsList.get(commandsListIndex));
                        commandsListIndex++;
                        components = new ArrayList<>();
                        // task id, priority, status, result
                        components.add(String.valueOf(taskId));
                        components.add(String.valueOf(taskArray.get(taskId).getTaskPriority()));
                        components.add(taskArray.get(taskId).getStatus());
                        components.add(taskArray.get(taskId).getResult());
                        components.add(String.valueOf(taskArray.get(taskId).getServerTimeTaken()));
                        components.add(String.valueOf(taskArray.get(taskId).getClientTimeTaken()));
                        //System.out.println(components);
                        sendMsg.println(components);
                        break;
                    case "HOST_RETURN_ALL_TASKS": // 1 string in buffer, 1 string return
                        sendMsg.println("RESPONSE_GET_ALL_TASKS");
                        ArrayList<ArrayList<String>> componentsArrayArray = new ArrayList<>();
                        for (RemoteHostTask task : taskArray)
                        {
                            components = new ArrayList<>();
                            components.add(String.valueOf(task.getTaskId()));
                            components.add(String.valueOf(task.getTaskPriority()));
                            components.add(task.getStatus());
                            components.add(task.getResult());
                            components.add(String.valueOf(task.getServerTimeTaken()));
                            components.add(String.valueOf(task.getClientTimeTaken()));
                            componentsArrayArray.add(components);
                        }
                        sendMsg.println(componentsArrayArray);
                        System.out.println("RETURNING ALL TASKS HOST " + hostId);
                        break;
                    default:
                        break;
                }
                //serialOut.close();
                //System.out.println("HOST DONE PARSING");

            }

            // check if thread is done

            /*
            for (int i=currentWorkingThread; i<taskArray.size(); i++)
            {
                if (!taskArray.get(i).getStatus().equals("Done"))
                {
                    currentWorkingThread = i;
                    taskArrayFuture.get(i).notify();
                    break;
                }
            }

             */
            //System.out.println("HOST END OF LOOP");
        }
        System.out.println("HOSTTHREADEND");
        exec.shutdown();
        return null;
    }
}
