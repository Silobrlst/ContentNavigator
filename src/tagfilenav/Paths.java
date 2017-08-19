package tagfilenav;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Paths extends ArrayList<Path> {
    private ArrayList<PathListener> listeners;

    public Paths(){
        listeners = new ArrayList<>();
    }

    void addListener(PathListener pathListenerIn){
        listeners.add(pathListenerIn);
    }

    Path newPath(String pathIn, String nameIn){
        Path path = newPathWithoutNotify(pathIn, nameIn);
        path.setDateAdded(new SimpleDateFormat("dd.MM.yy HH:mm").format(new Date()));
        listeners.forEach(pathListener -> pathListener.created(path));
        return path;
    }
    Path newPath(String pathIn){
        Path path = newPathWithoutNotify(pathIn);
        path.setDateAdded(new SimpleDateFormat("dd.MM.yy HH:mm").format(new Date()));
        listeners.forEach(pathListener -> pathListener.created(path));
        return path;
    }

    Path newPathWithoutNotify(String pathIn, String nameIn){
        Path path = new Path(listeners, pathIn, nameIn);
        this.add(path);
        return path;
    }
    Path newPathWithoutNotify(String pathIn){
        Path path = new Path(listeners, pathIn);
        this.add(path);
        return path;
    }

    void removePath(Path pathIn){
        pathIn.removeTagsWithoutNotifing();
        this.remove(pathIn);
        listeners.forEach(pathListener -> pathListener.removedPath(pathIn));
    }

    void pathsChanged(){
        listeners.forEach(pathListener -> pathListener.changedPaths());
    }

    boolean checkPathAdded(String pathIn){
        for(Path path: this){
            if(pathIn.equals(path.getPath())){
                return true;
            }
        }

        return false;
    }
}
