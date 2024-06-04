// NOTE: get rid of all the sendCommandTo functions in the main loop once the GUI has a manual option send them out
// or don't, do whatever
// Also starting too many new tasks at once maxes out the CPU. No, I will not optimize it
// Sometimes multiple tasks of same host will have the "Working" status in the database. They just got caught in the middle of being updated when their data was sent out.

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Main server thread, mainframe of the entire program
 */
public class ServerThread implements Callable<String> {
    /**
     * Array of remote hosts that have ever registered under the server
     */
    private ArrayList<RemoteHostMasterThread> remoteHost = new ArrayList<>();
    /**
     * Array of remote hosts converted to Future class by ExecutorService
     */
    private ArrayList<Future<String>> remoteHostFuture = new ArrayList<>();
    /**
     * Array of server threads responsible for communicating with remote hosts over the network
     */
    private ArrayList<ServerComThread> serverCom = new ArrayList<>();
    /**
     * Array of communications server threads converted to Future class by ExecutorService
     */
    private ArrayList<Future<String>> serverComFuture = new ArrayList<>();
    /**
     * Port for receiving network communications
     */
    private int port = 51707;
    /**
     * Server Socket to handle network communications
     */
    private ServerSocket serverSocket = new ServerSocket(port);
    /**
     * Server address
     */
    InetAddress serverAddress = InetAddress.getLocalHost();
    /**
     * ExecutorService with a cached thread pool
     */
    private ExecutorService exec = Executors.newCachedThreadPool();
    /**
     * Keep server alive, change this to false if you want the server to quit
     */
    public boolean keepAlive = true;

    /**
     * URL to the database, just assume it runs local to the server on XAMPP, IDC
     */
    private String url = "jdbc:Mysql://127.0.0.1:3306";
    /**
     * Database login
     */
    private String login = "root";
    /**
     * Database password
     */
    private String password = ""; // people cannot steal your plaintext if you just so no. What are criminals going to do? Break the law? That's illegal.
    /**
     * Database name
     */
    private String dbname = "distributed_tasks_management";
    /**
     * Database connector
     */
    private Connection connection = DriverManager.getConnection(url, login, password);
    /**
     * Main database thingy to use commands from
     */
    public Statement statement = connection.createStatement();
    /**
     * Second main database command thingy
     */
    PreparedStatement preparedStatement;

    /**
     * ID to be used for identification of ServerComThread and RemoteHostMasterThread, same value for both if they're the ones talking with each other
     */
    static int id = 0;

    public ServerThread() throws IOException, SQLException {
    }

    /**
     * Adds and immediately starts a new remote host
     */
    public void addNewRemoteHost() throws IOException {
        RemoteHostMasterThread tempThread = new RemoteHostMasterThread(serverAddress, port, id);
        remoteHost.add(tempThread);
        remoteHostFuture.add(exec.submit(remoteHost.getLast()));

        Socket socket = serverSocket.accept();
        socket.setSoTimeout(1000);
        ServerComThread tempServerCom = new ServerComThread(socket, id);
        serverCom.add(tempServerCom);
        serverComFuture.add(exec.submit(tempServerCom));
        System.out.println("ServerThread added new host " + id);
        id++;
    }

    /**
     * Cancels remote host with given ID. Does nothing if the host is already canceled or done
     * @param id ID of remote host to cancel
     * @param interruptIfRunning Whether to cancel host immediately or wait until host ends naturally
     */
    public void cancelHost(int id, boolean interruptIfRunning)
    {
        if (id < remoteHost.size())
        {
            Future<String> host = remoteHostFuture.get(id);
            if (host.isCancelled()) return;
            else if (host.isDone()) return;
            host.cancel(interruptIfRunning);
        }
    }

    /**
     * Remote host getter
     * @return ArrayList of remote hosts
     */
    public ArrayList<RemoteHostMasterThread> getRemoteHost()
    {
        return remoteHost;
    }

    /**
     * Remote host Future getter
     * @return ArrayList of Future class remote hosts
     */
    public ArrayList<Future<String>> getRemoteHostFuture()
    {
        return remoteHostFuture;
    }

    /**
     * Sends given command to Server Communication Thread responsible for remote host of given ID
     * @param hostId ID of remote host to give command to
     * @param command Command to give
     */
    public void sendCommandTo(int hostId, String command)
    {
        //System.out.println("sendCommand");
        /*
        if (serverCom.get(hostId).talksWith == hostId) // if ServerComThread and remote host have matching IDs
        {
            //serverCom.get(hostId).receiveCommands(command); // server gets stuck if you use theses
            serverCom.get(hostId).receiveCommands(command);
        }
        else
        {
            for (ServerComThread com : serverCom)
            {
                if (com.talksWith == hostId)
                {
                    //com.receiveCommands(command);
                    com.receiveCommands(command);
                    break;
                }
            }
        }
        */

        serverCom.get(hostId).receiveCommands(command);
        System.out.println("ServerThread sent command " + command + " to " + hostId);
    }

