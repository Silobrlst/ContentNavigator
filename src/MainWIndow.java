import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
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

public class MainWIndow extends Application {
    private Stage primaryStage;

    private SplitPane splitPane;
    //private ListView<Tag> tagsListView;

    private Menu openRecentMenu;
    private Menu menuEdit;
    private MenuItem renameTagItem;
    private TextField byTagsSearch;

    private final String appName = "Content navigator";

    private Tags tags;
    private Paths paths;

    private File contenetInfoFile;
    private JSONObject tagsJSON;
    //private JSONObject pathsJSON;

    private AddEditTagDialog addEditTagDialog;
    private AddEditPathDialog addEditPathDialog;

    //<string names>==========================
    private static final String tagsJSONName = "tags";
    private static final String pathsJSONName = "paths";
    private static final String nameJSONName = "name";
    private static final String searchedPathsNameWidth = "searchedPathsNameWidth";
    private static final String searchedPathsPathWidth = "searchedPathsPathWidth";
    //</string names>=========================

    private MainController mainController;

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
            primaryStage.setWidth(mainWindowJSON.getDouble("width"));
        }
        if (mainWindowJSON.getDouble("height") > 0) {
            primaryStage.setHeight(mainWindowJSON.getDouble("height"));
        }

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            splitPane.setDividerPositions(mainWindowJSON.getDouble("tagsPathsSplitPosition"));
            if(guiJSON.getDouble(searchedPathsNameWidth) != 0.d){
                mainController.setSearchedPathsNameWidth(guiJSON.getDouble(searchedPathsNameWidth));
            }
            if(guiJSON.getDouble(searchedPathsPathWidth) != 0.d){
                mainController.setSearchedPathsPathWidth(guiJSON.getDouble(searchedPathsPathWidth));
            }
        })).play();
    }

    private void postLoadGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(new File("guiSettings.json"));
        validateGuiSettings(guiJSON);
        JSONObject mainWindowJSON = guiJSON.getJSONObject("mainWindow");

        JSONArray selectedTagsJSON = mainWindowJSON.getJSONArray("selectedTags");
        for (int i = 0; i < selectedTagsJSON.length(); i++) {
            mainController.getTagsTree().getSelectionModel().select(new TreeItem<>(tags.getTagById(selectedTagsJSON.getString(i))));
        }
        filterPathsBySelectedTags();
        mainController.getTagsTree().requestFocus();
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
        mainWindowJSON.put("x", primaryStage.getX());
        mainWindowJSON.put("y", primaryStage.getY());
        mainWindowJSON.put("width", primaryStage.getWidth());
        mainWindowJSON.put("height", primaryStage.getHeight());
        mainWindowJSON.put("tagsPathsSplitPosition", splitPane.getDividerPositions()[0]);

        JSONArray selectedTags = new JSONArray();
        for (TreeItem<Tag> selectedTagItem : mainController.getTagsTree().getSelectionModel().getSelectedItems()) {
            selectedTags.put(selectedTagItem.getValue().getName());
        }
        mainWindowJSON.put("selectedTags", selectedTags);

        guiJSON.put(searchedPathsNameWidth, mainController.getSearchedPathsNameWidth());
        guiJSON.put(searchedPathsPathWidth, mainController.getSearchedPathsPathWidth());

        System.out.println("name: " + mainController.getSearchedPathsNameWidth());
        System.out.println("path: " + mainController.getSearchedPathsPathWidth());

        JSONLoader.saveJSON(new File("guiSettings.json"), guiJSON);
    }
    //</GUI settings i/o>=====================

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
            mainController.getTreeItemByTag(parentIn).getChildren().add(tagItem);

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
        tagsJSON = json.getJSONObject(tagsJSONName);
        for (String tagName : tagsJSON.keySet()) {
            Tag tag = tags.newTagWithoutNotifing(tagName, tags);
            TreeItem<Tag> tagItem = new TreeItem<>(tag);
            tagItem.setExpanded(true);
            mainController.getTagsTree().getRoot().getChildren().add(tagItem);
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

        mainController.getTagsTree().setDisable(false);
        mainController.getPathsTable().setDisable(false);
        mainController.clearPaths();

        menuEdit.setDisable(false);

        contenetInfoFile = fileIn;
        primaryStage.setTitle(appName + " [" + fileIn.getName() + "]");
    }

    private void tagsToJSON(Tag tagIn, JSONObject tagJSONIn){
        validateTag(tagJSONIn);
        JSONObject tagsJSON = tagJSONIn.getJSONObject(tagsJSONName);

        for(Tag tag: tagIn.getChildren()){
            JSONObject tagJSON = new JSONObject();
            tagsJSON.put(tag.getName(), tagJSON);
            tagsToJSON(tag, tagJSON);
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
            tagsToJSON(tag, tagJSON);
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
        tagsJSON = new JSONObject();
        menuEdit.setDisable(false);
        mainController.clearPaths();
        mainController.clearTags();

        contenetInfoFile = fileIn;
        primaryStage.setTitle(appName + " [" + fileIn.getName() + "]");
    }
    //</Content info i/o>=====================

    //<editing>===============================
    private void removeSelectedTags() {
        mainController.removeSelectedTags();
        byTagsSearch.setText("");
        saveContentInfo(contenetInfoFile);
    }

    private void removeSelectedTagsConfirm() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Tags");
        alert.setHeaderText("Remove selected tagsJSON?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            removeSelectedTags();
        }
    }

    private void removeSelectedPathsFromSelectedTags() {
        mainController.removeSelectedPathsFromSelectedTags(mainController.getTagsTree().getSelectionModel().getSelectedItems());
        saveContentInfo(contenetInfoFile);
    }

    private void removeSelectedPathsFromSelectedTagsConfirm() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Paths");
        alert.setHeaderText("Remove selected paths from selected tags?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            removeSelectedPathsFromSelectedTags();
        }
    }
    //</editing>==============================

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
        List<Path> filteredPaths = getPathsByTags(mainController.getTagsTree().getSelectionModel().getSelectedItems());

        if (filteredPaths != null) {
            mainController.setPaths(filteredPaths);

            String tagsStr = "";
            for (TreeItem<Tag> tagItem : mainController.getTagsTree().getSelectionModel().getSelectedItems()) {
                String id = tags.getTagId(tagItem.getValue());
                if (id.contains(" ")) {
                    tagsStr += "\"" + id + "\" ";
                } else {
                    tagsStr += id + " ";
                }
            }

            byTagsSearch.setText(tagsStr);
        }

        if (mainController.getTagsTree().getSelectionModel().getSelectedItems().size() == 1) {
            renameTagItem.setDisable(false);
        } else {
            renameTagItem.setDisable(true);
        }
    }

    @Override
    public void start(Stage primaryStageIn) throws Exception {
        primaryStage = primaryStageIn;
        contenetInfoFile = null;

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("MainWindow.fxml"));
        loader.load();
        Scene scene = new Scene(loader.getRoot());
        scene.getStylesheets().add("modena_dark.css");

        FXMLLoader addEditPathDialogloader = new FXMLLoader();
        addEditPathDialogloader.setLocation(getClass().getResource("AddEditPathDialog.fxml"));
        addEditPathDialogloader.load();
        Scene addEditPathDialogscene = new Scene(addEditPathDialogloader.getRoot());
        addEditPathDialogscene.getStylesheets().add("modena_dark.css");
        Stage addEditPathDialogStage = new Stage();
        addEditPathDialogStage.setScene(addEditPathDialogscene);

        FXMLLoader addEditTagDialogloader = new FXMLLoader();
        addEditTagDialogloader.setLocation(getClass().getResource("AddEditTagDialog.fxml"));
        addEditTagDialogloader.load();
        Scene addEditTagDialogscene = new Scene(addEditTagDialogloader.getRoot());
        addEditTagDialogscene.getStylesheets().add("modena_dark.css");
        Stage addEditTagDialogStage = new Stage();
        addEditTagDialogStage.setScene(addEditTagDialogscene);

        tags = new Tags();
        paths = new Paths();

        mainController = loader.getController();
        mainController.setTags(tags);
        mainController.getTagsTree().setDisable(true);
        mainController.getPathsTable().setDisable(true);

        primaryStage.setOnCloseRequest(event -> saveGuiSettings());
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("file:images/appIcon.png"));
        primaryStage.show();

        //<paths table>==========================
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
        //</paths table>=========================

        //<tags list>============================
        mainController.getTagsTree().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> filterPathsBySelectedTags());

        tags.addTagObserver(new EmptyTagListener(){
            @Override
            public void created(Tag tagIn, Tag parentIn) {
                TreeItem<Tag> tagTreeItem = new TreeItem<>(tagIn);
                mainController.getTreeItemByTag(parentIn).getChildren().add(tagTreeItem);
                mainController.getTagsTree().getSelectionModel().clearSelection();
                mainController.getTagsTree().getSelectionModel().select(tagTreeItem);
                mainController.getTagsTree().requestFocus();

                saveContentInfo(contenetInfoFile);
            }

            @Override
            public void renamed(Tag tagIn) {
                mainController.getTagsTree().refresh();
                saveContentInfo(contenetInfoFile);
            }
        });
        //</tags list>===========================

        MenuBar menuBar = (MenuBar) scene.lookup("#menuBar");

        splitPane = (SplitPane) scene.lookup("#tagsPathsSpliter");

        byTagsSearch = (TextField) scene.lookup("#byTagsSearch");

        Button searchButton = (Button) scene.lookup("#search");

        addEditTagDialog = addEditTagDialogloader.getController();
        addEditTagDialog.setTagsParent(primaryStage, addEditTagDialogStage, tags);

        addEditPathDialog = addEditPathDialogloader.getController();
        addEditPathDialog.setPathsTagsParent(primaryStage, addEditPathDialogStage, paths, tags);

        SettingsWindow settingsWindow = new SettingsWindow(primaryStage);

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All (*.*)", "*.*"));

        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Content file info (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(extensionFilter);
        fileChooser.setSelectedExtensionFilter(extensionFilter);


        Image openIcon = new Image("file:images/openIcon.png");
        Image settingsIcon = new Image("file:images/settingsIcon.png");
        Image addIcon = new Image("file:images/addIcon.png");
        Image removeIcon = new Image("file:images/removeIcon.png");
        Image searchIcon = new Image("file:images/searchIcon.png");
        Image editIcon = new Image("file:images/editIcon.png");

        searchButton.setGraphic(new ImageView(searchIcon));

        //<menu file>====================
        MenuItem newItem = new MenuItem("New");
        newItem.setOnAction(event -> {
            fileChooser.setTitle("New content info file");
            File file = fileChooser.showSaveDialog(primaryStage);
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
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                loadContentInfo(file);
                addRecent(file, true);
            }
        });

        openRecentMenu = new Menu("Open Recent");

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setGraphic(new ImageView(settingsIcon));
        settingsItem.setOnAction(event -> settingsWindow.showAndWait());

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
        //</menu file>===================

        //<serached paths listView ContextMenu>==========
        ContextMenu serchedPathsListViewContextMenu = new ContextMenu();
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

        serchedPathsListViewContextMenu.getItems().add(openPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(openInFolderPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsListViewContextMenu.getItems().add(addPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsListViewContextMenu.getItems().add(editPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsListViewContextMenu.getItems().add(removePathsFromSelectedTagsItemContext);
        mainController.getPathsTable().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                if(mainController.getPathsTable().getSelectionModel().getSelectedItems().size() == 1){
                    removePathsFromSelectedTagsItemContext.setDisable(false);
                    openPathItemContext.setDisable(false);
                    openInFolderPathItemContext.setDisable(false);
                    editPathItemContext.setDisable(false);
                }
                if(mainController.getPathsTable().getSelectionModel().getSelectedItems().size() > 1){
                    removePathsFromSelectedTagsItemContext.setDisable(false);
                    openPathItemContext.setDisable(true);
                    openInFolderPathItemContext.setDisable(true);
                    editPathItemContext.setDisable(true);
                }
                if(mainController.getPathsTable().getSelectionModel().getSelectedItems().size() == 0){
                    removePathsFromSelectedTagsItemContext.setDisable(true);
                    openPathItemContext.setDisable(true);
                    openInFolderPathItemContext.setDisable(true);
                    editPathItemContext.setDisable(true);
                }

                serchedPathsListViewContextMenu.show(mainController.getTagsTree(), event.getScreenX(), event.getScreenY());
            } else if (event.getButton() == MouseButton.PRIMARY) {
                serchedPathsListViewContextMenu.hide();
            }
        });
        //</serached paths listView ContextMenu>=========

        //<tagsJSON listView ContextMenu>====================
        ContextMenu tagsListViewContextMenu = new ContextMenu();
        MenuItem renameTagItemContext = new MenuItem("Rename Tag");
        renameTagItemContext.setGraphic(new ImageView(editIcon));
        MenuItem addTagItemContext = new MenuItem("Add Tag");
        addTagItemContext.setGraphic(new ImageView(addIcon));
        MenuItem addPathSelectedWithTagsItemContext = new MenuItem("Add Path with selected tags");
        addPathSelectedWithTagsItemContext.setGraphic(new ImageView(addIcon));
        MenuItem removeTagItemContext = new MenuItem("Remove Tags");
        removeTagItemContext.setGraphic(new ImageView(removeIcon));

        tagsListViewContextMenu.getItems().add(addTagItemContext);
        tagsListViewContextMenu.getItems().add(addPathSelectedWithTagsItemContext);
        tagsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        tagsListViewContextMenu.getItems().add(renameTagItemContext);
        tagsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        tagsListViewContextMenu.getItems().add(removeTagItemContext);
        mainController.getTagsTree().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                tagsListViewContextMenu.show(mainController.getTagsTree(), event.getScreenX(), event.getScreenY());
            } else if (event.getButton() == MouseButton.PRIMARY) {
                tagsListViewContextMenu.hide();
            }
        });
        //</tagsJSON listView ContextMenu>===================

        //<menu edit>====================================
        MenuItem addTagItem = new MenuItem("Add Tag");
        addTagItem.setGraphic(new ImageView(addIcon));
        MenuItem addPathItem = new MenuItem("Add Path");
        addPathItem.setGraphic(new ImageView(addIcon));
        renameTagItem = new MenuItem("Rename Tag");
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
            if(mainController.getTagsTree().getSelectionModel().getSelectedItems().size() == 1){
                addEditTagDialog.setAddTag(mainController.getTagsTree().getSelectionModel().getSelectedItem().getValue());
            }else{
                addEditTagDialog.setAddTag();
            }

            addEditTagDialog.open();
        };

        EventHandler<ActionEvent> addPath = (event) -> {
            addEditPathDialog.setAddPath();
            addEditPathDialog.open();
        };

        EventHandler<ActionEvent> renameTag = (event) -> {
            if (mainController.getTagsTree().getSelectionModel().getSelectedItems().size() == 1) {
                addEditTagDialog.setRenameTag(mainController.getTagsTree().getSelectionModel().getSelectedItem().getValue());
                addEditTagDialog.open();
            }
        };

        EventHandler<ActionEvent> editPath = (event) -> {
            if (mainController.getPathsTable().getSelectionModel().getSelectedItems().size() == 1) {
                editPathItemContext.setDisable(false);
                addEditPathDialog.setEditPath(mainController.getPathsTable().getSelectionModel().getSelectedItem());
                addEditPathDialog.open();
            } else {
                editPathItemContext.setDisable(true);
            }
        };

        primaryStage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
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
                        File file = new File(mainController.getPathsTable().getSelectionModel().getSelectedItem().getPath());
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
                        File file = new File(mainController.getPathsTable().getSelectionModel().getSelectedItem().getPath());
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
            for(TreeItem<Tag> tagItem: mainController.getTagsTree().getSelectionModel().getSelectedItems()){
                tags1.add(tagItem.getValue());
            }

            addEditPathDialog.setAddPath(tags1);
            addEditPathDialog.open();
        });
        mainController.getTagsTree().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                tagsListViewContextMenu.show(mainController.getTagsTree(), event.getScreenX(), event.getScreenY());

                int selectedCount = mainController.getTagsTree().getSelectionModel().getSelectedItems().size();
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
                tagsListViewContextMenu.hide();
            }
        });
        //</menu events>====================================

        mainController.getPathsTable().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedTagsConfirm();
            }
        });

        mainController.getPathsTable().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedPathsFromSelectedTagsConfirm();
            }
        });

        menuBar.getMenus().add(menuFile);
        menuBar.getMenus().add(menuEdit);

        preLoadGuiSettings();

        if (openRecentMenu.getItems().size() > 0) {
            loadContentInfo(new File(openRecentMenu.getItems().get(0).getText()));
        }

        postLoadGuiSettings();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
