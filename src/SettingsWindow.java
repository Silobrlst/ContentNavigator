import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;


public class SettingsWindow extends Stage {
    SettingsWindow()throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("SettingsWindow.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add("modena_dark.css");

        ((Button)scene.lookup("#cancel")).setOnAction(event -> onCancel());

        ((Button)scene.lookup("#openAppFolder")).setOnAction(event -> {
            if( Desktop.isDesktopSupported()){
                new Thread(() -> {
                    try {
                        System.out.println(new File(".").getAbsolutePath());
                        Desktop.getDesktop().open(new File(new File(".").getAbsolutePath()));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });

        this.setScene(scene);
    }

    private void onCancel() {
        // add your code here if necessary
        this.hide();
    }
}
