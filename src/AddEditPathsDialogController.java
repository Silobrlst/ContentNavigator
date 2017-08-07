import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AddEditPathsDialogController {
    private boolean editing;

    private List<Path> editingPaths;

    private List<Tag> tagsTemp;

    private List<File> pathsToAdd = null;

    @FXML
    private ListView<String> availableTags;
    @FXML
    private ListView<String> addedTags;
    @FXML
    private Button ok;
    @FXML
    private Button cancel;
    @FXML
    private Button addTags;
    @FXML
    private Button removeTags;
    @FXML
    private Button apply;
    @FXML
    private TableColumn<PathTemp, String> pathsTablePath;
    @FXML
    private TableColumn<PathTemp, String> pathsTableName;
    @FXML
    private TableView<PathTemp> pathsTable;

    //<string names>======================
    private static final String AddEditPathsDialogJsonName = "AddEditPathsDialog";
    private static final String xJsonName = "x";
    private static final String yJsonName = "y";
    private static final String widthJsonName = "width";
    private static final String heightJsonName = "height";
    private static final String pathsTablePathJsonName = "pathsTablePath";
    private static final String pathsTableNameJsonName = "pathsTableName";
    private static final File guiSettings = new File("guiSettings.json");
    //</string names>=====================

    private final Alert alertConfirm = new Alert(Alert.AlertType.INFORMATION);

    private Paths paths;

    private Tags tags;

    private Stage stage;

    private String styleFileName;

    @FXML
    public void initialize() {
        ok.setOnAction(event -> onOK());

        cancel.setOnAction(event -> onCancel());

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

        pathsTableName.setCellValueFactory(cellData -> cellData.getValue().nameText);
        pathsTablePath.setCellValueFactory(cellData -> cellData.getValue().pathText);

        pathsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    public void setPathsTagsParent(Stage parentStageIn, FXMLLoader loaderIn, Paths pathsIn, Tags tagsIn)throws Exception{
        paths = pathsIn;
        tags = tagsIn;

        Scene scene = new Scene(loaderIn.getRoot());
        scene.getRoot().setId("addEditPathsDialogRoot");
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

        pathsTable.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                pathsTable.getItems().removeAll(pathsTable.getSelectionModel().getSelectedItems());
            }
        });

        EventHandler<DragEvent> dropFiles = event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;

                for(File file: db.getFiles()){
                    pathsTable.getItems().add(new PathTemp(file.getAbsolutePath(), file.getName()));
                }
            }
            event.setDropCompleted(success);
            event.consume();
        };

        EventHandler<DragEvent> dragOverFiles = event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        };

        pathsTable.setOnDragDropped(dropFiles);
        pathsTable.setOnDragOver(dragOverFiles);
        pathsTable.setRowFactory(tv -> new TableRow<PathTemp>() {
            private Tooltip tooltip = new Tooltip();

            @Override
            public void updateItem(PathTemp pathTempIn, boolean empty) {
                super.updateItem(pathTempIn, empty);
                if (pathTempIn == null) {
                    setTooltip(null);
                } else {
                    String message = pathTempIn.pathText.get();
                    tooltip.setText(message);
                    setTooltip(tooltip);
                }
            }
        });

        loadGuiSettings();
    }

    private void onShown(){
        availableTags.getItems().clear();
        availableTags.getItems().addAll(tags.getTagsIds());
        availableTags.getItems().sort(Comparator.naturalOrder());

        pathsTable.getItems().clear();

        if(editing){
            addedTags.getItems().clear();
            addedTags.getItems().addAll(tags.getTagsIds(tagsTemp));
            availableTags.getItems().removeAll(addedTags.getItems());

            for(Path path: editingPaths){
                PathTemp pathTemp = new PathTemp(path.getPath(), path.getName());
                pathTemp.path = path;
                pathsTable.getItems().add(pathTemp);
            }
        }else{
            addedTags.getItems().clear();

            if(tagsTemp != null){
                addedTags.getItems().addAll(tags.getTagsIds(tagsTemp));
                availableTags.getItems().removeAll(addedTags.getItems());
            }

            for(File pathToAdd: pathsToAdd){
                pathsTable.getItems().add(new PathTemp(pathToAdd.getAbsolutePath(), pathToAdd.getName()));
            }
        }
    }

    public AddEditPathsDialogController()throws Exception{
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
        if (!jsonIn.has(AddEditPathsDialogJsonName)) {
            jsonIn.put(AddEditPathsDialogJsonName, new JSONObject());
        }

        JSONObject windowJson = jsonIn.getJSONObject(AddEditPathsDialogJsonName);
        if (!windowJson.has(xJsonName)) {
            windowJson.put(xJsonName, 0.d);
        }
        if (!windowJson.has(xJsonName)) {
            windowJson.put(yJsonName, 0.d);
        }
        if (!windowJson.has(widthJsonName)) {
            windowJson.put(widthJsonName, 0.d);
        }
        if (!windowJson.has(heightJsonName)) {
            windowJson.put(heightJsonName, 0.d);
        }
        if (!windowJson.has(pathsTablePathJsonName)) {
            windowJson.put(pathsTablePathJsonName, 0.d);
        }
        if (!windowJson.has(pathsTableNameJsonName)) {
            windowJson.put(pathsTableNameJsonName, 0.d);
        }
    }

    private void loadGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject windowJson = guiJSON.getJSONObject(AddEditPathsDialogJsonName);
        if (windowJson.getDouble(widthJsonName) > 0) {
            stage.setWidth(windowJson.getDouble(widthJsonName));
        }
        if (windowJson.getDouble(heightJsonName) > 0) {
            stage.setHeight(windowJson.getDouble(heightJsonName));
        }
        if (windowJson.getDouble(pathsTablePathJsonName) > 0) {
            pathsTablePath.setPrefWidth(windowJson.getDouble(pathsTablePathJsonName));
        }
        if (windowJson.getDouble(pathsTableNameJsonName) > 0) {
            pathsTableName.setPrefWidth(windowJson.getDouble(pathsTableNameJsonName));
        }
    }

    private void saveGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject settingsWindowJSON = guiJSON.getJSONObject(AddEditPathsDialogJsonName);
        settingsWindowJSON.put(xJsonName, stage.getX());
        settingsWindowJSON.put(yJsonName, stage.getY());
        settingsWindowJSON.put(widthJsonName, stage.getWidth());
        settingsWindowJSON.put(heightJsonName, stage.getHeight());
        settingsWindowJSON.put(pathsTablePathJsonName, pathsTablePath.getWidth());
        settingsWindowJSON.put(pathsTableNameJsonName, pathsTableName.getWidth());

        JSONLoader.saveJSON(guiSettings, guiJSON);
    }
    //</GUI settings i/o>=====================

    public void getTagsFromPaths(List<Path> pathsIn){
        if(pathsIn.size() == 0){
            return;
        }

        tagsTemp = new ArrayList<>();
        tagsTemp.addAll(pathsIn.get(0).getTags());

        ArrayList<Tag> tagsTempToDel = new ArrayList<>();

        for(Path path: pathsIn){
            List<Tag> pathTags = path.getTags();

            tagsTempToDel.clear();
            for(Tag tag: tagsTemp){
                if(!pathTags.contains(tag)){
                    tagsTempToDel.add(tag);
                }
            }
            tagsTemp.removeAll(tagsTempToDel);

            if(tagsTemp.size() == 0){
                return;
            }
        }
    }

    public void setAddPaths(List<File> pathsToAddIn, List<Tag> tagsIn){
        stage.setTitle("Add Paths");
        apply.setDisable(true);
        editing = false;

        pathsToAdd = pathsToAddIn;
        tagsTemp = tagsIn;
    }

    public void setAddPaths(List<File> pathsToAddIn){
        setAddPaths(pathsToAddIn, null);
    }

    public void setEditPaths(List<Path> pathsIn){
        stage.setTitle("Edit Paths");
        apply.setDisable(false);
        editing = true;
        editingPaths = pathsIn;

        getTagsFromPaths(pathsIn);
    }

    private void onOK() {
        List<Tag> addedTagsTemp = getTagsByIds(addedTags.getItems());

        if(editing){
            ArrayList<Tag> tagsToRemove = new ArrayList<>();
            tagsToRemove.addAll(tagsTemp);
            tagsToRemove.removeAll(addedTagsTemp);

            for(PathTemp pathTemp: pathsTable.getItems()){
                pathTemp.path.removeTags(tagsToRemove);
                pathTemp.path.addTags(addedTagsTemp);
            }

            stage.hide();
        }else{
            boolean b = true;
            String existedPaths = "";
            ArrayList<PathTemp> pathTemps = new ArrayList<>();
            for(PathTemp pathTemp: pathsTable.getItems()){
                if(paths.checkPathExist(pathTemp.pathText.get())){
                    b = false;
                    existedPaths += pathTemp.pathText.get() + "\n";
                    pathTemps.add(pathTemp);
                }
            }

            if(b){
                for(PathTemp pathTemp: pathsTable.getItems()){
                    Path newPath;

                    if(pathTemp.nameText.get().isEmpty()){
                        newPath = paths.newPath(pathTemp.pathText.get());
                    }else{
                        newPath = paths.newPath(pathTemp.pathText.get(), pathTemp.nameText.get());
                    }

                    newPath.addTags(addedTagsTemp);
                }

                stage.hide();
            }else{
                alertConfirm.setTitle("Paths already exists");
                alertConfirm.setHeaderText("This paths already exists:");
                alertConfirm.setContentText(existedPaths);
                alertConfirm.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> {
                    ((Label)node).setWrapText(false);
                    ((Label)node).setMinHeight(Region.USE_PREF_SIZE);
                    ((Label)node).setTooltip(new Tooltip(((Label)node).getText()));
                });
                alertConfirm.showAndWait();

                pathsTable.getSelectionModel().clearSelection();
                for(PathTemp pathTemp: pathTemps){
                    pathsTable.getSelectionModel().select(pathTemp);
                }
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


    private class PathTemp{
        public StringProperty pathText;
        public StringProperty nameText;
        public Path path;

        PathTemp(String pathIn, String nameIn){
            pathText = new SimpleStringProperty(pathIn);
            nameText = new SimpleStringProperty(nameIn);
        }
    }
}
