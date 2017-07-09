import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MainWIndow extends Application {
    private Stage primaryStage;

    private SplitPane splitPane;
    private ListView<String> tagsListView;
    private ListView<String> searchedPaths;
    private static final ObservableList<String> tagsList = FXCollections.observableArrayList();

    private Menu openRecentMenu;
    private Menu menuEdit;
    private MenuItem renameTagItem;
    private MenuItem editPathItem;
    private TextField byTagsSearch;


    private final String appName = "Content navigator";

    private File contenetInfoFile;
    private JSONObject tags;
    private JSONObject paths;

    private AddRenameTagDialog addRenameTagDialog;
    private AddEditPathDialog addPathDialog;


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
    }

    private void loadGuiSettings() {
        JSONObject guiJSON = JSONLoader.loadJSON(new File("guiSettings.json"));
        validateGuiSettings(guiJSON);

        JSONArray recentJSON = guiJSON.getJSONArray("recent");

        for (int i = recentJSON.length()-1; i >= 0; i--) {
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

            ArrayList<String> selectedTags = new ArrayList<>();
            JSONArray selectedTagsJSON = mainWindowJSON.getJSONArray("selectedTags");
            for(int i=0; i<selectedTagsJSON.length(); i++){
                selectedTags.add(selectedTagsJSON.getString(i));
                tagsListView.getSelectionModel().select(selectedTagsJSON.getString(i));
            }
            filterPathsBySelectedTags();
        })).play();
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
        for(String selectedTag: tagsListView.getSelectionModel().getSelectedItems()){
            selectedTags.put(selectedTag);
        }
        mainWindowJSON.put("selectedTags", selectedTags);

        JSONLoader.saveJSON(new File("guiSettings.json"), guiJSON);
    }
    //</GUI settings i/o>=====================


    private void addRecent(File fileIn, boolean saveIn) {
        for (MenuItem recentItem: openRecentMenu.getItems()) {
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

        if(openRecentMenu.getItems().size() > 15){
            openRecentMenu.getItems().remove(15);
        }

        if (saveIn) {
            saveGuiSettings();
        }
    }


    //<Content info i/o>======================
    private void validateContentInfo(JSONObject jsonIn) {
        if (!jsonIn.has("tags")) {
            jsonIn.put("tags", new JSONObject());
        }
    }

    private void loadContentInfo(File fileIn) {
        JSONObject json = JSONLoader.loadJSON(fileIn);
        validateContentInfo(json);

        tags = json.getJSONObject("tags");

        tagsList.removeAll(tagsList);
        tagsList.addAll(tags.keySet());
        searchedPaths.getItems().removeAll(searchedPaths.getItems());


        menuEdit.setDisable(false);

        contenetInfoFile = fileIn;
        primaryStage.setTitle(appName + " [" + fileIn.getName() + "]");
    }

    private void saveContentInfo(File fileIn) {
        JSONObject json = new JSONObject();
        json.put("tags", tags);
        JSONLoader.saveJSON(fileIn, json);
    }

    private void newContentInfo(File fileIn) {
        tags = new JSONObject();
        menuEdit.setDisable(false);
        searchedPaths.getItems().removeAll(searchedPaths.getItems());
        tagsListView.getItems().removeAll(tagsListView.getItems());

        contenetInfoFile = fileIn;
        primaryStage.setTitle(appName + " [" + fileIn.getName() + "]");
    }
    //</Content info i/o>=====================


    //<editing>===============================
    private void addTag(String tagNameIn) {
        tags.put(tagNameIn, new JSONArray());
        tagsList.add(tagNameIn);
        saveContentInfo(contenetInfoFile);
    }

    private void renameTag(String oldTagNameIn, String newTagNameIn) {
        tags.put(newTagNameIn, tags.get(oldTagNameIn));
        tags.remove(oldTagNameIn);

        for (int i = 0; i < tagsList.size(); i++) {
            if (tagsList.get(i).equals(oldTagNameIn)) {
                tagsList.set(i, oldTagNameIn);
                saveContentInfo(contenetInfoFile);
                return;
            }
        }
    }

    private void removeSelectedTags() {
        ObservableList<String> selectedTags = tagsListView.getSelectionModel().getSelectedItems();
        for(String selTag: selectedTags){
            tags.remove(selTag);
        }
        tagsList.removeAll(tagsListView.getSelectionModel().getSelectedItems());
        saveContentInfo(contenetInfoFile);
    }

    private void addPath(String pathIn, List<String> tagNamesIn) {
        for(String tag: tagNamesIn){
            tags.getJSONArray(tag).put(pathIn);
        }

        saveContentInfo(contenetInfoFile);
    }

    private void removePathFromTag(String tagNameIn, String pathIn) {
        JSONArray objects = tags.getJSONArray(tagNameIn);

        for (int i = 0; i < objects.length(); i++) {
            if (objects.getString(i).equals(pathIn)) {
                objects.remove(i);
                saveContentInfo(contenetInfoFile);
                return;
            }
        }
    }
    //</editing>==============================

    private List<String> getTagsByPath(String pathIn){
        List<String> tagsByPath = new ArrayList<>();

        for(String tagName: tags.keySet()){
            if(tags.getJSONArray(tagName).toList().contains(pathIn)){
                tagsByPath.add(tagName);
            }
        }

        return tagsByPath;
    }

    private List<String> getPathsByTags(List<String> tagNamesIn){
        if(tagNamesIn.size() == 0){
            return null;
        }

        ArrayList<String> filteredPaths = new ArrayList<>();
        ArrayList<String> filteredPathsTemp = new ArrayList<>();

        JSONArray paths = tags.getJSONArray(tagNamesIn.get(0));
        for(int i=0; i<paths.length(); i++){
            filteredPaths.add(paths.getString(i));
        }

        for(String tagName: tagNamesIn){
            filteredPathsTemp.clear();
            paths = tags.getJSONArray(tagName);
            for(int i=0; i<paths.length(); i++){
                if(filteredPaths.contains(paths.getString(i))){
                    filteredPathsTemp.add(paths.getString(i));
                }
            }
            filteredPaths.clear();
            filteredPaths.addAll(filteredPathsTemp);
        }

        return filteredPaths;
    }

    private void filterPathsBySelectedTags(){
        List<String> filteredPaths = getPathsByTags(tagsListView.getSelectionModel().getSelectedItems());
        if(filteredPaths != null){
            searchedPaths.getItems().removeAll(searchedPaths.getItems());
            searchedPaths.getItems().addAll(filteredPaths);

            String tagsStr = "";
            for(String tagName: tagsListView.getSelectionModel().getSelectedItems()){
                if(tagName.contains(" ")){
                    tagsStr += "\"" + tagName + "\" ";
                }else{
                    tagsStr += tagName + " ";
                }
            }

            byTagsSearch.setText(tagsStr);
        }

        if(tagsListView.getSelectionModel().getSelectedItems().size() == 1){
            renameTagItem.setDisable(false);
        }else{
            renameTagItem.setDisable(true);
        }
    }

    @Override
    public void start(Stage primaryStageIn) throws Exception {
        primaryStage = primaryStageIn;
        contenetInfoFile = null;

        Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add("modena_dark.css");

        primaryStage.setOnCloseRequest(event -> saveGuiSettings());
        primaryStage.setScene(scene);
        primaryStage.show();

        searchedPaths = (ListView) scene.lookup("#searchedPaths");
        searchedPaths.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(searchedPaths.getSelectionModel().getSelectedItems().size() == 1){
                editPathItem.setDisable(false);
            }else{
                editPathItem.setDisable(true);
            }
        });

        tagsListView = (ListView) scene.lookup("#tagsList");
        tagsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tagsListView.setItems(tagsList);
        tagsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> filterPathsBySelectedTags());

        MenuBar menuBar = (MenuBar) scene.lookup("#menuBar");

        splitPane = (SplitPane) scene.lookup("#tagsPathsSpliter");

        byTagsSearch = (TextField)scene.lookup("#byTagsSearch");

        addRenameTagDialog = new AddRenameTagDialog(new AddChangeInterface() {
            @Override
            public void add(String newValueIn) {
                addTag(newValueIn);
            }

            @Override
            public void change(String oldValueIn, String newValueIn) {
                renameTag(oldValueIn, newValueIn);
            }
        });
        addPathDialog = new AddEditPathDialog(tagsListView, new AddEditPathInterface() {
            @Override
            public void add(String path, List<String> tagNamesIn) {
                addPath(path, tagNamesIn);
            }

            @Override
            public void change(String path, List<String> tagNamesIn) {

            }
        });
        SettingsWindow settingsWindow = new SettingsWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All (*.*)", "*.*"));

        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Content file info (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(extensionFilter);
        fileChooser.setSelectedExtensionFilter(extensionFilter);

        //<menu file>====================
        MenuItem newItem = new MenuItem("New");
        newItem.setOnAction(event -> {
            fileChooser.setTitle("New content info file");
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                if(!file.getName().endsWith(".json")){
                    file = new File(file.getAbsoluteFile()+".json");
                }

                newContentInfo(file);
                addRecent(file, true);
            }
        });

        MenuItem openItem = new MenuItem("Open");
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
        settingsItem.setOnAction(event -> settingsWindow.show());

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

        //<tags listView ContextMenu>====================
        ContextMenu serchedPathsListViewContextMenu = new ContextMenu();
        MenuItem openPathItemContext = new MenuItem("Open");
        MenuItem openInFolderPathItemContext = new MenuItem("Open in folder");
        MenuItem editPathItemContext = new MenuItem("Edit Path");
        MenuItem addPathItemContext = new MenuItem("Add Path");

        serchedPathsListViewContextMenu.getItems().add(openPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(openInFolderPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsListViewContextMenu.getItems().add(editPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsListViewContextMenu.getItems().add(addPathItemContext);
        searchedPaths.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY){
                serchedPathsListViewContextMenu.show(tagsListView, event.getScreenX(), event.getScreenY());
            }else if(event.getButton() == MouseButton.PRIMARY){
                serchedPathsListViewContextMenu.hide();
            }
        });
        //</tags listView ContextMenu>===================

        //<serached paths listView ContextMenu>==========
        ContextMenu tagsListViewContextMenu = new ContextMenu();
        MenuItem renameTagItemContext = new MenuItem("Rename Tag");
        MenuItem removeTagItemContext = new MenuItem("Remove Tags");
        MenuItem addTagItemContext = new MenuItem("Add Tag");

        tagsListViewContextMenu.getItems().add(renameTagItemContext);
        tagsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        tagsListViewContextMenu.getItems().add(removeTagItemContext);
        tagsListViewContextMenu.getItems().add(addTagItemContext);
        tagsListView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY){
                tagsListViewContextMenu.show(tagsListView, event.getScreenX(), event.getScreenY());
            }else if(event.getButton() == MouseButton.PRIMARY){
                tagsListViewContextMenu.hide();
            }
        });
        //</serached paths listView ContextMenu>=========

        //<menu edit>====================================
        MenuItem addTagItem = new MenuItem("Add Tag");
        MenuItem addPathItem = new MenuItem("Add Path");
        renameTagItem = new MenuItem("Rename Tag");
        editPathItem = new MenuItem("Edit Path");
        MenuItem removeTagItem = new MenuItem("Remove Tags");

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
            addRenameTagDialog.setAddTag();
            try {
                addRenameTagDialog.setAddTag();
                addRenameTagDialog.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        EventHandler<ActionEvent> addPath = (event) -> {
            addPathDialog.setAddPath();
            addPathDialog.show();
        };

        EventHandler<ActionEvent> renameTag = (event) -> {
            if (tagsListView.getSelectionModel().getSelectedItems().size() == 1) {
                addRenameTagDialog.setRenameTag(tagsListView.getSelectionModel().getSelectedItem());
                addRenameTagDialog.show();
            }
        };

        EventHandler<ActionEvent> editPath = (event) -> {
            if(searchedPaths.getSelectionModel().getSelectedItems().size() == 1){
                editPathItemContext.setDisable(false);
                addPathDialog.setEditPath(searchedPaths.getSelectionModel().getSelectedItem(), getTagsByPath(searchedPaths.getSelectionModel().getSelectedItem()));
                addPathDialog.show();
            }else{
                editPathItemContext.setDisable(true);
            }
        };

        addTagItem.setOnAction(addTag);
        addPathItem.setOnAction(addPath);
        renameTagItem.setOnAction(renameTag);
        editPathItem.setOnAction(editPath);
        removeTagItem.setOnAction(event -> removeSelectedTags());


        openPathItemContext.setOnAction(event -> {
            if( Desktop.isDesktopSupported()){
                new Thread(() -> {
                    try {
                        File file = new File(searchedPaths.getSelectionModel().getSelectedItem());
                        Desktop.getDesktop().open(file);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });

        openInFolderPathItemContext.setOnAction(event -> {
            if( Desktop.isDesktopSupported()){
                new Thread(() -> {
                    try {
                        File file = new File(searchedPaths.getSelectionModel().getSelectedItem());
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

        addTagItemContext.setOnAction(addTag);
        renameTagItemContext.setOnAction(renameTag);
        removeTagItemContext.setOnAction(event -> removeSelectedTags());
        tagsListView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY){
                tagsListViewContextMenu.show(tagsListView, event.getScreenX(), event.getScreenY());

                int selectedCount = tagsListView.getSelectionModel().getSelectedItems().size();
                if(selectedCount == 1){
                    renameTagItem.setDisable(false);
                    renameTagItemContext.setDisable(false);
                }else{
                    renameTagItem.setDisable(true);
                    renameTagItemContext.setDisable(true);
                }
                if(selectedCount != 0){
                    removeTagItem.setDisable(false);
                    removeTagItemContext.setDisable(false);
                }else{
                    removeTagItem.setDisable(true);
                    removeTagItemContext.setDisable(true);
                }
            }else if(event.getButton() == MouseButton.PRIMARY){
                tagsListViewContextMenu.hide();
            }
        });
        //</menu events>====================================

        menuBar.getMenus().add(menuFile);
        menuBar.getMenus().add(menuEdit);

        loadGuiSettings();

        if (openRecentMenu.getItems().size() > 0) {
            loadContentInfo(new File(openRecentMenu.getItems().get(0).getText()));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
