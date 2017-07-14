import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.Collection;

public class Path implements Comparable<Path> {
    private StringProperty path;
    private StringProperty name;

    private ArrayList<Tag> tags;
    private ArrayList<PathListener> pathListeners;

    public Path(ArrayList<PathListener> pathListenersIn, String pathIn, String nameIn){
        path = new SimpleStringProperty(pathIn);
        name = new SimpleStringProperty(nameIn);
        tags = new ArrayList<>();
        pathListeners = pathListenersIn;
    }
    public Path(ArrayList<PathListener> pathListenersIn, String pathIn){
        path = new SimpleStringProperty(pathIn);

        String regex = "/";
        String[] splited = getPath().split(regex);
        name = new SimpleStringProperty(splited[splited.length-1]);

        tags = new ArrayList<>();
        pathListeners = pathListenersIn;
    }

    public void setPath(String pathIn){
        path.set(pathIn);
        pathListeners.forEach(pathListener -> pathListener.changedPath(this));
    }

    public void rename(String newNameIn){
        name.set(newNameIn);
        pathListeners.forEach(pathListener -> pathListener.renamed(this));
    }

    public String getPath(){
        return path.get();
    }

    public String getName(){
        return name.get();
    }

    public StringProperty getNameProperty(){
        return name;
    }

    public StringProperty getPathProperty(){
        return path;
    }

    public void addTag(Tag tagIn){
        if(!tags.contains(tagIn)){
            addTagWithoutNotifing(tagIn);
            pathListeners.forEach(pathListener -> pathListener.addedTag(this, tagIn));
        }
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

    public void removeTags(Collection<Tag> tagsIn){
        tags.removeAll(tagsIn);

        for(Tag tag: tagsIn){
            tag.removePath(this);
        }

        pathListeners.forEach(pathListener -> pathListener.removedTags(this, tagsIn));
    }

    public void removeTag(Tag tagIn){
        if(tags.contains(tagIn)){
            tags.remove(tagIn);
            tagIn.removePath(this);
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


    public boolean equals(String tagNameIn){
        return name.equals(tagNameIn);
    }
}
