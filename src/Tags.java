import java.util.Map;

public class Tags {
    private Map<String, Tag> tagsMap;

    Tags(){

    }

    boolean newTag(String tagNameIn){
        if(tagsMap.containsKey(tagNameIn)){
            return false;
        }
        tagsMap.put(tagNameIn, new Tag(tagNameIn));
        return true;
    }

    boolean setTag(String oldTagNameIn, String newTagNameIn){
        if(tagsMap.containsKey(newTagNameIn)){
            return false;
        }
        tagsMap.put(newTagNameIn, tagsMap.get(oldTagNameIn));
        tagsMap.remove(oldTagNameIn);
        return true;
    }

    Tag getTag(String tagNameIn){
        if(tagsMap.containsKey(tagNameIn)){
            return tagsMap.get(tagNameIn);
        }
        return null;
    }
}
