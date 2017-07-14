import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Collection;
import java.util.Comparator;

public class PathsTableController {
    @FXML
    private TableView<Path> searchedPaths;
    @FXML
    private TableColumn<Path, String> searchedPathsName;
    @FXML
    private TableColumn<Path, String> searchedPathsPath;

    public PathsTableController(){
    }

    @FXML
    public void initialize() {
        // Инициализация таблицы адресатов с двумя столбцами.
        searchedPathsName.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
        searchedPathsPath.setCellValueFactory(cellData -> cellData.getValue().getPathProperty());
    }

    public void setPaths(Collection<Path> pathsIn){
        searchedPaths.getItems().clear();
        searchedPaths.getItems().addAll(pathsIn);
        searchedPaths.getItems().sort(Comparator.naturalOrder());
    }

    public void clear(){
        searchedPaths.getItems().clear();
    }

    public void removeSelectedPathsFromSelectedTags(Collection<Tag> selectedTagsIn) {
        for (Path selectedPath: searchedPaths.getSelectionModel().getSelectedItems()) {
            selectedPath.removeTags(selectedTagsIn);
        }
        searchedPaths.getItems().removeAll(searchedPaths.getSelectionModel().getSelectedItems());
    }

    public TableView<Path> getTable(){
        return searchedPaths;
    }

    public double getSearchedPathsNameWidth(){
        return searchedPathsName.getWidth();
    }
    public double getSearchedPathsPathWidth(){
        return searchedPathsPath.getWidth();
    }

    public void setSearchedPathsNameWidth(double widthIn){
        searchedPathsName.setPrefWidth(widthIn);
    }
    public void setSearchedPathsPathWidth(double widthIn){
        searchedPathsPath.setPrefWidth(widthIn);
    }
}
