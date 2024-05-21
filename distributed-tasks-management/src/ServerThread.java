import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
     * Keep server alive
     */
    public boolean keepAlive = true;

    public ServerThread() throws IOException {
    }

    /**
     * Adds and immediately starts a new remote host
     */
    public void addNewRemoteHost() throws IOException {
        RemoteHostMasterThread tempThread = new RemoteHostMasterThread(serverAddress, port);
        remoteHost.add(tempThread);
        remoteHostFuture.add(exec.submit(remoteHost.getLast()));

        Socket socket = serverSocket.accept();
        socket.setSoTimeout(5000);
        ServerComThread tempServerCom = new ServerComThread(socket);
        serverCom.add(tempServerCom);
        serverComFuture.add(exec.submit(tempServerCom));
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
        if (serverCom.get(hostId).talksWith == hostId) // if ServerComThread and remote host have matching IDs
        {
            serverCom.get(hostId).receiveCommands(command);
        }
        else
        {
            for (ServerComThread com : serverCom)
            {
                if (com.talksWith == hostId)
                {
                    com.receiveCommands(command);
                    break;
                }
            }
        }
    }

    @Override
    public String call() throws Exception {

        while (keepAlive)
        {

        }
        return null;
    }
}
