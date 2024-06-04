import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.util.Callback;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DoneTasksListView {
    private ServerThread serverThread;
    private BorderPane borderPane;
    private TableView doneTasksTable = new TableView<>();
    private ObservableList<ObservableList> data = FXCollections.observableArrayList();

    public DoneTasksListView(ServerThread serverThread, BorderPane borderPane){
        this.serverThread = serverThread;
        this.borderPane = borderPane;
    }

    public Parent getView() throws SQLException {
        GridPane layout = new GridPane();
        ChartView chartView = new ChartView(this.serverThread, this.borderPane, this);

        TableView doneTasksTable = new TableView();

        this.createTable();

        HBox buttonGroups = new HBox();

        Button ganttButton = new Button("Chart of tasks time execution");
        ganttButton.setOnAction((event) ->{
            this.borderPane.setCenter(chartView.getView());
        });
        Button refreshButton = new Button("Refresh table");
        refreshButton.setOnAction((event) ->{
            try {
                this.createTable();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

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

        buttonGroups.getChildren().addAll(ganttButton, refreshButton);
        buttonGroups.setSpacing(10);

        layout.setAlignment(Pos.CENTER);
        layout.setVgap(10);
        layout.setHgap(10);
        layout.setPadding(new Insets(10, 10, 10, 10));

        Label title = new Label("All tasks list");
        title.setFont(Font.font("Arial", 20));

        layout.add(title, 0, 0);
        layout.add(this.doneTasksTable, 0, 1);
        layout.add(buttonGroups, 0, 2);

        return layout;
    }

    private void createTable() throws SQLException {
        ResultSet rs = serverThread.searchDatabase("SELECT * FROM tasks");
        doneTasksTable.getItems().clear();
        doneTasksTable.getColumns().clear();

        for(int i = 0; i< rs.getMetaData().getColumnCount(); i++){
            //We are using non property style for making dynamic table
            final int j = i;
            TableColumn col = new TableColumn(rs.getMetaData().getColumnName(i+1));
            col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList,String>, ObservableValue<String>>(){
                public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                    return new SimpleStringProperty(param.getValue().get(j).toString());
                }
            });

            doneTasksTable.getColumns().addAll(col);
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
        doneTasksTable.setItems(data);
    }
}
