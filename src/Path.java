import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.Collection;

public class Path implements Comparable<Path> {
    private StringProperty path;
    private StringProperty name;
    private StringProperty date;

    private ArrayList<Tag> tags;
    private ArrayList<PathListener> pathListeners;

    public Path(ArrayList<PathListener> pathListenersIn, String pathIn, String nameIn){
        path = new SimpleStringProperty(pathIn);
        name = new SimpleStringProperty(nameIn);
        date = new SimpleStringProperty("");
        tags = new ArrayList<>();
        pathListeners = pathListenersIn;
    }
    public Path(ArrayList<PathListener> pathListenersIn, String pathIn){
        path = new SimpleStringProperty(pathIn);

        String regex = "/";
        String[] splited = getPath().split(regex);
        name = new SimpleStringProperty(splited[splited.length-1]);

        date = new SimpleStringProperty("");

        tags = new ArrayList<>();
        pathListeners = pathListenersIn;
    }

    public void setPath(String pathIn){
        path.set(pathIn);
        pathListeners.forEach(pathListener -> pathListener.changedPath(this));
    }

    public void setName(String nameIn){
        name.set(nameIn);
        pathListeners.forEach(pathListener -> pathListener.renamed(this));
    }

    public void setDateAdded(String dateTimeIn){
        date.set(dateTimeIn);
    }

    public String getPath(){
        return path.get();
    }

    public String getName(){
        return name.get();
    }

    public String getDateAdded(){
        return date.get();
    }

    public StringProperty getNameProperty(){
        return name;
    }

    public StringProperty getPathProperty(){
        return path;
    }

    public StringProperty getDateProperty(){
        return date;
    }

    public void addTagWithoutNotifing(Tag tagIn){
        if(!tags.contains(tagIn)){
            tags.add(tagIn);
            tagIn.addPathWithoutNotifing(this);
        }
    }


    public void addTags(Collection<Tag> tagsIn){
        for(Tag tag: tagsIn){
            if(!tags.contains(tag)){
                tags.add(tag);
                tag.addPath(this);
            }
        }

        pathListeners.forEach(pathListener -> pathListener.addedTags(this, tagsIn));
    }

    public void removeTagsWithoutNotifing(Collection<Tag> tagsIn){
        for(Tag tag: tagsIn){
            tag.removePathWithoutNotifing(this);
        }

        tags.removeAll(tagsIn);
    }

    //удаляет все теги
    public void removeTagsWithoutNotifing(){
        for(Tag tag: tags){
            tag.removePathOnly(this);
        }

        tags.clear();
    }

    public void removeTags(Collection<Tag> tagsIn){
        removeTagsWithoutNotifing(tagsIn);
        pathListeners.forEach(pathListener -> pathListener.removedTags(this, tagsIn));
    }

    public void removeTagWithoutNotifing(Tag tagIn){
        if(tags.contains(tagIn)){
            tags.remove(tagIn);
            tagIn.removePathOnly(this);
        }
    }

    public void removeTag(Tag tagIn){
        if(tags.contains(tagIn)){
            removeTagWithoutNotifing(tagIn);
            pathListeners.forEach(pathListener -> pathListener.removedTag(this, tagIn));
        }
    }

    public ArrayList<Tag> getTags(){
        return tags;
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
