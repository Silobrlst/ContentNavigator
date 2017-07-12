import java.util.ArrayList;

public class Tag implements Comparable<Tag> {
    private ArrayList<Path> paths;
    private String name;
    private ArrayList<TagListener> tagListeners;

    public Tag(ArrayList<TagListener> tagListenersIn, String nameIn){
        paths = new ArrayList<>();
        name = nameIn;
        tagListeners = tagListenersIn;
    }

    public void addPath(Path pathIn){
        if(!paths.contains(pathIn)) {
            addPathWithoutNotifing(pathIn);
            tagListeners.forEach(tagListener -> tagListener.addedPath(this, pathIn));
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
            tagListeners.forEach(tagListener -> tagListener.removedPath(this, pathIn));
        }
    }

    public void rename(String nameIn){
        name = nameIn;
        tagListeners.forEach(tagListener -> tagListener.renamed(this));
    }

    public ArrayList<Path> getPaths(){
        return paths;
    }

    public String getName(){
        return name;
    }

    @Override
    public String toString(){
        return name;
    }

    @Override
    public int compareTo(Tag tagIn) {
        return name.compareTo(tagIn.getName());
    }

    public boolean equals(String tagNameIn){
        return name.equals(tagNameIn);
    }
}
