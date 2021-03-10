
package app.crossword.yourealwaysbe.util.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Iterable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

/**
 * Abstraction layer for file operations
 *
 * Implementations provided for different file backends
 */
public abstract class FileHandler {
    public abstract DirHandle getCrosswordsDirectory();
    public abstract DirHandle getArchiveDirectory();
    public abstract DirHandle getTempDirectory(DirHandle baseDir);
    public abstract FileHandle getFileHandle(Uri uri);
    public abstract boolean exists(DirHandle dir);
    public abstract boolean exists(FileHandle file);
    public abstract boolean exists(DirHandle dir, String fileName);
    public abstract Iterable<FileHandle> listFiles(final DirHandle dir);
    public abstract Uri getUri(FileHandle f);
    public abstract String getName(FileHandle f);
    public abstract long getLastModified(FileHandle file);
    public abstract void delete(FileHandle fileHandle);
    public abstract void moveTo(FileHandle fileHandle, DirHandle dirHandle);
    public abstract void renameTo(FileHandle src, FileHandle dest);
    public abstract OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException;
    public abstract InputStream getInputStream(FileHandle fileHandle)
        throws IOException;
    public abstract LocalDate getModifiedDate(FileHandle file);
    public abstract boolean isStorageMounted();
    public abstract boolean isStorageFull();

    /**
     * Create a new file in the directory with the given display name
     *
     * Return null if could not be created. E.g. if the file already
     * exists.
     */
    public abstract FileHandle createFileHandle(DirHandle dir, String fileName);

    protected abstract FileHandle getMetaFileHandle(FileHandle puzFile);

    public boolean exists(PuzMetaFile pm) {
        return exists(pm.getPuzHandle());
    }

    public boolean exists(PuzHandle ph) {
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null) {
            return exists(ph.getPuzFileHandle())
                && exists(ph.getMetaFileHandle());
        } else {
            return exists(ph.getPuzFileHandle());
        }
    }

    public void delete(PuzMetaFile pm) {
        delete(pm.getPuzHandle());
    }

    public void delete(PuzHandle ph) {
        delete(ph.getPuzFileHandle());
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null)
            delete(metaHandle);
    }

    public void moveTo(PuzMetaFile pm, DirHandle dirHandle) {
        moveTo(pm.getPuzHandle(), dirHandle);
    }

    public void moveTo(PuzHandle ph, DirHandle dirHandle) {
        moveTo(ph.getPuzFileHandle(), dirHandle);
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null)
            moveTo(metaHandle, dirHandle);
    }

    public PuzMetaFile[] getPuzFiles(DirHandle dir) {
        return getPuzFiles(dir, null);
    }

    /**
     * Get puz files in directory matching a source
     *
     * Matches any source if sourceMatch is null
     */
    public PuzMetaFile[] getPuzFiles(DirHandle dirHandle, String sourceMatch) {
        ArrayList<PuzMetaFile> files = new ArrayList<>();

        // Use a caching approach to avoid repeated interaction with
        // filesystem (which is good for content resolver)
        Map<String, FileHandle> puzFiles = new HashMap<>();
        Map<String, FileHandle> metaFiles = new HashMap<>();

        for (FileHandle f : listFiles(dirHandle)) {
            String fileName = getName(f);
            if (fileName.endsWith(".puz")) {
                puzFiles.put(fileName, f);
            } else if (fileName.endsWith(".forkyz")) {
                metaFiles.put(fileName, f);
            }
        }

        for (Map.Entry<String, FileHandle> entry : puzFiles.entrySet()) {
            String fileName = entry.getKey();
            FileHandle puzFile = entry.getValue();
            FileHandle metaFile = null;

            String metaName = getMetaFileName(puzFile);

            PuzzleMeta meta = null;

            if (metaFiles.containsKey(metaName)) {
                metaFile = metaFiles.get(metaName);
                try (
                    DataInputStream is = new DataInputStream(
                        getInputStream(metaFile)
                    )
                ) {
                    meta = IO.readMeta(is);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            PuzMetaFile h = new PuzMetaFile(
                new PuzHandle(puzFile, metaFile),
                meta
            );

            if ((sourceMatch == null) || sourceMatch.equals(h.getSource())) {
                files.add(h);
            }
        }

        return files.toArray(new PuzMetaFile[files.size()]);
    }

    public Puzzle load(PuzMetaFile pm) throws IOException {
        return load(pm.getPuzHandle());
    }

    public Puzzle load(PuzHandle ph) throws IOException {
        return load(ph.getPuzFileHandle());
    }

    // TODO: replace with loadNoMeta
    public Puzzle load(FileHandle fileHandle) throws IOException {
        FileHandle metaFile = getMetaFileHandle(fileHandle);
        try (
            DataInputStream fis
                = new DataInputStream(getInputStream(fileHandle))
        ) {
            Puzzle puz = IO.loadNative(fis);
            if (exists(metaFile)) {
                try (
                    DataInputStream mis
                        = new DataInputStream(getInputStream(metaFile))
                ) {
                    IO.readCustom(puz, mis);
                }
            }
            return puz;
        }
    }

    public void save(Puzzle puz, PuzMetaFile puzMeta) throws IOException {
        save(puz, puzMeta.getPuzHandle().getPuzFileHandle());
    }

    // TODO: replace with saveCreateMeta
    public void save(Puzzle puz, FileHandle fileHandle) throws IOException {
        long incept = System.currentTimeMillis();
        FileHandle metaFile = getMetaFileHandle(fileHandle);

        DirHandle tempFolder = getSaveTempDirectory();
        FileHandle puzTemp = createFileHandle(tempFolder, getName(fileHandle));
        FileHandle metaTemp = createFileHandle(tempFolder, getName(metaFile));

        try (
            DataOutputStream puzzle
                = new DataOutputStream(getOutputStream(puzTemp));
            DataOutputStream meta
                = new DataOutputStream(getOutputStream(metaTemp));
        ) {
            IO.save(puz, puzzle, meta);
        }

        renameTo(puzTemp, fileHandle);
        renameTo(metaTemp, metaFile);
    }

    public void reloadMeta(PuzMetaFile pm) throws IOException {
        FileHandle metaHandle = pm.getPuzHandle().getMetaFileHandle();
        if (metaHandle == null)
            metaHandle = getMetaFileHandle(pm);

        try (
            DataInputStream is
                = new DataInputStream(
                    getInputStream(metaHandle)
                )
        ) {
            pm.setMeta(IO.readMeta(is));
        };
    }

    protected FileHandle getMetaFileHandle(PuzMetaFile pm) {
        return getMetaFileHandle(pm.getPuzHandle());
    }

    protected FileHandle getMetaFileHandle(PuzHandle ph) {
        return getMetaFileHandle(ph.getPuzFileHandle());
    }

    protected DirHandle getSaveTempDirectory() {
        return getTempDirectory(getCrosswordsDirectory());
    }

    protected String getMetaFileName(FileHandle puzFile) {
        String name = getName(puzFile);
        return name.substring(0, name.lastIndexOf(".")) + ".forkyz";
    }
}
