package app.crossword.yourealwaysbe.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.versions.DefaultUtil;

public abstract class AbstractDownloader implements Downloader {
    protected static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    public static final DirHandle DOWNLOAD_DIR
        = ForkyzApplication
            .getInstance()
            .getFileHandler()
            .getCrosswordsDirectory();
    public static final DirHandle ARCHIVE_DIR
        = ForkyzApplication
            .getInstance()
            .getFileHandler()
            .getArchiveDirectory();

    protected static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    protected DirHandle downloadDirectory;
    protected String baseUrl;
    protected final AndroidVersionUtils utils = AndroidVersionUtils.Factory.getInstance();
    private String downloaderName;
    protected DirHandle tempFolder;
    protected LocalDate goodThrough = LocalDate.now();

    protected AbstractDownloader(
        String baseUrl, DirHandle downloadDirectory, String downloaderName
    ) {
        this.baseUrl = baseUrl;
        this.downloadDirectory = downloadDirectory;
        this.downloaderName = downloaderName;
        this.tempFolder
            = ForkyzApplication
                .getInstance()
                .getFileHandler()
                .getTempDirectory(downloadDirectory);
    }

    public void setContext(Context ctx) {
        this.utils.setContext(ctx);
    }

    public String createFileName(LocalDate date) {
        return (
            date.getYear() + "-" +
            date.getMonthValue() + "-" +
            date.getDayOfMonth() + "-" +
            this.downloaderName.replaceAll(" ", "") + ".puz"
        );
    }

    public String sourceUrl(LocalDate date) {
        return this.baseUrl + this.createUrlSuffix(date);
    }

    public String toString() {
        return getName();
    }

    protected abstract String createUrlSuffix(LocalDate date);

    protected Downloader.DownloadResult download(
        LocalDate date,
        String urlSuffix,
        Map<String, String> headers
    ){
        System.out.println("DL From ASD");
        return download(date, urlSuffix, headers, true);
    }

    protected Downloader.DownloadResult download(
        LocalDate date,
        String urlSuffix,
        Map<String, String> headers,
        boolean canDefer
    ) {
        try {
            FileHandler fileHandler
                = ForkyzApplication.getInstance().getFileHandler();
            URL url = new URL(this.baseUrl + urlSuffix);
            System.out.println(url);

            FileHandle f = fileHandler.getFileHandle(
                downloadDirectory, this.createFileName(date)
            );
            PuzzleMeta meta = new PuzzleMeta();
            meta.date = date;
            meta.source = getName();
            meta.sourceUrl = url.toString();
            meta.updatable = false;

            Uri fileUri = fileHandler.getUri(f);
            utils.storeMetas(fileUri, meta);
            if( canDefer ){
                if (utils.downloadFile(url, f, headers, true, this.getName())) {
                    DownloadReceiver.metas.remove(fileUri);

                    return new Downloader.DownloadResult(f);
                } else {
                    return Downloader.DownloadResult.DEFERRED_FILE;
                }
            } else {
                AndroidVersionUtils.Factory.getInstance().downloadFile(url, f, headers, true, this.getName());
                return new Downloader.DownloadResult(f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected Downloader.DownloadResult download(
        LocalDate date, String urlSuffix
    ) {
        return download(date, urlSuffix, EMPTY_MAP);
    }


    protected FileHandle downloadToTempFile(String fullName, LocalDate date) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        FileHandle downloaded = fileHandler.getFileHandle(
            tempFolder,
            "txt-tmp"+System.currentTimeMillis()+".txt"
        );

        try {
            URL url = new URL(this.baseUrl + this.createUrlSuffix(date));
            LOG.log(Level.INFO, fullName +" "+url.toExternalForm());
            AndroidVersionUtils.Factory.getInstance().downloadFile(url, downloaded, EMPTY_MAP, false, null);
        } catch (Exception e) {
            e.printStackTrace();
            fileHandler.delete(downloaded);
            downloaded = null;
        }

        if (downloaded == null) {
            LOG.log(Level.SEVERE, "Unable to download plain text KFS file.");

            return null;
        } else {
            return downloaded;
        }
    }

    @Override
    public boolean alwaysRun(){
        return false;
    }

    public LocalDate getGoodThrough(){
        return this.goodThrough;
    }

    public LocalDate getGoodFrom(){
        return LocalDate.ofEpochDay(0L);
    }
}
