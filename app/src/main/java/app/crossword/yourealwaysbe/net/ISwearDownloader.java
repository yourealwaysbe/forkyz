package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
/**
 * http://wij.theworld.com/puzzles/dailyrecord/DR110401.puz
 * @author robert.cooper
 *
 */
public class ISwearDownloader extends AbstractDownloader {
    private static final String NAME = "I Swear";
    NumberFormat nf = NumberFormat.getInstance();

    public ISwearDownloader(){
    	super("http://wij.theworld.com/puzzles/dailyrecord/", DOWNLOAD_DIR, NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_FRIDAY;
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "DR" + (date.getYear() % 100) + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) + ".puz";
    }

	public String getName() {
		return NAME;
	}

	public Downloader.DownloadResult download(LocalDate date) {
		return super.download(date, this.createUrlSuffix(date));
	}


}
