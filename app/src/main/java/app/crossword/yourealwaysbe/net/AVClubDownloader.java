package app.crossword.yourealwaysbe.net;

import app.crossword.yourealwaysbe.io.IO;

import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * The Onion AV Club
 * URL: http://herbach.dnsalias.com/Tausig/avYYMMDD.puz
 * Date = Wednesdays
 */
public class AVClubDownloader extends AbstractDownloader {
    public static final String NAME = "The Onion AV Club";
    NumberFormat nf = NumberFormat.getInstance();

    protected AVClubDownloader() {
        super("http://herbach.dnsalias.com/Tausig/", DOWNLOAD_DIR, NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_WEDNESDAY;
    }

    public String getName() {
        return NAME;
    }

    @Override
    public Downloader.DownloadResult download(LocalDate date) {
        return this.download(date, this.createUrlSuffix(date));
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "av" + this.nf.format(date.getYear() % 100) + this.nf.format(date.getMonthValue()) +
        this.nf.format(date.getDayOfMonth()) + ".puz";
    }

    @Override
    protected Downloader.DownloadResult download(
        LocalDate date, String urlSuffix
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            System.out.println(url);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", this.baseUrl);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                FileHandle f = fileHandler.getFileHandle(
                    downloadDirectory, this.createFileName(date)
                );
                try (OutputStream fos = fileHandler.getOutputStream(f)) {
                    IO.copyStream(connection.getInputStream(), fos);
                }
                return new Downloader.DownloadResult(f);
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
