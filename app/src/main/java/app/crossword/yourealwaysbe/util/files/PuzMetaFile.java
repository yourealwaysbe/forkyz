
package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;

public class PuzMetaFile
        implements Comparable<PuzMetaFile> {
    public PuzHandle handle;
    public PuzzleMeta meta;

    PuzMetaFile(PuzHandle handle, PuzzleMeta meta) {
        this.handle = handle;
        this.meta = meta;
    }

    public PuzHandle getPuzHandle() { return handle; }

    public int compareTo(PuzMetaFile other) {
        try {
            // because LocalDate is day-month-year, fall back to name
            int dateCmp = other.getDate().compareTo(this.getDate());
            if (dateCmp != 0)
                return dateCmp;
            return getHandler().getName(
                this.handle.getPuzFileHandle()
            ).compareTo(
                getHandler().getName(
                    other.handle.getPuzFileHandle()
                )
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
            return getHandler().getModifiedDate(handle.getPuzFileHandle());
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
        if ((meta == null)
                || (meta.source == null)
                || (meta.source.length() == 0)) {
            String fileName = getHandler().getName(handle.getPuzFileHandle());
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return meta.source;
        }
    }

    @Override
    public String toString(){
        return getHandler().getUri(handle.getPuzFileHandle()).toString();
    }

    void setMeta(PuzzleMeta meta) {
        this.meta = meta;
    }

    private FileHandler getHandler() {
        return ForkyzApplication.getInstance().getFileHandler();
    }
}
