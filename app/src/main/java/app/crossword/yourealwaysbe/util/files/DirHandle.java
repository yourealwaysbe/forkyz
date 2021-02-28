package app.crossword.yourealwaysbe.util.files;

import java.io.File;

public class DirHandle {
    private File dir;

    public DirHandle(File dir) {
        this.dir = dir;
    }

    File getFile() { return dir; }
}

