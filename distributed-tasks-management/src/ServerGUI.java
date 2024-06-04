import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ServerGUI extends Application {
    private ServerThread serverThread;
    private ExecutorService exec;

    @Override
    public void start(Stage stage) throws SQLException, IOException {
        ExecutorService exec = Executors.newCachedThreadPool();
        this.exec = exec;
        ServerThread serverThread = new ServerThread();
        this.serverThread = serverThread;
        Future<String> future = exec.submit(serverThread);

        ToolBar toolBar = new ToolBar();
        BorderPane borderPane = new BorderPane();

        ClientsListView clientsListView = new ClientsListView(serverThread, stage);
        DoneTasksListView doneTasksListView = new DoneTasksListView(serverThread);

        Button clientsListButton = new Button("Clients list");
        Button doneTasksButton = new Button("Done tasks list");
        clientsListButton.setOnAction((event) -> {
            try {
                borderPane.setCenter(clientsListView.getView());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        doneTasksButton.setOnAction((event) -> {
            try {
                borderPane.setCenter(doneTasksListView.getView());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        toolBar.getItems().addAll(clientsListButton, doneTasksButton);

        borderPane.setTop(toolBar);
        borderPane.setCenter(clientsListView.getView());

        Scene scene = new Scene(borderPane, 720, 540);

        stage.setTitle("Distrubuted tasks management - Server");
        stage.setScene(scene);
        stage.show();
    }

    public void stop(){
        serverThread.keepAlive = false;
        exec.shutdown();
    }

    public static void main(String[] args) throws SQLException, IOException {
        launch(ServerGUI.class);
    }
}
