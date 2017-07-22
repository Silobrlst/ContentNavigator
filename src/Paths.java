import java.util.ArrayList;

public class Paths extends ArrayList<Path> {
    private ArrayList<PathListener> listeners;

    public Paths(){
        listeners = new ArrayList<>();
    }

    public void addListener(PathListener pathListenerIn){
        listeners.add(pathListenerIn);
    }

    public Path newPath(String pathIn, String nameIn){
        Path path = newPathWithoutNotifing(pathIn, nameIn);
        listeners.forEach(pathListener -> pathListener.created(path));
        return path;
    }
    public Path newPath(String pathIn){
        Path path = newPathWithoutNotifing(pathIn);
        listeners.forEach(pathListener -> pathListener.created(path));
        return path;
    }

    public Path newPathWithoutNotifing(String pathIn, String nameIn){
        Path path = new Path(listeners, pathIn, nameIn);
        this.add(path);
        return path;
    }
    public Path newPathWithoutNotifing(String pathIn){
        Path path = new Path(listeners, pathIn);
        this.add(path);
        return path;
    }

    boolean checkPathExist(String pathIn){
        for(Path path: this){
            if(pathIn.equals(path.getPath())){
                return true;
            }
        }

        return false;
    }
}
