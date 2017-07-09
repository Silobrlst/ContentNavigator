import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddRenameTagDialog extends Stage {
    private TextField tagName;

    private boolean renaming;
    private String oldTagName;

    private AddChangeInterface addChangeInterface;

    AddRenameTagDialog(AddChangeInterface addChangeInterfaceIn)throws Exception{
        addChangeInterface = addChangeInterfaceIn;

        Parent root = FXMLLoader.load(getClass().getResource("AddRenameTagDialog.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add("modena_dark.css");

        tagName = (TextField)scene.lookup("#tagName");

        tagName.setOnAction(event -> {
            if(tagName.getText().length() > 0){
                onOK();
            }
        });

        ((Button)scene.lookup("#ok")).setOnAction(event -> {
            onOK();
        });

        ((Button)scene.lookup("#cancel")).setOnAction(event -> {
            onCancel();
        });

        this.setOnShown(event -> {
            tagName.requestFocus();
        });

        this.setScene(scene);
    }

    public void setAddTag(){
        this.setTitle("Add Tag");
        renaming = false;
        tagName.setText("");
    }

    public void setRenameTag(String tagNameIn){
        this.setTitle("Rename Tag");
        renaming = true;
        tagName.setText(tagNameIn);
    }

    private void onOK() {
        if(renaming){
            addChangeInterface.change(oldTagName, tagName.getText());
        }else{
            addChangeInterface.add(tagName.getText());
        }

        this.hide();
    }

    private void onCancel() {
        // add your code here if necessary
        this.hide();
    }
}
