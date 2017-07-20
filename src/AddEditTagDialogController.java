import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AddEditTagDialogController {
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

    private String styleFileName;

    @FXML
    public void initialize() {
        ok.setOnAction(event -> onOK());
        cancel.setOnAction(event -> onCancel());

        tagName.setOnAction(event -> onOK());
    }

    public AddEditTagDialogController(){
        styleFileName = "";
    }

    public void setTagsParent(Stage parentStageIn, FXMLLoader loaderIn, Tags tagsIn){
        tags = tagsIn;

        Scene scene = new Scene(loaderIn.getRoot());
        stage = new Stage();
        stage.setScene(scene);
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

    public void setStyle(String styleFileNameIn){
        if(styleFileName.length() > 0){
            stage.getScene().getStylesheets().remove(styleFileName);
        }
        styleFileName = styleFileNameIn;
        stage.getScene().getStylesheets().add(styleFileName);
    }
}
