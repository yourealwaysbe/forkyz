package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

public class FileHandle {
    public File file;

    public FileHandle(File file) {
        this.file = file;
    }

    File getFile() {
        return file;
    }
}
