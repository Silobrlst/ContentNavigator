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
import javafx.util.Callback;
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
    private TableColumn<Path, String> searchedPathsAdded;
    @FXML
    private TreeView<Tag> tagsTree;
    @FXML
    private SplitPane tagsPathsSplitter;
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
    private Button showAllPaths;
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
    private final Image showIcon = new Image("file:images/showIcon.png");
    //</icons>================================

    //<JSON names>============================
    private static final String tagsJSONName = "tags";
    private static final String pathsJSONName = "paths";
    private static final String nameJsonName = "name";
    private static final String descriptionJsonName = "description";
    private static final String htmlDescriptionJsonName = "descriptionHtml";
    private static final String searchedPathsNameWidth = "searchedPathsNameWidth";
    private static final String searchedPathsPathWidth = "searchedPathsPathWidth";
    private static final String searchedPathsAddedWidth = "searchedPathsAddedWidth";
    private static final File guiSettings = new File("guiSettings.json");
    //</JSON names>===========================

    private Menu openRecentMenu;
    private Menu menuEdit;
    private MenuItem renameTagItem;

    private Tags tags = new Tags();
    private Paths paths = new Paths();

    private Stage stage;
    private String styleFileName = "";

    private SettingsDialogController settingsDialogController;
    private AddEditPathDialog addEditPathDialogController;
    private AddEditTagDialog addEditTagDialogController;
    private AddEditPathsDialog addEditPathsDialogController;
    private CheckNotAddedPathsWindowController checkNotAddedPathsWindow;
    private HtmlWindow htmlWindow;

    private File tagFile = null;

    //проверяем что Enter был нажат пока фокус был на таблице
    private boolean enterPressed = false;

    @FXML
    public void initialize() {}

    public MainWindowController(){}

    public void init(Stage stageIn, FXMLLoader loaderIn)throws Exception{
        stage = stageIn;
        stage.setOnCloseRequest(event -> {
            saveAppGuiSettings();

            //закрываем все окна которые не являются диалоговыми
            checkNotAddedPathsWindow.close();
            htmlWindow.close();
        });
        stage.setScene(new Scene(loaderIn.getRoot()));
        stage.getIcons().add(new Image("file:images/appIcon.png"));
        stage.show();
        stage.getScene().getRoot().setId("mainWindowRoot");
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if(event.isControlDown()){
                switch (event.getCode()){
                    case F:
                        byNameSearch.requestFocus();
                        break;
                    case P:
                        pathsTable.requestFocus();
                        break;
                    case T:
                        tagsTree.requestFocus();
                        break;
                }
            }
        });

        paths.addListener(new EmptyPathListener(){
            @Override
            public void created(Path pathIn) {
                saveTagsFile(tagFile);
            }

            @Override
            public void renamed(Path pathIn) {
                saveTagsFile(tagFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void removedTag(Path pathIn, Tag tagIn) {
                saveTagsFile(tagFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void removedTags(Path pathIn, Collection<Tag> tagsIn) {
                saveTagsFile(tagFile);
                filterPathsBySelectedTags();
            }

            @Override
            public void addedTags(Path pathIn, Collection<Tag> tagsIn) {
                saveTagsFile(tagFile);
                filterPathsBySelectedTags();
                pathsTable.getSelectionModel().select(pathIn);
                pathsTable.refresh();
                pathsTable.requestFocus();
                pathsTable.scrollTo(pathIn);
            }
        });
        tags.addTagObserver(new EmptyTagListener(){
            @Override
            public void createdTag(Tag tagIn, Tag parentIn) {
                System.out.println("44444444444444444444");
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

                saveTagsFile(tagFile);
            }

            @Override
            public void removedPathFromTag(Tag tagIn, Path pathIn) {
                System.out.println("33333333333333");
                filterPathsBySelectedTags();
            }

            @Override
            public void renamedTag(Tag tagIn) {
                System.out.println("2222222222222222");
                getTreeItemByTag(tagIn.getParent()).getChildren().sort((o1, o2) -> {
                    return o1.getValue().getName().compareTo(o2.getValue().getName());
                });
                tagsTree.refresh();
                saveTagsFile(tagFile);
            }

            @Override
            public void changedParent(Tag tagIn, Tag oldParentIn) {
                System.out.println("1111111111111111");
                TreeItem<Tag> tagTreeItem = getTreeItemByTag(tagIn);
                TreeItem<Tag> newParentTreeItem =  getTreeItemByTag(tagIn.getParent());
                getTreeItemByTag(oldParentIn).getChildren().remove(tagTreeItem);
                newParentTreeItem.getChildren().add(tagTreeItem);

                tagsTree.getSelectionModel().clearSelection();
                tagsTree.getSelectionModel().select(tagTreeItem);
                tagsTree.scrollTo(tagsTree.getRow(tagTreeItem));

                newParentTreeItem.getChildren().sort((o1, o2) -> {
                    return o1.getValue().getName().compareTo(o2.getValue().getName());
                });
                tagsTree.refresh();
                saveTagsFile(tagFile);
            }

            @Override
            public void changedDescriptions(Tag tagIn) {
                saveTagsFile(tagFile);
            }
        });

        //<tagsTree>============================================
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
        tagsTree.setCellFactory(new Callback<TreeView<Tag>, TreeCell<Tag>>() {
            @Override
            public TreeCell<Tag> call(TreeView<Tag> stringTreeView) {
                TreeCell<Tag> treeCell = new TreeCell<Tag>() {
                    protected void updateItem(Tag itemIn, boolean empty) {
                        super.updateItem(itemIn, empty);
                        if (itemIn != null) {
                            setText(itemIn.getName());
                        } else {
                            setText("");   // <== clear the now empty cell.
                        }
                    }
                };

                treeCell.setOnDragDetected(event -> {
                    Dragboard db = treeCell.startDragAndDrop(TransferMode.MOVE);

                    ClipboardContent content = new ClipboardContent();
                    content.putString(tags.getTagId(treeCell.getTreeItem().getValue()));
                    db.setContent(content);

                    event.consume();
                });

                treeCell.setOnDragEntered(event -> {
                    if (event.getGestureSource() != treeCell && event.getDragboard().hasString()) {
                        treeCell.setGraphic(new ImageView(addIcon));
                        treeCell.getTreeItem().setExpanded(true);
                    }

                    event.consume();
                });

                treeCell.setOnDragExited(event -> {
                    if (event.getGestureSource() != treeCell) {
                        treeCell.setGraphic(null);
                    }

                    event.consume();
                });

                treeCell.setOnDragOver(new EventHandler<DragEvent>() {
                    public void handle(DragEvent event) {
                        if (event.getGestureSource() != treeCell && event.getDragboard().hasString()) {
                            event.acceptTransferModes(TransferMode.MOVE);
                        }

                        event.consume();
                    }
                });

                treeCell.setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    boolean success = false;

                    boolean condition = event.getGestureSource() != treeCell;
                    condition = condition && db.hasString();
                    condition = condition && !tags.getTagId(treeCell.getTreeItem().getValue()).contains(tags.getTagId(((TreeCell<Tag>)event.getGestureSource()).getTreeItem().getValue()));
                    if (condition) {
                        System.out.println(db.getString());
                        tags.getTagById(db.getString()).setParentTag(treeCell.getTreeItem().getValue());
                        success = true;
                    }

                    event.setDropCompleted(success);
                    event.consume();
                });

                return treeCell;
            }
        });
        //</tagsTree>===========================================

        //<pathsTable>==========================================
        pathsTable.setDisable(true);
        pathsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        pathsTable.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if(event.isControlDown() && event.getCode() == KeyCode.V){
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                if(clipboard.hasFiles()){
                    if(clipboard.getFiles().size() == 1){
                        addEditPathDialogController.setAddPath(getSelectedTags(), clipboard.getFiles().get(0));
                        addEditPathDialogController.open();
                    }else{
                        addEditPathsDialogController.setAddPaths(clipboard.getFiles(), getSelectedTags());
                        addEditPathsDialogController.open();
                    }
                }else if(clipboard.hasString()){
                    String[] pathsStr = clipboard.getString().split("\\s*\\n\\s*");

                    if(pathsStr.length == 1){
                        addEditPathDialogController.setAddPath(getSelectedTags(), new File(pathsStr[0]));
                        addEditPathDialogController.open();
                    }else{
                        ArrayList<File> files = new ArrayList<>();

                        for(int i=0; i<pathsStr.length; i++){
                            files.add(new File(pathsStr[i]));
                        }

                        addEditPathsDialogController.setAddPaths(files, getSelectedTags());
                        addEditPathsDialogController.open();
                    }
                }
            }
        });
        pathsTable.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedTagsConfirm();
            }
        });
        pathsTable.setRowFactory( tv -> {
            TableRow<Path> row = new TableRow<Path>(){
                private Tooltip tooltip = new Tooltip();
                @Override
                public void updateItem(Path pathIn, boolean empty) {
                    super.updateItem(pathIn, empty);
                    if (pathIn == null) {
                        setTooltip(null);
                    } else {
                        String message = "Tags:";
                        for(Tag tag: pathIn.getTags()){
                            message += "\n" + tags.getTagId(tag);
                        }

                        message += "\n\nPath:\n" + pathIn.getPath();

                        message += "\n\nDescription:\n";

                        if(pathIn.getDescription() != null){
                            if(pathIn.getDescription().length() > 100){
                                message += pathIn.getDescription().substring(0, 100) + "...";
                            }else{
                                message += pathIn.getDescription();
                            }
                        }

                        message += "\n\nHTML description:\n";
                        message += pathIn.getHtmlDescription();

                        tooltip.setText(message);
                        setTooltip(tooltip);
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    Path path = row.getItem();
                    desktopOpenFile(new File(path.getPath()));
                }
            });
            return row;
        });

        searchedPathsName.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
        searchedPathsPath.setCellValueFactory(cellData -> cellData.getValue().getPathProperty());
        searchedPathsAdded.setCellValueFactory(cellData -> cellData.getValue().getDateProperty());
        //</pathsTable>=========================================

        //<addEditTagDialogController>==========================
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AddEditTagDialog.fxml"));
        loader.load();
        addEditTagDialogController = loader.getController();
        addEditTagDialogController.init(stage, loader, tags);
        //</addEditTagDialogController>=========================

        //<addEditPathDialogController>=========================
        loader = new FXMLLoader(getClass().getResource("AddEditPathDialog.fxml"));
        loader.load();
        addEditPathDialogController = loader.getController();
        addEditPathDialogController.init(stage, loader, paths, tags);
        //</addEditPathDialogController>========================

        //<addEditPathsDialogController>========================
        loader = new FXMLLoader(getClass().getResource("AddEditPathsDialog.fxml"));
        loader.load();
        addEditPathsDialogController = loader.getController();
        addEditPathsDialogController.init(stage, loader, paths, tags);
        //</addEditPathsDialogController>=======================

        //<SettingsDialogController>============================
        loader = new FXMLLoader(getClass().getResource("SettingsDialog.fxml"));
        loader.load();
        settingsDialogController = loader.getController();

        MainWindowController mainWindowControllerContext = this;
        settingsDialogController.init(stage, loader, this, () -> {
            openInFolderCommand = settingsDialogController.getOpenInFolderCommand();
            openInFolderArgument = settingsDialogController.getOpenInFolderArgument();
            mainWindowControllerContext.setStyle(settingsDialogController.getSelectedStyle());
            saveSettings();
        });
        //</SettingsDialogController>===========================

        //<CheckNotAddedPathsWindowController===================
        loader = new FXMLLoader(getClass().getResource("CheckNotAddedPathsWindow.fxml"));
        loader.load();
        checkNotAddedPathsWindow = loader.getController();
        checkNotAddedPathsWindow.init(loader, paths);
        //</CheckNotAddedPathsWindowController==================

        //<htmlWindow===========================================
        loader = new FXMLLoader(getClass().getResource("HtmlWindow.fxml"));
        loader.load();
        htmlWindow = loader.getController();
        htmlWindow.init(loader);
        //</htmlWindow==========================================

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

        showAllPaths.setOnAction(event -> onShowAllPaths());

        initMenu();

        loadSettings();
        loadAppGuiSettings();

        if (openRecentMenu.getItems().size() > 0) {
            loadTagFile(new File(openRecentMenu.getItems().get(0).getText()));
        }
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

    private List<Tag> getTagsByTreeItems(List<TreeItem<Tag>> treeItemsIn){
        List<Tag> tags_ = new ArrayList<>();

        for(TreeItem<Tag> tagTreeItem: treeItemsIn){
            tags_.add(tagTreeItem.getValue());
        }

        return tags_;
    }

    private List<Tag> getSelectedTags(){
        return getTagsByTreeItems(tagsTree.getSelectionModel().getSelectedItems());
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
        if (!mainWindowJSON.has("maximized")) {
            mainWindowJSON.put("maximized", false);
        }

        if (!appGuiSettingsJsonIn.has(searchedPathsNameWidth)) {
            appGuiSettingsJsonIn.put(searchedPathsNameWidth, 0.d);
        }
        if (!appGuiSettingsJsonIn.has(searchedPathsPathWidth)) {
            appGuiSettingsJsonIn.put(searchedPathsPathWidth, 0.d);
        }
        if (!appGuiSettingsJsonIn.has(searchedPathsAddedWidth)) {
            appGuiSettingsJsonIn.put(searchedPathsAddedWidth, 0.d);
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

        stage.setMaximized(mainWindowJSON.getBoolean("maximized"));

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            tagsPathsSplitter.setDividerPositions(mainWindowJSON.getDouble("tagsPathsSplitPosition"));
            if(appGuiJson.getDouble(searchedPathsNameWidth) != 0.d){
                searchedPathsName.setPrefWidth(appGuiJson.getDouble(searchedPathsNameWidth));
            }
            if(appGuiJson.getDouble(searchedPathsPathWidth) != 0.d){
                searchedPathsPath.setPrefWidth(appGuiJson.getDouble(searchedPathsPathWidth));
            }
            if(appGuiJson.getDouble(searchedPathsAddedWidth) != 0.d){
                searchedPathsAdded.setPrefWidth(appGuiJson.getDouble(searchedPathsAddedWidth));
            }
        })).play();

        pathsTable.getSortOrder().add(searchedPathsName);
        searchedPathsName.setSortType(TableColumn.SortType.ASCENDING);
        searchedPathsName.setSortable(true);
    }

    private void loadTagsFileGuiSettings(){
        JSONObject tagsFileGuiSettingsJson = JSONLoader.loadJSON(new File(tagFile.getName() + ".guiSettings.json"));
        validateTagsFileGuiSettings(tagsFileGuiSettingsJson);

        JSONObject tagsExpandJSON = tagsFileGuiSettingsJson.getJSONObject("tagsExpand");
        for(String tagId: tagsExpandJSON.keySet()){
            if(tags.getTagById(tagId) != null){
                getTreeItemByTag(tags.getTagById(tagId)).setExpanded(tagsExpandJSON.getBoolean(tagId));
            }
        }

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            tagsTree.getSelectionModel().clearSelection();

            JSONArray selectedTagsJSON = tagsFileGuiSettingsJson.getJSONArray("selectedTags");

            if(selectedTagsJSON.length() == 1){
                TreeItem<Tag> tagTreeItem = getTreeItemByTag(tags.getTagById(selectedTagsJSON.getString(0)));
                tagsTree.getSelectionModel().select(tagTreeItem);
                tagsTree.scrollTo(tagsTree.getRow(tagTreeItem));
            }else{
                for (int i = 0; i < selectedTagsJSON.length(); i++) {
                    tagsTree.getSelectionModel().select(getTreeItemByTag(tags.getTagById(selectedTagsJSON.getString(i))));
                }
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
        mainWindowJSON.put("tagsPathsSplitPosition", tagsPathsSplitter.getDividerPositions()[0]);
        if(!stage.isMaximized()){
            mainWindowJSON.put("x", stage.getX());
            mainWindowJSON.put("y", stage.getY());
            mainWindowJSON.put("width", stage.getWidth());
            mainWindowJSON.put("height", stage.getHeight());
        }
        mainWindowJSON.put("maximized", stage.isMaximized());

        appGuiJson.put(searchedPathsNameWidth, searchedPathsName.getWidth());
        appGuiJson.put(searchedPathsPathWidth, searchedPathsPath.getWidth());
        appGuiJson.put(searchedPathsAddedWidth, searchedPathsAdded.getWidth());

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

        System.out.println(tagFile);

        if(tagFile != null){
            JSONLoader.saveJSON(new File(tagFile.getName() + ".guiSettings.json"), tagsFileGuiSettingsJson);
        }
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

            if(!file.exists()){
                alertConfirm.setTitle("Path does not exist");
                alertConfirm.setHeaderText("Path does not exist, Remove this path?");
                Optional<ButtonType> result = alertConfirm.showAndWait();
                if (result.get() == ButtonType.OK) {
                    openRecentMenu.getItems().remove(menuItem);
                    saveAppGuiSettings();
                }
            }else{
                if(tagFile == null){
                    loadTagFile(file);
                    addRecent(file, true);
                }else if(!tagFile.getAbsolutePath().equals(file.getAbsolutePath())){
                    loadTagFile(file);
                    addRecent(file, true);
                }
            }
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
                if(path.getName().toLowerCase().contains(byNameSearch.getText().toLowerCase())){
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
                if(path.getPath().toLowerCase().contains(byPathSearch.getText().toLowerCase())){
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

    private void onShowAllPaths(){
        pathsTable.getItems().clear();

        for(Path path: paths){
            pathsTable.getItems().add(path);
        }
    }

    private void showNonexistentPaths(){
        pathsTable.getItems().clear();

        for(Path path: paths){
            if(!paths.checkPathExist(path.getPath())){
                pathsTable.getItems().add(path);
            }
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

            if(!pathJSONObj.has(nameJsonName)){
                pathJSONObj.put(nameJsonName, "");
            }

            if(!pathJSONObj.has("dateAdded")){
                pathJSONObj.put("dateAdded", "");
            }
        }
    }

    private void validateTag(JSONObject jsonIn){
        if(!jsonIn.has(tagsJSONName)){
            jsonIn.put(tagsJSONName, new JSONObject());
        }
        if(!jsonIn.has(descriptionJsonName)){
            jsonIn.put(descriptionJsonName, "");
        }
        if(!jsonIn.has(htmlDescriptionJsonName)){
            jsonIn.put(htmlDescriptionJsonName, "");
        }
    }

    private void loadTags(JSONObject parentTagJSONIn, Tag parentIn){
        validateTag(parentTagJSONIn);

        parentIn.setDescriptionWithoutNotify(parentTagJSONIn.getString(descriptionJsonName));
        parentIn.setHtmlDescriptionWithoutNotify(parentTagJSONIn.getString(htmlDescriptionJsonName));

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

    private void loadTagFile(File fileIn) {
        if(!fileIn.exists()){
            return;
        }

        //если уже был открыт другой тег-файл сохраняем его
        if(tagFile != null){
            saveTagsFile(tagFile);
            saveTagsFileGuiSettings();
            clearTagFile();
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

        //<load paths>===============================
        JSONObject pathsJSON = json.getJSONObject(pathsJSONName);
        for(String pathJSONKey: pathsJSON.keySet()){
            Path path;
            JSONObject pathJSON = pathsJSON.getJSONObject(pathJSONKey);

            if(pathJSON.getString(nameJsonName).length() > 0){
                path = paths.newPathWithoutNotifing(pathJSONKey, pathJSON.getString(nameJsonName));
            }else{
                path = paths.newPathWithoutNotifing(pathJSONKey);
            }

            if(!pathJSON.has("dateAdded")){
                pathJSON.put("dateAdded", "");
            }
            if(!pathJSON.has(descriptionJsonName)){
                pathJSON.put(descriptionJsonName, "");
            }
            if(!pathJSON.has(htmlDescriptionJsonName)){
                pathJSON.put(htmlDescriptionJsonName, "");
            }

            path.setDateAdded(pathJSON.getString("dateAdded"));
            path.setDescription(pathJSON.getString(descriptionJsonName));
            path.setHtmlDescription(pathJSON.getString(htmlDescriptionJsonName));

            for(String tagJSON: pathJSON.getJSONObject(tagsJSONName).keySet()){
                Tag tag = tags.getTagById(tagJSON);

                if(tag != null){
                    path.addTagWithoutNotifing(tag);
                }
            }
        }
        //</load paths>==============================

        tagsTree.setDisable(false);
        pathsTable.setDisable(false);
        pathsTable.getItems().clear();

        menuEdit.setDisable(false);

        tagFile = fileIn;
        stage.setTitle("[" + fileIn.getName() + "] " + appName);

        loadTagsFileGuiSettings();
    }

    private void tagsToJsonRecursive(Tag tagIn, JSONObject tagJSONIn){
        validateTag(tagJSONIn);

        tagJSONIn.put(descriptionJsonName, tagIn.getDescription());
        tagJSONIn.put(htmlDescriptionJsonName, tagIn.getHtmlDescription());

        JSONObject tagsJSON = tagJSONIn.getJSONObject(tagsJSONName);

        for(Tag tag: tagIn.getChildren()){
            JSONObject tagJSON = new JSONObject();
            tagsJSON.put(tag.getName(), tagJSON);
            tagsToJsonRecursive(tag, tagJSON);
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
            tagsToJsonRecursive(tag, tagJSON);
        }

        for(Path path: paths){
            if(path.getTags().size() > 0){
                JSONObject pathJSON = new JSONObject();
                pathsJSON.put(path.getPath(), pathJSON);

                JSONObject pathTagsJSON = new JSONObject();
                pathJSON.put(tagsJSONName, pathTagsJSON);
                pathJSON.put(nameJsonName, path.getName());
                pathJSON.put("dateAdded", path.getDateAdded());
                pathJSON.put(descriptionJsonName, path.getDescription());
                pathJSON.put(htmlDescriptionJsonName, path.getHtmlDescription());

                for(Tag tag: path.getTags()){
                    pathTagsJSON.put(tags.getTagId(tag), "");
                }
            }
        }

        JSONLoader.saveJSON(fileIn, json);
    }

    private void newTagsFile(File fileIn) {
        clearTagFile();

        tagFile = fileIn;
        stage.setTitle(appName + " [" + fileIn.getName() + "]");
    }

    private void clearTagFile(){
        paths.clear();
        tags.getChildren().clear();
        pathsTable.getItems().clear();
        tagsTree.getRoot().getChildren().clear();

        tagFile = null;

        menuEdit.setDisable(false);
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
            saveTagsFile(tagFile);
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

            saveTagsFile(tagFile);
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

            saveTagsFile(tagFile);
        }
    }

    private void desktopOpenInFolder(File fileIn){
        if(!fileIn.exists()){
            alertConfirm.setTitle("Path does not exist");
            alertConfirm.setHeaderText("Path does not exist, Remove this path?");
            Optional<ButtonType> result = alertConfirm.showAndWait();
            if (result.get() == ButtonType.OK) {
                Path path = pathsTable.getSelectionModel().getSelectedItem();
                path.removeTags(path.getTags());
                paths.removePath(path);

                saveTagsFile(tagFile);
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

                saveTagsFile(tagFile);
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

        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("tag-files (*.json)", "*.json");
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
                loadTagFile(file);
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
        MenuItem showDescriptionPathsContext = new MenuItem("Show description");
        showDescriptionPathsContext.setGraphic(new ImageView(showIcon));
        MenuItem showHtmlDescriptionPathsContext = new MenuItem("Show HTML description");
        showHtmlDescriptionPathsContext.setGraphic(new ImageView(showIcon));
        MenuItem editPathsItemContext = new MenuItem("Edit Path/Paths");
        editPathsItemContext.setGraphic(new ImageView(editIcon));
        MenuItem addPathItemContext = new MenuItem("Add Path");
        addPathItemContext.setGraphic(new ImageView(addIcon));
        MenuItem addPathSelectedWithTagsItemContextPaths = new MenuItem("Add Path with selected tags");
        addPathSelectedWithTagsItemContextPaths.setGraphic(new ImageView(addIcon));
        MenuItem removePathsItemContext = new MenuItem("Remove Paths");
        removePathsItemContext.setGraphic(new ImageView(removeIcon));
        MenuItem removePathsFromSelectedTagsItemContext = new MenuItem("Remove Paths from selected tags");
        removePathsFromSelectedTagsItemContext.setGraphic(new ImageView(removeIcon));

        ArrayList<MenuItem> singleSelectionPathsContextItems = new ArrayList<>();
        singleSelectionPathsContextItems.add(openPathItemContext);
        singleSelectionPathsContextItems.add(openInFolderPathItemContext);
        singleSelectionPathsContextItems.add(showDescriptionPathsContext);
        singleSelectionPathsContextItems.add(showHtmlDescriptionPathsContext);

        ArrayList<MenuItem> oneOrMoreSelectionPathsContextItems = new ArrayList<>();
        oneOrMoreSelectionPathsContextItems.add(editPathsItemContext);
        oneOrMoreSelectionPathsContextItems.add(removePathsItemContext);
        oneOrMoreSelectionPathsContextItems.add(removePathsFromSelectedTagsItemContext);

        serchedPathsContextMenu.getItems().add(openPathItemContext);
        serchedPathsContextMenu.getItems().add(openInFolderPathItemContext);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(showDescriptionPathsContext);
        serchedPathsContextMenu.getItems().add(showHtmlDescriptionPathsContext);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(addPathItemContext);
        serchedPathsContextMenu.getItems().add(addPathSelectedWithTagsItemContextPaths);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(editPathsItemContext);
        serchedPathsContextMenu.getItems().add(new SeparatorMenuItem());
        serchedPathsContextMenu.getItems().add(removePathsItemContext);
        serchedPathsContextMenu.getItems().add(removePathsFromSelectedTagsItemContext);
        pathsTable.setOnContextMenuRequested(event -> {
            if(pathsTable.getSelectionModel().getSelectedItems().size() == 1){
                singleSelectionPathsContextItems.forEach(menuItem -> {
                    menuItem.setDisable(false);
                });
                oneOrMoreSelectionPathsContextItems.forEach(menuItem -> {
                    menuItem.setDisable(false);
                });
            }
            if(pathsTable.getSelectionModel().getSelectedItems().size() > 1){
                singleSelectionPathsContextItems.forEach(menuItem -> {
                    menuItem.setDisable(true);
                });
                oneOrMoreSelectionPathsContextItems.forEach(menuItem -> {
                    menuItem.setDisable(false);
                });
            }
            if(pathsTable.getSelectionModel().getSelectedItems().size() == 0){
                singleSelectionPathsContextItems.forEach(menuItem -> {
                    menuItem.setDisable(false);
                });
                oneOrMoreSelectionPathsContextItems.forEach(menuItem -> {
                    menuItem.setDisable(false);
                });
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
            if (event.getCode() == KeyCode.CONTEXT_MENU) {
                Bounds boundsInScene = pathsTable.localToScene(pathsTable.getBoundsInLocal());
                Bounds boundsInScreen = pathsTable.localToScreen(pathsTable.getBoundsInLocal());
                pathsTable.fireEvent(new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, boundsInScene.getMinX(), boundsInScene.getMinY(), boundsInScreen.getMinX(), boundsInScreen.getMinY(), true, null));
            }

            if(enterPressed){
                enterPressed = false;
                if(event.getCode() == KeyCode.ENTER){
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
        MenuItem showDescriptionTagsContext = new MenuItem("Show description");
        showDescriptionTagsContext.setGraphic(new ImageView(showIcon));
        MenuItem showHtmlDescriptionTagsContext = new MenuItem("Show HTML description");
        showHtmlDescriptionTagsContext.setGraphic(new ImageView(showIcon));
        MenuItem addPathSelectedWithTagsItemContext = new MenuItem("Add Path with selected tags");
        addPathSelectedWithTagsItemContext.setGraphic(new ImageView(addIcon));
        MenuItem removeTagItemContext = new MenuItem("Remove Tags");
        removeTagItemContext.setGraphic(new ImageView(removeIcon));

        tagsTreeContextMenu.getItems().add(showDescriptionTagsContext);
        tagsTreeContextMenu.getItems().add(showHtmlDescriptionTagsContext);
        tagsTreeContextMenu.getItems().add(new SeparatorMenuItem());
        tagsTreeContextMenu.getItems().add(addTagItemContext);
        tagsTreeContextMenu.getItems().add(addPathSelectedWithTagsItemContext);
        tagsTreeContextMenu.getItems().add(new SeparatorMenuItem());
        tagsTreeContextMenu.getItems().add(renameTagItemContext);
        tagsTreeContextMenu.getItems().add(new SeparatorMenuItem());
        tagsTreeContextMenu.getItems().add(removeTagItemContext);
        tagsTree.setContextMenu(tagsTreeContextMenu);
        tagsTree.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.CONTEXT_MENU) {
                Bounds boundsInScene = tagsTree.localToScene(tagsTree.getBoundsInLocal());
                Bounds boundsInScreen = tagsTree.localToScreen(tagsTree.getBoundsInLocal());
                tagsTree.fireEvent(new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, boundsInScene.getMinX(), boundsInScene.getMinY(), boundsInScreen.getMinX(), boundsInScreen.getMinY(), true, null));
            }
        });
        //</tags ContextMenu>============================

        //<menu edit>====================================
        MenuItem addTagItem = new MenuItem("Add Tag");
        addTagItem.setGraphic(new ImageView(addIcon));
        MenuItem addPathItem = new MenuItem("Add Path");
        addPathItem.setGraphic(new ImageView(addIcon));
        renameTagItem = new MenuItem("Edit Tag");
        renameTagItem.setGraphic(new ImageView(editIcon));
        MenuItem editPathItem = new MenuItem("Edit Paths");
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

        //<menu utils>====================================
        MenuItem copyPathsAsListItem = new MenuItem("Copy selected paths as list");
        MenuItem checkNotAddedPaths = new MenuItem("Check not added paths");
        MenuItem showNonexistentPathsItem = new MenuItem("Show nonexistent paths");

        Menu menuUtils = new Menu("Utils");
        ObservableList<MenuItem> menuUtilsItems = menuUtils.getItems();
        menuUtilsItems.add(copyPathsAsListItem);
        menuUtilsItems.add(checkNotAddedPaths);
        menuUtilsItems.add(showNonexistentPathsItem);
        //</menu utils>===================================

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
                addEditTagDialogController.setEditTag(tagsTree.getSelectionModel().getSelectedItem().getValue());
                addEditTagDialogController.open();
            }
        };

        EventHandler<ActionEvent> editPaths = (event) -> {
            if (pathsTable.getSelectionModel().getSelectedItems().size() == 1) {
                addEditPathDialogController.setEditPath(pathsTable.getSelectionModel().getSelectedItem());
                addEditPathDialogController.open();
            } else if(pathsTable.getSelectionModel().getSelectedItems().size() > 1){
                addEditPathsDialogController.setEditPaths(pathsTable.getSelectionModel().getSelectedItems());
                addEditPathsDialogController.open();
            }
        };

        EventHandler<ActionEvent> addPathWithSelectedTags = event -> {
            ArrayList<Tag> tags1 = new ArrayList<>();
            for(TreeItem<Tag> tagItem: tagsTree.getSelectionModel().getSelectedItems()){
                tags1.add(tagItem.getValue());
            }

            addEditPathDialogController.setAddPath(tags1);
            addEditPathDialogController.open();
        };

        EventHandler<DragEvent> dropFiles = event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;

                ArrayList<Tag> selectedTags = new ArrayList<>();
                for(TreeItem<Tag> tagTreeItem: tagsTree.getSelectionModel().getSelectedItems()){
                    selectedTags.add(tagTreeItem.getValue());
                }

                if(db.getFiles().size() == 1){
                    addEditPathDialogController.setAddPath(selectedTags, db.getFiles().get(0));
                    addEditPathDialogController.open();
                }else{
                    addEditPathsDialogController.setAddPaths(db.getFiles(), selectedTags);
                    addEditPathsDialogController.open();
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


        tagsTree.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.F2) {
                editTag.handle(new ActionEvent());
            }
        });

        pathsTable.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.F2) {
                editPaths.handle(new ActionEvent());
            }
        });

        pathsTable.setOnDragDropped(dropFiles);
        pathsTable.setOnDragOver(dragOverFiles);

        tagsTree.setOnDragDropped(dropFiles);
        tagsTree.setOnDragOver(dragOverFiles);

        addTagItem.setOnAction(addTag);
        addPathItem.setOnAction(addPath);
        renameTagItem.setOnAction(editTag);
        editPathItem.setOnAction(editPaths);
        removeTagItem.setOnAction(event -> removeSelectedTagsConfirm());

        openPathItemContext.setOnAction(event -> desktopOpenFile(new File(pathsTable.getSelectionModel().getSelectedItem().getPath())));
        openInFolderPathItemContext.setOnAction(event -> desktopOpenInFolder(new File(pathsTable.getSelectionModel().getSelectedItem().getPath())));
        addPathItemContext.setOnAction(addPath);
        editPathsItemContext.setOnAction(editPaths);
        removePathsItemContext.setOnAction(event -> removeSelectedPathsConfirm());
        removePathsFromSelectedTagsItemContext.setOnAction(event -> removeSelectedPathsFromSelectedTagsConfirm());

        addTagItemContext.setOnAction(addTag);
        renameTagItemContext.setOnAction(editTag);
        removeTagItemContext.setOnAction(event -> removeSelectedTagsConfirm());
        addPathSelectedWithTagsItemContext.setOnAction(addPathWithSelectedTags);
        addPathSelectedWithTagsItemContextPaths.setOnAction(addPathWithSelectedTags);
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

        copyPathsAsListItem.setOnAction(event -> {
            String copied = "";

            for(Path path: pathsTable.getSelectionModel().getSelectedItems()){
                copied += path.getPath() + "\n";
            }

            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(copied);
            clipboard.setContent(content);
        });

        checkNotAddedPaths.setOnAction(event -> {
            checkNotAddedPathsWindow.open();
        });

        showNonexistentPathsItem.setOnAction(event -> showNonexistentPaths());

        showDescriptionPathsContext.setOnAction(event -> htmlWindow.openText(pathsTable.getSelectionModel().getSelectedItem().getDescription()));
        showHtmlDescriptionPathsContext.setOnAction(event -> htmlWindow.openInternetLink(pathsTable.getSelectionModel().getSelectedItem().getHtmlDescription()));

        showDescriptionTagsContext.setOnAction(event -> htmlWindow.openText(tagsTree.getSelectionModel().getSelectedItem().getValue().getDescription()));
        showHtmlDescriptionTagsContext.setOnAction(event -> htmlWindow.openInternetLink(tagsTree.getSelectionModel().getSelectedItem().getValue().getHtmlDescription()));
        //</menu events>====================================

        menuBar.getMenus().add(menuFile);
        menuBar.getMenus().add(menuEdit);
        menuBar.getMenus().add(menuUtils);
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
        addEditPathsDialogController.setStyle(styleFileName);
        checkNotAddedPathsWindow.setStyle(styleFileName);
        htmlWindow.setStyle(styleFileName);
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
