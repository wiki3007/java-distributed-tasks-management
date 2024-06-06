import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientGUI extends Application {
    private RemoteHostMasterThread hostThread = null;
    private ExecutorService exec;

    public void start(Stage stage){
        GridPane layout = new GridPane();
        Label ipAddressLabel = new Label("IP Address:");
        TextField ipAddressField = new TextField();
        Label portLabel = new Label("Port:");
        TextField portField = new TextField();
        Button connectionButton = new Button("Connect");
        Label infoLabel = new Label();

        connectionButton.setOnAction((event) ->{
            String address = ipAddressField.getText();
            String portText = portField.getText();
            int port;

            if(address.isBlank()){
                infoLabel.setText("Empty field for IP address!");
                return;
            }

            if(portText.isBlank()){
                infoLabel.setText("Empty field for IP address!");
                return;
            }

            port = Integer.parseInt(portText.strip());

            infoLabel.setText("Connecting...");

            try {
                InetAddress serverAddress = InetAddress.getByName(address);
                this.hostThread = new RemoteHostMasterThread(serverAddress, port);
            } catch (UnknownHostException e) {
                infoLabel.setText("This socket doesn't exists!");
                throw new RuntimeException(e);
            } catch (IOException e) {
                infoLabel.setText("Something went wrong...");
                throw new RuntimeException(e);
            }

            this.exec = Executors.newCachedThreadPool();
            exec.submit(hostThread);
            infoLabel.setText("Connected!");
        });

        layout.add(ipAddressLabel, 0, 1);
        layout.add(ipAddressField, 0, 2);
        layout.add(portLabel, 0, 3);
        layout.add(portField, 0, 4);
        layout.add(connectionButton, 0, 5);
        layout.add(infoLabel, 0, 6);
        layout.setAlignment(Pos.CENTER);
        layout.setVgap(10);
        layout.setHgap(10);
        layout.setPadding(new Insets(10, 10, 10, 10));

        Scene scene = new Scene(layout, 540, 320);

        stage.setTitle("Distrubuted Tasks Management - Client");
        stage.setScene(scene);
        stage.show();
    }

    public void stop(){
        this.hostThread.exec.shutdownNow();
        this.exec.shutdownNow();
        System.out.println("Closing...");
        Platform.exit();
    }

    public static void main(String[] args){
        launch(ClientGUI.class);
    }
}
