import java.util.ArrayList;
import java.util.List;

public class Tag {
    private String name;
    private ArrayList<Path> paths;
    private ArrayList<TagListener> tagListeners;
    private Tag parent;
    private ArrayList<Tag> children;

    public Tag(ArrayList<TagListener> tagListenersIn, String nameIn, Tag parentIn){
        name = nameIn;
        paths = new ArrayList<>();
        tagListeners = tagListenersIn;
        parent = parentIn;
        children = new ArrayList<>();
    }

    public Tag(ArrayList<TagListener> tagListenersIn, String nameIn){
        name = nameIn;
        paths = new ArrayList<>();
        tagListeners = tagListenersIn;
        parent = null;
        children = new ArrayList<>();
    }

    public void addPath(Path pathIn){
        if(!paths.contains(pathIn)) {
            addPathWithoutNotifing(pathIn);
            tagListeners.forEach(tagListener -> tagListener.addedPathToTag(this, pathIn));
        }
    }
    public void addPathWithoutNotifing(Path pathIn){
        if(!paths.contains(pathIn)){
            pathIn.addTagWithoutNotifing(this);
            paths.add(pathIn);
        }
    }

    public void removePath(Path pathIn){
        if(paths.contains(pathIn)){
            paths.remove(pathIn);
            this.removePath(pathIn);
            pathIn.removeTag(this);
            tagListeners.forEach(tagListener -> tagListener.removedPathFromTag(this, pathIn));
        }
    }

    public void rename(String nameIn){
        name = nameIn;
        tagListeners.forEach(tagListener -> tagListener.editedTag(this));
    }

    public ArrayList<Path> getPaths(){
        return paths;
    }

    public String getName(){
        return name;
    }

    public void setParentTag(Tag tagIn){
        parent = tagIn;
    }

    public Tag getParent(){
        return parent;
    }

    public List<Tag> getChildren(){
        return children;
    }

    public void addChildWithoutNotify(Tag tagIn){
        children.add(tagIn);
    }

    public void removeChild(Tag tagIn){
        children.remove(tagIn);
    }
}
