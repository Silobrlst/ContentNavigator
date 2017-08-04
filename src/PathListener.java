import java.util.Collection;

public interface PathListener {
    void created(Path pathIn);
    void changedPath(Path pathIn);
    void renamed(Path pathIn);
    void addedTags(Path pathIn, Collection<Tag> tagsIn);
    void removedTag(Path pathIn, Tag tagIn);
    void removedTags(Path pathIn, Collection<Tag> tagsIn);
    void removedPath(Path pathIn);
}
