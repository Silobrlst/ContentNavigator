package tagfilenav;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class HtmlWindow {
    @FXML private TextField url;
    @FXML private WebView html;
    @FXML private ProgressBar loadProgress;

    private Stage stage;
    private StyledGuiSaver savableStyledGui;

    @FXML
    public void initialize() {}

    public HtmlWindow(){}

    public void init(FXMLLoader loaderIn){
        String windowName = "htmlWindow";

        Scene scene = new Scene(loaderIn.getRoot());
        scene.getRoot().setId(windowName + "Root");
        stage = new Stage();
        stage.setTitle("HTML description");
        stage.setScene(scene);
        stage.setOnShown(event -> onShown());
        stage.setOnHidden(event -> savableStyledGui.save());
        stage.setOnCloseRequest(event -> savableStyledGui.save());
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.hide();
            }
        });

        WebEngine webEngine = html.getEngine();

        url.textProperty().bind(webEngine.locationProperty());

        loadProgress.progressProperty().bind(webEngine.getLoadWorker().progressProperty());

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                loadProgress.setVisible(false);
            }
        });

        savableStyledGui = new StyledGuiSaver(windowName, stage);
        savableStyledGui.load();
    }

    private void onShown(){
    }

    void setStyle(String styleFileNameIn){
        savableStyledGui.setStyle(styleFileNameIn);
    }

    void openInternetLink(String urlIn){
        stage.setTitle("HTML description");
        html.getEngine().load(urlIn);
        stage.show();
    }

    void openText(String textIn){
        stage.setTitle("Description");
        html.getEngine().loadContent(textIn);
        stage.show();
    }

    void close(){
        stage.hide();
    }
}
