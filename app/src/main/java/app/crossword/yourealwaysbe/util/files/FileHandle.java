package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileHandle) {
            FileHandle other = (FileHandle) o;
            return Objects.equals(this.file, other.file);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(file);
    }
}
