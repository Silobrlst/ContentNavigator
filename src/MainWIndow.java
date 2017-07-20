import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class MainWIndow extends Application {

    @Override
    public void start(Stage primaryStageIn) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        loader.load();
        MainWindowController mainWindowController = loader.getController();
        mainWindowController.setTagsStage(primaryStageIn, loader);
        mainWindowController.setStyle("file:styles/modena_dark.css");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
