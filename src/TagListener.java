public interface TagListener {
    /**
     * сообщает о создании тега
     *
     * @param tagIn    созданный тег
     * @param parentIn родительский тег
     */
    void createdTag(Tag tagIn, Tag parentIn);

    /**
     * сообщает переименовании тега
     *
     * @param tagIn переименнованный тег
     */
    void renamedTag(Tag tagIn);

    /**
     * сообщает о добавлении пути в тег
     *
     * @param tagIn  тег в который добавлен путь
     * @param pathIn добавленный путь
     */
    void addedPathToTag(Tag tagIn, Path pathIn);

    /**
     * сообщает об удалении пути из тега
     *
     * @param tagIn  тег из которого произошло удаление
     * @param pathIn удаленный путь
     */
    void removedPathFromTag(Tag tagIn, Path pathIn);
    void removed(Tag tagIn);
    void changedParent(Tag tagIn, Tag oldParentIn);
    void addedChild(Tag tagIn);
    void removedChild(Tag tagIn);
}
