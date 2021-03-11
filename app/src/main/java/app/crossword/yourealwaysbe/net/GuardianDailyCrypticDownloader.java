package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;

/**
 * Guardian Daily Cryptic downloader
 * URL: https://www.theguardian.com/crosswords/cryptic/
 * Date = Daily
 */
public class GuardianDailyCrypticDownloader extends AbstractDownloader {
    private static final String NAME = "Guardian Daily Cryptic";

    private static final int BASE_CW_NUMBER = 28112;
    private static final LocalDate BASE_CW_DATE = LocalDate.of(2020, 4, 20);

    private static final int CW_WIDTH = 15;
    private static final int CW_HEIGHT = 15;

    public GuardianDailyCrypticDownloader() {
        super(
            "https://www.theguardian.com/crosswords/cryptic/",
            getStandardDownloadDir(),
            NAME
        );
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_DAILY;
    }

    public String getName() {
        return NAME;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        return download(date, this.createUrlSuffix(date), EMPTY_MAP);
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

            String fileName = createFileName(date);

            FileHandle f = fileHandler.createFileHandle(
                downloadDirectory, fileName
            );
            if (f == null)
                return null;

            URL url = new URL(this.baseUrl + urlSuffix);
            JSONObject cw = getCrosswordJSON(url);

            if (cw == null)
                return null;

            Puzzle puz = readPuzzleFromJSON(cw, date);

            try (
                DataOutputStream dos = new DataOutputStream(
                    fileHandler.getOutputStream(f)
                )
            ) {
                puz.setVersion(IO.VERSION_STRING);
                IO.saveNative(puz, dos);
                return new Downloader.DownloadResult(f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected String createUrlSuffix(LocalDate date) {
        LocalDate lower = BASE_CW_DATE;
        LocalDate upper = date;
        int direction = 1;

        if (lower.isAfter(upper)) {
            lower = date;
            upper = BASE_CW_DATE;
            direction = -1;
        }

        Duration diff = Duration.between(lower.atStartOfDay(),
                                         upper.atStartOfDay());

        long daysDiff = diff.toDays();
        int yearsDiff = Period.between(lower, upper).getYears();

        long cwNumOffset = daysDiff;
        // no Sundays (base day is Monday so negative gets one more)
        cwNumOffset -= (daysDiff / 7);
        if (direction < 0 && daysDiff % 7 != 0)
            cwNumOffset -= 1;
        // no Christmas
        cwNumOffset -= countNonSundayChristmas(lower, upper);
        // no Boxing day pre 2010
        cwNumOffset -= countNonSundayBoxing(lower, LocalDate.of(2009, 12, 26));

        long cwNum = BASE_CW_NUMBER + direction * cwNumOffset;

        return Long.toString(cwNum);
    }

    private static JSONObject getCrosswordJSON(URL url)
            throws IOException, JSONException {
        try {
            LOG.info("Downloading " + url);
            Document doc = Jsoup.connect(url.toString()).get();
            String cwJson = doc.select(".js-crossword")
                               .attr("data-crossword-data");

            if (!cwJson.isEmpty())
                return new JSONObject(cwJson);
        } catch (HttpStatusException e) {
            LOG.info("Could not download " + url);
        }
        return null;
    }

    // take date argument as reading from json date field brings
    // timezones into play
    private static Puzzle readPuzzleFromJSON(
        JSONObject json, LocalDate date
    ) throws JSONException {
        Puzzle puz = new Puzzle();

        puz.setTitle(json.getString("name"));
        puz.setAuthor(json.getJSONObject("creator").getString("name"));
        puz.setCopyright("Guardian / " + puz.getAuthor());
        puz.setDate(date);
        puz.setWidth(CW_WIDTH);
        puz.setHeight(CW_HEIGHT);

        puz.setBoxes(getBoxes(json));
        puz.setRawClues(getRawClues(json));

        return puz;
    }

    private static Box[][] getBoxes(JSONObject json) throws JSONException {
        Box[][] boxes = new Box[CW_HEIGHT][CW_WIDTH];

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            JSONObject position = entry.getJSONObject("position");
            int x = position.getInt("x");
            int y = position.getInt("y");

            if (x < 0 || x >= CW_WIDTH || y < 0 || y >= CW_HEIGHT)
                continue;

            int num = entry.getInt("number");
            String clueSol = entry.getString("solution");
            String direction = entry.getString("direction");

            int dx = 0;
            int dy = 0;
            if (direction.equals("across"))
                dx = 1;
            else
                dy = 1;

            int boxX = x;
            int boxY = y;
            for (int j = 0; j < clueSol.length(); j++) {
                if (boxX >= CW_WIDTH || boxY >= CW_HEIGHT)
                    break;

                if (boxes[boxY][boxX] == null)
                    boxes[boxY][boxX] = new Box();
                boxes[boxY][boxX].setSolution(clueSol.charAt(j));

                boxX += dx;
                boxY += dy;
            }

            boxes[y][x].setClueNumber(num);
        }

        return boxes;
    }

    private static String[] getRawClues(JSONObject json) throws JSONException {
        Map<Integer, String> acrossClues = new HashMap<>();
        Map<Integer, String> downClues = new HashMap<>();
        int maxClueNum = 0;

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            int num = entry.getInt("number");
            boolean across = entry.getString("direction").equals("across");
            String clue = entry.getString("clue");

            if (across)
                acrossClues.put(num, clue);
            else
                downClues.put(num, clue);

            maxClueNum = Math.max(maxClueNum, num);
        }

        String[] rawClues = new String[entries.length()];

        int cluePos = 0;
        for (int i = 0; i <= maxClueNum; i++) {
            if (acrossClues.containsKey(i))
                rawClues[cluePos++] = acrossClues.get(i);
            if (downClues.containsKey(i))
                rawClues[cluePos++] = downClues.get(i);
        }

        return rawClues;
    }

    /**
     * Counts number of Christmasses that aren't Sunday between dates
     * (inclusive).
     *
     * Returns 0 if upper below lower
     */
    private static int countNonSundayChristmas(LocalDate lower, LocalDate upper) {
        return countNonSundaySpecial(lower, upper, 12, 25);
    }

    /**
     * Counts number of Boxing Days that aren't Sunday between dates
     * (inclusive)
     *
     * Returns 0 if upper below lower
     */
    private static int countNonSundayBoxing(LocalDate lower, LocalDate upper) {
        return countNonSundaySpecial(lower, upper, 12, 26);
    }

    /**
     * Counts number of special days that aren't Sunday between dates
     * (inclusive)
     *
     * @param lower start date inclusive
     * @param upper end date inclusive
     * @param month month of special date (1-12)
     * @param day day of special date (1-31)
     * @return number of non-sunday occurences, or 0 if upper &lt; lower
     */
    private static int countNonSundaySpecial(LocalDate lower,
                                             LocalDate upper,
                                             int month,
                                             int day) {
        if (upper.isBefore(lower))
            return 0;

        LocalDate special = LocalDate.of(lower.getYear(), month, day);
        if (lower.isAfter(special))
            special = special.plusYears(1);

        int count = 0;
        while (!special.isAfter(upper)) {
            if (DayOfWeek.from(special) != DayOfWeek.SUNDAY)
                count += 1;
            special = special.plusYears(1);
        }

        return count;
    }

}