    @Override
    public String call() throws Exception {
        // DROP previous DB
        execUpdate("DROP DATABASE " + dbname);

        // USE or CREATE database
        if (execUpdate("USE " + dbname) != -1) // if database exists, good
        {
            System.out.println("Database \"" + dbname + "\" successfully selected");
        }
        else // time to create it
        {
            System.out.println("No database \"" + dbname + "\" found, creating...");
            // create db
            if (execUpdate("CREATE DATABASE " + dbname) != -1)
            {
                //System.out.println("Database \""+ dbname + "\" created");
            }
            else
            {
                System.out.println("Error creating database \"" + dbname + "\"");
            }

            // select it
            if (execUpdate("USE " + dbname) != -1)
            {
                //System.out.println("Database \"" + dbname + "\" successfully selected");
            }
            else
            {
                System.out.println("Unrecoverable error selecting database \"" + dbname + "\", contact system administrator");
                return "UNRECOVERABLE DATABASE ERROR";
            }

            // create questions table
            if (execUpdate("CREATE TABLE `tasks` (`hostId` INT(4) NOT NULL, `taskId` INT(4) NOT NULL, `priority` INT(4) NULL, `status` VARCHAR(30) NULL, `result` VARCHAR(200) NULL, `serverExecutionTime` INT(8) NULL, `clientExecutionTime` INT(8) NULL, PRIMARY KEY (`hostId`, `taskId`))") != -1)
            {
                //System.out.println("Table \"Questions\" created");
            }
            else
            {
                System.out.println("Error creating table \"tasks\", contact system administrator");
                //execUpdate(statement, "DROP DATABASE " + dbname);
                return "TABLE CREATION ERROR";
            }
        }

        int delayCounter = 1;
        while (keepAlive)
        {
            if (serverCom.isEmpty())
            {
                /*
                addNewRemoteHost();
                sendCommandTo(0, "HOST_START_TASK");
                sendCommandTo(0, "7");

                 */

                /*
                sendCommandTo(0, "HOST_START_TASK");
                sendCommandTo(0, "8");
                sendCommandTo(0, "HOST_RETURN_TASK");
                sendCommandTo(0, "0");

                 */

                /*
                addNewRemoteHost();
                sendCommandTo(1, "HOST_START_TASK");
                sendCommandTo(1, "1");

                 */

                /*
                sendCommandTo(1, "HOST_START_TASK");
                sendCommandTo(1, "7");
                sendCommandTo(1, "HOST_START_TASK");
                sendCommandTo(1, "5");

                 */


                //sendCommandTo(0, "EXIT_THREAD");
            }

            // can't have sleep here either, else it just instaquits
            // nevermind, it just decides to fix itself. cool. really cool.
            // I don't know why, but this can't be at the beginning or end of the loop, so instead it has to just awkwardly sit in the middle here
            Thread.sleep(2000);
            System.out.println("SERVER MAIN LOOP");
            // uncommenting any of these sets of sendCommandTo maxes out your CPU. Don't start too many at once. Or just too many in general
            //sendCommandTo(serverCom.get(0).talksWith, "HOST_START_TASK");
            //sendCommandTo(serverCom.get(0).talksWith, String.valueOf(delayCounter));
            //delayCounter++;
            //sendCommandTo(serverCom.get(1).talksWith, "HOST_START_TASK");
            //sendCommandTo(serverCom.get(1).talksWith, String.valueOf(delayCounter));
            //System.out.println("test");
            //addNewRemoteHost();




            // do whatever here send commands or whatnot, or just do it from outside, not like anyone will care



            // update database
            for (ServerComThread com : serverCom)
            {
                sendCommandTo(com.talksWith, "HOST_RETURN_ALL_TASKS");
                ArrayList<ArrayList<String>> taskDummyArray = com.getSerializationArrayAll();
                //System.out.println("TASK DUMMY ARRAY " + taskDummyArray);
                if (taskDummyArray.isEmpty()) break;
                //System.out.println("TASK DUMMY ARRAY " + taskDummyArray);
                for (ArrayList<String> task : taskDummyArray)
                {
                    //System.out.println("SQL_COMMAND " + "SELECT * FROM tasks WHERE hostId = " + task.get(0) + " AND taskId = " + task.get(1));
                    ResultSet set = searchDatabase("SELECT * FROM tasks WHERE hostId = " + task.get(0) + " AND taskId = " + task.get(1));
                    //ResultSet set = searchDatabase("SELECT * FROM tasks");
                    //System.out.println(set);
                    //if (!set.next()) break;
                    int hostId = -1;
                    int taskId = -1;
                    int priority = -1;
                    String status = "";
                    String result = "";
                    long serverTimeTaken = 0;
                    long clientTimeTaken = 0;
                    while (set.next()) // even though there's only one result, this has to be in a while loop, because ???, there's only one result anyway, but it's just weird
                    {
                        hostId = set.getInt("hostId");
                        taskId = set.getInt("taskId");
                        priority = set.getInt("priority");
                        status = set.getString("status");
                        result = set.getString("result");
                        serverTimeTaken = set.getLong("serverExecutionTime");
                        clientTimeTaken = set.getLong("clientExecutionTime");
                        //System.out.println(hostId + " " + taskId + " " + priority + " " + status + " " + result);
                    }

                    if (hostId != Integer.parseInt(task.get(0)) && taskId != Integer.parseInt(task.get(1))) // if no conflict with primary keys
                    {
                        //System.out.println("TASK " + taskId + " IS INSERT");
                        if (execUpdate("INSERT INTO `tasks` VALUES(" + task.get(0) + ", " +  task.get(1) + ", " + task.get(2) + ", \"" + task.get(3) +  "\", \"" + task.get(4) + "\", " + task.get(5) + ", " + task.get(6) + ")") != -1);
                        {
                            //System.out.println("RECORD " + hostId + " " + taskId + " " + priority + " " + status + " " + result + " INSERTED");
                        }
                    }
                    else if (priority != Integer.parseInt(task.get(2)) || !status.equals(task.get(3)) || !result.equals(task.get(4))) // if row is different, then update it, executionTimes only change when taks is done, so ignore it in the checks
                    {
                        if (execUpdate("UPDATE `tasks` SET priority = " + task.get(2) + ", status = \"" + task.get(3) + "\", result = \"" + task.get(4) + "\", serverExecutionTime = " + task.get(5) + ", clientExecutionTime = " + task.get(6) + " WHERE hostId = " +  task.get(0) + " AND taskId = " + task.get(1)) != -1)
                        {
                            //System.out.println("RECORD " + hostId + " " + taskId + " " + priority + " " + status + " " + result + " UPDATED");
                        }
                    }
                }
                //keepAlive = false;
            }
            //Thread.currentThread().wait(100); // this and sleep make somehow permanently brick the thread. I'm tired of this, so enjoy the spam if you ever put a print in here
            //break;
        }
        for (ServerComThread com : serverCom)
        {
            sendCommandTo(com.talksWith, "EXIT_THREAD");
            sendCommandTo(com.talksWith, "HOST_EXIT_THREAD");
        }
        System.out.println("SERVERTHREADEND");
        exec.shutdownNow();
        return null;
    }

