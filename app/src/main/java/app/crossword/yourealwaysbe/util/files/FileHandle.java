package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

public class FileHandle implements Comparable<FileHandle> {
    public File file;
    public PuzzleMeta meta;

    public FileHandle(File f) {
        this(f, null);
    }

    public FileHandle(File f, PuzzleMeta meta) {
        this.file = f;
        this.meta = meta;
    }

    File getFile() {
        return file;
    }

    public int compareTo(FileHandle another) {
        FileHandle h = (FileHandle) another;

        try {
            // because LocalDate is day-month-year, fall back to file
            // modification time
            int dateCmp = h.getDate().compareTo(this.getDate());
            if (dateCmp != 0)
                return dateCmp;
            return Long.compare(
                this.file.lastModified(), another.file.lastModified()
            );
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isUpdatable() {
        return (meta == null) ? false : meta.updatable;
    }

    public String getCaption() {
        return (meta == null) ? "" : meta.title;
    }

    public LocalDate getDate() {
        if (meta == null) {
            return Instant.ofEpochMilli(file.lastModified())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
        } else {
            return meta.date;
        }
    }

    public int getComplete() {
        return (meta == null) ? 0 : (meta.updatable ? (-1) : meta.percentComplete);
    }

    public int getFilled() {
        return (meta == null) ? 0 : (meta.updatable ? (-1) : meta.percentFilled);
    }

    public String getSource() {
        return ((meta == null) || (meta.source == null)) ? "Unknown" : meta.source;
    }

    public String getTitle() {
        return ((meta == null) || (meta.source == null) || (meta.source.length() == 0))
        ? file.getName()
              .substring(0, file.getName().lastIndexOf(".")) : meta.source;
    }

    @Override
    public String toString(){
        return file.getAbsolutePath();
    }

    public Uri getUri() { return Uri.fromFile(file); }

    public String getName() { return file.getName(); }

    void setMeta(PuzzleMeta meta) {
        this.meta = meta;
    }
}
