
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

import android.content.Intent;
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
    private static final String DIR_URI
        = "app.crossword.yourealwaysbe.util.files.PuzHandle.dirUri";
    private static final String PUZ_URI
        = "app.crossword.yourealwaysbe.util.files.PuzHandle.puzUri";
    private static final String META_URI
        = "app.crossword.yourealwaysbe.util.files.PuzHandle.metaUri";

    public abstract DirHandle getCrosswordsDirectory();
    public abstract DirHandle getArchiveDirectory();
    public abstract DirHandle getTempDirectory(DirHandle baseDir);
    public abstract DirHandle getDirHandle(Uri uri);
    public abstract FileHandle getFileHandle(Uri uri);
    public abstract boolean exists(DirHandle dir);
    public abstract boolean exists(FileHandle file);
    public abstract boolean exists(DirHandle dir, String fileName);
    public abstract Iterable<FileHandle> listFiles(final DirHandle dir);
    public abstract Uri getUri(DirHandle f);
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
            } else {
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
                new PuzHandle(dirHandle, puzFile, metaFile),
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

    /**
     * Loads puzzle with meta
     *
     * If the meta file of puz handle is null, loads without meta
     */
    public Puzzle load(PuzHandle ph) throws IOException {
        FileHandle metaFile = ph.getMetaFileHandle();

        if (metaFile == null)
            return load(ph.getPuzFileHandle());

        try (
            DataInputStream pis
                = new DataInputStream(
                    getInputStream(ph.getPuzFileHandle())
                );
            DataInputStream mis
                = new DataInputStream(
                    getInputStream(ph.getMetaFileHandle())
                )
        ) {
            return IO.load(pis, mis);
        }
    }

    /**
     * Loads without any meta data
     */
    public Puzzle load(FileHandle fileHandle) throws IOException {
        try (
            DataInputStream fis
                = new DataInputStream(getInputStream(fileHandle))
        ) {
            return IO.loadNative(fis);
        }
    }

    public void save(Puzzle puz, PuzMetaFile puzMeta) throws IOException {
        save(puz, puzMeta.getPuzHandle());
    }

    /**
     * Save puzzle and meta data
     *
     * If puzHandle's meta handle is null, a new meta file will be
     * created and puzHandle is updated with the new meta file handle
     */
    public void save(Puzzle puz, PuzHandle puzHandle) throws IOException {
        long incept = System.currentTimeMillis();

        FileHandle puzFile = puzHandle.getPuzFileHandle();
        FileHandle metaFile = puzHandle.getMetaFileHandle();

        if (metaFile == null) {
            String metaName = getMetaFileName(puzFile);
            metaFile = createFileHandle(puzHandle.getDirHandle(), metaName);
            if (metaFile == null)
                throw new IOException("Could not create meta file");
        }

        DirHandle tempFolder = getSaveTempDirectory();
        FileHandle puzTemp = createFileHandle(tempFolder, getName(puzFile));
        FileHandle metaTemp = createFileHandle(tempFolder, getName(metaFile));

        try (
            DataOutputStream puzzle
                = new DataOutputStream(getOutputStream(puzTemp));
            DataOutputStream meta
                = new DataOutputStream(getOutputStream(metaTemp));
        ) {
            IO.save(puz, puzzle, meta);
        }

        renameTo(puzTemp, puzFile);
        renameTo(metaTemp, metaFile);

        puzHandle.setMetaFileHandle(metaFile);
    }

    /**
     * Save the puz file to the file handle and create a meta file
     *
     * Assumed that a meta file does not exist already
     *
     * @param puzDir the directory containing puzFile (and where the
     * metta will be created)
     */
    public void saveCreateMeta(Puzzle puz, DirHandle puzDir, FileHandle puzFile)
        throws IOException {
        save(puz, new PuzHandle(puzDir, puzFile, null));
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

    /**
     * Write the handle to an intent
     *
     * Useful for starting activities
     */
    public void writePuzHandleToIntent(PuzHandle ph, Intent i) {
        i.putExtra(DIR_URI, getUri(ph.getDirHandle()).toString());
        i.putExtra(PUZ_URI, getUri(ph.getPuzFileHandle()).toString());

        String metaUri = null;
        FileHandle metaHandle = ph.getMetaFileHandle();
        if (metaHandle != null)
            metaUri = getUri(metaHandle).toString();

        i.putExtra(META_URI, metaUri);
    }

    /**
     * Read a previously written handle from an intent
     *
     * Useful for starting activities
     */
    public PuzHandle readPuzHandleFromIntent(Intent i) {
        String dirUri = i.getStringExtra(DIR_URI);
        String puzUri = i.getStringExtra(PUZ_URI);
        String metaUri = i.getStringExtra(META_URI);

        DirHandle dirHandle = getDirHandle(Uri.parse(dirUri));
        FileHandle puzHandle = getFileHandle(Uri.parse(puzUri));
        FileHandle metaHandle = null;
        if (metaUri != null)
            metaHandle = getFileHandle(Uri.parse(metaUri));

        return new PuzHandle(dirHandle, puzHandle, metaHandle);
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
