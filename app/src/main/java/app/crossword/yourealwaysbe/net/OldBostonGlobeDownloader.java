package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;


/**
 * Boston Globe
 * URL: http://standalone.com/dl/puz/boston/boston_MMDDYY.puz
 * Date = Sundays
 */
public class OldBostonGlobeDownloader extends AbstractDownloader {
    private static final String NAME = "Boston Globe";
    NumberFormat nf = NumberFormat.getInstance();

    public OldBostonGlobeDownloader() {
        super("http://standalone.com/dl/puz/boston/", getStandardDownloadDir(), NAME);
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
        return "boston_" + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) + date.getYear() +
        ".puz";
    }
}
