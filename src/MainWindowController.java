import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MainWindowController {
    @FXML
    private TableView<Path> searchedPaths;
    @FXML
    private TableColumn<Path, String> searchedPathsName;
    @FXML
    private TableColumn<Path, String> searchedPathsPath;
    @FXML
    private TreeView<Tag> tagsTree;
    @FXML
    private SplitPane tagsPathsSpliter;
    @FXML
    private MenuBar menuBar;
    @FXML
    private TextField byTagsSearch;
    @FXML
    private Button search;
    @FXML
    private TextField byNameSearch;
    @FXML
    private Label searchValidation;

    private final String appName = "Tag file navigator";

    //<icons>=================================
    private final Image openIcon = new Image("file:images/openIcon.png");
    private final Image settingsIcon = new Image("file:images/settingsIcon.png");
    private final Image addIcon = new Image("file:images/addIcon.png");
    private final Image removeIcon = new Image("file:images/removeIcon.png");
    private final Image searchIcon = new Image("file:images/searchIcon.png");
    private final Image editIcon = new Image("file:images/editIcon.png");
    //</icons>================================

    //<JSON names>============================
    private static final String tagsJSONName = "tags";
    private static final String pathsJSONName = "paths";
    private static final String nameJSONName = "name";
    private static final String searchedPathsNameWidth = "searchedPathsNameWidth";
    private static final String searchedPathsPathWidth = "searchedPathsPathWidth";
    //</JSON names>===========================

    private Menu openRecentMenu;
    private Menu menuEdit;
    private MenuItem renameTagItem;

    private Tags tags;
    private Paths paths;

    private Stage stage;
    private String styleFileName;

    private SettingsDialogController settingsDialogController;
    private AddEditPathDialogController addEditPathDialogController;
    private AddEditTagDialogController addEditTagDialogController;

    private File contenetInfoFile;

    public MainWindowController(){
        styleFileName = "";
    }

    public void setTagsStage(Stage stageIn, FXMLLoader loaderIn)throws Exception{
        stage = stageIn;
        stage.setOnCloseRequest(event -> saveGuiSettings());
        stage.setScene(new Scene(loaderIn.getRoot()));
        stage.getIcons().add(new Image("file:images/appIcon.png"));
        stage.show();

        paths = new Paths();
        paths.addListener(new EmptyPathListener(){
            @Override
            public void created(Path pathIn) {
                saveContentInfo(contenetInfoFile);
            }

            @Override
            public void renamed(Path pathIn) {
                saveContentInfo(contenetInfoFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void removedTag(Path pathIn, Tag tagIn) {
                saveContentInfo(contenetInfoFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void removedTags(Path pathIn, Collection<Tag> tagsIn) {
                saveContentInfo(contenetInfoFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void addedTags(Path pathIn, Collection<Tag> tagsIn) {
                saveContentInfo(contenetInfoFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void addedTag(Path pathIn, Tag tagIn) {
                saveContentInfo(contenetInfoFile);
                filterPathsBySelectedTags();
            }
        });

        tags = new Tags();
        tags.addTagObserver(new EmptyTagListener(){
            @Override
            public void createdTag(Tag tagIn, Tag parentIn) {
                TreeItem<Tag> tagTreeItem = new TreeItem<>(tagIn);
                getTreeItemByTag(parentIn).getChildren().add(tagTreeItem);
                tagsTree.getSelectionModel().clearSelection();
                tagsTree.getSelectionModel().select(tagTreeItem);
                tagsTree.requestFocus();

                saveContentInfo(contenetInfoFile);
            }

            @Override
            public void removedPathFromTag(Tag tagIn, Path pathIn) {
                filterPathsBySelectedTags();
            }

            @Override
            public void renamedTag(Tag tagIn) {
                tagsTree.refresh();
                saveContentInfo(contenetInfoFile);
            }
        });

        //<tagsTree>=================================
        TreeItem<Tag> root = new TreeItem<>(tags);
        root.setExpanded(true);
        tagsTree.setRoot(root);
        tagsTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> filterPathsBySelectedTags());
        tagsTree.setDisable(true);
        tagsTree.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedPathsFromSelectedTagsConfirm();
            }
        });
        //</tagsTree>================================

        //<searchedPaths>============================
        searchedPaths.setDisable(true);
        searchedPaths.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedTagsConfirm();
            }
        });
        //</searchedPaths>===========================

        //<addEditTagDialogController>==========================
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AddEditTagDialog.fxml"));
        loader.load();
        addEditTagDialogController = loader.getController();
        addEditTagDialogController.setTagsParent(stage, loader, tags);
        //</addEditTagDialogController>=========================

        //<addEditPathDialogController>=========================
        loader = new FXMLLoader(getClass().getResource("AddEditPathDialog.fxml"));
        loader.load();
        addEditPathDialogController = loader.getController();
        addEditPathDialogController.setPathsTagsParent(stage, loader, paths, tags);
        //</addEditPathDialogController>========================

        //<SettingsDialogController>============================
        loader = new FXMLLoader(getClass().getResource("SettingsWindow.fxml"));
        loader.load();
        settingsDialogController = loader.getController();
        settingsDialogController.setParentStage(stage, loader);
        //</SettingsDialogController>===========================

        search.setGraphic(new ImageView(searchIcon));

        byNameSearch.setOnAction(event -> filterPathsBySearchQuery());

        initMenu();

        preLoadGuiSettings();

        if (openRecentMenu.getItems().size() > 0) {
            loadContentInfo(new File(openRecentMenu.getItems().get(0).getText()));
        }

        postLoadGuiSettings();
    }

    @FXML
    public void initialize() {
        searchedPathsName.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
        searchedPathsPath.setCellValueFactory(cellData -> cellData.getValue().getPathProperty());
        searchedPaths.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tagsTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tagsTree.setCellFactory(param -> new TreeCell<Tag>() {
            @Override
            protected void updateItem(Tag tagIn, boolean empty) {
                super.updateItem(tagIn, empty);
                if (tagIn != null) {
                    setText(tagIn.getName());
                } else {
                    setText("");   // <== clear the now empty cell.
                }
            }
        });
    }

    private void removeSelectedTags() {
        Collection<TreeItem<Tag>> selectedTags = tagsTree.getSelectionModel().getSelectedItems();

        for (TreeItem<Tag> selectedTag: selectedTags) {
            if(selectedTag != tagsTree.getRoot()){
                selectedTag.getParent().getChildren().remove(selectedTag);
                selectedTag.getValue().getParent().removeChild(selectedTag.getValue());
            }
        }
    }

    private TreeItem<Tag> getTreeItemByTag(Tag tagIn, TreeItem<Tag> treeItemParentIn){
        if(treeItemParentIn.getValue() == tagIn){
            return treeItemParentIn;
        }else{
            for(TreeItem<Tag> tagTreeItem: treeItemParentIn.getChildren()){
                TreeItem<Tag> tagTreeItemTemp = getTreeItemByTag(tagIn, tagTreeItem);
                if(tagTreeItemTemp != null){
                    return tagTreeItemTemp;
                }
            }
        }

        return null;
    }

    private TreeItem<Tag> getTreeItemByTag(Tag tagIn){
        return getTreeItemByTag(tagIn, tagsTree.getRoot());
    }

    private void reSortPaths(){
        TableColumn<Path, String> sortcolumn = null;
        TableColumn.SortType st = null;
        if (searchedPaths.getSortOrder().size()>0) {
            sortcolumn = (TableColumn<Path, String>)searchedPaths.getSortOrder().get(0);
            st = sortcolumn.getSortType();
        }

        if (sortcolumn!=null) {
            searchedPaths.getSortOrder().add(sortcolumn);
            sortcolumn.setSortType(st);
            sortcolumn.setSortable(true); // This performs a sort
        }
    }


    //<GUI settings i/o>======================
    private void validateGuiSettings(JSONObject jsonIn) {
        if (!jsonIn.has("recent")) {
            jsonIn.put("recent", new JSONArray());
        }

        if (!jsonIn.has("mainWindow")) {
            jsonIn.put("mainWindow", new JSONObject());
        }

        JSONObject mainWindowJSON = jsonIn.getJSONObject("mainWindow");
        if (!mainWindowJSON.has("x")) {
            mainWindowJSON.put("x", 0.d);
        }
        if (!mainWindowJSON.has("x")) {
            mainWindowJSON.put("y", 0.d);
        }
        if (!mainWindowJSON.has("width")) {
            mainWindowJSON.put("width", 0.d);
        }
        if (!mainWindowJSON.has("height")) {
            mainWindowJSON.put("height", 0.d);
        }
        if (!mainWindowJSON.has("tagsPathsSplitPosition")) {
            mainWindowJSON.put("tagsPathsSplitPosition", 0.2d);
        }

        if (!mainWindowJSON.has("selectedTags")) {
            mainWindowJSON.put("selectedTags", new JSONArray());
        }

        if (!jsonIn.has(searchedPathsNameWidth)) {
            jsonIn.put(searchedPathsNameWidth, 0.d);
        }
        if (!jsonIn.has(searchedPathsPathWidth)) {
            jsonIn.put(searchedPathsPathWidth, 0.d);
        }
    }

    private void preLoadGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(new File("guiSettings.json"));
        validateGuiSettings(guiJSON);

        JSONArray recentJSON = guiJSON.getJSONArray("recent");

        for (int i = recentJSON.length() - 1; i >= 0; i--) {
            addRecent(new File(recentJSON.getString(i)), false);
        }

        JSONObject mainWindowJSON = guiJSON.getJSONObject("mainWindow");
        if (mainWindowJSON.getDouble("width") > 0) {
            stage.setWidth(mainWindowJSON.getDouble("width"));
        }
        if (mainWindowJSON.getDouble("height") > 0) {
            stage.setHeight(mainWindowJSON.getDouble("height"));
        }

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            tagsPathsSpliter.setDividerPositions(mainWindowJSON.getDouble("tagsPathsSplitPosition"));
            if(guiJSON.getDouble(searchedPathsNameWidth) != 0.d){
                searchedPathsName.setPrefWidth(guiJSON.getDouble(searchedPathsNameWidth));
            }
            if(guiJSON.getDouble(searchedPathsPathWidth) != 0.d){
                searchedPathsPath.setPrefWidth(guiJSON.getDouble(searchedPathsPathWidth));
            }
        })).play();
    }

    private void postLoadGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(new File("guiSettings.json"));
        validateGuiSettings(guiJSON);
        JSONObject mainWindowJSON = guiJSON.getJSONObject("mainWindow");

        JSONArray selectedTagsJSON = mainWindowJSON.getJSONArray("selectedTags");
        for (int i = 0; i < selectedTagsJSON.length(); i++) {
            tagsTree.getSelectionModel().select(new TreeItem<>(tags.getTagById(selectedTagsJSON.getString(i))));
        }
        filterPathsBySelectedTags();
        tagsTree.requestFocus();
    }

    private void saveGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(new File("guiSettings.json"));
        validateGuiSettings(guiJSON);

        JSONArray recentJSON = new JSONArray();
        for (int i = 0; i < openRecentMenu.getItems().size(); i++) {
            recentJSON.put(openRecentMenu.getItems().get(i).getText());
        }

        guiJSON.put("recent", recentJSON);
        JSONObject mainWindowJSON = guiJSON.getJSONObject("mainWindow");
        mainWindowJSON.put("x", stage.getX());
        mainWindowJSON.put("y", stage.getY());
        mainWindowJSON.put("width", stage.getWidth());
        mainWindowJSON.put("height", stage.getHeight());
        mainWindowJSON.put("tagsPathsSplitPosition", tagsPathsSpliter.getDividerPositions()[0]);

        JSONArray selectedTags = new JSONArray();
        for (TreeItem<Tag> selectedTagItem : tagsTree.getSelectionModel().getSelectedItems()) {
            selectedTags.put(selectedTagItem.getValue().getName());
        }
        mainWindowJSON.put("selectedTags", selectedTags);

        guiJSON.put(searchedPathsNameWidth, searchedPathsName.getWidth());
        guiJSON.put(searchedPathsPathWidth, searchedPathsPath.getWidth());

        JSONLoader.saveJSON(new File("guiSettings.json"), guiJSON);
    }
    //</GUI settings i/o>=====================

    private void addRecent(File fileIn, boolean saveIn) {
        for (MenuItem recentItem : openRecentMenu.getItems()) {
            if (recentItem.getText().equals(fileIn.getAbsolutePath())) {
                openRecentMenu.getItems().remove(recentItem);
                openRecentMenu.getItems().add(0, recentItem);

                return;
            }
        }

        MenuItem menuItem = new MenuItem(fileIn.getAbsolutePath());
        menuItem.setOnAction(e -> {
            File file = new File(menuItem.getText());
            loadContentInfo(file);
            addRecent(file, true);
        });

        openRecentMenu.getItems().add(0, menuItem);

        if (openRecentMenu.getItems().size() > 15) {
            openRecentMenu.getItems().remove(15);
        }

        if (saveIn) {
            saveGuiSettings();
        }
    }

    private List<Path> getPathsByTags(List<TreeItem<Tag>> tagsIn) {
        if (tagsIn.size() == 0) {
            return null;
        }

        if(tagsIn.get(0) == null){
            return null;
        }

        ArrayList<Path> filteredPaths = new ArrayList<>();
        ArrayList<Path> filteredPathsTemp = new ArrayList<>();

        filteredPaths.addAll(tagsIn.get(0).getValue().getPaths());

        for (TreeItem<Tag> tagItem : tagsIn) {
            filteredPathsTemp.clear();

            for (Path path : tagItem.getValue().getPaths()) {
                if (filteredPaths.contains(path)) {
                    filteredPathsTemp.add(path);
                }
            }

            filteredPaths.clear();
            filteredPaths.addAll(filteredPathsTemp);
        }

        return filteredPaths;
    }

    private void filterPathsBySelectedTags() {
        List<Path> filteredPaths = getPathsByTags(tagsTree.getSelectionModel().getSelectedItems());

        if (filteredPaths != null) {
            searchedPaths.getItems().clear();
            searchedPaths.getItems().addAll(filteredPaths);
            searchedPaths.getItems().sort(Comparator.naturalOrder());

            String tagsStr = "";
            for (TreeItem<Tag> tagItem : tagsTree.getSelectionModel().getSelectedItems()) {
                String id = tags.getTagId(tagItem.getValue());
                if (id.contains(" ")) {
                    tagsStr += "\"" + id + "\" ";
                } else {
                    tagsStr += id + " ";
                }
            }

            byTagsSearch.setText(tagsStr);
        }

        reSortPaths();

        if (tagsTree.getSelectionModel().getSelectedItems().size() == 1) {
            renameTagItem.setDisable(false);
        } else {
            renameTagItem.setDisable(true);
        }
    }

    private void filterPathsBySearchQuery(){
        searchedPaths.getItems().clear();

        if(byNameSearch.getText().length() > 0){
            for(Path path: paths){
                if(path.getPath().toLowerCase().matches(".*" + byNameSearch.getText().toLowerCase() + ".*")){
                    searchedPaths.getItems().add(path);
                }
            }

            if(searchedPaths.getItems().size() == 0){
                searchValidation.setText("No matched paths");
            }else{
                searchValidation.setText("");
            }

            searchedPaths.getItems().sort(Comparator.naturalOrder());
            reSortPaths();
        }else{
            searchValidation.setText("Search query is empty");
        }
    }

    //<Content info i/o>======================
    private void validateContentInfo(JSONObject jsonIn) {
        if (!jsonIn.has(tagsJSONName)) {
            jsonIn.put(tagsJSONName, new JSONObject());
        }

        if (!jsonIn.has(pathsJSONName)) {
            jsonIn.put(pathsJSONName, new JSONObject());
        }

        JSONObject pathsJSON = jsonIn.getJSONObject(pathsJSONName);
        for(String pathJSON: pathsJSON.keySet()){
            JSONObject pathJSONObj = pathsJSON.getJSONObject(pathJSON);

            if(!pathJSONObj.has(tagsJSONName)){
                pathJSONObj.put(tagsJSONName, new JSONObject());
            }

            if(!pathJSONObj.has(nameJSONName)){
                pathJSONObj.put(nameJSONName, "");
            }
        }
    }

    private void validateTag(JSONObject jsonIn){
        if(!jsonIn.has(tagsJSONName)){
            jsonIn.put(tagsJSONName, new JSONObject());
        }
    }

    private void loadTags(JSONObject parentTagJSONIn, Tag parentIn){
        validateTag(parentTagJSONIn);

        JSONObject tagsJSON = parentTagJSONIn.getJSONObject(tagsJSONName);
        for (String tagName : tagsJSON.keySet()) {
            Tag tag = tags.newTagWithoutNotifing(tagName, parentIn);
            TreeItem<Tag> tagItem = new TreeItem<>(tag);
            tagItem.setExpanded(true);
            getTreeItemByTag(parentIn).getChildren().add(tagItem);

            if(tagsJSON.keySet().size() > 0){
                loadTags(tagsJSON.getJSONObject(tagName), tag);
            }
        }
    }

    private void loadContentInfo(File fileIn) {
        if(!fileIn.exists()){
            return;
        }

        JSONObject json = JSONLoader.loadJSON(fileIn);
        validateContentInfo(json);

        //<load tags>================================
        JSONObject tagsJSON = json.getJSONObject(tagsJSONName);
        for (String tagName : tagsJSON.keySet()) {
            Tag tag = tags.newTagWithoutNotifing(tagName, tags);
            TreeItem<Tag> tagItem = new TreeItem<>(tag);
            tagItem.setExpanded(true);
            tagsTree.getRoot().getChildren().add(tagItem);
            loadTags(tagsJSON.getJSONObject(tagName), tag);
        }
        //</load tags>===============================

        JSONObject pathsJSON = json.getJSONObject(pathsJSONName);
        for(String pathJSONKey: pathsJSON.keySet()){
            Path path;
            JSONObject pathJSON = pathsJSON.getJSONObject(pathJSONKey);

            if(pathJSON.getString(nameJSONName).length() > 0){
                path = paths.newPathWithoutNotifing(pathJSONKey, pathJSON.getString(nameJSONName));
            }else{
                path = paths.newPathWithoutNotifing(pathJSONKey);
            }

            for(String tagJSON: pathJSON.getJSONObject(tagsJSONName).keySet()){
                Tag tag = tags.getTagById(tagJSON);

                if(tag != null){
                    path.addTagWithoutNotifing(tag);
                }
            }
        }

        tagsTree.setDisable(false);
        searchedPaths.setDisable(false);
        searchedPaths.getItems().clear();

        menuEdit.setDisable(false);

        contenetInfoFile = fileIn;
        stage.setTitle(appName + " [" + fileIn.getName() + "]");
    }

    private void tagsToJSONRecursive(Tag tagIn, JSONObject tagJSONIn){
        validateTag(tagJSONIn);
        JSONObject tagsJSON = tagJSONIn.getJSONObject(tagsJSONName);

        for(Tag tag: tagIn.getChildren()){
            JSONObject tagJSON = new JSONObject();
            tagsJSON.put(tag.getName(), tagJSON);
            tagsToJSONRecursive(tag, tagJSON);
        }
    }

    private void saveContentInfo(File fileIn) {
        JSONObject json = new JSONObject();

        JSONObject tagsJSON = new JSONObject();
        json.put(tagsJSONName, tagsJSON);

        JSONObject pathsJSON = new JSONObject();
        json.put(pathsJSONName, pathsJSON);

        for(Tag tag: tags.getChildren()){
            JSONObject tagJSON = new JSONObject();
            tagsJSON.put(tag.getName(), tagJSON);
            tagsToJSONRecursive(tag, tagJSON);
        }

        for(Path path: paths){
            if(path.getTags().size() > 0){
                JSONObject pathJSON = new JSONObject();
                pathsJSON.put(path.getPath(), pathJSON);

                JSONObject pathTagsJSON = new JSONObject();
                pathJSON.put(tagsJSONName, pathTagsJSON);
                pathJSON.put(nameJSONName, path.getName());

                for(Tag tag: path.getTags()){
                    pathTagsJSON.put(tags.getTagId(tag), "");
                }
            }
        }

        JSONLoader.saveJSON(fileIn, json);
    }

    private void newContentInfo(File fileIn) {
        menuEdit.setDisable(false);
        searchedPaths.getItems().clear();
        tagsTree.getRoot().getChildren().clear();

        contenetInfoFile = fileIn;
        stage.setTitle(appName + " [" + fileIn.getName() + "]");
    }
    //</Content info i/o>=====================

    private void removeSelectedTagsConfirm() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Tags");
        alert.setHeaderText("Remove selected tagsJSON?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            removeSelectedTags();
            byTagsSearch.setText("");
            saveContentInfo(contenetInfoFile);
        }
    }

    private void removeSelectedPathsFromSelectedTagsConfirm() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Paths");
        alert.setHeaderText("Remove selected paths from selected tags?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            for (Path selectedPath: searchedPaths.getSelectionModel().getSelectedItems()) {
                ArrayList<Tag> tags = new ArrayList<>();
                for(TreeItem<Tag> tagItem: tagsTree.getSelectionModel().getSelectedItems()){
                    tags.add(tagItem.getValue());
                }
                selectedPath.removeTags(tags);
            }
            searchedPaths.getItems().removeAll(searchedPaths.getSelectionModel().getSelectedItems());

            saveContentInfo(contenetInfoFile);
        }
    }

    private void initMenu()throws Exception{
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All (*.*)", "*.*"));

        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Content file info (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(extensionFilter);
        fileChooser.setSelectedExtensionFilter(extensionFilter);

        //<menu file>====================================
        MenuItem newItem = new MenuItem("New");
        newItem.setOnAction(event -> {
            fileChooser.setTitle("New content info file");
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                if (!file.getName().endsWith(".json")) {
                    file = new File(file.getAbsoluteFile() + ".json");
                }

                newContentInfo(file);
                addRecent(file, true);
            }
        });

        MenuItem openItem = new MenuItem("Open");
        openItem.setGraphic(new ImageView(openIcon));
        openItem.setOnAction(event -> {
            fileChooser.setTitle("Open content info file");
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                loadContentInfo(file);
                addRecent(file, true);
            }
        });

        openRecentMenu = new Menu("Open Recent");

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setGraphic(new ImageView(settingsIcon));
        settingsItem.setOnAction(event -> settingsDialogController.open());

        MenuItem exitItem = new MenuItem("Exit");

        Menu menuFile = new Menu("File");
        menuFile.getItems().add(newItem);
        menuFile.getItems().add(new SeparatorMenuItem());
        menuFile.getItems().add(openItem);
        menuFile.getItems().add(openRecentMenu);
        menuFile.getItems().add(new SeparatorMenuItem());
        menuFile.getItems().add(settingsItem);
        menuFile.getItems().add(new SeparatorMenuItem());
        menuFile.getItems().add(exitItem);
        //</menu file>===================================

        //<serached paths listView ContextMenu>==========
        ContextMenu serchedPathsContextMenu = new ContextMenu();
        MenuItem openPathItemContext = new MenuItem("Open");
        openPathItemContext.setGraphic(new ImageView(openIcon));
        MenuItem openInFolderPathItemContext = new MenuItem("Open in folder");
        openInFolderPathItemContext.setGraphic(new ImageView(openIcon));
        MenuItem editPathItemContext = new MenuItem("Edit Path");
        editPathItemContext.setGraphic(new ImageView(editIcon));
        MenuItem addPathItemContext = new MenuItem("Add Path");
        addPathItemContext.setGraphic(new ImageView(addIcon));
        MenuItem removePathsFromSelectedTagsItemContext = new MenuItem("Remove Paths from selected tags");
        removePathsFromSelectedTagsItemContext.setGraphic(new ImageView(removeIcon));

        serchedPathsContextMenu.getItems().add(openPathItemContext);
        serchedPathsContextMenu.getItems().add(openInFolderPathItemContext);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(addPathItemContext);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(editPathItemContext);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(removePathsFromSelectedTagsItemContext);
        searchedPaths.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                if(searchedPaths.getSelectionModel().getSelectedItems().size() == 1){
                    removePathsFromSelectedTagsItemContext.setDisable(false);
                    openPathItemContext.setDisable(false);
                    openInFolderPathItemContext.setDisable(false);
                    editPathItemContext.setDisable(false);
                }
                if(searchedPaths.getSelectionModel().getSelectedItems().size() > 1){
                    removePathsFromSelectedTagsItemContext.setDisable(false);
                    openPathItemContext.setDisable(true);
                    openInFolderPathItemContext.setDisable(true);
                    editPathItemContext.setDisable(true);
                }
                if(searchedPaths.getSelectionModel().getSelectedItems().size() == 0){
                    removePathsFromSelectedTagsItemContext.setDisable(true);
                    openPathItemContext.setDisable(true);
                    openInFolderPathItemContext.setDisable(true);
                    editPathItemContext.setDisable(true);
                }

                serchedPathsContextMenu.show(tagsTree, event.getScreenX(), event.getScreenY());
            } else if (event.getButton() == MouseButton.PRIMARY) {
                serchedPathsContextMenu.hide();
            }
        });
        //</serached paths listView ContextMenu>=========

        //<tags listView ContextMenu>====================
        ContextMenu tagsTreeContextMenu = new ContextMenu();
        MenuItem renameTagItemContext = new MenuItem("Edit Tag");
        renameTagItemContext.setGraphic(new ImageView(editIcon));
        MenuItem addTagItemContext = new MenuItem("Add Tag");
        addTagItemContext.setGraphic(new ImageView(addIcon));
        MenuItem addPathSelectedWithTagsItemContext = new MenuItem("Add Path with selected tags");
        addPathSelectedWithTagsItemContext.setGraphic(new ImageView(addIcon));
        MenuItem removeTagItemContext = new MenuItem("Remove Tags");
        removeTagItemContext.setGraphic(new ImageView(removeIcon));

        tagsTreeContextMenu.getItems().add(addTagItemContext);
        tagsTreeContextMenu.getItems().add(addPathSelectedWithTagsItemContext);
        tagsTreeContextMenu.getItems().add(new SeparatorMenuItem());
        tagsTreeContextMenu.getItems().add(renameTagItemContext);
        tagsTreeContextMenu.getItems().add(new SeparatorMenuItem());
        tagsTreeContextMenu.getItems().add(removeTagItemContext);
        tagsTree.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                tagsTreeContextMenu.show(tagsTree, event.getScreenX(), event.getScreenY());
            } else if (event.getButton() == MouseButton.PRIMARY) {
                tagsTreeContextMenu.hide();
            }
        });
        //</tags listView ContextMenu>===================

        //<menu edit>====================================
        MenuItem addTagItem = new MenuItem("Add Tag");
        addTagItem.setGraphic(new ImageView(addIcon));
        MenuItem addPathItem = new MenuItem("Add Path");
        addPathItem.setGraphic(new ImageView(addIcon));
        renameTagItem = new MenuItem("Edit Tag");
        renameTagItem.setGraphic(new ImageView(editIcon));
        MenuItem editPathItem = new MenuItem("Edit Path");
        editPathItem.setGraphic(new ImageView(editIcon));
        MenuItem removeTagItem = new MenuItem("Remove Tags");
        removeTagItem.setGraphic(new ImageView(removeIcon));

        menuEdit = new Menu("Edit");
        ObservableList<MenuItem> menuEditItems = menuEdit.getItems();
        menuEditItems.add(addTagItem);
        menuEditItems.add(addPathItem);
        menuEditItems.add(new SeparatorMenuItem());
        menuEditItems.add(renameTagItem);
        menuEditItems.add(editPathItem);
        menuEditItems.add(new SeparatorMenuItem());
        menuEditItems.add(removeTagItem);
        menuEdit.setDisable(true);
        //</menu edit>====================================

        //<menu events>===================================
        EventHandler<ActionEvent> addTag = (event) -> {
            if(tagsTree.getSelectionModel().getSelectedItems().size() == 1){
                addEditTagDialogController.setAddTag(tagsTree.getSelectionModel().getSelectedItem().getValue());
            }else{
                addEditTagDialogController.setAddTag();
            }

            addEditTagDialogController.open();
        };

        EventHandler<ActionEvent> addPath = (event) -> {
            addEditPathDialogController.setAddPath();
            addEditPathDialogController.open();
        };

        EventHandler<ActionEvent> renameTag = (event) -> {
            if (tagsTree.getSelectionModel().getSelectedItems().size() == 1) {
                addEditTagDialogController.setRenameTag(tagsTree.getSelectionModel().getSelectedItem().getValue());
                addEditTagDialogController.open();
            }
        };

        EventHandler<ActionEvent> editPath = (event) -> {
            if (searchedPaths.getSelectionModel().getSelectedItems().size() == 1) {
                editPathItemContext.setDisable(false);
                addEditPathDialogController.setEditPath(searchedPaths.getSelectionModel().getSelectedItem());
                addEditPathDialogController.open();
            } else {
                editPathItemContext.setDisable(true);
            }
        };

        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.T && event.isControlDown()) {
                addTag.handle(new ActionEvent());
            }else if (event.getCode() == KeyCode.P && event.isControlDown()) {
                addPath.handle(new ActionEvent());
            }
        });

        addTagItem.setOnAction(addTag);
        addPathItem.setOnAction(addPath);
        renameTagItem.setOnAction(renameTag);
        editPathItem.setOnAction(editPath);
        removeTagItem.setOnAction(event -> removeSelectedTagsConfirm());

        openPathItemContext.setOnAction(event -> {
            if (Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try {
                        File file = new File(searchedPaths.getSelectionModel().getSelectedItem().getPath());
                        Desktop.getDesktop().open(file);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });
        openInFolderPathItemContext.setOnAction(event -> {
            if (Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try {
                        File file = new File(searchedPaths.getSelectionModel().getSelectedItem().getPath());
//                        if(file.isDirectory()){
//                            file = new File(searchedPaths.getSelectionModel().getSelectedItem().split("(\\\\|\\/)[^\\\\|\\/]*$")[0]);
//                        }
                        String[] command = {"nemo", file.getAbsolutePath()};
                        Runtime.getRuntime().exec(command);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });
        addPathItemContext.setOnAction(addPath);
        editPathItemContext.setOnAction(editPath);
        removePathsFromSelectedTagsItemContext.setOnAction(event -> removeSelectedPathsFromSelectedTagsConfirm());

        addTagItemContext.setOnAction(addTag);
        renameTagItemContext.setOnAction(renameTag);
        removeTagItemContext.setOnAction(event -> removeSelectedTagsConfirm());
        addPathSelectedWithTagsItemContext.setOnAction(event -> {
            ArrayList<Tag> tags1 = new ArrayList<>();
            for(TreeItem<Tag> tagItem: tagsTree.getSelectionModel().getSelectedItems()){
                tags1.add(tagItem.getValue());
            }

            addEditPathDialogController.setAddPath(tags1);
            addEditPathDialogController.open();
        });
        tagsTree.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                tagsTreeContextMenu.show(tagsTree, event.getScreenX(), event.getScreenY());

                int selectedCount = tagsTree.getSelectionModel().getSelectedItems().size();
                if (selectedCount == 1) {
                    renameTagItem.setDisable(false);
                    renameTagItemContext.setDisable(false);
                } else {
                    renameTagItem.setDisable(true);
                    renameTagItemContext.setDisable(true);
                }
                if (selectedCount != 0) {
                    removeTagItem.setDisable(false);
                    removeTagItemContext.setDisable(false);
                } else {
                    removeTagItem.setDisable(true);
                    removeTagItemContext.setDisable(true);
                }
            } else if (event.getButton() == MouseButton.PRIMARY) {
                tagsTreeContextMenu.hide();
            }
        });

        //прячем контекстное меню таблицы путей при потере фокуса
        searchedPaths.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue){
                serchedPathsContextMenu.hide();
            }
        });
        //</menu events>====================================

        menuBar.getMenus().add(menuFile);
        menuBar.getMenus().add(menuEdit);
    }

    public void setStyle(String styleFileNameIn){
        if(styleFileName.length() > 0){
            stage.getScene().getStylesheets().remove(styleFileName);
        }
        styleFileName = styleFileNameIn;
        stage.getScene().getStylesheets().add(styleFileName);

        addEditTagDialogController.setStyle(styleFileName);
        addEditPathDialogController.setStyle(styleFileName);
        settingsDialogController.setStyle(styleFileName);
    }
}
