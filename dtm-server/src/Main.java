import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws SQLException, IOException {
        ServerThread serverThread = new ServerThread();
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.submit(serverThread);
    }
}