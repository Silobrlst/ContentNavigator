package tagfilenav;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AddEditPathDialog {
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
    @FXML
    private TextArea description;
    @FXML
    private TextField htmlFile;
    @FXML
    private Button exploreHtmlFile;

    private Stage stage;
    private StyledGuiSaver savableStyledGui;

    private boolean editing;
    private Path editingPath;
    private List<Tag> tagsTemp;
    private Paths paths;
    private Tags tags;

    private final Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);
    private FileChooser fileChooser = new FileChooser();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private File initialDirectory = new File(System.getProperty("user.dir"));

    @FXML
    public void initialize() {
        ok.setOnAction(event -> onOK());

        cancel.setOnAction(event -> onCancel());

        exploreFile.setOnAction(event -> onExploreFile());

        exploreDirectory.setOnAction(event -> onExploreDirectory());

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

        exploreHtmlFile.setOnAction(event -> onExploreHtmlFile());

        htmlFile.setOnAction(event -> {
            final WebView browser = new WebView();
            final WebEngine webEngine = browser.getEngine();
            webEngine.load(htmlFile.getText());

            Stage htmlWindow = new Stage();
            htmlWindow.setScene(new Scene(new VBox()));
            ((VBox)htmlWindow.getScene().getRoot()).getChildren().add(browser);
            htmlWindow.initOwner(stage);
            htmlWindow.show();
        });

        availableTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addedTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

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
        stage.setOnShown(event -> onShown());
        stage.setOnHidden(event -> savableStyledGui.save());
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                onCancel();
            }
        });

        savableStyledGui = new StyledGuiSaver(windowName, stage);
        savableStyledGui.load();

        setAddPath();
    }

    //<on>==============================================================================================================
    private void onShown(){
        pathValidation.setText("");

        availableTags.getItems().clear();
        availableTags.getItems().addAll(tags.getTagsIds());
        availableTags.getItems().sort(Comparator.naturalOrder());

        if(editing){
            addedTags.getItems().clear();
            addedTags.getItems().addAll(tags.getTagsIds(tagsTemp));
            availableTags.getItems().removeAll(addedTags.getItems());

            description.setText(editingPath.getDescription());
            htmlFile.setText(editingPath.getHtmlDescription());
        }else{
            addedTags.getItems().clear();

            description.setText("");
            htmlFile.setText("");

            if(tagsTemp != null){
                addedTags.getItems().addAll(tags.getTagsIds(tagsTemp));
                availableTags.getItems().removeAll(addedTags.getItems());
            }
        }

        path.requestFocus();
    }

    private void onExploreFile(){
        File file = fileChooser.showOpenDialog(stage);
        if(file != null){
            path.setText(file.getAbsolutePath());
            if(name.getText().isEmpty()){
                name.setText(file.getName());
            }
            pathValidation.setText("");
            initialDirectory = new File(file.getAbsolutePath().split("[/\\\\][^\\\\/]*$")[0]);
        }
    }

    private void onExploreDirectory(){
        File file = directoryChooser.showDialog(stage);
        if(file != null){
            path.setText(file.getAbsolutePath());
            if(name.getText().isEmpty()){
                name.setText(file.getName());
            }
            pathValidation.setText("");
            initialDirectory = new File(file.getAbsolutePath().split("[/\\\\][^\\\\/]*$")[0]);
        }
    }

    private void onExploreHtmlFile(){
        File file = fileChooser.showOpenDialog(stage);
        if(file != null){
            htmlFile.setText(file.toURI().toString());
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
    //</on>=============================================================================================================

    //<set>=============================================================================================================
    void setAddPath(List<Tag> tagsIn){
        stage.setTitle("Add Path");
        apply.setDisable(true);
        editing = false;
        name.setText("");
        path.setText("");

        tagsTemp = tagsIn;
    }

    void setAddPath(List<Tag> tagsIn, File pathIn){
        setAddPath(tagsIn);
        path.setText(pathIn.getAbsolutePath());
        name.setText(pathIn.getName());
    }

    void setAddPath(){
        setAddPath(null);
    }

    void setEditPath(Path pathIn){
        stage.setTitle("Edit Path");
        apply.setDisable(false);
        editing = true;
        editingPath = pathIn;
        path.setText(editingPath.getPath());
        name.setText(editingPath.getName());
        System.out.println(editingPath.getName());
        tagsTemp = pathIn.getTags();
    }

    void setStyle(String styleFileNameIn){
        savableStyledGui.setStyle(styleFileNameIn);
    }
    //</set>============================================================================================================

    private void saveAndExit(){
        List<Tag> addedTagsTemp = tags.getTagsByIds(addedTags.getItems());

        if(editing){
            ArrayList<Tag> tagsToRemove = new ArrayList<>();
            tagsToRemove.addAll(editingPath.getTags());
            tagsToRemove.removeAll(addedTagsTemp);

            editingPath.setPath(path.getText());
            editingPath.setName(name.getText());
            editingPath.setDescription(description.getText());
            editingPath.setHtmlDescription(htmlFile.getText());

            editingPath.removeTags(tagsToRemove);
            editingPath.addTags(addedTagsTemp);

            stage.hide();
        }else{
            if(!paths.checkPathAdded(path.getText())){
                Path newPath;

                if(name.getText().isEmpty()){
                    newPath = paths.newPath(path.getText());
                }else{
                    newPath = paths.newPath(path.getText(), name.getText());
                }

                newPath.addTags(addedTagsTemp);

                newPath.setDescription(description.getText());
                newPath.setHtmlDescription(htmlFile.getText());

                stage.hide();
            }else{
                pathValidation.setText("This path is already added");
            }
        }
    }

    void open(){
        stage.showAndWait();
    }
}
