import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class DoneTasksListView {
    public Parent getView(){
        GridPane layout = new GridPane();

        TableView doneTasksTable = new TableView();

        TableColumn<String, String> column1 = new TableColumn<>("Column 1");
        column1.setCellValueFactory(new PropertyValueFactory<>("column1"));

        TableColumn<String, String> column2 = new TableColumn<>("Column 2");
        column2.setCellValueFactory(new PropertyValueFactory<>("column2"));

        doneTasksTable.getColumns().addAll(column1, column2);
        doneTasksTable.getItems().addAll("aaa", "bbb");

        HBox buttonGroups = new HBox();

        Button button1 = new Button("Button Task 1");
        Button button2 = new Button("Button Task 2");
        Button button3 = new Button("Button Task 3");

        buttonGroups.getChildren().addAll(button1, button2, button3);
        buttonGroups.setSpacing(10);

        layout.setAlignment(Pos.CENTER);
        layout.setVgap(10);
        layout.setHgap(10);
        layout.setPadding(new Insets(10, 10, 10, 10));

        layout.add(new Label("Done tasks list"), 0, 0);
        layout.add(doneTasksTable, 0, 1);
        layout.add(buttonGroups, 0, 2);

        return layout;
    }
}