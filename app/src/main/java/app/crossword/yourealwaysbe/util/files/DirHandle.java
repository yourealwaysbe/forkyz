package app.crossword.yourealwaysbe.util.files;

import java.io.File;

import android.net.Uri;

public class DirHandle {
    // common to all implementations
    private Uri uri;

    Uri getUri() { return uri; }

    @Override
    public String toString() { return uri.toString(); }

    //////////////////////////////////////////////////////////////
    // for FileHandlerJavaFile

    private File file;

    DirHandle(File file) {
        this.uri = Uri.parse(file.toURI().toString());
        this.file = file;
    }

    File getFile() {
        return file != null ? file : new File(uri.getPath());
    }

    //////////////////////////////////////////////////////////////
    // for FileHandlerSAF

    DirHandle(Uri uri) {
        this.uri = uri;
    }
}

