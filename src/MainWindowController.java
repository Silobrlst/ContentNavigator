import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
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
import javafx.scene.input.*;
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
    private TableView<Path> pathsTable;
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
    private Button searchByName;
    @FXML
    private Button searchByPath;
    @FXML
    private Button searchByTags;
    @FXML
    private Button searchByAll;
    @FXML
    private TextField byNameSearch;
    @FXML
    private TextField byPathSearch;
    @FXML
    private TextField byTagsSearch;
    @FXML
    private Label searchValidation;

    private String openInFolderCommand;
    private String openInFolderArgument;

    private final String appName = "Tag file navigator";

    private final Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);

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
    private static final File guiSettings = new File("guiSettings.json");
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

    private File tagsFile;

    //проверяем что Enter был нажат пока фокус был на таблице
    private boolean enterPressed = false;

    public MainWindowController(){
        styleFileName = "";
    }

    public void setTagsStage(Stage stageIn, FXMLLoader loaderIn)throws Exception{
        tagsFile = null;

        stage = stageIn;
        stage.setOnCloseRequest(event -> saveAppGuiSettings());
        stage.setScene(new Scene(loaderIn.getRoot()));
        stage.getIcons().add(new Image("file:images/appIcon.png"));
        stage.show();
        stage.getScene().getRoot().setId("mainWindowRoot");

        paths = new Paths();
        paths.addListener(new EmptyPathListener(){
            @Override
            public void created(Path pathIn) {
                saveTagsFile(tagsFile);
            }

            @Override
            public void renamed(Path pathIn) {
                saveTagsFile(tagsFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void removedTag(Path pathIn, Tag tagIn) {
                saveTagsFile(tagsFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void removedTags(Path pathIn, Collection<Tag> tagsIn) {
                saveTagsFile(tagsFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void addedTags(Path pathIn, Collection<Tag> tagsIn) {
                saveTagsFile(tagsFile);
                filterPathsBySelectedTags();
                pathsTable.getSelectionModel().select(pathIn);
                pathsTable.refresh();
                pathsTable.requestFocus();
                pathsTable.scrollTo(pathIn);
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
                tagsTree.scrollTo(tagsTree.getRow(tagTreeItem));

                getTreeItemByTag(parentIn).getChildren().sort((o1, o2) -> {
                    return o1.getValue().getName().compareTo(o2.getValue().getName());
                });
                tagsTree.refresh();

                saveTagsFile(tagsFile);
            }

            @Override
            public void removedPathFromTag(Tag tagIn, Path pathIn) {
                filterPathsBySelectedTags();
            }

            @Override
            public void editedTag(Tag tagIn) {
                getTreeItemByTag(tagIn.getParent()).getChildren().sort((o1, o2) -> {
                    return o1.getValue().getName().compareTo(o2.getValue().getName());
                });
                tagsTree.refresh();
                saveTagsFile(tagsFile);
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
        //</tagsTree>================================

        //<pathsTable>============================
        pathsTable.setDisable(true);
        pathsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        pathsTable.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedTagsConfirm();
            }
        });
        pathsTable.setRowFactory( tv -> {
            TableRow<Path> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    Path path = row.getItem();
                    desktopOpenFile(new File(path.getPath()));
                }
            });
            return row;
        });
        //</pathsTable>===========================

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

        MainWindowController mainWindowControllerContext = this;
        //<SettingsDialogController>============================
        loader = new FXMLLoader(getClass().getResource("SettingsDialog.fxml"));
        loader.load();
        settingsDialogController = loader.getController();
        settingsDialogController.setParentStage(stage, loader, this, () -> {
            openInFolderCommand = settingsDialogController.getOpenInFolderCommand();
            openInFolderArgument = settingsDialogController.getOpenInFolderArgument();
            mainWindowControllerContext.setStyle(settingsDialogController.getSelectedStyle());
            saveSettings();
        });
        //</SettingsDialogController>===========================

        searchByName.setGraphic(new ImageView(searchIcon));
        searchByPath.setGraphic(new ImageView(searchIcon));
        searchByTags.setGraphic(new ImageView(searchIcon));
        searchByAll.setGraphic(new ImageView(searchIcon));

        searchByName.setOnAction(event -> filterPathsByNameQuery());
        searchByPath.setOnAction(event -> filterPathsByPathQuery());
        searchByTags.setOnAction(event -> filterPathsBySelectedTags());

        byNameSearch.setOnAction(event -> filterPathsByNameQuery());
        byPathSearch.setOnAction(event -> filterPathsByPathQuery());
        byTagsSearch.setOnAction(event -> filterPathsBySelectedTags());

        initMenu();

        loadSettings();
        loadAppGuiSettings();

        if (openRecentMenu.getItems().size() > 0) {
            loadTagsFile(new File(openRecentMenu.getItems().get(0).getText()));
        }
    }

    @FXML
    public void initialize() {
        searchedPathsName.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
        searchedPathsPath.setCellValueFactory(cellData -> cellData.getValue().getPathProperty());
    }

    private void removeSelectedTags() {
        Collection<TreeItem<Tag>> selectedTags = tagsTree.getSelectionModel().getSelectedItems();

        for (TreeItem<Tag> selectedTag: selectedTags) {
            if(selectedTag != tagsTree.getRoot()){
                tags.removeTag(selectedTag.getValue());
            }
        }

        for (TreeItem<Tag> selectedTag: selectedTags) {
            if(selectedTag != tagsTree.getRoot()){
                selectedTag.getParent().getChildren().removeAll(selectedTags);
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
        if (pathsTable.getSortOrder().size()>0) {
            sortcolumn = (TableColumn<Path, String>) pathsTable.getSortOrder().get(0);
            st = sortcolumn.getSortType();
        }
        if (sortcolumn!=null) {
            pathsTable.getSortOrder().add(sortcolumn);
            sortcolumn.setSortType(st);
            sortcolumn.setSortable(true); // This performs a sort
        }
    }


    private void validateSettings(JSONObject settingsJSONIn){
        String os = System.getProperty("os.name").toLowerCase();

        if(!settingsJSONIn.has("openInFolderCommand")){
            if(os.contains("win")){
                settingsJSONIn.put("openInFolderCommand", "explorer");
            }else if(os.contains("nix") || os.contains("nux") || os.contains("aix")){
                settingsJSONIn.put("openInFolderCommand", "nautilus");
            }else{
                settingsJSONIn.put("openInFolderCommand", "nautilus");
            }
        }
        if(!settingsJSONIn.has("openInFolderArgument")){
            if(os.contains("win")){
                settingsJSONIn.put("openInFolderArgument", "/select,");
            }else if(os.contains("nix") || os.contains("nux") || os.contains("aix")){
                settingsJSONIn.put("openInFolderArgument", "");
            }else{
                settingsJSONIn.put("openInFolderArgument", "");
            }
        }

        if(!settingsJSONIn.has("style")){
            settingsJSONIn.put("style", "default");
        }
    }

    private void loadSettings(){
        JSONObject settingsJSON = JSONLoader.loadJSON(new File("settings.json"));
        validateSettings(settingsJSON);

        openInFolderCommand = settingsJSON.getString("openInFolderCommand");
        openInFolderArgument = settingsJSON.getString("openInFolderArgument");

        String style = settingsJSON.getString("style");
        if(new File("styles/" + style).exists() && style.length() > 0){
            setStyle("file:styles/" + style);
        }else{
            setStyle("default");
        }
    }

    private void saveSettings(){
        JSONObject settingsJSON = new JSONObject();
        validateSettings(settingsJSON);

        settingsJSON.put("openInFolderCommand", openInFolderCommand);
        settingsJSON.put("openInFolderArgument", openInFolderArgument);

        if(!styleFileName.equals("default")){
            settingsJSON.put("style", styleFileName.replace("file:styles/", ""));
        }

        JSONLoader.saveJSON(new File("settings.json"), settingsJSON);
    }

    //<GUI settings i/o>======================
    private void validateGuiSettings(JSONObject appGuiSettingsJsonIn) {
        if (!appGuiSettingsJsonIn.has("recent")) {
            appGuiSettingsJsonIn.put("recent", new JSONArray());
        }

        if (!appGuiSettingsJsonIn.has("mainWindow")) {
            appGuiSettingsJsonIn.put("mainWindow", new JSONObject());
        }

        JSONObject mainWindowJSON = appGuiSettingsJsonIn.getJSONObject("mainWindow");
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

        if (!appGuiSettingsJsonIn.has(searchedPathsNameWidth)) {
            appGuiSettingsJsonIn.put(searchedPathsNameWidth, 0.d);
        }
        if (!appGuiSettingsJsonIn.has(searchedPathsPathWidth)) {
            appGuiSettingsJsonIn.put(searchedPathsPathWidth, 0.d);
        }
    }

    private void validateTagsFileGuiSettings(JSONObject tagsFileGuiSettingsJsonIn) {
        if (!tagsFileGuiSettingsJsonIn.has("selectedTags")) {
            tagsFileGuiSettingsJsonIn.put("selectedTags", new JSONArray());
        }
        if (!tagsFileGuiSettingsJsonIn.has("tagsExpand")) {
            tagsFileGuiSettingsJsonIn.put("tagsExpand", new JSONObject());
        }
    }

    private void loadAppGuiSettings() {
        JSONObject appGuiJson = JSONLoader.loadJSON(new File("guiSettings.json"));
        validateGuiSettings(appGuiJson);

        JSONArray recentJSON = appGuiJson.getJSONArray("recent");

        for (int i = recentJSON.length() - 1; i >= 0; i--) {
            addRecent(new File(recentJSON.getString(i)), false);
        }

        JSONObject mainWindowJSON = appGuiJson.getJSONObject("mainWindow");
        if (mainWindowJSON.getDouble("width") > 0) {
            stage.setWidth(mainWindowJSON.getDouble("width"));
        }
        if (mainWindowJSON.getDouble("height") > 0) {
            stage.setHeight(mainWindowJSON.getDouble("height"));
        }

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            tagsPathsSpliter.setDividerPositions(mainWindowJSON.getDouble("tagsPathsSplitPosition"));
            if(appGuiJson.getDouble(searchedPathsNameWidth) != 0.d){
                searchedPathsName.setPrefWidth(appGuiJson.getDouble(searchedPathsNameWidth));
            }
            if(appGuiJson.getDouble(searchedPathsPathWidth) != 0.d){
                searchedPathsPath.setPrefWidth(appGuiJson.getDouble(searchedPathsPathWidth));
            }
        })).play();

        pathsTable.getSortOrder().add(searchedPathsName);
        searchedPathsName.setSortType(TableColumn.SortType.ASCENDING);
        searchedPathsName.setSortable(true);
    }

    private void loadTagsFileGuiSettings(){
        JSONObject tagsFileGuiSettingsJson = JSONLoader.loadJSON(new File(tagsFile.getName() + ".guiSettings.json"));
        validateTagsFileGuiSettings(tagsFileGuiSettingsJson);

        JSONObject tagsExpandJSON = tagsFileGuiSettingsJson.getJSONObject("tagsExpand");
        for(String tagId: tagsExpandJSON.keySet()){
            if(tags.getTagById(tagId) != null){
                getTreeItemByTag(tags.getTagById(tagId)).setExpanded(tagsExpandJSON.getBoolean(tagId));
            }
        }

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            JSONArray selectedTagsJSON = tagsFileGuiSettingsJson.getJSONArray("selectedTags");
            for (int i = 0; i < selectedTagsJSON.length(); i++) {
                tagsTree.getSelectionModel().select(getTreeItemByTag(tags.getTagById(selectedTagsJSON.getString(i))));
            }

            filterPathsBySelectedTags();
            tagsTree.requestFocus();
        })).play();
    }

    private void getAllTagItems(TreeItem<Tag> parentTagItemIn, List<TreeItem<Tag>> tagItemsOut){
        if(parentTagItemIn != tagsTree.getRoot()){
            tagItemsOut.add(parentTagItemIn);
        }
        for(TreeItem<Tag> tagItem: parentTagItemIn.getChildren()){
            getAllTagItems(tagItem, tagItemsOut);
        }
    }

    private List<TreeItem<Tag>> getAllTagItems(){
        ArrayList<TreeItem<Tag>> tagItems = new ArrayList<>();
        getAllTagItems(tagsTree.getRoot(), tagItems);
        return tagItems;
    }

    private void saveAppGuiSettings() {
        saveTagsFileGuiSettings();

        //загружаем из файла данные, т.к. другие окна тоже сохраняют свои данные в этот файл
        JSONObject appGuiJson = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(appGuiJson);

        JSONArray recentJSON = new JSONArray();
        for (int i = 0; i < openRecentMenu.getItems().size(); i++) {
            recentJSON.put(openRecentMenu.getItems().get(i).getText());
        }

        appGuiJson.put("recent", recentJSON);
        JSONObject mainWindowJSON = appGuiJson.getJSONObject("mainWindow");
        mainWindowJSON.put("x", stage.getX());
        mainWindowJSON.put("y", stage.getY());
        mainWindowJSON.put("width", stage.getWidth());
        mainWindowJSON.put("height", stage.getHeight());
        mainWindowJSON.put("tagsPathsSplitPosition", tagsPathsSpliter.getDividerPositions()[0]);

        appGuiJson.put(searchedPathsNameWidth, searchedPathsName.getWidth());
        appGuiJson.put(searchedPathsPathWidth, searchedPathsPath.getWidth());

        JSONLoader.saveJSON(guiSettings, appGuiJson);
    }

    private void saveTagsFileGuiSettings() {
        JSONObject tagsFileGuiSettingsJson = new JSONObject();
        validateTagsFileGuiSettings(tagsFileGuiSettingsJson);

        JSONArray selectedTagsJSON = new JSONArray();
        for (TreeItem<Tag> selectedTagItem : tagsTree.getSelectionModel().getSelectedItems()) {
            if(selectedTagItem != null){
                selectedTagsJSON.put(tags.getTagId(selectedTagItem.getValue()));
            }
        }
        tagsFileGuiSettingsJson.put("selectedTags", selectedTagsJSON);

        JSONObject tagsExpandJSON = new JSONObject();
        for(TreeItem<Tag> tagTreeItem: getAllTagItems()){
            tagsExpandJSON.put(tags.getTagId(tagTreeItem.getValue()), tagTreeItem.isExpanded());
        }
        tagsFileGuiSettingsJson.put("tagsExpand", tagsExpandJSON);

        JSONLoader.saveJSON(new File(tagsFile.getName() + ".guiSettings.json"), tagsFileGuiSettingsJson);
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
            loadTagsFile(file);
            addRecent(file, true);
        });

        openRecentMenu.getItems().add(0, menuItem);

        if (openRecentMenu.getItems().size() > 15) {
            openRecentMenu.getItems().remove(15);
        }

        if (saveIn) {
            saveAppGuiSettings();
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
            pathsTable.getItems().clear();
            pathsTable.getItems().addAll(filteredPaths);
            pathsTable.getItems().sort(Comparator.naturalOrder());

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

        searchValidation.setText("");
    }

    private void filterPathsByNameQuery(){
        pathsTable.getItems().clear();

        if(byNameSearch.getText().length() > 0){
            for(Path path: paths){
                if(path.getName().toLowerCase().matches(".*" + byNameSearch.getText().toLowerCase() + ".*")){
                    pathsTable.getItems().add(path);
                }
            }

            if(pathsTable.getItems().size() == 0){
                searchValidation.setText("No matched paths");
            }else{
                searchValidation.setText("");
            }

            pathsTable.getItems().sort(Comparator.naturalOrder());
            reSortPaths();
        }else{
            searchValidation.setText("Name search query is empty");
        }
    }

    private void filterPathsByPathQuery(){
        pathsTable.getItems().clear();

        if(byPathSearch.getText().length() > 0){
            for(Path path: paths){
                if(path.getPath().toLowerCase().matches(".*" + byPathSearch.getText().toLowerCase() + ".*")){
                    pathsTable.getItems().add(path);
                }
            }

            if(pathsTable.getItems().size() == 0){
                searchValidation.setText("No matched paths");
            }else{
                searchValidation.setText("");
            }

            pathsTable.getItems().sort(Comparator.naturalOrder());
            reSortPaths();
        }else{
            searchValidation.setText("Path search query is empty");
        }
    }

    //<Tag file i/o>==========================
    private void validateTagsFile(JSONObject jsonIn) {
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

        getTreeItemByTag(parentIn).getChildren().sort((o1, o2) -> {
            return o1.getValue().getName().compareTo(o2.getValue().getName());
        });
    }

    private void loadTagsFile(File fileIn) {
        if(!fileIn.exists()){
            return;
        }

        if(tagsFile != null){
            saveTagsFileGuiSettings();
        }

        JSONObject json = JSONLoader.loadJSON(fileIn);
        validateTagsFile(json);

        tagsTree.getRoot().getChildren().clear();
        pathsTable.getItems().clear();
        tags.getChildren().clear();
        paths.clear();

        //<load tags>================================
        JSONObject tagsJSON = json.getJSONObject(tagsJSONName);
        for (String tagName : tagsJSON.keySet()) {
            Tag tag = tags.newTagWithoutNotifing(tagName, tags);
            TreeItem<Tag> tagItem = new TreeItem<>(tag);
            tagItem.setExpanded(true);
            tagsTree.getRoot().getChildren().add(tagItem);
            loadTags(tagsJSON.getJSONObject(tagName), tag);
        }

        tagsTree.getRoot().getChildren().sort((o1, o2) -> {
            return o1.getValue().getName().compareTo(o2.getValue().getName());
        });
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
        pathsTable.setDisable(false);
        pathsTable.getItems().clear();

        menuEdit.setDisable(false);

        tagsFile = fileIn;
        stage.setTitle("[" + fileIn.getName() + "] " + appName);

        loadTagsFileGuiSettings();
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

    private void saveTagsFile(File fileIn) {
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

        //JSONLoader.saveJSON(fileIn, json);
    }

    private void newTagsFile(File fileIn) {
        menuEdit.setDisable(false);
        pathsTable.getItems().clear();
        tagsTree.getRoot().getChildren().clear();

        tagsFile = fileIn;
        stage.setTitle(appName + " [" + fileIn.getName() + "]");
    }
    //</Tag file i/o>=========================

    private void removeSelectedTagsConfirm() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Tags");
        alert.setHeaderText("Remove selected tagsJSON?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            removeSelectedTags();
            byTagsSearch.setText("");
            saveTagsFile(tagsFile);
        }
    }

    private void removeSelectedPathsFromSelectedTagsConfirm() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Paths from tags");
        alert.setHeaderText("Remove selected paths from selected tags?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            //выделенные теги
            ArrayList<Tag> tags = new ArrayList<>();
            for(TreeItem<Tag> tagItem: tagsTree.getSelectionModel().getSelectedItems()){
                tags.add(tagItem.getValue());
            }

            for (Path selectedPath: pathsTable.getSelectionModel().getSelectedItems()) {
                selectedPath.removeTags(tags);
            }
            pathsTable.getItems().removeAll(pathsTable.getSelectionModel().getSelectedItems());

            saveTagsFile(tagsFile);
        }
    }

    private void removeSelectedPathsConfirm(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Paths");
        alert.setHeaderText("Remove selected paths?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            for (Path selectedPath: pathsTable.getSelectionModel().getSelectedItems()) {
                paths.removePath(selectedPath);
            }
            pathsTable.getItems().removeAll(pathsTable.getSelectionModel().getSelectedItems());

            saveTagsFile(tagsFile);
        }
    }

    private void desktopOpenInFolder(File fileIn){
        if(!fileIn.exists()){
            Optional<ButtonType> result = alertConfirm.showAndWait();
            if (result.get() == ButtonType.OK) {
                Path path = pathsTable.getSelectionModel().getSelectedItem();
                path.removeTags(path.getTags());
                paths.removePath(path);

                saveTagsFile(tagsFile);
            }
        }else{
            if (Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try {
                        if(!fileIn.exists()){
                        }else{
                            String[] command;
                            if(openInFolderArgument.length() > 0){
                                command = new String[3];
                                command[0] = openInFolderCommand;
                                command[1] = openInFolderArgument;
                                command[2] = fileIn.getAbsolutePath();
                            }else{
                                command = new String[2];
                                command[0] = openInFolderCommand;
                                command[1] = fileIn.getAbsolutePath();
                            }

                            Runtime.getRuntime().exec(command);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        }
    }

    private void desktopOpenFile(File fileIn){
        if(!fileIn.exists()){
            alertConfirm.setTitle("Path does not exist");
            alertConfirm.setHeaderText("Path does not exist, Remove this path?");
            Optional<ButtonType> result = alertConfirm.showAndWait();
            if (result.get() == ButtonType.OK) {
                Path path = pathsTable.getSelectionModel().getSelectedItem();
                path.removeTags(path.getTags());
                paths.removePath(path);

                saveTagsFile(tagsFile);
            }
        }else{
            if (Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try {
                        Desktop.getDesktop().open(fileIn);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
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

                newTagsFile(file);
                addRecent(file, true);
            }
        });

        MenuItem openItem = new MenuItem("Open");
        openItem.setGraphic(new ImageView(openIcon));
        openItem.setOnAction(event -> {
            fileChooser.setTitle("Open content info file");
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                loadTagsFile(file);
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

        //<paths ContextMenu>============================
        ContextMenu serchedPathsContextMenu = new ContextMenu();
        MenuItem openPathItemContext = new MenuItem("Open");
        openPathItemContext.setGraphic(new ImageView(openIcon));
        MenuItem openInFolderPathItemContext = new MenuItem("Open in folder");
        openInFolderPathItemContext.setGraphic(new ImageView(openIcon));
        MenuItem editPathItemContext = new MenuItem("Edit Path");
        editPathItemContext.setGraphic(new ImageView(editIcon));
        MenuItem addPathItemContext = new MenuItem("Add Path");
        addPathItemContext.setGraphic(new ImageView(addIcon));
        MenuItem addPathSelectedWithTagsItemContextPaths = new MenuItem("Add Path with selected tags");
        addPathSelectedWithTagsItemContextPaths.setGraphic(new ImageView(addIcon));
        MenuItem removePathsItemContext = new MenuItem("Remove Paths");
        removePathsItemContext.setGraphic(new ImageView(removeIcon));
        MenuItem removePathsFromSelectedTagsItemContext = new MenuItem("Remove Paths from selected tags");
        removePathsFromSelectedTagsItemContext.setGraphic(new ImageView(removeIcon));

        serchedPathsContextMenu.getItems().add(openPathItemContext);
        serchedPathsContextMenu.getItems().add(openInFolderPathItemContext);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(addPathItemContext);
        serchedPathsContextMenu.getItems().add(addPathSelectedWithTagsItemContextPaths);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(editPathItemContext);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(removePathsItemContext);
        serchedPathsContextMenu.getItems().add(removePathsFromSelectedTagsItemContext);
        pathsTable.setOnContextMenuRequested(event -> {
            if(pathsTable.getSelectionModel().getSelectedItems().size() == 1){
                removePathsFromSelectedTagsItemContext.setDisable(false);
                openPathItemContext.setDisable(false);
                openInFolderPathItemContext.setDisable(false);
                editPathItemContext.setDisable(false);
            }
            if(pathsTable.getSelectionModel().getSelectedItems().size() > 1){
                removePathsFromSelectedTagsItemContext.setDisable(false);
                openPathItemContext.setDisable(true);
                openInFolderPathItemContext.setDisable(true);
                editPathItemContext.setDisable(true);
            }
            if(pathsTable.getSelectionModel().getSelectedItems().size() == 0){
                removePathsFromSelectedTagsItemContext.setDisable(true);
                openPathItemContext.setDisable(true);
                openInFolderPathItemContext.setDisable(true);
                editPathItemContext.setDisable(true);
            }
            if(tagsTree.getSelectionModel().getSelectedItems().size() > 0){
                addPathSelectedWithTagsItemContextPaths.setDisable(false);
            }else{
                addPathSelectedWithTagsItemContextPaths.setDisable(true);
            }
        });
        pathsTable.setContextMenu(serchedPathsContextMenu);

        pathsTable.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                enterPressed = true;
            }
        });
        pathsTable.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if(enterPressed){
                enterPressed = false;
                if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                    Bounds boundsInScene = pathsTable.localToScene(pathsTable.getBoundsInLocal());
                    Bounds boundsInScreen = pathsTable.localToScreen(pathsTable.getBoundsInLocal());
                    pathsTable.fireEvent(new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, boundsInScene.getMinX(), boundsInScene.getMinY(), boundsInScreen.getMinX(), boundsInScreen.getMinY(), true, null));
                }else if(event.getCode() == KeyCode.ENTER){
                    if(pathsTable.getSelectionModel().getSelectedItems().size() == 1){
                        desktopOpenFile(new File(pathsTable.getSelectionModel().getSelectedItem().getPath()));
                    }
                }
            }
        });
        //</paths ContextMenu>===========================

        //<tags ContextMenu>=============================
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
        tagsTree.setContextMenu(tagsTreeContextMenu);
        //</tags ContextMenu>============================

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

        EventHandler<ActionEvent> editTag = (event) -> {
            if (tagsTree.getSelectionModel().getSelectedItems().size() == 1) {
                addEditTagDialogController.setRenameTag(tagsTree.getSelectionModel().getSelectedItem().getValue());
                addEditTagDialogController.open();
            }
        };

        EventHandler<ActionEvent> editPath = (event) -> {
            if (pathsTable.getSelectionModel().getSelectedItems().size() == 1) {
                editPathItemContext.setDisable(false);
                addEditPathDialogController.setEditPath(pathsTable.getSelectionModel().getSelectedItem());
                addEditPathDialogController.open();
            } else {
                editPathItemContext.setDisable(true);
            }
        };

        EventHandler<ActionEvent> addPathSelectedWithTags = event -> {
            ArrayList<Tag> tags1 = new ArrayList<>();
            for(TreeItem<Tag> tagItem: tagsTree.getSelectionModel().getSelectedItems()){
                tags1.add(tagItem.getValue());
            }

            addEditPathDialogController.setAddPath(tags1);
            addEditPathDialogController.open();
        };

        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.T && event.isControlDown()) {
                tagsTree.requestFocus();
            }else if (event.getCode() == KeyCode.P && event.isControlDown()) {
                pathsTable.requestFocus();
            }else if (event.getCode() == KeyCode.F && event.isControlDown()) {
                byNameSearch.requestFocus();
            }
        });

        addTagItem.setOnAction(addTag);
        addPathItem.setOnAction(addPath);
        renameTagItem.setOnAction(editTag);
        editPathItem.setOnAction(editPath);
        removeTagItem.setOnAction(event -> removeSelectedTagsConfirm());

        openPathItemContext.setOnAction(event -> desktopOpenFile(new File(pathsTable.getSelectionModel().getSelectedItem().getPath())));
        openInFolderPathItemContext.setOnAction(event -> desktopOpenInFolder(new File(pathsTable.getSelectionModel().getSelectedItem().getPath())));
        addPathItemContext.setOnAction(addPath);
        editPathItemContext.setOnAction(editPath);
        removePathsItemContext.setOnAction(event -> removeSelectedPathsConfirm());
        removePathsFromSelectedTagsItemContext.setOnAction(event -> removeSelectedPathsFromSelectedTagsConfirm());

        addTagItemContext.setOnAction(addTag);
        renameTagItemContext.setOnAction(editTag);
        removeTagItemContext.setOnAction(event -> removeSelectedTagsConfirm());
        addPathSelectedWithTagsItemContext.setOnAction(addPathSelectedWithTags);
        addPathSelectedWithTagsItemContextPaths.setOnAction(addPathSelectedWithTags);
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
        //</menu events>====================================

        menuBar.getMenus().add(menuFile);
        menuBar.getMenus().add(menuEdit);
    }

    public void setStyle(String styleFileNameIn){
        if(styleFileName.length() > 0){
            stage.getScene().getStylesheets().remove(styleFileName);
        }

        styleFileName = "default";
        if(!styleFileNameIn.equals("default")){
            styleFileName = styleFileNameIn;
            stage.getScene().getStylesheets().add(styleFileName);
        }

        addEditTagDialogController.setStyle(styleFileName);
        addEditPathDialogController.setStyle(styleFileName);
        settingsDialogController.setStyle(styleFileName);
    }

    public String getStyle(){
        return styleFileName;
    }

    public String getOpenInFolderCommand(){
        return openInFolderCommand;
    }
    public String getOpenInFolderArgument(){
        return openInFolderArgument;
    }
}
