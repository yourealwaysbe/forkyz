
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

public class PuzHandle {
    public FileHandle puzHandle;
    public FileHandle metaHandle;

    public PuzHandle(FileHandle puzHandle, FileHandle metaHandle) {
        this.puzHandle = puzHandle;
        this.metaHandle = metaHandle;
    }

    public FileHandle getPuzFileHandle() { return puzHandle; }
    public FileHandle getMetaFileHandle() { return metaHandle; }
}
