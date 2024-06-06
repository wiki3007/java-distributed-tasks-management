import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ServerComThread implements Callable<String> {
    /**
     * Reader used to read messages coming over the network
     */
    BufferedReader receiveMsg;
    /**
     * Writer used to send messages over the network
     */
    PrintWriter sendMsg;
    /**
     * Socekt to facilitate communicating over the network
     */
    Socket socket;
    /**
     * List of commands sent to remote host
     */
    public ArrayList<String> commandsList = new ArrayList<>();
    /**
     * Index of next-to-send command
     */
    int commandsListIndex = 0;
    /**
     * List of responses sent from remote host
     */
    private ArrayList<String> responseList = new ArrayList<>();
    /**
     * Index of next-to-read-response
     */
    int responsesListIndex = 0;
    /**
     * ID of the remote host that this thread communicates with, double as id of com thread
     */
    int talksWith = -1;

    /**
     * Instead of serialization, you get this. Yes, it works, unlike serialization
     */
    private ArrayList<RemoteHostTaskDummy> thisIsYourSerializationNow = new ArrayList<>();

    /**
     * If host responded back to a ping
     */
    private boolean pingAck = true;
    /**
     * If host doesn't respond to a ping after sending one, tick up the counter
     */
    private int pingDelay = 0;
    /**
     * If pingDelay reaches a certain value, assume host is dead
     */
    public boolean assumeDead = false;
    ServerComThread(Socket socket, int id) throws IOException {
        this.socket = socket;
        this.talksWith = id;
        receiveMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        sendMsg = new PrintWriter(socket.getOutputStream(), true);
        sendMsg.println(talksWith);
    }

    /**
     * Receive command to send to remote host
     * @param command String of what to send to remote host
     */
    public void receiveCommands(String command)
    {
        commandsList.add(command);
    }

    /**
     * Receive commands to send to remote host
     * @param commands Array of commands to send to remote host
     */
    public void receiveCommands(ArrayList<String> commands)
    {
        //System.out.println(commands);
        commandsList.addAll(commands);
    }

    /**
     * Exchanges commands
     * @throws InterruptedException If thread interrupted while exchanging
     */
    private void readResponses() throws InterruptedException, IOException {
        try
        {
            while (true) // keep reading until .readLine throws a timeout
            {
                String response = receiveMsg.readLine();
                responseList.add(response);
            }
        }
        catch (SocketTimeoutException waitTooLong)
        {
            // means no more messages to read, so just go on with your life
        }
    }

    /**
     * Gets an ArrayList of parameters of a task with given taskId. If task doesn't exist, returns empty ArrayList
     * @param taskId ID of task to get
     * @return ArrayList of hostId, taskId, priority, status, result if given tasks exists, empty ArrayList otherwise
     */
    public ArrayList<String> getSerializationArray(int taskId)
    {
        for (RemoteHostTaskDummy dummy : thisIsYourSerializationNow)
        {
            if (dummy.taskId == taskId) return dummy.getArray();
        }
        return new ArrayList<>();
    }

    /**
     * Gets an ArrayList of parameters of all tasks
     * @return ArrayList of ArrayLists each with parameters hostId, taskId, priority, status, result
     */
    public ArrayList<ArrayList<String>> getSerializationArrayAll()
    {
        for (RemoteHostTaskDummy dummy : thisIsYourSerializationNow)
        {
            //System.out.print(dummy.getArray());
        }
        ArrayList<ArrayList<String>> mainArray = new ArrayList<>();
        //System.out.println("GET ALL SERIALIZATION " + thisIsYourSerializationNow);
        for (RemoteHostTaskDummy dummy : thisIsYourSerializationNow)
        {
            //System.out.print("ASDSADSAD " + dummy.getArray());
            mainArray.add(dummy.getArray());
        }
        //System.out.println("MAIN ARRAY " + mainArray);
        return mainArray;
    }


    /**
     * Main working environment of the server communication thread
     * @return String of I don't even know what frankly
     * @throws Exception Way too much stuff
     */
    @Override
    public String call() throws Exception {
        // get the ID of host this thread is talking with
        //talksWith = Integer.parseInt(receiveMsg.readLine());
        //System.out.println(talksWith);

        boolean keepAlive = true;
        while (keepAlive)
        {

            //System.out.println("SERVERCOM THREAD CHECKIN " + talksWith);
            try
            {
                readResponses();
            }
            catch (IOException io) // if trying to read from a closed socket
            {
                System.out.println("Host ID " + talksWith + " socket reading error PING ACK " + pingDelay);
                Thread.sleep(1000); // just so it emulates the normal time of 'operations'
            }
            //System.out.println("PING ACK " + pingDelay);
            //System.out.println(responseList);

            //System.out.println( responsesListIndex + " / " + responseList.size());
            while (responsesListIndex < responseList.size())
            {
                String response = responseList.get(responsesListIndex);
                responsesListIndex++;
                //System.out.println("SERVER PARSING RESPONSE " + response);
                String rawArray;
                boolean componentAlreadyExists;
                RemoteHostTaskDummy component;
                switch (response)
                {
                    case "RESPONSE_GET_TASK":
                        rawArray = responseList.get(responsesListIndex);
                        responsesListIndex++;
                        //System.out.println(rawArray);
                        rawArray = rawArray.replace("[", "");
                        rawArray = rawArray.replace("]", "");
                        //System.out.println(rawArray);
                        String[] rawSplit = rawArray.split(", ");
                        ArrayList<String> components = new ArrayList<>(List.of(rawSplit));
                        //System.out.println(components);

                        // search for if given task was already read, if yes, then update it, if no then add it
                        componentAlreadyExists = false;
                        component = null;
                        for (RemoteHostTaskDummy dummy : thisIsYourSerializationNow)
                        {
                            //System.out.println("COMPARE " + dummy.taskId + " TO " + components.get(0));
                            if (dummy.taskId == Integer.parseInt(components.get(0)))
                            {
                                componentAlreadyExists = true;
                                component = dummy;
                                break;
                            }
                        }
                        if (componentAlreadyExists)
                        {
                            thisIsYourSerializationNow.set(thisIsYourSerializationNow.indexOf(component), new RemoteHostTaskDummy(components, talksWith));
                        }
                        else
                        {
                            thisIsYourSerializationNow.add(new RemoteHostTaskDummy(components, talksWith));
                        }
                        //System.out.println("AFTER SERIALIZATION " + thisIsYourSerializationNow);
                        for (RemoteHostTaskDummy dummy : thisIsYourSerializationNow)
                        {
                            //System.out.println("SERIALIZATION " + dummy.getArray());
                        }
                        break;
                    case "RESPONSE_GET_ALL_TASKS":
                        rawArray = responseList.get(responsesListIndex);
                        responsesListIndex++;
                        rawArray = rawArray.replace("[", "");
                        rawArray = rawArray.replace("]", "");
                        String[] rawArraySplit = rawArray.split(", ");
                        //System.out.println(Arrays.toString(rawArraySplit));
                        int counter = 0;
                        ArrayList<String> rawArrayTemp = new ArrayList<>();
                        for (int i=0; i<rawArraySplit.length; i++)
                        {
                            //System.out.println(rawArraySplit[i%4]);
                            rawArrayTemp.add(rawArraySplit[i]);
                            //System.out.println(counter + "\t" + rawArraySplit[i%4]);
                            // search for if given task was already read, if yes, then update it, if no then add it
                            counter++;
                            if (counter == 6)
                            {
                                componentAlreadyExists = false;
                                component = null;
                                //System.out.println(rawArrayTemp);
                                for (RemoteHostTaskDummy dummy : thisIsYourSerializationNow)
                                {
                                    //System.out.println("COMPARE " + dummy.taskId + " TO " + rawArrayTemp.get(0));
                                    if (dummy.taskId == Integer.parseInt(rawArrayTemp.get(0)))
                                    {
                                        componentAlreadyExists = true;
                                        component = dummy;
                                        break;
                                    }
                                }
                                if (componentAlreadyExists)
                                {
                                    thisIsYourSerializationNow.set(thisIsYourSerializationNow.indexOf(component), new RemoteHostTaskDummy(rawArrayTemp, talksWith));
                                }
                                else
                                {
                                    thisIsYourSerializationNow.add(new RemoteHostTaskDummy(rawArrayTemp, talksWith));
                                }
                                counter = 0;
                                //System.out.println("RAW ARRAY TEMP " + rawArrayTemp);
                                rawArrayTemp = new ArrayList<>();

                                for (RemoteHostTaskDummy dummy : thisIsYourSerializationNow)
                                {
                                    //System.out.print("OFPDOPFG{FGOP{" + dummy.getArray());
                                }
                            }

                        }
                        break;
                    case "PING":
                        pingAck = true;
                        pingDelay = 0;
                        break;
                    case "HOST_EXITING":
                        keepAlive = false;
                        break;
                }

            }

            if (!keepAlive) break;

            while (commandsListIndex < commandsList.size())
            {
                //System.out.println("read command " + commandsList.get(commandsListIndex));
                if (commandsList.get(commandsListIndex).equals("PING"))
                {
                    pingAck = false;
                }
                sendMsg.println(commandsList.get(commandsListIndex));
                commandsListIndex++;
            }


            if (!pingAck) pingDelay++;
            if (pingDelay >= 10)
            {
                assumeDead = true;
                keepAlive = false;
                System.out.println("HOST " + talksWith + " is dead");
            }
        }
        System.out.println("COMTHREADEND");
        assumeDead = true;
        return null;
    }
}

