package tagfilenav;

import javafx.stage.Stage;

class StyledGuiSaver extends GuiSaver {
    private String styleFileName = "";

    StyledGuiSaver(String windowNameIn, Stage stageIn){
        super(windowNameIn, stageIn);
    }

    void setStyle(String styleFileNameIn){
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

    String getStyle(){
        return styleFileName;
    }
}
