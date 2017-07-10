public class Path implements Comparable<Path> {
    private String path;
    private String name;

    public Path(String pathIn, String nameIn){
        path = pathIn;
        name = nameIn;
    }
    public Path(String pathIn){
        path = pathIn;
        name = pathIn.split("[\\/][^\\/]*$")[0];
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
    public String setName(){
        return name;
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
