import java.util.List;

public interface AddEditPathInterface {
    void add(String path, List<String> tagNamesIn);
    void change(String path, List<String> tagNamesIn);
}
