package tagfilenav;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.Comparator;
import java.util.Optional;

public class AddEditTagDialog {
    @FXML private TextField tagName;
    @FXML private ComboBox<String> parent;
    @FXML private Button ok;
    @FXML private Button cancel;
    @FXML private Button exploreHtml;
    @FXML private TextField htmlFile;
    @FXML private TextArea description;

    private Stage stage;
    private StyledGuiSaver savableStyledGui;

    private boolean editing;
    private Tag editingTag;
    private Tag parentTag;
    private Tags tags;

    private final Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);
    private final FileChooser fileChooser = new FileChooser();

    @FXML public void initialize() {
        ok.setOnAction(event -> onOK());
        cancel.setOnAction(event -> onCancel());

        tagName.setOnAction(event -> onOK());

        exploreHtml.setOnAction(event -> onExploreHtmlFile());
    }

    public AddEditTagDialog(){}

    public void init(Stage parentStageIn, FXMLLoader loaderIn, Tags tagsIn){
        tags = tagsIn;

        String windowName = "addEditTagDialog";

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
    }

    //<on>==============================================================================================================
    private void onExploreHtmlFile(){
        File file = fileChooser.showOpenDialog(stage);
        if(file != null){
            htmlFile.setText(file.toURI().toString());
        }
    }

    private void onShown(){
        tagName.requestFocus();

        description.setText("");
        htmlFile.setText("");

        parent.getItems().clear();
        parent.getItems().add("");
        parent.getItems().addAll(tags.getTagsIds());
        parent.getItems().sort(Comparator.naturalOrder());

        new ComboBoxAutoComplete<>(parent);

        if(editing){
            tagName.setText(editingTag.getName());
            tagName.selectAll();

            description.setText(editingTag.getDescription());
            htmlFile.setText(editingTag.getHtmlDescription());

            if(editingTag.getParent() != tags){
                parent.getSelectionModel().select(tags.getTagId(editingTag.getParent()));
            }else{
                parent.getSelectionModel().select("");
            }
        }else{
            tagName.setText("");

            if(parentTag != null && parentTag != tags){
                parent.getSelectionModel().select(tags.getTagId(parentTag));
            }
        }
    }

    private void onOK() {
        boolean valid = true;

        if(editing){
            if(!editingTag.getDescription().equals(description.getText())){
                editingTag.setDescription(description.getText());
            }

            if(!editingTag.getHtmlDescription().equals(htmlFile.getText())){
                editingTag.setHtmlDescription(htmlFile.getText());
            }

            if(!tagName.getText().equals(editingTag.getName())){
                editingTag.rename(tagName.getText());
            }

            if(parent.getSelectionModel().getSelectedItem().isEmpty() && tags != editingTag.getParent()){
                editingTag.setParentTag(tags);
            }else if(!parent.getSelectionModel().getSelectedItem().equals(tags.getTagId(editingTag.getParent()))){
                Tag newParent = tags.getTagById(parent.getSelectionModel().getSelectedItem());

                //проверяем чтобы новый родительский тег не был потомком текущего тега либо самим потомком
                boolean recursiveParent = parent.getSelectionModel().getSelectedItem().contains(tags.getTagId(editingTag));
                if(!recursiveParent){
                    editingTag.setParentTag(newParent);
                }

                if(recursiveParent){
                    alertConfirm.setTitle("Incorrect parent");
                    alertConfirm.setHeaderText("Choosed parent is sub-child of current tag.\nPlease choose parent that is not child of current tag");
                    Optional<ButtonType> result = alertConfirm.showAndWait();
                    if (result.get() == ButtonType.OK) {
                        valid = false;
                    }
                }
            }
        }else{
            Tag tag;

            if(parentTag == null || parentTag == tags){
                tag = tags.newTag(tagName.getText());
            }else{
                tag = tags.newTag(tagName.getText(), tags.getTagById(parent.getSelectionModel().getSelectedItem()));
            }

            tag.setDescription(description.getText());
            tag.setHtmlDescription(htmlFile.getText());
        }

        if(valid){
            stage.hide();
        }
    }

    private void onCancel() {
        stage.hide();
    }
    //</on>=============================================================================================================

    //<set>=============================================================================================================
    void setAddTag(){
        setAddTag(null);
    }

    void setAddTag(Tag parentIn){
        stage.setTitle("Add Tag");
        editing = false;
        editingTag = null;
        parentTag = parentIn;

        if(parentIn == null || parentIn == tags){
            parent.getSelectionModel().select("");
        }

        tagName.setText("");
    }

    void setEditTag(Tag tagIn){
        stage.setTitle("Edit Tag");
        editing = true;
        editingTag = tagIn;
    }

    void setStyle(String styleFileNameIn){
        savableStyledGui.setStyle(styleFileNameIn);
    }
    //</set>============================================================================================================

    void open() {
        stage.showAndWait();
    }
}
