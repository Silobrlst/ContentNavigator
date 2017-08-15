import javafx.stage.Stage;

public class SavableStyledGui extends SavableGui {
    private String styleFileName = "";

    SavableStyledGui(String windowNameIn, Stage stageIn){
        super(windowNameIn, stageIn);
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

    public String getStyle(){
        return styleFileName;
    }
}
