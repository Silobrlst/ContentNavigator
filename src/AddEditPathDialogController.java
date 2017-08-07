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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    private static final String AddEditPathDialogJsonName = "AddEditPathDialog";
    private static final String xJsonName = "x";
    private static final String yJsonName = "y";
    private static final String widthJsonName = "width";
    private static final String heightJsonName = "height";
    private static final String exploreCurrentDirectory = "exploreCurrentDirectory";
    private static final File guiSettings = new File("guiSettings.json");
    //</string names>=====================

    private final Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);

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
                if(name.getText().isEmpty()){
                    name.setText(file.getName());
                }
                pathValidation.setText("");
                initialDirectory = new File(file.getAbsolutePath().split("[/\\\\][^\\\\/]*$")[0]);
            }
        });

        exploreDirectory.setOnAction(event -> {
            File file = directoryChooser.showDialog(stage);
            if(file != null){
                path.setText(file.getAbsolutePath());
                if(name.getText().isEmpty()){
                    name.setText(file.getName());
                }
                pathValidation.setText("");
                initialDirectory = new File(file.getAbsolutePath().split("[/\\\\][^\\\\/]*$")[0]);
            }
        });

        addTags.setOnAction(event -> {
            List<String> selectedTags = availableTags.getSelectionModel().getSelectedItems();
            addedTags.getItems().addAll(selectedTags);
            addedTags.getItems().sort(Comparator.naturalOrder());

            for(String selectedTag: selectedTags){
                addedTags.getSelectionModel().select(selectedTag);
            }

            availableTags.getItems().removeAll(availableTags.getSelectionModel().getSelectedItems());
            availableTags.getSelectionModel().clearSelection();
        });

        removeTags.setOnAction(event -> {
            List<String> selectedTags = addedTags.getSelectionModel().getSelectedItems();
            availableTags.getItems().addAll(addedTags.getSelectionModel().getSelectedItems());
            availableTags.getItems().sort(Comparator.naturalOrder());

            for(String selectedTag: selectedTags){
                availableTags.getSelectionModel().select(selectedTag);
            }

            addedTags.getItems().removeAll(selectedTags);
            addedTags.getSelectionModel().clearSelection();
        });

        path.setOnAction(event -> onOK());

        name.setOnAction(event -> onOK());

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

        stage.setOnShown(event -> onShown());

        loadGuiSettings();

        setAddPath();
    }

    private void onShown(){
        pathValidation.setText("");

        availableTags.getItems().clear();
        availableTags.getItems().addAll(tags.getTagsIds());
        availableTags.getItems().sort(Comparator.naturalOrder());

        if(editing){
            addedTags.getItems().clear();
            addedTags.getItems().addAll(tags.getTagsIds(tagsTemp));
            availableTags.getItems().removeAll(addedTags.getItems());
        }else{
            path.setText("");
            addedTags.getItems().clear();

            if(tagsTemp != null){
                addedTags.getItems().addAll(tags.getTagsIds(tagsTemp));
                availableTags.getItems().removeAll(addedTags.getItems());
            }
        }

        path.requestFocus();
    }

    public AddEditPathDialogController()throws Exception{
        fileChooser = new FileChooser();
        directoryChooser = new DirectoryChooser();
        initialDirectory = new File(System.getProperty("user.dir"));
        styleFileName = "";
    }

    private List<Tag> getTagsByIds(List<String> idsIn){
        ArrayList<Tag> tags_ = new ArrayList<>();

        for(String id: idsIn){
            tags_.add(tags.getTagById(id));
        }

        return tags_;
    }

    //<GUI settings i/o>======================
    private void validateGuiSettings(JSONObject jsonIn) {
        if (!jsonIn.has(AddEditPathDialogJsonName)) {
            jsonIn.put(AddEditPathDialogJsonName, new JSONObject());
        }

        JSONObject settingsWindowJSON = jsonIn.getJSONObject(AddEditPathDialogJsonName);
        if (!settingsWindowJSON.has(xJsonName)) {
            settingsWindowJSON.put(xJsonName, 0.d);
        }
        if (!settingsWindowJSON.has(xJsonName)) {
            settingsWindowJSON.put(yJsonName, 0.d);
        }
        if (!settingsWindowJSON.has(widthJsonName)) {
            settingsWindowJSON.put(widthJsonName, 0.d);
        }
        if (!settingsWindowJSON.has(heightJsonName)) {
            settingsWindowJSON.put(heightJsonName, 0.d);
        }
        if (!settingsWindowJSON.has(exploreCurrentDirectory)) {
            settingsWindowJSON.put(exploreCurrentDirectory, initialDirectory.getAbsolutePath());
        }
    }

    private void loadGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject mainWindowJSON = guiJSON.getJSONObject(AddEditPathDialogJsonName);
        if (mainWindowJSON.getDouble(widthJsonName) > 0) {
            stage.setWidth(mainWindowJSON.getDouble(widthJsonName));
        }
        if (mainWindowJSON.getDouble(heightJsonName) > 0) {
            stage.setHeight(mainWindowJSON.getDouble(heightJsonName));
        }

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            if (mainWindowJSON.getString(exploreCurrentDirectory).length() > 0) {
                File file = new File(mainWindowJSON.getString(exploreCurrentDirectory));
                if(file.exists()){
                    fileChooser.setInitialDirectory(file);
                    directoryChooser.setInitialDirectory(file);
                    initialDirectory = file;
                }
            }
        })).play();
    }

    private void saveGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject settingsWindowJSON = guiJSON.getJSONObject(AddEditPathDialogJsonName);
        settingsWindowJSON.put(xJsonName, stage.getX());
        settingsWindowJSON.put(yJsonName, stage.getY());
        settingsWindowJSON.put(widthJsonName, stage.getWidth());
        settingsWindowJSON.put(heightJsonName, stage.getHeight());
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

    private void saveAndExit(){
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
                Path newPath;

                if(name.getText().isEmpty()){
                    newPath = paths.newPath(path.getText());
                }else{
                    newPath = paths.newPath(path.getText(), name.getText());
                }

                newPath.addTags(addedTagsTemp);

                stage.hide();
            }else{
                pathValidation.setText("This path is already added");
            }
        }
    }

    private void onOK() {
        if(new File(path.getText()).exists()){
            saveAndExit();
        }else{
            alertConfirm.setTitle("Path does not exists");
            alertConfirm.setHeaderText("Path does not exists, save anyway?");
            Optional<ButtonType> result = alertConfirm.showAndWait();
            if (result.get() == ButtonType.OK) {
                saveAndExit();
            }
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
