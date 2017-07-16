public interface TagListener {
    void created(Tag tagIn, Tag parentIn);
    void renamed(Tag tagIn);
    void addedPath(Tag tagIn, Path pathIn);
    void removedPath(Tag tagIn, Path pathIn);
    void removed(Tag tagIn);
    void changedParent(Tag tagIn, Tag oldParentIn);
    void addedChild(Tag tagIn);
    void removedChild(Tag tagIn);
}
