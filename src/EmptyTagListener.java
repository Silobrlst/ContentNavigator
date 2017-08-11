public class EmptyTagListener implements TagListener {
    public void createdTag(Tag tagIn, Tag parentIn) {}
    public void renamedTag(Tag tagIn) {}
    public void addedPathToTag(Tag tagIn, Path pathIn) {}
    public void removedPathFromTag(Tag tagIn, Path pathIn) {}
    public void removed(Tag tagIn) {}
    public void changedParent(Tag tagIn, Tag oldParentIn){}
    public void addedChild(Tag tagIn){}
    public void removedChild(Tag tagIn){}
    public void changedDescriptions(Tag tagIn){}
}
