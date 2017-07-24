import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddEditPathDialogController {
    private boolean editing;

    private Path editingPath;

    private List<Tag> tagsTemp;
    private FileChooser fileChooser;
    private DirectoryChooser directoryChooser;
    private File initialDirectory;

    @FXML
    private ListView<String> availableTags;
    @FXML
    private ListView<String> addedTags;
    @FXML
    private Button ok;
    @FXML
    private Button cancel;
    @FXML
    private Button exploreFile;
    @FXML
    private Button exploreDirectory;
    @FXML
    private Button addTags;
    @FXML
    private Button removeTags;
    @FXML
    private Button apply;
    @FXML
    private Label pathValidation;
    @FXML
    private TextField path;
    @FXML
    private TextField name;

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

    private Tags tags;

    private Stage stage;

    private String styleFileName;

    @FXML
    public void initialize() {
        ok.setOnAction(event -> onOK());

        cancel.setOnAction(event -> onCancel());

        exploreFile.setOnAction(event -> {
            File file = fileChooser.showOpenDialog(stage);
            if(file != null){
                path.setText(file.getAbsolutePath());
                name.setText(file.getName());
                pathValidation.setText("");
                initialDirectory = new File(file.getAbsolutePath().split("[/\\\\][^\\\\/]*$")[0]);
            }
        });

        exploreDirectory.setOnAction(event -> {
            File file = directoryChooser.showDialog(stage);
            if(file != null){
                path.setText(file.getAbsolutePath());
                name.setText(file.getName());
                pathValidation.setText("");
                initialDirectory = new File(file.getAbsolutePath().split("[/\\\\][^\\\\/]*$")[0]);
            }
        });

        addTags.setOnAction(event -> {
            addedTags.getItems().addAll(availableTags.getSelectionModel().getSelectedItems());
            availableTags.getItems().removeAll(availableTags.getSelectionModel().getSelectedItems());
        });

        removeTags.setOnAction(event -> {
            availableTags.getItems().addAll(addedTags.getSelectionModel().getSelectedItems());
            addedTags.getItems().removeAll(addedTags.getSelectionModel().getSelectedItems());
        });

        availableTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addedTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    public void setPathsTagsParent(Stage parentStageIn, FXMLLoader loaderIn, Paths pathsIn, Tags tagsIn)throws Exception{
        paths = pathsIn;
        tags = tagsIn;

        Scene scene = new Scene(loaderIn.getRoot());
        scene.getRoot().setId("addEditPathDialogRoot");
        stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(parentStageIn);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnHidden(event -> saveGuiSettings());
        stage.setOnCloseRequest(event -> saveGuiSettings());
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                onCancel();
            }
        });

        stage.setOnShown(event -> {
            pathValidation.setText("");

            getTags(tagsIn);
            availableTags.getItems().clear();
            availableTags.getItems().addAll(getTagIds(getTags(tagsIn)));

            if(editing){
                addedTags.getItems().clear();
                addedTags.getItems().addAll(getTagIds(tagsTemp));
                availableTags.getItems().removeAll(addedTags.getItems());
            }else{
                path.setText("");
                addedTags.getItems().clear();

                if(tagsTemp != null){
                    addedTags.getItems().addAll(getTagIds(tagsTemp));
                    availableTags.getItems().removeAll(addedTags.getItems());
                }
            }

            path.requestFocus();
        });

        loadGuiSettings();

        setAddPath();
    }

    public AddEditPathDialogController()throws Exception{
        fileChooser = new FileChooser();
        directoryChooser = new DirectoryChooser();
        initialDirectory = new File(System.getProperty("user.dir"));
        styleFileName = "";
    }

    private List<String> getTagIds(List<Tag> tagsIn){
        ArrayList<String> ids = new ArrayList<>();
        for(Tag tag: tagsIn){
            if(tag != tags){
                ids.add(tags.getTagId(tag));
            }
        }
        return ids;
    }

    private List<Tag> getTagsByIds(List<String> idsIn){
        ArrayList<Tag> tags_ = new ArrayList<>();

        for(String id: idsIn){
            tags_.add(tags.getTagById(id));
        }

        return tags_;
    }

    private void getTags(Tag parentIn, List<Tag> tagsOut){
        if(parentIn != tags){
            tagsOut.add(parentIn);
        }
        for(Tag tag: parentIn.getChildren()){
            getTags(tag, tagsOut);
        }
    }

    private List<Tag> getTags(Tag parentIn){
        ArrayList<Tag> allTags = new ArrayList<>();
        getTags(parentIn, allTags);
        return allTags;
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
            stage.setWidth(mainWindowJSON.getDouble(width));
        }
        if (mainWindowJSON.getDouble(height) > 0) {
            stage.setHeight(mainWindowJSON.getDouble(height));
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
        JSONObject guiJSON = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject settingsWindowJSON = guiJSON.getJSONObject(settingsWindow);
        settingsWindowJSON.put(x, stage.getX());
        settingsWindowJSON.put(y, stage.getY());
        settingsWindowJSON.put(width, stage.getWidth());
        settingsWindowJSON.put(height, stage.getHeight());
        settingsWindowJSON.put(exploreCurrentDirectory, initialDirectory.getAbsolutePath());

        JSONLoader.saveJSON(guiSettings, guiJSON);
    }
    //</GUI settings i/o>=====================

    public void setAddPath(List<Tag> tagsIn){
        stage.setTitle("Add Path");
        apply.setDisable(true);
        editing = false;
        name.setText("");

        tagsTemp = tagsIn;
    }

    public void setAddPath(){
        setAddPath(null);
    }

    public void setEditPath(Path pathIn){
        stage.setTitle("Edit Path");
        apply.setDisable(false);
        editing = true;
        editingPath = pathIn;
        path.setText(editingPath.getPath());
        name.setText(editingPath.getName());
        System.out.println(editingPath.getName());
        tagsTemp = pathIn.getTags();
    }

    private void onOK() {
        if(new File(path.getText()).exists()){
            List<Tag> addedTagsTemp = getTagsByIds(addedTags.getItems());

            if(editing){
                ArrayList<Tag> tagsToRemove = new ArrayList<>();
                tagsToRemove.addAll(editingPath.getTags());
                tagsToRemove.removeAll(addedTagsTemp);

                editingPath.setPath(path.getText());
                editingPath.setName(name.getText());
                editingPath.removeTags(tagsToRemove);
                editingPath.addTags(addedTagsTemp);

                stage.hide();
            }else{
                if(!paths.checkPathExist(path.getText())){
                    Path newPath = paths.newPath(path.getText());
                    newPath.addTags(addedTagsTemp);

                    stage.hide();
                }else{
                    pathValidation.setText("This path is already added");
                }
            }
        }else{
            pathValidation.setText("Path does not exists");
        }
    }

    private void onCancel() {
        // add your code here if necessary
        stage.hide();
    }

    public void open(){
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
}
