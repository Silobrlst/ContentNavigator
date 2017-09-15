package tagfilenav;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;

public class SettingsDialog {
    @FXML private Button cancel;
    @FXML private Button ok;
    @FXML private Button apply;
    @FXML private Button openAppFolder;
    @FXML private TextField openInFolderCommand;
    @FXML private TextField openInFolderArgument;
    @FXML private ComboBox<String> style;

    private Stage stage;
    private StyledGuiSaver savableStyledGui;

    private MainWindow mainWindow;
    private SettingsDialogInterface settingsDialogInterface;

    @FXML public void initialize() {
    }

    public SettingsDialog(){}

    public void init(Stage parentStageIn, FXMLLoader loaderIn, MainWindow mainWindowIn, SettingsDialogInterface settingsDialogInterfaceIn){
        mainWindow = mainWindowIn;
        settingsDialogInterface = settingsDialogInterfaceIn;

        String windowName = "settingsDialog";

        Scene scene = new Scene(loaderIn.getRoot());
        scene.getRoot().setId(windowName + "Root");
        stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(parentStageIn);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnHidden(event -> savableStyledGui.save());
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                onCancel();
            }
        });

        cancel.setOnAction(event -> onCancel());
        ok.setOnAction(event -> onOk());
        apply.setOnAction(event -> settingsDialogInterface.applied());

        openAppFolder.setOnAction(event -> {
            if( Desktop.isDesktopSupported()){
                new Thread(() -> {
                    try {
                        Desktop.getDesktop().open(new File(new File(".").getAbsolutePath()));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });

        savableStyledGui = new StyledGuiSaver(windowName, stage);
        savableStyledGui.load();
    }

    //<set>=============================================================================================================
    private void onCancel() {
        stage.hide();
    }

    private void onOk(){
        settingsDialogInterface.applied();
        stage.hide();
    }
    //</set>============================================================================================================

    void setStyle(String styleFileNameIn) {
        savableStyledGui.setStyle(styleFileNameIn);
    }

    //<get>=============================================================================================================
    String getOpenInFolderCommand(){
        return openInFolderCommand.getText();
    }
    String getOpenInFolderArgument(){
        return openInFolderArgument.getText();
    }
    String getSelectedStyle(){
        String selectedStyle = style.getSelectionModel().getSelectedItem();

        if(selectedStyle.equals("default")){
            return "default";
        }

        return "file:styles/" + style.getSelectionModel().getSelectedItem();
    }
    //</get>============================================================================================================

    void open() {
        File folder = new File("styles");
        File[] listOfFiles = folder.listFiles();

        style.getItems().clear();
        style.getItems().add("default");
        for(File file: listOfFiles){
            if (file.isFile() && file.getName().endsWith(".css")) {
                style.getItems().add(file.getName());
            }
        }
        style.getItems().sort(Comparator.naturalOrder());
        style.getSelectionModel().select(mainWindow.getStyle().replace("file:styles/", ""));

        openInFolderCommand.setText(mainWindow.getOpenInFolderCommand());
        openInFolderArgument.setText(mainWindow.getOpenInFolderArgument());
        stage.showAndWait();
    }
}
