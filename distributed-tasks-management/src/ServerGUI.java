import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ServerGUI extends Application {

    @Override
    public void start(Stage stage){
        ToolBar toolBar = new ToolBar();
        BorderPane borderPane = new BorderPane();

        ClientsListView clientsListView = new ClientsListView();
        DoneTasksListView doneTasksListView = new DoneTasksListView();

        Button clientsListButton = new Button("Clients list");
        Button doneTasksButton = new Button("Done tasks list");
        clientsListButton.setOnAction((event) -> borderPane.setCenter(clientsListView.getView()));
        doneTasksButton.setOnAction((event) -> borderPane.setCenter(doneTasksListView.getView()));
        toolBar.getItems().addAll(clientsListButton, doneTasksButton);

        borderPane.setTop(toolBar);
        borderPane.setCenter(clientsListView.getView());

        Scene scene = new Scene(borderPane, 720, 540);

        stage.setTitle("Distrubuted tasks management - Server");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args){
        launch(ServerGUI.class);
    }
}
