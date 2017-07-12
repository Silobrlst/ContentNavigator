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

    public Tag newTag(String tagNameIn){
        Tag tag = newTagWithoutNotifing(tagNameIn);
        listeners.forEach(tagListener -> tagListener.created(tag));
        return tag;
    }

    public Tag newTagWithoutNotifing(String tagNameIn){
        for(Tag tag: this){
            if(tag.equals(tagNameIn)){
                return null;
            }
        }

        Tag tag = new Tag(listeners, tagNameIn);
        this.add(tag);

        return tag;
    }

    public boolean removeTag(Tag tagIn){
        if(this.contains(tagIn)){
            this.remove(tagIn);
            listeners.forEach(tagListener -> tagListener.removed(tagIn));

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
