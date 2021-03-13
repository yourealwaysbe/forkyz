package app.crossword.yourealwaysbe.util.files;

import java.io.File;

import android.net.Uri;

public class DirHandle {
    private Uri uri;

    public DirHandle(Uri uri) {
        this.uri = uri;
    }

    public DirHandle(File file) {
        this.uri = Uri.parse(file.toURI().toString());
    }

    File getFile() {
        return new File(uri.getPath());
    }
}

