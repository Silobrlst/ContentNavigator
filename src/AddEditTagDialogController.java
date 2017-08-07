import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.Optional;

public class AddEditTagDialogController {
    @FXML
    private TextField tagName;
    @FXML
    private ComboBox<String> parent;
    @FXML
    private Button ok;
    @FXML
    private Button cancel;

    private boolean editing;
    private Tag editingTag;
    private Tag parentTag;

    private Tags tags;

    private Stage stage;

    private String styleFileName;

    private final Alert alertConfirm = new Alert(Alert.AlertType.CONFIRMATION);

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
        scene.getRoot().setId("addEditTagDialogRoot");
        stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(parentStageIn);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnShown(event -> onShown());
        stage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                onCancel();
            }
        });
    }

    private void onShown(){
        tagName.requestFocus();

        parent.getItems().clear();
        parent.getItems().add("");
        parent.getItems().addAll(tags.getTagsIds());
        parent.getItems().sort(Comparator.naturalOrder());

        new ComboBoxAutoComplete<>(parent);

        if(editing){
            tagName.setText(editingTag.getName());
            tagName.selectAll();

            if(editingTag.getParent() != tags){
                parent.getSelectionModel().select(tags.getTagId(editingTag.getParent()));
            }
        }else{
            tagName.setText("");

            if(parentTag != null && parentTag != tags){
                parent.getSelectionModel().select(tags.getTagId(parentTag));
            }
        }

        stage.sizeToScene();
    }

    public void setAddTag(){
        setAddTag(null);
    }

    public void setAddTag(Tag parentIn){
        stage.setTitle("Add Tag");
        editing = false;
        editingTag = null;
        parentTag = parentIn;

        if(parentIn == null){
            parent.getSelectionModel().select("");
        }

        tagName.setText("");
    }

    public void setEditTag(Tag tagIn){
        stage.setTitle("Edit Tag");
        editing = true;
        editingTag = tagIn;
    }

    private void onOK() {
        boolean valid = true;

        if(editing){
            if(!tagName.getText().equals(editingTag.getName())){
                editingTag.rename(tagName.getText());
            }

            if(parent.getSelectionModel().getSelectedItem().isEmpty()){
                editingTag.setParentTag(tags);
            }else{
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
            if(parentTag == null || parentTag == tags){
                tags.newTag(tagName.getText());
            }else{
                tags.newTag(tagName.getText(), tags.getTagById(parent.getSelectionModel().getSelectedItem()));
            }
        }

        if(valid){
            stage.hide();
        }
    }

    private void onCancel() {
        stage.hide();
    }

    public void open() {
        stage.showAndWait();
    }

    public void setStyle(String styleFileNameIn){
        if(styleFileNameIn.equals("default")){
            if(styleFileName.length() > 0){
                stage.getScene().getStylesheets().remove(styleFileName);
            }
            styleFileName = "";
            return;
        }

        if(styleFileName.length() > 0){
            stage.getScene().getStylesheets().remove(styleFileName);
        }
        styleFileName = styleFileNameIn;
        stage.getScene().getStylesheets().add(styleFileName);
    }
}
