package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.logging.Level;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.UclickXMLIO;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.versions.DefaultUtil;

/**
 * Uclick XML Puzzles
 * URL: http://picayune.uclick.com/comics/[puzzle]/data/[puzzle]YYMMDD-data.xml
 * crnet (Newsday) = Daily
 * usaon (USA Today) = Monday-Saturday (not holidays)
 * fcx (Universal) = Daily
 * lacal (LA Times Sunday Calendar) = Sunday
 */
public class UclickDownloader extends AbstractDownloader {
    DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
    NumberFormat nf = NumberFormat.getInstance();
    private String copyright;
    private String fullName;
    private String shortName;
    private DayOfWeek[] days;

    public UclickDownloader(String prefix, String shortName, String fullName, String copyright, DayOfWeek[] days){
        super(prefix+shortName+"/data/", DOWNLOAD_DIR, fullName);
        this.shortName = shortName;
        this.fullName = fullName;
        this.copyright = copyright;
        this.days = days;
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public UclickDownloader(String shortName, String fullName, String copyright, DayOfWeek[] days) {
        this("http://picayune.uclick.com/comics/",shortName, fullName, copyright, days);

    }

    public DayOfWeek[] getDownloadDates() {
        return days;
    }

    public String getName() {
        return fullName;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        FileHandle downloadTo = fileHandler.getFileHandle(
            this.downloadDirectory, this.createFileName(date)
        );

        if (fileHandler.exists(downloadTo)) {
            return null;
        }

        FileHandle plainText = downloadToTempFile(date);

        if (plainText == null) {
            return null;
        }

        try (
            InputStream is = fileHandler.getInputStream(plainText);
            DataOutputStream os = new DataOutputStream(
                fileHandler.getOutputStream(downloadTo)
            );
        ) {
            boolean retVal = UclickXMLIO.convertUclickPuzzle(is, os,
                    "\u00a9 " + date.getYear() + " " + copyright, date);
            os.close();
            is.close();
            fileHandler.delete(plainText);

            if (!retVal) {
                LOG.log(Level.SEVERE, "Unable to convert uclick XML puzzle into Across Lite format.");
                fileHandler.delete(downloadTo);
                downloadTo = null;
            }
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Exception converting uclick XML puzzle into Across Lite format.", ioe);
            fileHandler.delete(downloadTo);
            downloadTo = null;
        }

        return new Downloader.DownloadResult(downloadTo);
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return this.shortName + nf.format(date.getYear() % 100) + nf.format(date.getMonthValue()) +
        nf.format(date.getDayOfMonth()) + "-data.xml";
    }

    private FileHandle downloadToTempFile(LocalDate date) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        FileHandle f = fileHandler.getFileHandle(
            downloadDirectory, this.createFileName(date)
        );

        try {
            URL url = new URL(this.baseUrl + this.createUrlSuffix(date));
            LOG.log(Level.INFO, this.fullName+" "+url.toExternalForm());
            AndroidVersionUtils.Factory.getInstance().downloadFile(url, f, EMPTY_MAP, false, null);
        } catch (Exception e) {
            e.printStackTrace();
            f = null;
        }

        if (f == null) {
            LOG.log(Level.SEVERE, "Unable to download uclick XML file.");

            return null;
        }

        try {

            FileHandle tmpFile = fileHandler.getFileHandle(
                this.tempFolder,
                "uclick-temp"+System.currentTimeMillis()+".xml"
            );
            fileHandler.renameTo(f, tmpFile);

            return tmpFile;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to move uclick XML file to temporary location.");

            return null;
        }
    }
}
