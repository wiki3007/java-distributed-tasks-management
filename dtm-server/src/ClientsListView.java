import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientsListView {
    private ServerThread serverThread;
    private TableView clientsTable = new TableView<>();
    private ObservableList<ObservableList> data = FXCollections.observableArrayList();
    private Integer selectedHostId;
    private boolean chooseInterruptIfRunning = false;

    public ClientsListView(ServerThread serverThread){
        this.serverThread = serverThread;
    }

    public Parent getView() throws SQLException {
        GridPane layout = new GridPane();
        this.createTable();
        TextField priorityField = new TextField();

        /*
        Thread refreshTable = new Thread(() -> {
            Platform.runLater(() -> {
                try {
                    System.out.println("Thread running");
                    Thread.sleep(5000);
                    this.createTable();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        });

         */

        HBox downButtonGroup = new HBox();
        HBox upButtonGroup = new HBox();


        Button createHostButton = new Button("Create new host");
        /*
        createHostButton.setOnAction((event) -> {
            try {
                int id = ServerThread.id, priority;
                try{
                    priority = Integer.parseInt(priorityField.getText());
                } catch (NumberFormatException e){
                    e.printStackTrace();
                    return;
                }
                serverThread.addNewRemoteHost();
                serverThread.sendCommandTo(id, "HOST_START_TASK");
                serverThread.sendCommandTo(id, String.valueOf(priority));
                this.createTable();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

         */

        Button endHostButton = new Button("End host");
        endHostButton.setOnAction((event) -> {
            ServerComThread temp = (ServerComThread) this.clientsTable.getSelectionModel().getSelectedItem();
            this.serverThread.sendCommandTo(temp, "HOST_EXIT_THREAD");
            try {
                this.createTable();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        Button refreshButton = new Button("Refresh table");
        refreshButton.setOnAction((event) -> {
            try {
                this.createTable();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        Button startTaskButton = new Button("Start new task");
        startTaskButton.setOnAction((event) -> {
            ServerComThread temp = (ServerComThread) this.clientsTable.getSelectionModel().getSelectedItem();
            this.selectedHostId = temp.talksWith;
            int priority;
            if(this.selectedHostId == null){
                return;
            }
            try{
                priority = Integer.parseInt(priorityField.getText());
            } catch (NumberFormatException e){
                e.printStackTrace();
                return;
            }
            serverThread.sendCommandTo(this.selectedHostId, "HOST_START_TASK");
            serverThread.sendCommandTo(this.selectedHostId, String.valueOf(priority));
            try {
                this.createTable();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        Button cancelTaskButton = new Button("Cancel task");
        cancelTaskButton.setOnAction((event) -> {

        });

        Button changeTaskPriorityButton = new Button("Change priority");
        changeTaskPriorityButton.setOnAction((event) -> {

        });

        Label chooseIfRunningText = new Label("Interrupt every running tasks?");
        Label priorityText = new Label("Priority:");
        ChoiceBox chooseIfRunning = new ChoiceBox();
        chooseIfRunning.getItems().add("true");
        chooseIfRunning.getItems().add("false");
        chooseIfRunning.setOnAction((event) -> {
            if(chooseIfRunning.getSelectionModel().getSelectedItem() == "false"){
                this.chooseInterruptIfRunning = false;
            }
            if(chooseIfRunning.getSelectionModel().getSelectedItem() == "true"){
                this.chooseInterruptIfRunning = true;
            }
            System.out.println(this.chooseInterruptIfRunning);
        });

        upButtonGroup.getChildren().addAll(refreshButton);
        //downButtonGroup.getChildren().addAll(startTaskButton, cancelTaskButton, changeTaskPriorityButton, priorityText, priorityField);
        downButtonGroup.getChildren().addAll(startTaskButton, priorityText, priorityField, endHostButton);

        upButtonGroup.setSpacing(10);
        downButtonGroup.setSpacing(10);

        layout.setAlignment(Pos.CENTER);
        layout.setVgap(10);
        layout.setHgap(10);
        layout.setPadding(new Insets(10, 10, 10, 10));
        Label title = new Label("Clients list");
        Label serverAddress = new Label("Server IP address: " + this.serverThread.serverAddress.getHostAddress());
        Label portText = new Label("Port: " + this.serverThread.port);
        title.setFont(Font.font("Arial", 20));
        serverAddress.setFont(Font.font("Arial", 14));
        portText.setFont(Font.font("Arial", 14));

        layout.add(title, 0, 0);
        layout.add(serverAddress, 1, 0);
        layout.add(portText, 2, 0);
        layout.add(upButtonGroup, 0, 1);
        layout.add(clientsTable, 0, 2);
        layout.add(downButtonGroup, 0, 3);

        return layout;
    }

    private void createTable() throws SQLException {
        clientsTable.getItems().clear();
        clientsTable.getColumns().clear();

        TableColumn<ServerComThread, Integer> hostColumn = new TableColumn<>("Host ID");
        hostColumn.setCellValueFactory(
                p -> new SimpleIntegerProperty(p.getValue().talksWith).asObject()
        );
        this.clientsTable.getColumns().add(hostColumn);

        if(this.serverThread.serverCom.isEmpty()){
            return;
        }

        for(ServerComThread host : this.serverThread.serverCom){
            if(!host.assumeDead){
                this.clientsTable.getItems().add(host);
            }
        }
    }
}