    /**
     * Executes command on database
     * @param command Command to execute
     * @return Integer that gets returned as result of statement.executeUpdate, or -1 if error occurs
     * @throws SQLException If database connection breaks
     */
    public int execUpdate(String command) throws SQLException {
        Statement statement = connection.createStatement();
        try
        {
            int returnCode = statement.executeUpdate(command);
            //System.out.println(returnCode);
            return returnCode;
        }
        catch (SQLException sqlException)
        {
            System.out.println("Command \"" + command + "\" failed\t" + sqlException.getMessage() + ": " + sqlException.getErrorCode());
        }
        finally {
            statement.close();
        }
        return -1;
    }

    /**
     * Executes INSERT, UPDATE or DELETE query on the database
     * @param command Command to execute
     * @return ResultSet that gets returned as a result of statement.executeQuery
     * @throws SQLException If database connection breaks
     */
    public ResultSet execQuery(String command) throws SQLException {
        Statement statement = connection.createStatement();
        try
        {
            ResultSet returnSet = statement.executeQuery(command);
            //System.out.println(returnCode);
            return returnSet;
        }
        catch (SQLException sqlException)
        {
            System.out.println("Command \"" + command + "\" failed\t" + sqlException.getMessage() + ": " + sqlException.getErrorCode());
        }
        finally {
            statement.close();
        }
        return statement.executeQuery("");
    }

    public ResultSet searchDatabase(String command) throws SQLException {
        preparedStatement = connection.prepareStatement(command);
        try
        {
            ResultSet returnSet = preparedStatement.executeQuery();
            return returnSet;
        }
        catch (SQLException sqlException)
        {
            System.out.println("Command \"" + command + "\" failed\t" + sqlException.getMessage() + ": " + sqlException.getErrorCode());
        }
        finally {
            //preparedStatement.close();
        }
        return (preparedStatement = connection.prepareStatement("")).executeQuery();
    }
}
