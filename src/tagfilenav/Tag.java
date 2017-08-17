package tagfilenav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Tag {
    private String name;
    private String description;
    private String htmlDescription;
    private ArrayList<Path> paths;
    private ArrayList<TagListener> tagListeners;
    private Tag parent;
    private ArrayList<Tag> children;


    private void init(ArrayList<TagListener> tagListenersIn, String nameIn, Tag parentIn){
        name = nameIn;
        paths = new ArrayList<>();
        description = "";
        htmlDescription = "";
        tagListeners = tagListenersIn;
        parent = parentIn;
        children = new ArrayList<>();
    }
    public Tag(ArrayList<TagListener> tagListenersIn, String nameIn, Tag parentIn){
        init(tagListenersIn, nameIn, parentIn);
    }
    public Tag(ArrayList<TagListener> tagListenersIn, String nameIn){
        init(tagListenersIn, nameIn, null);
    }


    void addPath(Path pathIn){
        if(!paths.contains(pathIn)) {
            addPathWithoutNotifing(pathIn);
            tagListeners.forEach(tagListener -> tagListener.addedPathToTag(this, pathIn));
        }
    }

    void addPathWithoutNotifing(Path pathIn){
        if(!paths.contains(pathIn)){
            pathIn.addTagWithoutNotify(this);
            paths.add(pathIn);
        }
    }

    void addChildWithoutNotify(Tag tagIn){
        children.add(tagIn);
    }


    void removePathWithoutNotifing(Path pathIn){
        if(paths.contains(pathIn)){
            removePathOnly(pathIn);
            pathIn.removeTagWithoutNotifing(this);
        }
    }

    void removePathOnly(Path pathIn){
        paths.remove(pathIn);
    }

    void removePath(Path pathIn){
        if(paths.contains(pathIn)){
            removePathWithoutNotifing(pathIn);
            tagListeners.forEach(tagListener -> tagListener.removedPathFromTag(this, pathIn));
        }
    }

    void removePaths(Collection<Path> pathsIn){
        for(Path path: pathsIn){
            path.removeTag(this);
            removePath(path);
        }
    }

    void removeChild(Tag tagIn){
        children.remove(tagIn);
    }


    void rename(String nameIn){
        name = nameIn;
        tagListeners.forEach(tagListener -> tagListener.renamedTag(this));
    }

    //<set>=============================================================================================================
    void setDescriptionWithoutNotify(String descriptionIn){
        description = descriptionIn;
    }

    void setHtmlDescriptionWithoutNotify(String htmlDescriptionIn){
        htmlDescription = htmlDescriptionIn;
    }

    void setDescription(String descriptionIn){
        setDescriptionWithoutNotify(descriptionIn);
        tagListeners.forEach(tagListener -> tagListener.changedDescriptions(this));
    }

    void setHtmlDescription(String htmlDescriptionIn){
        setHtmlDescriptionWithoutNotify(htmlDescriptionIn);
        tagListeners.forEach(tagListener -> tagListener.changedDescriptions(this));
    }

    void setParentTag(Tag tagIn){
        Tag prevParent = parent;
        parent = tagIn;
        parent.getChildren().add(this);
        prevParent.getChildren().remove(this);
        tagListeners.forEach(tagListener -> tagListener.changedParent(this, prevParent));
    }
    //</set>============================================================================================================

    //<get>=============================================================================================================
    String getDescription(){
        return description;
    }

    String getHtmlDescription(){
        return htmlDescription;
    }

    public ArrayList<Path> getPaths(){
        return paths;
    }

    public String getName(){
        return name;
    }

    Tag getParent(){
        return parent;
    }

    List<Tag> getChildren(){
        return children;
    }
    //</get>============================================================================================================
}
