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

public class SavableGui {
    //<string names>======================
    private static final String xJsonName = "x";
    private static final String yJsonName = "y";
    private static final String widthJsonName = "width";
    private static final String heightJsonName = "height";
    private static final String maximizedJsonName = "maximized";
    private static final String sortColumnJsonName = "sortColumn";
    private static final String sortTypeJsonName = "sortType";
    private static final File guiSettings = new File("guiSettings.json");
    //</string names>=====================

    protected String windowName;
    protected Stage stage;

    private boolean windowMaximizedSave = false;

    private Map<String, TableColumn> saveTableColumnMap = new HashMap<>();
    private Map<String, SplitPane> saveSplitPaneMap = new HashMap<>();
    private Map<String, TableView> saveTableViewMap = new HashMap<>();

    SavableGui(String windowNameIn, Stage stageIn){
        windowName = windowNameIn;
        stage = stageIn;
    }

    void saveWindowMaximized(boolean saveWindowMaximizedIn){
        windowMaximizedSave = saveWindowMaximizedIn;
    }

    //запоминать SplitPane
    public void saveSplitPane(SplitPane splitPaneIn, String nameIn){
        saveSplitPaneMap.put(nameIn, splitPaneIn);
    }

    //запоминать ширину для столбца таблицы
    public void saveTableColumn(TableColumn tableColumnIn, String nameIn){
        saveTableColumnMap.put(nameIn, tableColumnIn);
    }

    public void saveTableSorting(TableView tableViewIn, String nameIn){
        saveTableViewMap.put(nameIn, tableViewIn);
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

        if(windowMaximizedSave){
            if (!windowJSON.has(maximizedJsonName)) {
                windowJSON.put(maximizedJsonName, false);
            }
        }

        for (String columnName: saveTableColumnMap.keySet()){
            if (!windowJSON.has(columnName)) {
                windowJSON.put(columnName, 0);
            }
        }

        for (String splitPaneName: saveSplitPaneMap.keySet()){
            if (!windowJSON.has(splitPaneName)) {
                windowJSON.put(splitPaneName, 0.5d);
            }
        }

        for(String tableViewName: saveTableViewMap.keySet()){
            if (!windowJSON.has(tableViewName)) {
                windowJSON.put(tableViewName, new JSONObject());
            }

            JSONObject tableViewJson = windowJSON.getJSONObject(tableViewName);
            if (!tableViewJson.has(sortColumnJsonName)) {
                tableViewJson.put(sortColumnJsonName, "");
            }
            if (!tableViewJson.has(sortTypeJsonName)) {
                tableViewJson.put(sortTypeJsonName, TableColumn.SortType.DESCENDING.name());
            }
        }
    }

    public void load() {
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
                if (windowJson.getDouble(columnName) > 0) {
                    saveTableColumnMap.get(columnName).setPrefWidth(windowJson.getInt(columnName));
                }
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

    public void save() {
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
            windowJson.put(columnName, saveTableColumnMap.get(columnName).getWidth());
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
