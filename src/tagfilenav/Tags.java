package tagfilenav;

import java.util.ArrayList;
import java.util.List;

public class Tags extends Tag {
    private ArrayList<TagListener> listeners;

    public Tags(){
        super(null, "Tags");
        listeners = new ArrayList<>();
    }

    void addTagObserver(TagListener tagListenerIn){
        listeners.add(tagListenerIn);
    }

    Tag newTag(String tagNameIn, Tag parentIn){
        Tag tag = newTagWithoutNotifing(tagNameIn, parentIn);
        listeners.forEach(tagListener -> tagListener.createdTag(tag, parentIn));
        return tag;
    }
    Tag newTag(String tagNameIn){
        Tag tag = newTagWithoutNotifing(tagNameIn);
        listeners.forEach(tagListener -> tagListener.createdTag(tag, this));
        return tag;
    }

    Tag newTagWithoutNotifing(String tagNameIn, Tag parentIn){
        Tag tag = new Tag(listeners, tagNameIn, parentIn);
        parentIn.addChildWithoutNotify(tag);
        return tag;
    }
    Tag newTagWithoutNotifing(String tagNameIn){
        return newTagWithoutNotifing(tagNameIn, this);
    }

    void removeTag(Tag tagIn){
        tagIn.removePaths(tagIn.getPaths());
        tagIn.getParent().removeChild(tagIn);
    }

    Tag getTagById(String idIn){
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

    List<Tag> getTagsByIds(List<String> idsIn){
        ArrayList<Tag> tags_ = new ArrayList<>();

        for(String id: idsIn){
            tags_.add(getTagById(id));
        }

        return tags_;
    }

    String getTagId(Tag tagIn){
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

    List<String> getTagsIds(List<Tag> tagsIn){
        ArrayList<String> ids = new ArrayList<>();
        for(Tag tag: tagsIn){
            if(tag != this){
                ids.add(this.getTagId(tag));
            }
        }
        return ids;
    }

    List<String> getTagsIds(){
        ArrayList<String> ids = new ArrayList<>();
        for(Tag tag: getAllSubTags()){
            if(tag != this){
                ids.add(this.getTagId(tag));
            }
        }
        return ids;
    }

    void getAllSubTags(Tag parentIn, List<Tag> tagsOut){
        if(parentIn != this){
            tagsOut.add(parentIn);
        }
        for(Tag tag: parentIn.getChildren()){
            getAllSubTags(tag, tagsOut);
        }
    }

    List<Tag> getAllSubTags(Tag parentIn){
        ArrayList<Tag> allTags = new ArrayList<>();
        getAllSubTags(parentIn, allTags);
        return allTags;
    }

    List<Tag> getAllSubTags(){
        return getAllSubTags(this);
    }
}