/**
 * The things I do to avoid serialization. "Why?" you might be asking? If I knew "why" then I wouldn't be doing this. It's broken and that's all I can tell you.
 * This abomination holds all the data about a task that the server cares about putting into the database
 */
class RemoteHostTaskDummy
{
    /**
     * Internal values that will be put into the database
     */
    public int hostId, taskId, priority;
    /**
     * Internal values that will be put into the database
     */
    public String status, result;
    /**
     * Internal values that will be put into the database
     */
    public long serverTimeTaken, clientTimeTaken;

    /**
     * Constructor for this class that exists as Serialization pre-alpha version. IDK what I'm even saying anymore, just end this misery already
     * @param components ArrayList of String with values to be put into the database, in order: taskId, priority, status, result
     * @param hostId ID of the host that the values come from
     */
    public RemoteHostTaskDummy(ArrayList<String> components, int hostId)
    {
        this.hostId = hostId;
        this.taskId = Integer.parseInt(components.get(0));
        this.priority = Integer.parseInt(components.get(1));
        this.status = components.get(2);
        this.result = components.get(3);
        this.serverTimeTaken = Long.parseLong(components.get(4));
        this.clientTimeTaken = Long.parseLong(components.get(5));
    }

    /**
     * Returns all the internal values as an ArrayList
     * @return ArrayList of String containing all the internal values, in order: hostId, taskId, priority, status, result
     */
    public ArrayList<String> getArray()
    {
        ArrayList<String> components = new ArrayList<>();
        components.add(String.valueOf(hostId));
        components.add(String.valueOf(taskId));
        components.add(String.valueOf(priority));
        components.add(status);
        components.add(result);
        components.add(String.valueOf(serverTimeTaken));
        components.add(String.valueOf(clientTimeTaken));

        return components;
    }
}

