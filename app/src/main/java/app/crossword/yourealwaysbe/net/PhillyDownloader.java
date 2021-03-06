package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;


/**
 * Philadelphia Inquirer
 * URL: http://mazerlm.home.comcast.net/~mazerlm/piYYMMDD.puz
 * Date = Sundays
 */
public class PhillyDownloader extends AbstractDownloader {
    private static final String NAME = "Phil Inquirer";
    NumberFormat nf = NumberFormat.getInstance();

    public PhillyDownloader() {
        super("http://mazerlm.home.comcast.net/~mazerlm/", DOWNLOAD_DIR, NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_SUNDAY;
    }

    public String getName() {
        return NAME;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        return super.download(date, this.createUrlSuffix(date));
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "pi" + nf.format(date.getYear() % 100) + nf.format(date.getMonthValue() ) + nf.format(date.getDayOfMonth()) +
        ".puz";
    }
}
