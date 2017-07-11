import java.util.ArrayList;

public class Tags extends ArrayList<Tag> {
    private ArrayList<TagListener> listeners;

    public Tags(){
        super();
        listeners = new ArrayList<>();
    }

    public void addTagObserver(TagListener tagListenerIn){
        listeners.add(tagListenerIn);
    }

    public boolean newTag(String tagNameIn){
        for(Tag tag: this){
            if(tag.equals(tagNameIn)){
                return false;
            }
        }

        Tag tag = new Tag(tagNameIn);
        this.add(tag);

        listeners.forEach(tagListener -> tagListener.created(tag));

        return true;
    }

    public boolean renameTag(Tag tagIn, String newNameIn){
        if(this.contains(tagIn)){
            tagIn.setName(newNameIn);
            listeners.forEach(tagListener -> tagListener.renamed(tagIn));

            return true;
        }

        return false;
    }

    public boolean addPathToTag(Tag tagIn, Path pathIn){
        if(this.contains(tagIn)){
            tagIn.addPath(pathIn);
            listeners.forEach(tagListener -> tagListener.addedPath(tagIn, pathIn));

            return true;
        }

        return false;
    }

    public boolean removeTag(Tag tagIn){
        if(this.contains(tagIn)){
            this.remove(tagIn);
            listeners.forEach(tagListener -> tagListener.removed(tagIn));

            return true;
        }

        return false;
    }

    public boolean removePathFromTag(Tag tagIn, Path pathIn){
        if(this.contains(tagIn)){
            tagIn.removePath(pathIn);
            pathIn.removeTag(tagIn);
            listeners.forEach(tagListener -> tagListener.removedPath(tagIn, pathIn));

            return true;
        }

        return false;
    }

    public Tag getTag(String tagNameIn){
        for (Tag tag: this){
            if(tag.equals(tagNameIn)){
                return tag;
            }
        }
        return null;
    }
}
