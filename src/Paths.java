import java.util.ArrayList;
import java.util.Collection;

public class Paths extends ArrayList<Path> {
    private ArrayList<PathListener> listeners;

    public Paths(){
        listeners = new ArrayList<>();
    }

    public void addListener(PathListener pathListenerIn){
        listeners.add(pathListenerIn);
    }

    public Path newPath(String pathIn, String nameIn){
        Path path = new Path(pathIn, nameIn);
        this.add(path);
        listeners.forEach(pathListener -> pathListener.created(path));
        return path;
    }
    public Path newPath(String pathIn){
        Path path = new Path(pathIn);
        this.add(path);
        listeners.forEach(pathListener -> pathListener.created(path));
        return path;
    }

    public boolean renamePath(Path pathIn, String newNameIn){
        if(this.contains(pathIn)){
            pathIn.setName(newNameIn);
            listeners.forEach(pathListener -> pathListener.renamed(pathIn));

            return true;
        }

        return false;
    }

    public boolean changePath(Path pathIn, String newPathIn){
        if(this.contains(pathIn)){
            pathIn.setPath(newPathIn);
            listeners.forEach(pathListener -> pathListener.changedPath(pathIn));

            return true;
        }

        return false;
    }

    public boolean removeTagFromPath(Tag tagIn, Path pathIn){
        if(this.contains(pathIn)){
            pathIn.removeTag(tagIn);
            tagIn.removePath(pathIn);
            listeners.forEach(pathListener -> pathListener.removedTag(pathIn, tagIn));

            return true;
        }

        return false;
    }

    public boolean removeTagsFromPath(Collection<Tag> tagsIn, Path pathIn){
        if(this.contains(pathIn)){
            pathIn.removeTags(tagsIn);
            listeners.forEach(pathListener -> pathListener.removedTags(pathIn, tagsIn));

            for(Tag tag: tagsIn){
                tag.removePath(pathIn);
            }

            return true;
        }

        return false;
    }

    public boolean addTagToPath(Tag tagIn, Path pathIn){
        if(this.contains(pathIn)){
            pathIn.addTag(tagIn);
            tagIn.addPath(pathIn);
            listeners.forEach(pathListener -> pathListener.addedTag(pathIn, tagIn));

            return true;
        }

        return false;
    }

    public boolean addTagsToPath(Collection<Tag> tagsIn, Path pathIn){
        if(this.contains(pathIn)){
            pathIn.addTags(tagsIn);

            for(Tag tag: tagsIn){
                tag.addPath(pathIn);
            }

            listeners.forEach(pathListener -> pathListener.addedTags(pathIn, tagsIn));

            return true;
        }

        return false;
    }
}
