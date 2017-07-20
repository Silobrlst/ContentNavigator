import java.util.ArrayList;

public class Tags extends Tag {
    private ArrayList<TagListener> listeners;

    public Tags(){
        super(null, "Tags");
        listeners = new ArrayList<>();
    }

    public void addTagObserver(TagListener tagListenerIn){
        listeners.add(tagListenerIn);
    }

    public Tag newTag(String tagNameIn, Tag parentIn){
        Tag tag = newTagWithoutNotifing(tagNameIn, parentIn);
        listeners.forEach(tagListener -> tagListener.createdTag(tag, parentIn));
        return tag;
    }
    public Tag newTag(String tagNameIn){
        Tag tag = newTagWithoutNotifing(tagNameIn);
        listeners.forEach(tagListener -> tagListener.createdTag(tag, this));
        return tag;
    }

    public Tag newTagWithoutNotifing(String tagNameIn, Tag parentIn){
        Tag tag = new Tag(listeners, tagNameIn, parentIn);
        parentIn.addChildWithoutNotify(tag);
        return tag;
    }
    public Tag newTagWithoutNotifing(String tagNameIn){
        return newTagWithoutNotifing(tagNameIn, this);
    }

    public void removeTag(Tag tagIn){
        tagIn.getParent().removeChild(tagIn);
    }

    public Tag getTagById(String idIn){
        String[] tagNames = idIn.split("\\s*:\\s*");

        Tag currentTag = this;

        for(int i=0; i<tagNames.length; i++){
            boolean b = false;

            for(Tag tag: currentTag.getChildren()){
                if(tag.getName().equals(tagNames[i])){
                    b = true;

                    if(i == tagNames.length - 1){
                        return tag;
                    }else{
                        currentTag = tag;
                        break;
                    }
                }
            }

            if(!b){
                return null;
            }
        }

        return null;
    }

    public String getTagId(Tag tagIn){
        Tag currentTag = tagIn;
        String id = "";

        while(currentTag != this){
            if(id.length() == 0){
                id = currentTag.getName();
            }else{
                id = currentTag.getName() + " : " + id;
            }

            currentTag = currentTag.getParent();
        }

        return id;
    }
}
