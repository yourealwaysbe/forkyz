package app.crossword.yourealwaysbe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.widget.Toast;

import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Listener for WebBrowserActivity which handles downloading a puzzle and adding it to the puzzle
 * database.
 */
public class PuzzleDownloadListener implements DownloadListener {
    private static final String ERROR_MSG = "error";
    private Context mContext;
    private FileHandler fileHandler;
    private Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Toast.makeText(mContext, msg.getData().getString(ERROR_MSG), Toast.LENGTH_SHORT)
                     .show();
            }
        };

    public PuzzleDownloadListener(Context c, FileHandler fileHandler) {
        mContext = c;
        this.fileHandler = fileHandler;
    }

    public void onDownloadStart(final String url, final String userAgent, final String contentDisposition,
        final String mimetype, final long contentLength) {
        Thread t = new Thread() {
                public void run() {
                    performDownload(url, userAgent, contentDisposition, mimetype, contentLength);
                }
            };

        t.start();
    }

    private InputStream OpenHttpConnection(URL url, String cookies)
        throws IOException {
        InputStream in = null;
        URLConnection conn = url.openConnection();

        if ((cookies != null) && !cookies.trim()
                                             .equals("")) {
            conn.setRequestProperty("Cookie", cookies);
        }

        conn.setAllowUserInteraction(false);
        conn.connect();
        in = conn.getInputStream();

        return in;
    }

    private void performDownload(String url, String userAgent, String contentDisposition, String mimetype,
        long contentLength) {
        DirHandle crosswordFolder = fileHandler.getCrosswordsDirectory();
        DirHandle archiveFolder = fileHandler.getArchiveDirectory();
        String cookies = CookieManager.getInstance()
                                      .getCookie(url);
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

        if ((fileName == null) || fileName.equals("")) {
            fileName = "downloaded-puzzle-" + System.currentTimeMillis() + ".puz";
        }

        if (!fileName.endsWith(".puz")) {
            fileName = fileName + ".puz";
        }

        FileHandle outputFile
            = fileHandler.getFileHandle(crosswordFolder, fileName);
        FileHandle archiveOutputFile
            = fileHandler.getFileHandle(archiveFolder, fileName);

        try (
            InputStream in = OpenHttpConnection(new URL(url), cookies)
        ) {
            if (fileHandler.exists(outputFile)
                    || fileHandler.exists(archiveOutputFile)) {
                sendMessage("Puzzle " + fileName + " already exists.");

                return;
            }

            try (OutputStream fout = fileHandler.getOutputStream(outputFile);) {
                byte[] buffer = new byte[1024];
                int len = 0;

                while ((len = in.read(buffer)) != -1) {
                    fout.write(buffer, 0, len);
                }
            }

            PuzzleMeta meta = new PuzzleMeta();
            meta.date = LocalDate.now();

            boolean processed
                = Downloaders.processDownloadedPuzzle(outputFile, meta);

            if (processed) {
                sendMessage(
                    "Puzzle " + fileName + " downloaded successfully."
                );
            } else {
                sendMessage("Error parsing puzzle " + fileName);
            }
        } catch (Exception ex) {
            sendMessage("Error downloading puzzle " + fileName);
        }
    }

    private void sendMessage(String message) {
        Bundle data = new Bundle();
        data.putString(ERROR_MSG, message);

        Message msg = new Message();
        msg.setData(data);
        handler.sendMessage(msg);
    }
}
