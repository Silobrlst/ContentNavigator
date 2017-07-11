import java.util.ArrayList;

public class Tag implements Comparable<Tag> {
    private ArrayList<Path> paths;
    private String name;

    public Tag(String nameIn){
        paths = new ArrayList<>();
        name = nameIn;
    }

    public void addPath(Path pathIn){
        paths.add(pathIn);
    }

    public void removePath(Path pathIn){
        paths.remove(pathIn);
    }

    public void setName(String nameIn){
        name = nameIn;
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
