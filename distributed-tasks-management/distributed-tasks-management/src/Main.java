import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        ExecutorService exec = Executors.newCachedThreadPool();
        ServerThread serverThread = new ServerThread();
        Future<String> future = exec.submit(serverThread);
        //int id = serverThread.getRemoteHost().indexOf(serverThread.getRemoteHost().getFirst());
        //serverThread.sendCommandTo(0, "EXIT_THREAD");
        //
        Thread.sleep(600500);
        System.out.println("\n\n\n\n\n\nShutting down server");
        serverThread.keepAlive = false;
        exec.shutdown();
    }
}