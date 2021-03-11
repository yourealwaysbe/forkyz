package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;

import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.JPZIO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.versions.DefaultUtil;

public abstract class AbstractJPZDownloader extends AbstractDownloader {

    protected AbstractJPZDownloader(
        String baseUrl, DirHandle downloadDirectory, String downloaderName
    ) {
        super(baseUrl, downloadDirectory, downloaderName);
    }

    protected Downloader.DownloadResult download(
        LocalDate date, String urlSuffix, Map<String, String> headers
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        DownloadResult jpzResult = download(date, urlSuffix, headers, false);
        FileHandle jpzFile = jpzResult.getFileHandle();

        FileHandle puzFile = fileHandler.createFileHandle(
            downloadDirectory, this.createFileName(date)
        );
        if (puzFile == null)
            return null;

        try (
            InputStream is = fileHandler.getInputStream(jpzFile);
            DataOutputStream dos
                = new DataOutputStream(fileHandler.getOutputStream(puzFile));
        ) {
            JPZIO.convertJPZPuzzle(is, dos , date);
            fileHandler.delete(jpzFile);
            return new Downloader.DownloadResult(puzFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String createFileName(LocalDate date) {
        return date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth() + "-" +
        this.getName().replaceAll(" ", "") + ".puz";
    }

    protected Downloader.DownloadResult download(
        LocalDate date,
        String urlSuffix,
        Map<String, String> headers,
        boolean canDefer
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            LOG.info("Downloading from "+url);

            FileHandle f = fileHandler.createFileHandle(
                downloadDirectory, this.createFileName(date)+".jpz"
            );
            if (f == null)
                return null;

            PuzzleMeta meta = new PuzzleMeta();
            meta.date = date;
            meta.source = getName();
            meta.sourceUrl = url.toString();
            meta.updatable = false;

            Uri fileUri = fileHandler.getUri(f);
            utils.storeMetas(fileHandler.getUri(f), meta, downloadDirectory);

            if (utils.downloadFile(url, f, headers, true, this.getName())) {
                utils.removeMetas(fileHandler.getUri(f));
                return new Downloader.DownloadResult(f);
            } else if (canDefer) {
                return Downloader.DownloadResult.DEFERRED_FILE;
            } else {
                utils.removeMetas(fileUri);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
