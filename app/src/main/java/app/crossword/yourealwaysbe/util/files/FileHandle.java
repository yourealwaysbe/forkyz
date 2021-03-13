package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

public class FileHandle {
    public Uri uri;

    public FileHandle(Uri uri) {
        this.uri = uri;
    }

    public FileHandle(File file) {
        this.uri = Uri.parse(file.toURI().toString());
    }

    File getFile() {
        return new File(uri.getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileHandle) {
            FileHandle other = (FileHandle) o;
            return Objects.equals(this.uri, other.uri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }
}
