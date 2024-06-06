import javax.swing.plaf.synth.SynthUI;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerAwait implements Callable<ServerComThread> {
    static int id = 0;

    ServerComThread tempServerCom = null;
    boolean addedNew = false;

    ServerSocket serverSocket;
    public String status = "Created";
    public ServerAwait(ServerSocket serverSocket)
    {
        this.serverSocket = serverSocket;
    }

    @Override
    public ServerComThread call() throws Exception {
        Socket socket = null;
        ExecutorService exec = null;
        try
        {
            exec = Executors.newFixedThreadPool(1);
            status = "Awaiting";
            socket = serverSocket.accept();
            status = "Working";
            System.out.println("New connection made");
            socket.setSoTimeout(1000);
            tempServerCom = new ServerComThread(socket, id);
            exec.submit(tempServerCom);
            addedNew = true;
            System.out.println("ServerThread added new host " + id);
            id++;
        }
        catch (SocketTimeoutException noNewConnections)
        {
            status = "NoNewConnections";
            //System.out.println("No new connections made\nPlease connect to: " + serverSocket.getInetAddress());
            return null;
        }
        finally {
            assert exec != null;
            exec.shutdownNow();
        }
        status = "Done";
        return tempServerCom;
    }
}
