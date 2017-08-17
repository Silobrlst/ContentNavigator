package tagfilenav;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.Collection;

public class Path implements Comparable<Path> {
    private StringProperty path;
    private StringProperty name;
    private StringProperty date;
    private String description;
    private String htmlDescription;

    private ArrayList<Tag> tags;
    private ArrayList<PathListener> pathListeners;

    private void init(ArrayList<PathListener> pathListenersIn, String pathIn, String nameIn){
        path = new SimpleStringProperty(pathIn);
        name = new SimpleStringProperty(nameIn);
        date = new SimpleStringProperty("");
        description = "";
        htmlDescription = "";
        tags = new ArrayList<>();
        pathListeners = pathListenersIn;
    }
    public Path(ArrayList<PathListener> pathListenersIn, String pathIn, String nameIn){
        init(pathListenersIn, pathIn, nameIn);
    }
    public Path(ArrayList<PathListener> pathListenersIn, String pathIn){
        String regex = "/";
        String[] splited = getPath().split(regex);
        init(pathListenersIn, pathIn, splited[splited.length-1]);
    }

    //<set>=============================================================================================================
    public void setPath(String pathIn){
        path.set(pathIn);
        pathListeners.forEach(pathListener -> pathListener.changedPath(this));
    }

    public void setName(String nameIn){
        name.set(nameIn);
        pathListeners.forEach(pathListener -> pathListener.renamed(this));
    }

    void setDateAdded(String dateTimeIn){
        date.set(dateTimeIn);
    }

    void setDescription(String descriptionIn){
        description = descriptionIn;
    }

    void setHtmlDescription(String htmlDescriptionIn){
        htmlDescription = htmlDescriptionIn;
    }
    //</set>============================================================================================================

    //<get>=============================================================================================================
    public String getPath(){
        return path.get();
    }

    public String getName(){
        return name.get();
    }

    String getDateAdded(){
        return date.get();
    }

    String getDescription(){
        return description;
    }

    String getHtmlDescription(){
        return htmlDescription;
    }

    StringProperty getNameProperty(){
        return name;
    }

    StringProperty getPathProperty(){
        return path;
    }

    StringProperty getDateProperty(){
        return date;
    }

    public ArrayList<Tag> getTags(){
        return tags;
    }
    //</get>============================================================================================================

    void addTagWithoutNotify(Tag tagIn){
        if(!tags.contains(tagIn)){
            tags.add(tagIn);
            tagIn.addPathWithoutNotifing(this);
        }
    }

    void addTags(Collection<Tag> tagsIn){
        for(Tag tag: tagsIn){
            if(!tags.contains(tag)){
                tags.add(tag);
                tag.addPath(this);
            }
        }

        pathListeners.forEach(pathListener -> pathListener.addedTags(this, tagsIn));
    }


    void removeTagsWithoutNotifing(Collection<Tag> tagsIn){
        for(Tag tag: tagsIn){
            tag.removePathWithoutNotifing(this);
        }

        tags.removeAll(tagsIn);
    }

    //удаляет все теги
    void removeTagsWithoutNotifing(){
        for(Tag tag: tags){
            tag.removePathOnly(this);
        }

        tags.clear();
    }

    void removeTags(Collection<Tag> tagsIn){
        removeTagsWithoutNotifing(tagsIn);
        pathListeners.forEach(pathListener -> pathListener.removedTags(this, tagsIn));
    }

    void removeTagWithoutNotifing(Tag tagIn){
        if(tags.contains(tagIn)){
            tags.remove(tagIn);
            tagIn.removePathOnly(this);
        }
    }

    void removeTag(Tag tagIn){
        if(tags.contains(tagIn)){
            removeTagWithoutNotifing(tagIn);
            pathListeners.forEach(pathListener -> pathListener.removedTag(this, tagIn));
        }
    }

    @Override
    public String toString(){
        return name.getName();
    }

    @Override
    public int compareTo(Path pathIn) {
        return getName().compareTo(pathIn.getPath());
    }
}
