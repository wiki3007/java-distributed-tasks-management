import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
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
    ArrayList<String> commandsList = new ArrayList<>();
    /**
     * Index of next-to-send command
     */
    int commandsListIndex = 0;
    /**
     * ID of the remote host that this thread communicates with
     */
    int talksWith = -1;
    ServerComThread(Socket socket) throws IOException {
        this.socket = socket;
        receiveMsg = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        sendMsg = new PrintWriter(socket.getOutputStream(), true);
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
        commandsList.addAll(commands);
    }

    @Override
    public String call() throws Exception {
        // get the ID of host this thread is talking with
        talksWith = Integer.parseInt(receiveMsg.readLine());

        boolean keepAlive = true;
        while (keepAlive)
        {

            while (commandsListIndex < commandsList.size())
            {
                sendMsg.println(commandsList.get(commandsListIndex));
                if (commandsList.get(commandsListIndex).equals("EXIT_THREAD"))
                {
                    keepAlive = false;
                    break;
                }
                commandsListIndex++;
            }
        }
        return null;
    }
}
