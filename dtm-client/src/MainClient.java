import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainClient {
    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        String address;

        // Keep asking the user for server address
        RemoteHostMasterThread hostThread = null;
        do {
            try
            {
                System.out.print("Connect to: ");
                address = in.next();

                InetAddress serverAddress = InetAddress.getByName(address);
                //System.out.println(serverAddress);
                hostThread = new RemoteHostMasterThread(serverAddress, 51707);
                break;
            }
            catch (ConnectException connectionRefused)
            {
                System.out.println("Connection refused, bad address or server is closed");
            }
        } while(true);
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.submit(hostThread);
    }
}