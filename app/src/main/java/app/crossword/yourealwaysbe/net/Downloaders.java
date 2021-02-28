package app.crossword.yourealwaysbe.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import androidx.core.app.NotificationCompat;

import app.crossword.yourealwaysbe.BrowseActivity;
import app.crossword.yourealwaysbe.PlayActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Downloaders {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    private Context context;
    private NotificationManager notificationManager;
    private boolean supressMessages;
    private SharedPreferences prefs;

    public Downloaders(SharedPreferences prefs,
                       NotificationManager notificationManager,
                       Context context) {
        this(prefs, notificationManager, context, true);
    }


    // Set isInteractive to true if this class can ask for user interaction when needed (e.g. to
    // refresh NYT credentials), false if otherwise.
    public Downloaders(SharedPreferences prefs,
                       NotificationManager notificationManager,
                       Context context,
                       boolean challengeForCredentials) {
        this.prefs = prefs;
        this.notificationManager = notificationManager;
        this.context = context;
        this.supressMessages = prefs.getBoolean("supressMessages", false);
    }

    public List<Downloader> getDownloaders(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<Downloader> retVal = new LinkedList<Downloader>();

        for (Downloader d : getDownloadersFromPrefs()) {
            // TODO: Downloader.getGoodThrough() should account for the day of week.
            if (Arrays.binarySearch(d.getDownloadDates(), dayOfWeek) >= 0) {
                LocalDate dGoodFrom = d.getGoodFrom();
                boolean isGoodFrom
                    = date.isEqual(dGoodFrom) || date.isAfter(dGoodFrom);
                LocalDate dGoodThrough = d.getGoodThrough();
                boolean isGoodThrough
                    = date.isBefore(dGoodThrough) || date.isEqual(dGoodThrough);

                if(isGoodFrom && isGoodThrough) {
                    retVal.add(d);
                }
            }
        }

        return retVal;
    }

    public void download(LocalDate date) {
        download(date, getDownloaders(date));
    }

    // Downloads the latest puzzles newer/equal to than the given date for the given set of
    // downloaders.
    //
    // If downloaders is null, then the full list of downloaders will be used.
    public void downloadLatestIfNewerThanDate(LocalDate oldestDate, List<Downloader> downloaders) {
        if (downloaders == null) {
            downloaders = new ArrayList<Downloader>();
        }

        if (downloaders.size() == 0) {
            downloaders.addAll(getDownloadersFromPrefs());
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            LocalDate goodThrough = d.getGoodThrough();
            DayOfWeek goodThroughDayOfWeek = goodThrough.getDayOfWeek();
            boolean isDay
                = Arrays.binarySearch(
                    d.getDownloadDates(), goodThroughDayOfWeek
                ) >= 0;
            boolean isGoodThrough
                = goodThrough.isEqual(oldestDate)
                    || goodThrough.isAfter(oldestDate);
            if (isDay && isGoodThrough) {
                LOG.info("Will try to download puzzle " + d + " @ " + goodThrough);
                puzzlesToDownload.put(d, goodThrough);
            }
        }

        if (!puzzlesToDownload.isEmpty()) {
            download(puzzlesToDownload);
        }
    }

    public void download(LocalDate date, List<Downloader> downloaders) {
        if ((downloaders == null) || (downloaders.size() == 0)) {
            downloaders = getDownloaders(date);
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            puzzlesToDownload.put(d, date);
        }

        download(puzzlesToDownload);
    }

    private void download(Map<Downloader, LocalDate> puzzlesToDownload) {
        String contentTitle = "Downloading Puzzles";

        NotificationCompat.Builder not =
                new NotificationCompat.Builder(context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(contentTitle)
                        .setWhen(System.currentTimeMillis());

        boolean somethingDownloaded = false;
        File crosswords = new File(Environment.getExternalStorageDirectory(), "crosswords/");
        File archive = new File(Environment.getExternalStorageDirectory(), "crosswords/archive/");
        crosswords.mkdirs();

        if (crosswords.listFiles() != null) {
            for (File isDel : crosswords.listFiles()) {
                if (isDel.getName()
                        .endsWith(".tmp")) {
                    isDel.delete();
                }
            }
        }

        HashSet<File> newlyDownloaded = new HashSet<File>();

        int nextNotificationId = 1;
        for (Map.Entry<Downloader, LocalDate> puzzle : puzzlesToDownload.entrySet()) {
            File downloaded = downloadPuzzle(puzzle.getKey(),
                    puzzle.getValue(),
                    not,
                    nextNotificationId++,
                    crosswords,
                    archive);
            if (downloaded != null) {
                somethingDownloaded = true;
                newlyDownloaded.add(downloaded);
            }

        }

        { // DO UPDATES

            ArrayList<File> checkUpdate = new ArrayList<File>();

            try {
                for (File file : crosswords.listFiles()) {
                    if (file.getName()
                            .endsWith(".forkyz")) {
                        File puz = new File(file.getAbsolutePath().substring(0,
                                file.getAbsolutePath().lastIndexOf('.') + 1) + "puz");
                        System.out.println(puz.getAbsolutePath());

                        if (!newlyDownloaded.contains(puz)) {
                            checkUpdate.add(puz);
                        }
                    }
                }

                archive.mkdirs();

                for (File file : archive.listFiles()) {
                    if (file.getName()
                            .endsWith(".forkyz")) {
                        checkUpdate.add(new File(file.getAbsolutePath().substring(0,
                                file.getAbsolutePath().lastIndexOf('.') + 1) + "puz"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (File file : checkUpdate) {
                try {
                    IO.meta(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (this.notificationManager != null) {
            this.notificationManager.cancel(0);
        }

        if (somethingDownloaded) {
            this.postDownloadedGeneral();
        }
    }

    private File downloadPuzzle(Downloader d,
                                LocalDate date,
                                NotificationCompat.Builder not,
                                int notificationId,
                                File crosswords,
                                File archive) {
        LOG.info("Downloading " + d.toString());
        d.setContext(context);

        try {
            String contentText = "Downloading from " + d.getName();
            Intent notificationIntent = new Intent(context, PlayActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            not.setContentText(contentText).setContentIntent(contentIntent);

            File downloaded = new File(crosswords, d.createFileName(date));
            File archived = new File(archive, d.createFileName(date));

            System.out.println(downloaded.getAbsolutePath() + " " + downloaded.exists() + " OR " +
                    archived.getAbsolutePath() + " " + archived.exists());

            if (!d.alwaysRun() && (downloaded.exists() || archived.exists())) {
                System.out.println("==Skipping " + d.toString());
                return null;
            }

            if (!this.supressMessages && this.notificationManager != null) {
                this.notificationManager.notify(0, not.build());
            }

            downloaded = d.download(date);

            if (downloaded == Downloader.DEFERRED_FILE) {
                return null;
            }

            if (downloaded != null) {
                boolean updatable = false;
                PuzzleMeta meta = new PuzzleMeta();
                meta.date = date;
                meta.source = d.getName();
                meta.sourceUrl = d.sourceUrl(date);
                meta.updatable = updatable;

                if (processDownloadedPuzzle(downloaded, meta)) {
                    if (!this.supressMessages) {
                        this.postDownloadedNotification(notificationId, d.getName(), downloaded);
                    }

                    return downloaded;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to download "+d.getName(), e);
            return null;
        }
        return null;
    }

    public static boolean processDownloadedPuzzle(
        FileHandle downloaded, PuzzleMeta meta
    ) {
        try {
            System.out.println("==PROCESSING " + downloaded + " hasmeta: "
                    + (meta != null));

            final FileHandler fileHandler
                = ForkyzApplication.getInstance().getFileHandler();
            final Puzzle puz = fileHandler.load(downloaded);
            if(puz == null){
                return false;
            }
            puz.setDate(meta.date);
            puz.setSource(meta.source);
            puz.setSourceUrl(meta.sourceUrl);
            puz.setUpdatable(meta.updatable);

            IO.save(puz, downloaded);

            return true;
        } catch (Exception ioe) {
            LOG.log(Level.WARNING, "Exception reading " + downloaded, ioe);
            downloaded.delete();

            return false;
        }
    }

    public void supressMessages(boolean b) {
        this.supressMessages = b;
    }

    private void postDownloadedGeneral() {
        String contentTitle = "Downloaded new puzzles!";

        Intent notificationIntent = new Intent(Intent.ACTION_EDIT, null,
                context, BrowseActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        Notification not = new NotificationCompat.Builder(context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(contentTitle)
                .setContentText("New puzzles were downloaded.")
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(0, not);
        }
    }

    private void postDownloadedNotification(int i, String name, File puzFile) {
        String contentTitle = "Downloaded " + name;

        Intent notificationIntent = new Intent(Intent.ACTION_EDIT,
                Uri.fromFile(puzFile), context, PlayActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        Notification not = new NotificationCompat.Builder(context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(contentTitle)
                .setContentText(puzFile.getName())
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(i, not);
        }
    }

    private List<Downloader> getDownloadersFromPrefs() {
        List<Downloader> downloaders = new LinkedList<>();

        if (prefs.getBoolean("downloadGuardianDailyCryptic", true)) {
            downloaders.add(new GuardianDailyCrypticDownloader());
        }

        if (prefs.getBoolean("downloadIndependentDailyCryptic", true)) {
            downloaders.add(new IndependentDailyCrypticDownloader());
        }

        if (prefs.getBoolean("downloadWsj", true)) {
            downloaders.add(new WSJFridayDownloader());
            downloaders.add(new WSJSaturdayDownloader());
        }

        if (prefs.getBoolean("downloadWaPoPuzzler", true)) {
            downloaders.add(new WaPoPuzzlerDownloader());
        }

        if (prefs.getBoolean("downloadJonesin", true)) {
            downloaders.add(new JonesinDownloader());
        }

        if (prefs.getBoolean("downloadLat", true)) {
//           downloaders.add(new UclickDownloader("tmcal", "Los Angeles Times", "Rich Norris", Downloader.DATE_NO_SUNDAY));
            downloaders.add(new LATimesDownloader());
        }

        if (prefs.getBoolean("downloadCHE", true)) {
            downloaders.add(new CHEDownloader());
        }

        if (prefs.getBoolean("downloadJoseph", true)) {
            downloaders.add(new KFSDownloader("joseph", "Joseph Crosswords",
                    "Thomas Joseph", Downloader.DATE_NO_SUNDAY));
        }

        if (prefs.getBoolean("downloadSheffer", true)) {
            downloaders.add(new KFSDownloader("sheffer", "Sheffer Crosswords",
                    "Eugene Sheffer", Downloader.DATE_NO_SUNDAY));
        }

        if (prefs.getBoolean("downloadNewsday", true)) {
            downloaders.add(new BrainsOnlyDownloader(
                    "http://brainsonly.com/servlets-newsday-crossword/newsdaycrossword?date=",
                    "Newsday"));
        }

        if (prefs.getBoolean("downloadUSAToday", true)) {
            downloaders.add(new UclickDownloader("usaon", "USA Today",
                    "USA Today", Downloader.DATE_NO_SUNDAY));
        }

        if (prefs.getBoolean("downloadUniversal", true)) {
            downloaders.add(new UclickDownloader("fcx", "Universal Crossword",
                    "uclick LLC", Downloader.DATE_DAILY));
        }

        if (prefs.getBoolean("downloadLACal", true)) {
            downloaders.add(new LATSundayDownloader());
        }

        return downloaders;
    }
}
