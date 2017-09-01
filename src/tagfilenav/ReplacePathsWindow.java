package tagfilenav;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Optional;

public class ReplacePathsWindow {
    @FXML
    private TextField find;
    @FXML
    private TextField replaceBy;
    @FXML
    private Button replaceAll;
    @FXML
    private ListView<Path> pathsList;

    private Stage stage;
    private StyledGuiSaver savableStyledGui;

    private Paths paths;

    private final Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);

    @FXML
    public void initialize() {}

    public ReplacePathsWindow(){}

    public void init(FXMLLoader loaderIn, Paths pathsIn){
        paths = pathsIn;

        String windowName = "replacePathsWindow";

        Scene scene = new Scene(loaderIn.getRoot());
        scene.getRoot().setId(windowName + "Root");
        stage = new Stage();
        stage.setTitle("Replace paths");
        stage.setScene(scene);
        stage.setOnShown(event -> onShown());
        stage.setOnHidden(event -> savableStyledGui.save());
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.hide();
            }
        });

        replaceBy.setOnAction(event -> onReplaceAll());

        replaceAll.setOnAction(event -> onReplaceAll());

        find.setOnKeyTyped(event -> onPathsUpdate());

        pathsList.setCellFactory(new Callback<ListView<Path>, ListCell<Path>>() {
            @Override
            public ListCell<Path> call(ListView<Path> stringTreeView) {
                return new ListCell<Path>() {
                    protected void updateItem(Path itemIn, boolean empty) {
                        super.updateItem(itemIn, empty);
                        if (itemIn != null) {
                            setText(itemIn.getPath());
                        } else {
                            setText("");   // <== clear the now empty cell.
                        }
                    }
                };
            }
        });

        pathsList.setOnKeyPressed(event -> {
            if(event.isControlDown() && event.getCode() == KeyCode.C){
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(pathsList.getSelectionModel().getSelectedItem().getPath());
                clipboard.setContent(content);
            }
        });

        savableStyledGui = new StyledGuiSaver(windowName, stage);
        savableStyledGui.load();
    }

    //<on>==============================================================================================================
    private void onPathsUpdate(){
        pathsList.getItems().clear();

        for(Path path: paths){
            if(path.getPath().toLowerCase().contains(find.getText().toLowerCase())){
                pathsList.getItems().add(path);
            }
        }
    }

    private void onReplaceAll(){
        alertConfirm.setTitle("Replace paths");
        alertConfirm.setHeaderText("Replace paths?");
        alertConfirm.setAlertType(Alert.AlertType.CONFIRMATION);
        Optional<ButtonType> result = alertConfirm.showAndWait();
        if (result.get() == ButtonType.OK) {
            ArrayList<Path> pathsToReplace = new ArrayList<>();
            ArrayList<Path> existedPaths = new ArrayList<>();
            boolean save = false;

            for(Path path: pathsList.getItems()){
                String pathStr = path.getPath();
                pathStr = pathStr.replace(find.getText(), replaceBy.getText());

                if(paths.checkPathAdded(pathStr)){
                    existedPaths.add(path);
                }else{
                    pathsToReplace.add(path);
                }
            }

            if(existedPaths.size() > 0){
                String existedPathsStr = "";
                for(Path path: existedPaths){
                    existedPathsStr += path.getPath() + "\n";
                }

                alertConfirm.setTitle("Paths can not be replaced");
                alertConfirm.setContentText(existedPathsStr);

                if(pathsToReplace.size() == 0){
                    alertConfirm.setAlertType(Alert.AlertType.WARNING);
                    alertConfirm.setHeaderText("These paths can not be replaced, because such paths already exist.");
                }else{
                    alertConfirm.setAlertType(Alert.AlertType.CONFIRMATION);
                    alertConfirm.setHeaderText("These paths can not be replaced, because such paths already exist. Save others?");
                }

                Optional<ButtonType> result2 = alertConfirm.showAndWait();
                if (result2.get() == ButtonType.OK) {
                    save = true;
                }
            }else{
                save = true;
            }

            if(save){
                for(Path path: pathsToReplace){
                    String pathStr = path.getPath();
                    pathStr = pathStr.replace(find.getText(), replaceBy.getText());
                    path.setPath(pathStr);
                }

                paths.pathsChanged();
            }

            onPathsUpdate();
        }
    }

    private void onShown(){
        find.setText("");
        replaceBy.setText("");
        pathsList.getItems().clear();
        pathsList.getItems().addAll(paths);
    }
    //</on>=============================================================================================================

    void setStyle(String styleFileNameIn){
        savableStyledGui.setStyle(styleFileNameIn);
    }

    void open(){
        stage.show();
    }

    void close(){
        stage.hide();
    }
}
