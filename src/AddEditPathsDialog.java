import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AddEditPathsDialog {
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

    private Stage stage;
    private SavableStyledGui savableStyledGui;

    private boolean editing;
    private List<Path> editingPaths;
    private List<Tag> tagsTemp;
    private List<File> pathsToAdd = null;
    private Paths paths;
    private Tags tags;

    private final Alert alertInformation = new Alert(Alert.AlertType.INFORMATION);
    private final Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);

    @FXML
    public void initialize() {
        ok.setOnAction(event -> onOK());

        cancel.setOnAction(event -> onCancel());

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

        availableTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addedTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        pathsTableName.setCellValueFactory(cellData -> cellData.getValue().nameText);
        pathsTableName.setCellFactory(TextFieldTableCell.forTableColumn());
        pathsTableName.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).nameText.set(event.getNewValue());
        });

        pathsTablePath.setCellValueFactory(cellData -> cellData.getValue().pathText);
        pathsTablePath.setCellFactory(TextFieldTableCell.forTableColumn());
        pathsTablePath.setOnEditCommit(event -> {
            PathTemp pathTemp = event.getTableView().getItems().get(event.getTablePosition().getRow());

            if(editing){
                if(!paths.checkPathExist(event.getNewValue()) || pathTemp.path.getPath().equals(event.getNewValue())){
                    pathTemp.pathText.set(event.getNewValue());
                }else{
                    alertInformation.setTitle("Path already exist");
                    alertInformation.setHeaderText("This path already exist");
                    alertInformation.setContentText("");
                    alertInformation.showAndWait();
                    pathsTable.refresh();
                }
            }else{
                boolean exists = false;

                for(PathTemp pathTemp2: pathsTable.getItems()){
                    if(pathTemp2.pathText.get().equals(event.getNewValue()) && pathTemp != pathTemp2){
                        exists = true;
                    }
                }

                System.out.println(exists);

                if(!paths.checkPathExist(event.getNewValue()) && !exists){
                    pathTemp.pathText.set(event.getNewValue());
                }else{
                    alertInformation.setTitle("Path already exist");
                    alertInformation.setHeaderText("This path already exist");
                    alertInformation.setContentText("");
                    alertInformation.showAndWait();
                    pathsTable.refresh();
                }
            }
        });

        pathsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        pathsTable.setEditable(true);
    }

    public AddEditPathsDialog()throws Exception{}

    public void init(Stage parentStageIn, FXMLLoader loaderIn, Paths pathsIn, Tags tagsIn)throws Exception{
        paths = pathsIn;
        tags = tagsIn;

        String windowName = "addEditPathDialog";

        Scene scene = new Scene(loaderIn.getRoot());
        scene.getRoot().setId(windowName + "Root");
        stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(parentStageIn);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnHidden(event -> savableStyledGui.save());
        stage.setOnShown(event -> onShown());

        pathsTable.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if(event.isControlDown() && event.getCode() == KeyCode.V){
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                if(clipboard.hasFiles()){
                    for(File file: clipboard.getFiles()){
                        pathsTable.getItems().add(new PathTemp(file));
                    }
                }else if(clipboard.hasString()){
                    String[] pathsStr = clipboard.getString().split("\\s*\\n\\s*");
                    for(int i=0; i<pathsStr.length; i++){
                        pathsTable.getItems().add(new PathTemp(new File(pathsStr[i])));
                    }
                }
            }
        });
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

        savableStyledGui = new SavableStyledGui(windowName, stage);
        savableStyledGui.saveTableColumn(pathsTablePath, "pathsTablePath");
        savableStyledGui.saveTableColumn(pathsTableName, "pathsTableName");
        savableStyledGui.load();
    }

    //<on>====================================
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

    private void onOK() {
        List<Tag> addedTagsTemp = tags.getTagsByIds(addedTags.getItems());

        boolean validPaths = true;
        String NonexistentPaths = "";
        for(PathTemp pathTemp: pathsTable.getItems()){
            if(!(new File(pathTemp.pathText.get()).exists())){
                NonexistentPaths += pathTemp.pathText.get() + "\n";
                validPaths = false;
            }
        }

        if(editing){
            ArrayList<Tag> tagsToRemove = new ArrayList<>();
            tagsToRemove.addAll(tagsTemp);
            tagsToRemove.removeAll(addedTagsTemp);

            boolean save = false;

            if(validPaths){
                save = true;
            }else{
                alertConfirm.setTitle("Paths does not exists");
                alertConfirm.setHeaderText("This paths does not exists, save anyway?");
                alertConfirm.setContentText(NonexistentPaths);
                alertConfirm.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> {
                    ((Label)node).setWrapText(false);
                    ((Label)node).setMinHeight(Region.USE_PREF_SIZE);
                    ((Label)node).setTooltip(new Tooltip(((Label)node).getText()));
                });
                Optional<ButtonType> result = alertConfirm.showAndWait();
                if (result.get() == ButtonType.OK) {
                    save = true;
                }
            }

            if(save){
                for(PathTemp pathTemp: pathsTable.getItems()){
                    pathTemp.path.removeTags(tagsToRemove);
                    pathTemp.path.addTags(addedTagsTemp);

                    if(!pathTemp.path.getName().equals(pathTemp.nameText.get())){
                        pathTemp.path.setName(pathTemp.nameText.get());
                    }

                    if(!pathTemp.path.getPath().equals(pathTemp.pathText.get())){
                        pathTemp.path.setPath(pathTemp.pathText.get());
                    }
                }

                stage.hide();
            }
        }else{
            boolean pathsNotAdded = true;
            boolean save = false;

            String addedPaths = "";
            ArrayList<PathTemp> pathTemps = new ArrayList<>();
            for(PathTemp pathTemp: pathsTable.getItems()){
                if(paths.checkPathExist(pathTemp.pathText.get())){
                    pathsNotAdded = false;
                    addedPaths += pathTemp.pathText.get() + "\n";
                    pathTemps.add(pathTemp);
                }
            }

            if(pathsNotAdded && validPaths) {
                save = true;
            }else if(!validPaths){
                alertConfirm.setTitle("Paths does not exists");
                alertConfirm.setHeaderText("This paths does not exists, save anyway?");
                alertConfirm.setContentText(NonexistentPaths);
                alertConfirm.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> {
                    ((Label)node).setWrapText(false);
                    ((Label)node).setMinHeight(Region.USE_PREF_SIZE);
                    ((Label)node).setTooltip(new Tooltip(((Label)node).getText()));
                });
                Optional<ButtonType> result = alertConfirm.showAndWait();
                if (result.get() == ButtonType.OK) {
                    save = true;
                }
            }else if(!pathsNotAdded){
                alertInformation.setTitle("Paths already exists");
                alertInformation.setHeaderText("This paths already exists:");
                alertInformation.setContentText(addedPaths);
                alertInformation.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> {
                    ((Label)node).setWrapText(false);
                    ((Label)node).setMinHeight(Region.USE_PREF_SIZE);
                    ((Label)node).setTooltip(new Tooltip(((Label)node).getText()));
                });
                alertInformation.showAndWait();

                pathsTable.getSelectionModel().clearSelection();
                for(PathTemp pathTemp: pathTemps){
                    pathsTable.getSelectionModel().select(pathTemp);
                }
            }

            if(save){
                for (PathTemp pathTemp : pathsTable.getItems()) {
                    Path newPath;

                    if (pathTemp.nameText.get().isEmpty()) {
                        newPath = paths.newPath(pathTemp.pathText.get());
                    } else {
                        newPath = paths.newPath(pathTemp.pathText.get(), pathTemp.nameText.get());
                    }

                    newPath.addTags(addedTagsTemp);
                }

                stage.hide();
            }
        }
    }

    private void onCancel() {
        // add your code here if necessary
        stage.hide();
    }
    //</on>===================================

    //<set>===================================
    public void setStyle(String styleFileNameIn){
        savableStyledGui.setStyle(styleFileNameIn);
    }

    public void setAddPaths(List<File> pathsToAddIn, List<Tag> tagsIn){
        stage.setTitle("Add Paths");
        apply.setDisable(true);
        editing = false;

        pathsToAdd = pathsToAddIn;
        tagsTemp = tagsIn;
    }

    public void setEditPaths(List<Path> pathsIn){
        stage.setTitle("Edit Paths");
        apply.setDisable(false);
        editing = true;
        editingPaths = pathsIn;

        getTagsFromPaths(pathsIn);
    }
    //</set>==================================

    private void getTagsFromPaths(List<Path> pathsIn){
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

    public void open(){
        stage.showAndWait();
    }

    public boolean checkPathInTable(File pathIn){
        for(PathTemp pathTemp: pathsTable.getItems()){
            if(pathIn.getAbsolutePath().equals(pathTemp.pathText.get())){
                return true;
            }
        }

        return false;
    }


    private class PathTemp{
        public StringProperty pathText;
        public StringProperty nameText;
        public Path path;

        PathTemp(String pathIn, String nameIn){
            pathText = new SimpleStringProperty(pathIn);
            nameText = new SimpleStringProperty(nameIn);
        }
        PathTemp(File fileIn){
            pathText = new SimpleStringProperty(fileIn.getAbsolutePath());
            nameText = new SimpleStringProperty(fileIn.getName());
        }
    }
}
