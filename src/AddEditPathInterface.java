import java.util.List;

public interface AddEditPathInterface {
    void add(String path, List<Tag> tagsIn);
    void change(String path, List<Tag> tagsIn);
}
