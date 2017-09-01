package tagfilenav;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GuiSaver {
    //<string names>====================================================================================================
    private static final String xJsonName = "x";
    private static final String yJsonName = "y";
    private static final String widthJsonName = "width";
    private static final String heightJsonName = "height";
    private static final String maximizedJsonName = "maximized";
    private static final String sortColumnJsonName = "sortColumn";
    private static final String sortTypeJsonName = "sortType";
    private static final String visibilityJsonName = "visibility";
    private static final File guiSettings = new File("guiSettings.json");
    //</string names>===================================================================================================

    protected String windowName;
    protected Stage stage;

    private boolean windowMaximizedSave = false;

    private Map<String, TableColumn> saveTableColumnMap = new HashMap<>();
    private Map<String, SplitPane> saveSplitPaneMap = new HashMap<>();
    private Map<String, TableView> saveTableViewMap = new HashMap<>();

    GuiSaver(String windowNameIn, Stage stageIn){
        windowName = windowNameIn;
        stage = stageIn;
    }

    void saveWindowMaximized(boolean saveWindowMaximizedIn){
        windowMaximizedSave = saveWindowMaximizedIn;
    }

    //запоминать SplitPane
    void saveSplitPane(SplitPane splitPaneIn, String nameIn){
        saveSplitPaneMap.put(nameIn, splitPaneIn);
    }

    //запоминать ширину и видимость для столбца таблицы
    void saveTableColumn(TableColumn tableColumnIn, String nameIn){
        saveTableColumnMap.put(nameIn, tableColumnIn);
    }

    void saveTableSorting(TableView tableViewIn, String nameIn){
        saveTableViewMap.put(nameIn, tableViewIn);
    }

    private void validateJsonKey(JSONObject jsonIn, String nameIn, Object defaultIn){
        if (!jsonIn.has(nameIn)) {
            jsonIn.put(nameIn, defaultIn);
        }
    }

    private void validateJsonKey(JSONObject jsonIn, String nameIn, JSONObject defaultIn){
        if (!jsonIn.has(nameIn)) {
            jsonIn.put(nameIn, defaultIn);
        }

        if(jsonIn.optJSONObject(nameIn) == null){
            jsonIn.put(nameIn, defaultIn);
        }
    }

    private void validateGuiSettings(JSONObject jsonIn) {
        if (!jsonIn.has(windowName)) {
            jsonIn.put(windowName, new JSONObject());
        }

        JSONObject windowJSON = jsonIn.getJSONObject(windowName);
        validateJsonKey(windowJSON, xJsonName, 0.d);
        validateJsonKey(windowJSON, yJsonName, 0.d);
        validateJsonKey(windowJSON, widthJsonName, 0.d);
        validateJsonKey(windowJSON, heightJsonName, 0.d);

        if(windowMaximizedSave){
            validateJsonKey(windowJSON, maximizedJsonName, false);
        }

        for (String columnName: saveTableColumnMap.keySet()){
            validateJsonKey(windowJSON, columnName, new JSONObject());

            JSONObject columnJson = windowJSON.optJSONObject(columnName);
            validateJsonKey(columnJson, widthJsonName, 0);
            validateJsonKey(columnJson, visibilityJsonName, true);
        }

        for (String splitPaneName: saveSplitPaneMap.keySet()){
            validateJsonKey(windowJSON, splitPaneName, 0.5d);
        }

        for(String tableViewName: saveTableViewMap.keySet()){
            validateJsonKey(windowJSON, tableViewName, new JSONObject());

            JSONObject tableViewJson = windowJSON.getJSONObject(tableViewName);

            validateJsonKey(tableViewJson, sortColumnJsonName, "");
            validateJsonKey(tableViewJson, sortTypeJsonName, TableColumn.SortType.DESCENDING.name());
        }
    }

    void load() {
        JSONObject guiJSON = JsonLoader.loadJSON(guiSettings);
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

        if(windowMaximizedSave){
            stage.setMaximized(windowJson.getBoolean(maximizedJsonName));
        }

        new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            for (String splitPaneName: saveSplitPaneMap.keySet()){
                saveSplitPaneMap.get(splitPaneName).setDividerPosition(0, windowJson.getDouble(splitPaneName));
            }

            for (String columnName: saveTableColumnMap.keySet()){
                JSONObject columnJson = windowJson.getJSONObject(columnName);

                if (columnJson.getDouble(widthJsonName) > 0) {
                    saveTableColumnMap.get(columnName).setPrefWidth(columnJson.getInt(widthJsonName));
                }

                saveTableColumnMap.get(columnName).setVisible(columnJson.getBoolean(visibilityJsonName));
            }

            for(String tableViewName: saveTableViewMap.keySet()){
                TableView tableView = saveTableViewMap.get(tableViewName);
                JSONObject tableViewJson = windowJson.getJSONObject(tableViewName);

                TableColumn tableColumn = getColumnById(tableView, tableViewJson.getString(sortColumnJsonName));
                if(tableColumn != null){
                    tableView.getSortOrder().set(0, tableColumn);
                    tableColumn.setSortType(TableColumn.SortType.valueOf(tableViewJson.getString(sortTypeJsonName)));
                    tableColumn.setSortable(true);
                }
            }
        })).play();
    }

    void save() {
        JSONObject guiJSON = JsonLoader.loadJSON(guiSettings);
        validateGuiSettings(guiJSON);

        JSONObject windowJson = guiJSON.getJSONObject(windowName);

        if(windowMaximizedSave){
            windowJson.put(maximizedJsonName, stage.isMaximized());
        }else{
            windowJson.put(xJsonName, stage.getX());
            windowJson.put(yJsonName, stage.getY());
            windowJson.put(widthJsonName, stage.getWidth());
            windowJson.put(heightJsonName, stage.getHeight());
        }

        for(String splitPaneName: saveSplitPaneMap.keySet()){
            windowJson.put(splitPaneName, saveSplitPaneMap.get(splitPaneName).getDividerPositions()[0]);
        }

        for(String columnName: saveTableColumnMap.keySet()){
            JSONObject columnJson = new JSONObject();
            windowJson.put(columnName, columnJson);

            columnJson.put(widthJsonName, saveTableColumnMap.get(columnName).getWidth());
            columnJson.put(visibilityJsonName, saveTableColumnMap.get(columnName).isVisible());
        }

        for(String tableViewName: saveTableViewMap.keySet()){
            JSONObject tableViewJson = new JSONObject();
            windowJson.put(tableViewName, tableViewJson);

            if (saveTableViewMap.get(tableViewName).getSortOrder().size()>0) {
                TableColumn sortColumn = (TableColumn) saveTableViewMap.get(tableViewName).getSortOrder().get(0);
                TableColumn.SortType sortType = sortColumn.getSortType();

                tableViewJson.put(sortColumnJsonName, sortColumn.getId());
                tableViewJson.put(sortTypeJsonName, sortType.name());
            }
        }

        JsonLoader.saveJSON(guiSettings, guiJSON);
    }

    private TableColumn getColumnById(TableView tableView, String columnIdIn){
        for(TableColumn tableColumn: (ObservableList<TableColumn>)tableView.getColumns()){
            if(tableColumn.getId().equals(columnIdIn)){
                return tableColumn;
            }
        }

        return null;
    }
}
