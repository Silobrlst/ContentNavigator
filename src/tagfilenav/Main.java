package tagfilenav;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import tagfilenav.MainWindow;

public class Main extends Application {

    @Override
    public void start(Stage primaryStageIn) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        loader.load();
        MainWindow mainWindow = loader.getController();
        mainWindow.init(primaryStageIn, loader);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
