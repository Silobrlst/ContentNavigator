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
import javafx.util.Callback;
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

    private Map<String, Tag> tagsMap;

    private File contenetInfoFile;
    private JSONObject tagsJSON;
    private JSONObject paths;

    private AddRenameTagDialog addRenameTagDialog;
    private AddEditPathDialog addEditPathDialog;

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
                tagsListView.getSelectionModel().select(new Tag(selectedTagsJSON.getString(i)));
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
        if (!jsonIn.has("tagsJSON")) {
            jsonIn.put("tagsJSON", new JSONObject());
        }
    }

    private void loadContentInfo(File fileIn) {
        JSONObject json = JSONLoader.loadJSON(fileIn);
        validateContentInfo(json);

        tagsJSON = json.getJSONObject("tags");

        tagsListView.getItems().removeAll(tagsListView.getItems());

        for (String tagName : tagsJSON.keySet()) {
            Tag tag = new Tag(tagName);
            tagsMap.put(tagName, tag);
            tagsListView.getItems().add(tag);

            JSONArray paths = tagsJSON.getJSONArray(tagName);
            for(int i=0; i<paths.length(); i++){
                tag.setPath(new Path(paths.getString(i)));
            }
        }

        tagsListView.getItems().sort(Comparator.naturalOrder());
        searchedPaths.getItems().removeAll(searchedPaths.getItems());

        menuEdit.setDisable(false);

        contenetInfoFile = fileIn;
        primaryStage.setTitle(appName + " [" + fileIn.getName() + "]");
    }

    private void saveContentInfo(File fileIn) {
        JSONObject json = new JSONObject();
        json.put("tagsJSON", tagsJSON);
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
    private void addTag(String tagNameIn) {
        tagsJSON.put(tagNameIn, new JSONArray());
        tagsListView.getItems().add(new Tag(tagNameIn));
        tagsListView.getItems().sort(Comparator.naturalOrder());
        saveContentInfo(contenetInfoFile);

        tagsListView.getSelectionModel().clearSelection();
        tagsListView.getSelectionModel().select(new Tag(tagNameIn));
        tagsListView.requestFocus();
    }

    private void renameTag(String oldTagNameIn, String newTagNameIn) {
        tagsJSON.put(newTagNameIn, tagsJSON.get(oldTagNameIn));
        tagsJSON.remove(oldTagNameIn);

        for (Tag tag : tagsListView.getItems()) {
            if (tag.equals(oldTagNameIn)) {
                tag.setName(newTagNameIn);
                saveContentInfo(contenetInfoFile);
                return;
            }
        }
    }

    private void removeSelectedTags() {
        ObservableList<Tag> selectedTags = tagsListView.getSelectionModel().getSelectedItems();
        for (Tag selTag : selectedTags) {
            tagsJSON.remove(selTag.getName());
        }
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

    private void addPath(String pathIn, List<Tag> tagsIn) {
        for (Tag tag : tagsIn) {
            JSONArray tagJSON = tagsJSON.getJSONArray(tag.getName());
            boolean add = true;

            for (int i = 0; i < tagJSON.length(); i++) {
                if (tagJSON.getString(i).equals(pathIn)) {
                    add = false;
                }
            }

            if (add) {
                tagsJSON.getJSONArray(tag.getName()).put(pathIn);
            }
        }

        saveContentInfo(contenetInfoFile);
        filterPathsBySelectedTags();
    }

    private void removeSelectedPathsFromSelectedTags() {
        for (Tag selectedTag : tagsListView.getSelectionModel().getSelectedItems()) {
            for (Path selectedPath : searchedPaths.getSelectionModel().getSelectedItems()) {
                JSONArray tagJSON = tagsJSON.getJSONArray(selectedTag.getName());
                for (int i = 0; i < tagJSON.length(); i++) {
                    if (selectedPath.equals(tagJSON.getString(i))) {
                        tagJSON.remove(i);
                    }
                }
            }
        }
        saveContentInfo(contenetInfoFile);

        searchedPaths.getItems().removeAll(searchedPaths.getSelectionModel().getSelectedItems());
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

    private List<Tag> getTagsByPath(Path pathIn) {
        List<Tag> tagsByPath = new ArrayList<>();

        for (Tag tag : tagsMap.values()) {
            if (tagsJSON.getJSONArray(tag.getName()).toList().contains(pathIn)) {
                tagsByPath.add(tag);
            }
        }

        return tagsByPath;
    }

    private List<Path> getPathsByTags(List<Tag> tagsIn) {
        if (tagsIn.size() == 0) {
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

        tagsMap = new HashMap<>();

        Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add("modena_dark.css");

        primaryStage.setOnCloseRequest(event -> saveGuiSettings());
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("file:icon.png"));
        primaryStage.show();

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

        MenuBar menuBar = (MenuBar) scene.lookup("#menuBar");

        splitPane = (SplitPane) scene.lookup("#tagsPathsSpliter");

        byTagsSearch = (TextField) scene.lookup("#byTagsSearch");

        addRenameTagDialog = new AddRenameTagDialog(new AddRenameInterface() {
            @Override
            public void add(String newValueIn) {
                addTag(newValueIn);
            }

            @Override
            public void change(String oldValueIn, String newValueIn) {
                renameTag(oldValueIn, newValueIn);
            }
        });
        addEditPathDialog = new AddEditPathDialog(tagsListView, new AddEditPathInterface() {
            @Override
            public void add(String path, List<Tag> tagsIn) {
                addPath(path, tagsIn);
            }

            @Override
            public void change(String path, List<Tag> tagsIn) {
                removeSelectedPathsFromSelectedTags();
                for (Tag tag : tagsIn) {
                    tagsJSON.getJSONArray(tag.getName()).put(path);
                }
                saveContentInfo(contenetInfoFile);
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

        //<serached paths listView ContextMenu>==========
        ContextMenu serchedPathsListViewContextMenu = new ContextMenu();
        MenuItem openPathItemContext = new MenuItem("Open");
        MenuItem openInFolderPathItemContext = new MenuItem("Open in folder");
        MenuItem editPathItemContext = new MenuItem("Edit Path");
        MenuItem addPathItemContext = new MenuItem("Add Path");
        MenuItem removePathsFromSelectedTagsItemContext = new MenuItem("Remove Paths from selected tagsJSON");

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
        MenuItem addPathSelectedWithTagsItemContext = new MenuItem("Add Path with selected tagsJSON");

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
                addEditPathDialog.setEditPath(searchedPaths.getSelectionModel().getSelectedItem(), getTagsByPath(searchedPaths.getSelectionModel().getSelectedItem()));
                addEditPathDialog.show();
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
