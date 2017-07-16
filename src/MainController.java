import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class MainController {
    @FXML
    private TableView<Path> searchedPaths;
    @FXML
    private TableColumn<Path, String> searchedPathsName;
    @FXML
    private TableColumn<Path, String> searchedPathsPath;
    @FXML
    private TreeView<Tag> tagsTree;

    public MainController(){

    }

    public void setTags(Tags tagsIn){
        TreeItem<Tag> root = new TreeItem<>(tagsIn);
        tagsTree.setRoot(root);
        root.setExpanded(true);
    }

    @FXML
    public void initialize() {
        searchedPathsName.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
        searchedPathsPath.setCellValueFactory(cellData -> cellData.getValue().getPathProperty());
        searchedPaths.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tagsTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tagsTree.setCellFactory(param -> new TreeCell<Tag>() {
            @Override
            protected void updateItem(Tag tagIn, boolean empty) {
                super.updateItem(tagIn, empty);
                if (tagIn != null) {
                    setText(tagIn.getName());
                } else {
                    setText("");   // <== clear the now empty cell.
                }
            }
        });
    }

    public void removeSelectedTags() {
        Collection<TreeItem<Tag>> selectedTags = tagsTree.getSelectionModel().getSelectedItems();

        for (TreeItem<Tag> selectedTag: selectedTags) {
            if(selectedTag != tagsTree.getRoot()){
                selectedTag.getParent().getChildren().remove(selectedTag);
                selectedTag.getValue().getParent().removeChild(selectedTag.getValue());
            }
        }
    }

    public TreeView<Tag> getTagsTree(){
        return tagsTree;
    }

    public void addTagToRoot(Tag tagTreeItemIn){
        tagsTree.getRoot().getChildren().add(new TreeItem<>(tagTreeItemIn));
    }

    public void clearTags(){
        tagsTree.getRoot().getChildren().clear();
    }

    public Collection<TreeItem<Tag>> getSelectedTags(){
        return tagsTree.getSelectionModel().getSelectedItems();
    }

    private TreeItem<Tag> getTreeItemByTag(Tag tagIn, TreeItem<Tag> treeItemParentIn){
        if(treeItemParentIn.getValue() == tagIn){
            return treeItemParentIn;
        }else{
            for(TreeItem<Tag> tagTreeItem: treeItemParentIn.getChildren()){
                TreeItem<Tag> tagTreeItemTemp = getTreeItemByTag(tagIn, tagTreeItem);
                if(tagTreeItemTemp != null){
                    return tagTreeItemTemp;
                }
            }
        }

        return null;
    }

    public TreeItem<Tag> getTreeItemByTag(Tag tagIn){
        return getTreeItemByTag(tagIn, tagsTree.getRoot());
    }

    public void setPaths(Collection<Path> pathsIn){
        searchedPaths.getItems().clear();
        searchedPaths.getItems().addAll(pathsIn);
        searchedPaths.getItems().sort(Comparator.naturalOrder());
    }

    public void clearPaths(){
        searchedPaths.getItems().clear();
    }

    public void removeSelectedPathsFromSelectedTags(Collection<TreeItem<Tag>> selectedTagsIn) {
        for (Path selectedPath: searchedPaths.getSelectionModel().getSelectedItems()) {
            ArrayList<Tag> tags = new ArrayList<>();
            for(TreeItem<Tag> tagItem: selectedTagsIn){
                tags.add(tagItem.getValue());
            }
            selectedPath.removeTags(tags);
        }
        searchedPaths.getItems().removeAll(searchedPaths.getSelectionModel().getSelectedItems());
    }

    public TableView<Path> getPathsTable(){
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
