import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SavableGui {
    //<string names>======================
    private static final String xJsonName = "x";
    private static final String yJsonName = "y";
    private static final String widthJsonName = "width";
    private static final String heightJsonName = "height";
    private static final File guiSettings = new File("guiSettings.json");
    //</string names>=====================

    protected String windowName;
    protected Stage stage;

    protected Map<String, TableColumn> tableColumnMap = new HashMap<>();

    SavableGui(String windowNameIn, Stage stageIn){
        windowName = windowNameIn;
        stage = stageIn;
    }

    //запоминать ширину для столбца таблицы
    public void tableColumn(TableColumn tableColumnIn, String nameIn){
        tableColumnMap.put(nameIn, tableColumnIn);
    }

    public void validateGuiSettings(JSONObject jsonIn) {
        if (!jsonIn.has(windowName)) {
            jsonIn.put(windowName, new JSONObject());
        }

        JSONObject windowJSON = jsonIn.getJSONObject(windowName);
        if (!windowJSON.has(xJsonName)) {
            windowJSON.put(xJsonName, 0.d);
        }
        if (!windowJSON.has(yJsonName)) {
            windowJSON.put(yJsonName, 0.d);
        }
        if (!windowJSON.has(widthJsonName)) {
            windowJSON.put(widthJsonName, 0.d);
        }
        if (!windowJSON.has(heightJsonName)) {
            windowJSON.put(heightJsonName, 0.d);
        }

        for (String columnName: tableColumnMap.keySet()){
            if (!windowJSON.has(columnName)) {
                windowJSON.put(columnName, 0);
            }
        }
    }

    public void load() {
        JSONObject guiJSON = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject windowJson = guiJSON.getJSONObject(windowName);
        if (windowJson.getDouble(widthJsonName) > 0) {
            stage.setWidth(windowJson.getDouble(widthJsonName));
        }
        if (windowJson.getDouble(heightJsonName) > 0) {
            stage.setHeight(windowJson.getDouble(heightJsonName));
        }
        if (windowJson.getDouble(xJsonName) > 0) {
            stage.setX(windowJson.getDouble(xJsonName));
        }
        if (windowJson.getDouble(yJsonName) > 0) {
            stage.setY(windowJson.getDouble(yJsonName));
        }

        for (String columnName: tableColumnMap.keySet()){
            if (windowJson.getDouble(columnName) > 0) {
                tableColumnMap.get(columnName).setPrefWidth(windowJson.getInt(columnName));
            }
        }
    }

    public void save() {
        JSONObject guiJSON = JSONLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject windowJson = guiJSON.getJSONObject(windowName);
        windowJson.put(xJsonName, stage.getX());
        windowJson.put(yJsonName, stage.getY());
        windowJson.put(widthJsonName, stage.getWidth());
        windowJson.put(heightJsonName, stage.getHeight());

        for(String columnName: tableColumnMap.keySet()){
            windowJson.put(columnName, tableColumnMap.get(columnName).getWidth());
        }

        JSONLoader.saveJSON(guiSettings, guiJSON);
    }
}
