package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;


/**
 * Washington Post Puzzler
 * URL: http://crosswords.washingtonpost.com/wp-srv/style/crosswords/util/csserve2.cgi?t=puz&z=puzzler&f=csYYMMDD.puz
 * Date = Sundays
 */
public class WaPoPuzzlerDownloader extends AbstractDownloader {
    private static final String NAME = "Washington Post Puzzler";
    NumberFormat nf = NumberFormat.getInstance();

    public WaPoPuzzlerDownloader() {
        super(
            "http://crosswords.washingtonpost.com/wp-srv/style/crosswords/util/csserve2.cgi?t=puz&z=puzzler&f=",
            getStandardDownloadDir(),
            NAME
        );
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
        return "cs" + nf.format(date.getYear() % 100) + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) +
        ".puz";
    }
}
