public class EmptyTagListener implements TagListener {
    public void created(Tag tagIn, Tag parentIn) {}
    public void renamed(Tag tagIn) {}
    public void addedPath(Tag tagIn, Path pathIn) {}
    public void removedPath(Tag tagIn, Path pathIn) {}
    public void removed(Tag tagIn) {}
    public void changedParent(Tag tagIn, Tag oldParentIn){}
    public void addedChild(Tag tagIn){}
    public void removedChild(Tag tagIn){}
}
