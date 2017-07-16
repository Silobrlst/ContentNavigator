import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AddEditTagDialog {
    @FXML
    private TextField tagName;
    @FXML
    private TextField parent;
    @FXML
    private Button ok;
    @FXML
    private Button cancel;

    private boolean renaming;
    private Tag renamingTag;
    private Tag parentTag;

    private Tags tags;

    private Stage stage;

    @FXML
    public void initialize() {
        ok.setOnAction(event -> onOK());
        cancel.setOnAction(event -> onCancel());

        tagName.setOnAction(event -> onOK());
    }

    public AddEditTagDialog(){
    }

    public void setTagsParent(Stage parentStageIn, Stage stageIn, Tags tagsIn){
        tags = tagsIn;

        stage = stageIn;
        stage.initOwner(parentStageIn);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnShown(event -> {
            tagName.requestFocus();

            if(renaming){
                tagName.setText(renamingTag.getName());
                tagName.selectAll();

                if(renamingTag.getParent() != tags){
                    parent.setText(tags.getTagId(renamingTag.getParent()));
                }
            }else{
                tagName.setText("");

                if(parentTag != null && parentTag != tags){
                    parent.setText(tags.getTagId(parentTag));
                }
            }
        });
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                onCancel();
            }
        });
    }

    public void setAddTag(){
        stage.setTitle("Add Tag");
        renaming = false;
        renamingTag = null;
        parentTag = null;
    }

    public void setAddTag(Tag parentIn){
        stage.setTitle("Add Tag");
        renaming = false;
        renamingTag = null;
        parentTag = parentIn;
    }

    public void setRenameTag(Tag tagIn){
        stage.setTitle("Rename Tag");
        renaming = true;
        renamingTag = tagIn;
    }

    private void onOK() {
        if(renaming){
            renamingTag.rename(tagName.getText());
        }else{
            if(parentTag == null || parentTag == tags){
                tags.newTag(tagName.getText());
            }else{
                tags.newTag(tagName.getText(), tags.getTagById(parent.getText()));
            }
        }

        stage.hide();
    }

    private void onCancel() {
        stage.hide();
    }

    public void open() {
        stage.showAndWait();
    }
}
