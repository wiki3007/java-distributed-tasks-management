import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChartView {
    private ServerThread serverThread;
    private BorderPane borderPane;
    private DoneTasksListView doneTasksListView;

    public ChartView(ServerThread serverThread, BorderPane borderPane, DoneTasksListView doneTasksListView){
        this.serverThread = serverThread;
        this.borderPane = borderPane;
        this.doneTasksListView = doneTasksListView;
    }

    public Parent getView(){
        GridPane layout = new GridPane();
        HBox buttonGroups = new HBox();

        Button backButton = new Button("Back");
        backButton.setOnAction((event) ->{
            try {
                this.borderPane.setCenter(this.doneTasksListView.getView());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        final NumberAxis xAxis = new NumberAxis();
        final CategoryAxis yAxis = new CategoryAxis();
        final BarChart<Number, String> bc =
                new BarChart<>(xAxis, yAxis);
        bc.setTitle("Chart of tasks time execution");
        xAxis.setLabel("Time of execution [ms]");
        xAxis.setTickLabelRotation(90);
        yAxis.setLabel("Hosts");

        XYChart.Series series = new XYChart.Series();
        series.setName("Amount of time in ms");
        try{
            ResultSet rs = this.serverThread.searchDatabase("SELECT taskId, clientExecutionTime FROM tasks WHERE status = 'Done' ORDER BY clientExecutionTime DESC");
            while(rs.next()){
                series.getData().add(new XYChart.Data(rs.getInt(2), "Task " + rs.getInt(1)));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        bc.getData().add(series);

        buttonGroups.getChildren().addAll(backButton);
        buttonGroups.setSpacing(10);

        layout.setAlignment(Pos.CENTER);
        layout.setVgap(10);
        layout.setHgap(10);
        layout.setPadding(new Insets(10, 10, 10, 10));
        layout.add(backButton, 0, 0);
        layout.add(bc, 0, 1);

        return layout;
    }
}
