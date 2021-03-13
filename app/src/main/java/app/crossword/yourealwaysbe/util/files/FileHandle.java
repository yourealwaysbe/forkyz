package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

public class FileHandle {
    // common to all implementations
    private Uri uri;

    Uri getUri() { return uri; }

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

    @Override
    public String toString() { return uri.toString(); }

    //////////////////////////////////////////////////////////////
    // for FileHandlerJavaFile

    private File file;

    FileHandle(File file) {
        this.uri = Uri.parse(file.toURI().toString());
        this.file = file;
    }

    File getFile() {
        return file != null ? file : new File(uri.getPath());
    }

    //////////////////////////////////////////////////////////////
    // for FileHandlerSAF

    private FileHandlerSAF.Meta safMeta;

    FileHandle(Uri uri, FileHandlerSAF.Meta safMeta) {
        this.uri = uri;
        this.safMeta = safMeta;
    }

    FileHandlerSAF.Meta getSAFMeta() { return safMeta; }
}
