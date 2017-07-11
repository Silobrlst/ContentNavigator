public interface TagListener {
    void created(Tag tagIn);
    void renamed(Tag tagIn);
    void addedPath(Tag tagIn, Path pathIn);
    void removedPath(Tag tagIn, Path pathIn);
    void removed(Tag tagIn);
}
