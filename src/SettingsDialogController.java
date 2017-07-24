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

public class SettingsDialogController {
    @FXML
    private Button cancel;
    @FXML
    private Button ok;
    @FXML
    private Button apply;
    @FXML
    private Button openAppFolder;
    @FXML
    private TextField openInFolderCommand;
    @FXML
    private TextField openInFolderArgument;
    @FXML
    private ComboBox<String> style;

    private Stage stage;

    //стиль этого окна
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
    }

    private void onCancel() {
        stage.hide();
    }

    private void onOk(){
        settingsDialogInterface.applied();
        stage.hide();
    }

    public void open() {
        File folder = new File("styles");
        File[] listOfFiles = folder.listFiles();

        style.getItems().clear();
        style.getItems().add("default");
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".css")) {
                style.getItems().add(listOfFiles[i].getName());
            }
        }
        style.getSelectionModel().select(mainWindowController.getStyle().replace("file:styles/", ""));

        openInFolderCommand.setText(mainWindowController.getOpenInFolderCommand());
        openInFolderArgument.setText(mainWindowController.getOpenInFolderArgument());
        stage.showAndWait();
    }

    public void setStyle(String styleFileNameIn){
        if(styleFileNameIn.equals("default")){
            if(styleFileName.length() > 0){
                stage.getScene().getStylesheets().remove(styleFileName);
            }
            styleFileName = "";
            return;
        }

        if(styleFileName.length() > 0){
            stage.getScene().getStylesheets().remove(styleFileName);
        }
        styleFileName = styleFileNameIn;
        stage.getScene().getStylesheets().add(styleFileName);
    }

    public String getOpenInFolderCommand(){
        return openInFolderCommand.getText();
    }
    public String getOpenInFolderArgument(){
        return openInFolderArgument.getText();
    }
    public String getSelectedStyle(){
        String selectedStyle = style.getSelectionModel().getSelectedItem();

        if(selectedStyle.equals("default")){
            return "default";
        }

        return "file:styles/" + style.getSelectionModel().getSelectedItem();
    }
}
