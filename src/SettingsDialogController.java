import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SettingsDialogController {
    @FXML
    private Button cancel;
    @FXML
    private Button ok;
    @FXML
    private Button openAppFolder;
    @FXML
    private TextField openInFolderCommand;

    private Stage stage;

    private String styleFileName;

    private MainWindowController mainWindowController;

    private SettingsDialogInterface settingsDialogInterface;

    public SettingsDialogController(){
        styleFileName = "";
    }

    @FXML
    public void initialize() {
    }

    public void setParentStage(Stage parentStageIn, FXMLLoader loaderIn, MainWindowController mainWindowControllerIn, SettingsDialogInterface settingsDialogInterfaceIn){
        mainWindowController = mainWindowControllerIn;
        settingsDialogInterface = settingsDialogInterfaceIn;

        openInFolderCommand.setText(mainWindowController.getOpenInFolderCommand());

        Scene scene = new Scene(loaderIn.getRoot());
        stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(parentStageIn);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                onCancel();
            }
        });

        cancel.setOnAction(event -> onCancel());
        ok.setOnAction(event -> onOk());

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
    }

    private void onCancel() {
        stage.hide();
    }

    private void onOk(){
        settingsDialogInterface.applied();
        stage.hide();
    }

    public void open() {
        stage.showAndWait();
    }

    public void setStyle(String styleFileNameIn){
        if(styleFileName.length() > 0){
            stage.getScene().getStylesheets().remove(styleFileName);
        }
        styleFileName = styleFileNameIn;
        stage.getScene().getStylesheets().add(styleFileName);
    }

    public String getOpenInFolderCommand(){
        return openInFolderCommand.getText();
    }
}
