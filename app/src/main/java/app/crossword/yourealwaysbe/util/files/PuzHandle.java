
package app.crossword.yourealwaysbe.util.files;

public class PuzHandle {
    public DirHandle dirHandle;
    public FileHandle puzHandle;
    public FileHandle metaHandle;

    public PuzHandle(
        DirHandle dirHandle, FileHandle puzHandle, FileHandle metaHandle
    ) {
        this.dirHandle = dirHandle;
        this.puzHandle = puzHandle;
        this.metaHandle = metaHandle;
    }

    public DirHandle getDirHandle() { return dirHandle; }
    public FileHandle getPuzFileHandle() { return puzHandle; }
    public FileHandle getMetaFileHandle() { return metaHandle; }

    // deliberately package-level
    void setMetaFileHandle(FileHandle metaHandle) {
        this.metaHandle = metaHandle;
    }
}
