package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;


/**
 * Los Angeles Times
 * URL: http://www.cruciverb.com/puzzles/lat/latYYMMDD.puz
 * Date = Daily
 */
public class OldLATDownloader extends AbstractDownloader {
    public static final String NAME = "Los Angeles Times";
    NumberFormat nf = NumberFormat.getInstance();

    protected OldLATDownloader() {
        super("http://www.cruciverb.com/puzzles/lat/", getStandardDownloadDir(), NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_DAILY;
    }

    public String getName() {
        return OldLATDownloader.NAME;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        return this.download(date, this.createUrlSuffix(date));
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "lat" + this.nf.format(date.getYear() % 100) + this.nf.format(date.getMonthValue()) +
        this.nf.format(date.getDayOfMonth()) + ".puz";
    }

    @Override
    protected Downloader.DownloadResult download(
        LocalDate date, String urlSuffix
    ) {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Referer", "http://www.cruciverb.com/puzzles.php?op=showarch&pub=lat");
        headers.put("User-Agent",
            "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.13 (KHTML, like Gecko) Chrome/9.0.597.0 Safari/534.13");

        return super.download(date, urlSuffix, headers);
    }
}
