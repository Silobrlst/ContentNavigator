import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class AddEditPathDialog extends Stage {
    private TextField path;
    private Label pathValidation;
    private Button apply;
    private ListView<Tag> addedTags;
    private ListView<Tag> availableTags;

    private boolean editing;

    private Path editingPath;

    private List<Tag> tagsTemp;
    private FileChooser fileChooser;
    private DirectoryChooser directoryChooser;
    private File initialDirectory;

    //<string names>======================
    private static final String settingsWindow = "settingsWindow";
    private static final String x = "x";
    private static final String y = "y";
    private static final String width = "width";
    private static final String height = "height";
    private static final String exploreCurrentDirectory = "exploreCurrentDirectory";
    private static final File guiSettings = new File("guiSettings.json");
    //</string names>=====================

    private Paths paths;

    AddEditPathDialog(Stage parentStageIn, Paths pathsIn, Tags tagsIn)throws Exception{
        this.initOwner(parentStageIn);
        this.initModality(Modality.APPLICATION_MODAL);

        paths = pathsIn;

        Parent root = FXMLLoader.load(getClass().getResource("AddEditPathDialog.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add("modena_dark.css");

        this.setScene(scene);

        path = (TextField)scene.lookup("#path");
        pathValidation = (Label)scene.lookup("#pathValidation");
        apply = (Button)scene.lookup("#apply");

        fileChooser = new FileChooser();
        directoryChooser = new DirectoryChooser();
        initialDirectory = new File(System.getProperty("user.dir"));

        ((Button)scene.lookup("#ok")).setOnAction(event -> onOK());

        ((Button)scene.lookup("#cancel")).setOnAction(event -> onCancel());

        ((Button)scene.lookup("#exploreFile")).setOnAction(event -> {
            File file = fileChooser.showOpenDialog(this);
            if(file != null){
                path.setText(file.getAbsolutePath());
                pathValidation.setText("");
                initialDirectory = new File(file.getAbsolutePath().split("[/\\\\][^\\\\/]*$")[0]);
            }
        });

        ((Button)scene.lookup("#exploreDirectory")).setOnAction(event -> {
            File file = directoryChooser.showDialog(this);
            if(file != null){
                path.setText(file.getAbsolutePath());
                pathValidation.setText("");
                initialDirectory = new File(file.getAbsolutePath().split("[/\\\\][^\\\\/]*$")[0]);
            }
        });

        ((Button)scene.lookup("#addTags")).setOnAction(event -> {
            addedTags.getItems().addAll(availableTags.getSelectionModel().getSelectedItems());
            availableTags.getItems().removeAll(availableTags.getSelectionModel().getSelectedItems());
        });

        ((Button)scene.lookup("#removeTags")).setOnAction(event -> {
            availableTags.getItems().addAll(addedTags.getSelectionModel().getSelectedItems());
            addedTags.getItems().removeAll(addedTags.getSelectionModel().getSelectedItems());
        });

        loadGuiSettings();

        this.setOnHidden(event -> saveGuiSettings());
        this.setOnCloseRequest(event -> saveGuiSettings());

        this.setOnShown(event -> {
            availableTags = (ListView)scene.lookup("#availableTags");
            availableTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            addedTags = (ListView)scene.lookup("#addedTags");
            addedTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            availableTags.getItems().clear();
            availableTags.getItems().addAll(tagsIn);

            if(editing){
                addedTags.getItems().clear();
                addedTags.getItems().addAll(tagsTemp);
                availableTags.getItems().removeAll(addedTags.getItems());
            }else{
                path.setText("");
                addedTags.getItems().clear();

                if(tagsTemp != null){
                    addedTags.getItems().addAll(tagsTemp);
                    availableTags.getItems().removeAll(addedTags.getItems());
                }
            }

            path.requestFocus();
        });

        setAddPath();
    }

    //<GUI settings i/o>======================
    private void validateGuiSettings(JSONObject jsonIn) {
        if (!jsonIn.has(settingsWindow)) {
            jsonIn.put(settingsWindow, new JSONObject());
        }

        JSONObject settingsWindowJSON = jsonIn.getJSONObject(settingsWindow);
        if (!settingsWindowJSON.has(x)) {
            settingsWindowJSON.put(x, 0.d);
        }
        if (!settingsWindowJSON.has(x)) {
            settingsWindowJSON.put(y, 0.d);
        }
        if (!settingsWindowJSON.has(width)) {
            settingsWindowJSON.put(width, 0.d);
        }
        if (!settingsWindowJSON.has(height)) {
            settingsWindowJSON.put(height, 0.d);
        }
        if (!settingsWindowJSON.has(exploreCurrentDirectory)) {
            settingsWindowJSON.put(exploreCurrentDirectory, initialDirectory.getAbsolutePath());
        }
    }

    private void loadGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject mainWindowJSON = guiJSON.getJSONObject(settingsWindow);
        if (mainWindowJSON.getDouble(width) > 0) {
            this.setWidth(mainWindowJSON.getDouble(width));
        }
        if (mainWindowJSON.getDouble(height) > 0) {
            this.setHeight(mainWindowJSON.getDouble(height));
        }

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            if (mainWindowJSON.getString(exploreCurrentDirectory).length() > 0) {
                File file = new File(mainWindowJSON.getString(exploreCurrentDirectory));
                fileChooser.setInitialDirectory(file);
                directoryChooser.setInitialDirectory(file);
                initialDirectory = file;
            }
        })).play();
    }

    private void saveGuiSettings() {
        JSONObject guiJSON = new JSONObject();
        validateGuiSettings(guiJSON);

        JSONObject settingsWindowJSON = guiJSON.getJSONObject(settingsWindow);
        settingsWindowJSON.put(x, this.getX());
        settingsWindowJSON.put(y, this.getY());
        settingsWindowJSON.put(width, this.getWidth());
        settingsWindowJSON.put(height, this.getHeight());
        settingsWindowJSON.put(exploreCurrentDirectory, initialDirectory.getAbsolutePath());

        JSONLoader.saveJSON(guiSettings, guiJSON);
    }
    //</GUI settings i/o>=====================

    public void setAddPath(List<Tag> tagsIn){
        this.setTitle("Add Path");
        apply.setDisable(true);
        editing = false;

        tagsTemp = tagsIn;
    }

    public void setAddPath(){
        setAddPath(null);
    }

    public void setEditPath(Path pathIn){
        this.setTitle("Edit Path");
        apply.setDisable(false);
        editing = true;
        editingPath = pathIn;
        path.setText(editingPath.getPath());
        tagsTemp = pathIn.getTags();
    }

    private void onOK() {
        if(new File(path.getText()).exists()){
            if(editing){
                editingPath.setPath(path.getText());
                editingPath.addTags(addedTags.getItems());
            }else{
                Path newPath = paths.newPath(path.getText());
                newPath.addTags(addedTags.getItems());
            }

            this.close();
        }else{
            pathValidation.setText("Path does not exists");
        }
    }

    private void onCancel() {
        // add your code here if necessary
        this.hide();
    }
}
