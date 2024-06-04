import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientsListView {
    private ServerThread serverThread;
    private TableView clientsTable = new TableView<>();
    private ObservableList<ObservableList> data = FXCollections.observableArrayList();
    private Integer selectedHostId;
    private boolean chooseInterruptIfRunning = false;

    public ClientsListView(ServerThread serverThread, Stage window){
        this.serverThread = serverThread;
    }

    public Parent getView() throws SQLException {
        GridPane layout = new GridPane();
        this.createTable();
        TextField priorityField = new TextField();

        Thread refreshTable = new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    try {
                        this.createTable();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        refreshTable.setDaemon(true);
        refreshTable.start();

        HBox downButtonGroup = new HBox();
        HBox upButtonGroup = new HBox();

        Button createHostButton = new Button("Create new host");
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
            }
        });

        Button endHostButton = new Button("End host");
        endHostButton.setOnAction((event) -> {
            String temp = clientsTable.getSelectionModel().getSelectedItem().toString();
            temp = temp.replaceAll("\\[", "").replaceAll("\\]","");
            this.selectedHostId = Integer.valueOf(temp);
            serverThread.cancelHost(this.selectedHostId, this.chooseInterruptIfRunning);
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
            String temp = clientsTable.getSelectionModel().getSelectedItem().toString();
            int priority;
            temp = temp.replaceAll("\\[", "").replaceAll("\\]","");
            this.selectedHostId = Integer.valueOf(temp);
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

        upButtonGroup.getChildren().addAll(createHostButton, endHostButton, chooseIfRunningText, chooseIfRunning, refreshButton);
        //downButtonGroup.getChildren().addAll(startTaskButton, cancelTaskButton, changeTaskPriorityButton, priorityText, priorityField);
        downButtonGroup.getChildren().addAll(startTaskButton, priorityText, priorityField);

        upButtonGroup.setSpacing(10);
        downButtonGroup.setSpacing(10);

        layout.setAlignment(Pos.CENTER);
        layout.setVgap(10);
        layout.setHgap(10);
        layout.setPadding(new Insets(10, 10, 10, 10));
        Label title = new Label("Clients list");
        title.setFont(Font.font("Arial", 20));

        layout.add(title, 0, 0);
        layout.add(upButtonGroup, 0, 1);
        layout.add(clientsTable, 0, 2);
        layout.add(downButtonGroup, 0, 3);

        return layout;
    }

    private void createTable() throws SQLException {
        ResultSet rs = serverThread.searchDatabase("SELECT DISTINCT hostId FROM tasks WHERE status != 'Done'");
        clientsTable.getItems().clear();
        clientsTable.getColumns().clear();

        for(int i = 0; i< rs.getMetaData().getColumnCount(); i++){
            //We are using non property style for making dynamic table
            final int j = i;
            TableColumn col = new TableColumn(rs.getMetaData().getColumnName(i+1));
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList,String>,ObservableValue<String>>(){
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });

            clientsTable.getColumns().addAll(col);
        }

        while(rs.next()){
            //Iterate Row
            ObservableList<String> row = FXCollections.observableArrayList();
            for(int i = 1; i<= rs.getMetaData().getColumnCount(); i++){
                //Iterate Column
                row.add(rs.getString(i));
            }
            data.add(row);
        }
        clientsTable.setItems(data);
    }
}
