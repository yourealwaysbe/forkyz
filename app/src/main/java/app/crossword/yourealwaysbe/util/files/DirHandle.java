package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

public class DirHandle {
    private File dir;

    public DirHandle(File dir) {
        this.dir = dir;
    }

    File getFile() { return dir; }

    public boolean exists() { return dir.exists(); }

    public int numFiles() {
        return dir.list().length;
    }

    public FileHandle[] getPuzFiles() { return getPuzFiles(null); }

    /**
     * Get puz files in directory matching a source
     *
     * Matches any source if sourceMatch is null
     */
    public FileHandle[] getPuzFiles(String sourceMatch) {
        ArrayList<FileHandle> files = new ArrayList<FileHandle>();
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".puz")) {
                PuzzleMeta m = null;

                try {
                    m = IO.meta(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                FileHandle h = new FileHandle(f, m);

                if ((sourceMatch == null) || sourceMatch.equals(h.getSource())) {
                    files.add(h);
                }
            }
        }

        return files.toArray(new FileHandle[files.size()]);
    }
}

