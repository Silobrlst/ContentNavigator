import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
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
import javafx.scene.image.Image;
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
    private ListView<Tag> tagsListView;
    private ListView<Path> searchedPaths;

    private Menu openRecentMenu;
    private Menu menuEdit;
    private MenuItem renameTagItem;
    private MenuItem editPathItem;
    private TextField byTagsSearch;

    private final String appName = "Content navigator";

    private Tags tags;
    private Paths paths;

    private File contenetInfoFile;
    private JSONObject tagsJSON;
    //private JSONObject pathsJSON;

    private AddRenameTagDialog addRenameTagDialog;
    private AddEditPathDialog addEditPathDialog;

    //<string names>==========================
    private static final String tagsJSONName = "tags";
    private static final String pathsJSONName = "paths";
    private static final String nameJSONName = "name";
    //</string names>=========================

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

            JSONArray selectedTagsJSON = mainWindowJSON.getJSONArray("selectedTags");
            for (int i = 0; i < selectedTagsJSON.length(); i++) {
                //tags.newTagWithoutNotifing();
                //tagsListView.getSelectionModel().select(new Tag(selectedTagsJSON.getString(i)));
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
        for (Tag selectedTag : tagsListView.getSelectionModel().getSelectedItems()) {
            selectedTags.put(selectedTag.getName());
        }
        mainWindowJSON.put("selectedTags", selectedTags);

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

    private void loadContentInfo(File fileIn) {
        JSONObject json = JSONLoader.loadJSON(fileIn);
        validateContentInfo(json);

        tagsJSON = json.getJSONObject(tagsJSONName);
        JSONObject pathsJSON = json.getJSONObject(pathsJSONName);

        tagsListView.getItems().clear();

        for (String tagName : tagsJSON.keySet()) {
            Tag tag = tags.newTagWithoutNotifing(tagName);
            tagsListView.getItems().add(tag);
        }

        for(String pathJSONKey: pathsJSON.keySet()){
            Path path;
            JSONObject pathJSON = pathsJSON.getJSONObject(pathJSONKey);

            if(pathJSON.getString(nameJSONName).length() > 0){
                path = paths.newPathWithoutNotifing(pathJSONKey, pathJSON.getString(nameJSONName));
            }else{
                path = paths.newPathWithoutNotifing(pathJSONKey);
            }

            for(String tagJSON: pathJSON.getJSONObject(tagsJSONName).keySet()){
                Tag tag = tags.getTag(tagJSON);

                if(tag != null){
                    path.addTagWithoutNotifing(tag);
                }
            }
        }

        tagsListView.getItems().sort(Comparator.naturalOrder());
        searchedPaths.getItems().clear();

        menuEdit.setDisable(false);

        contenetInfoFile = fileIn;
        primaryStage.setTitle(appName + " [" + fileIn.getName() + "]");
    }

    private void saveContentInfo(File fileIn) {
        JSONObject json = new JSONObject();

        JSONObject tagsJSON = new JSONObject();
        json.put(tagsJSONName, tagsJSON);

        JSONObject pathsJSON = new JSONObject();
        json.put(pathsJSONName, pathsJSON);

        for(Tag tag: tags){
            JSONObject tagJSON = new JSONObject();
            tagsJSON.put(tag.getName(), tagJSON);

            for(Path path: tag.getPaths()){
                tagJSON.put(path.getPath(), "");
            }
        }

        for(Path path: paths){
            JSONObject pathJSON = new JSONObject();
            pathsJSON.put(path.getPath(), pathJSON);

            JSONObject pathTagsJSON = new JSONObject();
            pathJSON.put(tagsJSONName, pathTagsJSON);
            pathJSON.put(nameJSONName, path.getName());

            for(Tag tag: path.getTags()){
                pathTagsJSON.put(tag.getName(), "");
            }
        }

        JSONLoader.saveJSON(fileIn, json);
    }

    private void newContentInfo(File fileIn) {
        tagsJSON = new JSONObject();
        menuEdit.setDisable(false);
        searchedPaths.getItems().removeAll(searchedPaths.getItems());
        tagsListView.getItems().removeAll(tagsListView.getItems());

        contenetInfoFile = fileIn;
        primaryStage.setTitle(appName + " [" + fileIn.getName() + "]");
    }
    //</Content info i/o>=====================

    //<editing>===============================
    private void removeSelectedTags() {
        ObservableList<Tag> selectedTags = tagsListView.getSelectionModel().getSelectedItems();
        for (Tag selTag: selectedTags) {
            tags.removeTag(selTag);
        }

        System.out.println(tagsListView.getSelectionModel().getSelectedItems());

        tagsListView.getItems().removeAll(tagsListView.getSelectionModel().getSelectedItems());
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
        for (Path selectedPath: searchedPaths.getSelectionModel().getSelectedItems()) {
            selectedPath.removeTags(tagsListView.getSelectionModel().getSelectedItems());
        }
        searchedPaths.getItems().removeAll(searchedPaths.getSelectionModel().getSelectedItems());
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

    private List<Path> getPathsByTags(List<Tag> tagsIn) {
        if (tagsIn.size() == 0) {
            return null;
        }

        if(tagsIn.get(0) == null){
            return null;
        }

        ArrayList<Path> filteredPaths = new ArrayList<>();
        ArrayList<Path> filteredPathsTemp = new ArrayList<>();

        filteredPaths.addAll(tagsIn.get(0).getPaths());

        for (Tag tag : tagsIn) {
            filteredPathsTemp.clear();

            for (Path path : tag.getPaths()) {
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
        List<Path> filteredPaths = getPathsByTags(tagsListView.getSelectionModel().getSelectedItems());

        if (filteredPaths != null) {
            searchedPaths.getItems().removeAll(searchedPaths.getItems());
            searchedPaths.getItems().addAll(filteredPaths);
            searchedPaths.getItems().sort(Comparator.naturalOrder());

            String tagsStr = "";
            for (Tag tag : tagsListView.getSelectionModel().getSelectedItems()) {
                if (tag.getName().contains(" ")) {
                    tagsStr += "\"" + tag.getName() + "\" ";
                } else {
                    tagsStr += tag.getName() + " ";
                }
            }

            byTagsSearch.setText(tagsStr);
        }

        if (tagsListView.getSelectionModel().getSelectedItems().size() == 1) {
            renameTagItem.setDisable(false);
        } else {
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
        primaryStage.getIcons().add(new Image("file:icon.png"));
        primaryStage.show();

        tags = new Tags();
        paths = new Paths();

        //<paths table>==========================
        searchedPaths = (ListView) scene.lookup("#searchedPaths");
        searchedPaths.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (searchedPaths.getSelectionModel().getSelectedItems().size() == 1) {
                editPathItem.setDisable(false);
            } else {
                editPathItem.setDisable(true);
            }
        });
        searchedPaths.setCellFactory(param -> new ListCell<Path>() {
            @Override
            protected void updateItem(Path pathIn, boolean empty) {
                super.updateItem(pathIn, empty);
                if (pathIn != null) {
                    setText(pathIn.getPath());
                } else {
                    setText("");   // <== clear the now empty cell.
                }
            }
        });

        paths.addListener(new EmptyPathListener(){
            @Override
            public void created(Path pathIn) {
                saveContentInfo(contenetInfoFile);
                filterPathsBySelectedTags();
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
        tagsListView = (ListView) scene.lookup("#tagsList");
        tagsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tagsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> filterPathsBySelectedTags());
        tagsListView.setCellFactory(param -> new ListCell<Tag>() {
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

        tags.addTagObserver(new EmptyTagListener(){
            @Override
            public void created(Tag tagIn) {
                tagsListView.getItems().add(tagIn);
                tagsListView.getItems().sort(Comparator.naturalOrder());

                tagsListView.getSelectionModel().clearSelection();
                tagsListView.getSelectionModel().select(tagIn);
                tagsListView.requestFocus();

                saveContentInfo(contenetInfoFile);
            }

            @Override
            public void renamed(Tag tagIn) {
                tagsListView.getItems().sort(Comparator.naturalOrder());
                tagsListView.refresh();
                saveContentInfo(contenetInfoFile);
            }
        });
        //</tags list>===========================

        MenuBar menuBar = (MenuBar) scene.lookup("#menuBar");

        splitPane = (SplitPane) scene.lookup("#tagsPathsSpliter");

        byTagsSearch = (TextField) scene.lookup("#byTagsSearch");

        addRenameTagDialog = new AddRenameTagDialog(primaryStage, tags);
        addEditPathDialog = new AddEditPathDialog(primaryStage, paths, tags);
        SettingsWindow settingsWindow = new SettingsWindow(primaryStage);

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
                if (!file.getName().endsWith(".json")) {
                    file = new File(file.getAbsoluteFile() + ".json");
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
        MenuItem openInFolderPathItemContext = new MenuItem("Open in folder");
        MenuItem editPathItemContext = new MenuItem("Edit Path");
        MenuItem addPathItemContext = new MenuItem("Add Path");
        MenuItem removePathsFromSelectedTagsItemContext = new MenuItem("Remove Paths from selected tags");

        serchedPathsListViewContextMenu.getItems().add(openPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(openInFolderPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsListViewContextMenu.getItems().add(editPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsListViewContextMenu.getItems().add(addPathItemContext);
        serchedPathsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsListViewContextMenu.getItems().add(removePathsFromSelectedTagsItemContext);
        searchedPaths.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                serchedPathsListViewContextMenu.show(tagsListView, event.getScreenX(), event.getScreenY());
            } else if (event.getButton() == MouseButton.PRIMARY) {
                serchedPathsListViewContextMenu.hide();
            }
        });
        //</serached paths listView ContextMenu>=========

        //<tagsJSON listView ContextMenu>====================
        ContextMenu tagsListViewContextMenu = new ContextMenu();
        MenuItem renameTagItemContext = new MenuItem("Rename Tag");
        MenuItem removeTagItemContext = new MenuItem("Remove Tags");
        MenuItem addTagItemContext = new MenuItem("Add Tag");
        MenuItem addPathSelectedWithTagsItemContext = new MenuItem("Add Path with selected tags");

        tagsListViewContextMenu.getItems().add(renameTagItemContext);
        tagsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        tagsListViewContextMenu.getItems().add(removeTagItemContext);
        tagsListViewContextMenu.getItems().add(addTagItemContext);
        tagsListViewContextMenu.getItems().add(new SeparatorMenuItem());
        tagsListViewContextMenu.getItems().add(addPathSelectedWithTagsItemContext);
        tagsListView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                tagsListViewContextMenu.show(tagsListView, event.getScreenX(), event.getScreenY());
            } else if (event.getButton() == MouseButton.PRIMARY) {
                tagsListViewContextMenu.hide();
            }
        });
        //</tagsJSON listView ContextMenu>===================

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
            addEditPathDialog.setAddPath();
            addEditPathDialog.show();
        };

        EventHandler<ActionEvent> renameTag = (event) -> {
            if (tagsListView.getSelectionModel().getSelectedItems().size() == 1) {
                addRenameTagDialog.setRenameTag(tagsListView.getSelectionModel().getSelectedItem());
                addRenameTagDialog.show();
            }
        };

        EventHandler<ActionEvent> editPath = (event) -> {
            if (searchedPaths.getSelectionModel().getSelectedItems().size() == 1) {
                editPathItemContext.setDisable(false);
                addEditPathDialog.setEditPath(searchedPaths.getSelectionModel().getSelectedItem());
                addEditPathDialog.showAndWait();
            } else {
                editPathItemContext.setDisable(true);
            }
        };

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
            addEditPathDialog.setAddPath(tagsListView.getSelectionModel().getSelectedItems());
            addEditPathDialog.show();
        });
        tagsListView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                tagsListViewContextMenu.show(tagsListView, event.getScreenX(), event.getScreenY());

                int selectedCount = tagsListView.getSelectionModel().getSelectedItems().size();
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

        tagsListView.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedTagsConfirm();
            }
        });

        searchedPaths.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedPathsFromSelectedTagsConfirm();
            }
        });

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
