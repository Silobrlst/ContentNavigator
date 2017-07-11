import java.util.ArrayList;
import java.util.Collection;

public class Path implements Comparable<Path> {
    private String path;
    private String name;

    private ArrayList<Tag> tags;

    public Path(String pathIn, String nameIn){
        path = pathIn;
        name = nameIn;
        tags = new ArrayList<>();
    }
    public Path(String pathIn){
        path = pathIn;
        name = pathIn.split("[\\/][^\\/]*$")[0];
        tags = new ArrayList<>();
    }

    public void setPath(String pathIn){
        path = pathIn;
    }

    public void setName(String nameIn){
        name = nameIn;
    }

    public String getPath(){
        return path;
    }

    public String getName(){
        return name;
    }

    public void addTag(Tag tagIn){
        tags.add(tagIn);
    }

    public void addTags(Collection<Tag> tagsIn){
        tags.addAll(tagsIn);
    }

    public void removeTags(Collection<Tag> tagsIn){
        tags.removeAll(tagsIn);
    }

    public void removeTag(Tag tagIn){
        tags.remove(tagIn);
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
