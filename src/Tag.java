import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Tag implements Comparable<Tag> {
    private Map<String, Path> paths;
    private String name;

    public Tag(String nameIn){
        paths = new HashMap<>();
        name = nameIn;
    }

    public void setPath(Path pathIn){
        paths.put(pathIn.getPath(), pathIn);
    }
    public void setName(String nameIn){
        name = nameIn;
    }

    public Path getPath(String pathIn){
        return paths.get(pathIn);
    }
    public Collection<Path> getPaths(){
        return paths.values();
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
