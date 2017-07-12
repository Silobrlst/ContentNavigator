import java.util.ArrayList;
import java.util.Collection;

public class Path implements Comparable<Path> {
    private String path;
    private String name;

    private ArrayList<Tag> tags;
    private ArrayList<PathListener> pathListeners;

    public Path(ArrayList<PathListener> pathListenersIn, String pathIn, String nameIn){
        path = pathIn;
        name = nameIn;
        tags = new ArrayList<>();
        pathListeners = pathListenersIn;
    }
    public Path(ArrayList<PathListener> pathListenersIn, String pathIn){
        path = pathIn;

        String regex = "/";
        String[] splited = path.split(regex);
        name = splited[splited.length-1];

        tags = new ArrayList<>();
        pathListeners = pathListenersIn;
    }

    public void setPath(String pathIn){
        path = pathIn;
        pathListeners.forEach(pathListener -> pathListener.changedPath(this));
    }

    public void rename(String newNameIn){
        name = newNameIn;
        pathListeners.forEach(pathListener -> pathListener.renamed(this));
    }

    public String getPath(){
        return path;
    }

    public String getName(){
        return name;
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
        return name;
    }

    @Override
    public int compareTo(Path pathIn) {
        return name.compareTo(pathIn.getPath());
    }


    public boolean equals(String tagNameIn){
        return name.equals(tagNameIn);
    }
}
