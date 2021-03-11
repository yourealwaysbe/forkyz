package app.crossword.yourealwaysbe.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

public class AbstractPageScraper {
    private static final String REGEX = "http://[^ ^']*\\.puz";
    private static final String REL_REGEX = "href=\"(.*\\.puz)\"";
    private static final Pattern PAT = Pattern.compile(REGEX);
    private static final Pattern REL_PAT = Pattern.compile(REL_REGEX);
    private String sourceName;
    private String url;
    protected boolean updateable = false;

    protected AbstractPageScraper(String url, String sourceName) {
        this.url = url;
        this.sourceName = sourceName;
    }

    public String getContent() throws IOException {
        URL u = new URL(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IO.copyStream(u.openStream(), baos);

        return new String(baos.toByteArray());
    }

    public static FileHandle download(String url, String fileName)
            throws IOException {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        URL u = new URL(url);

        FileHandle output = fileHandler.createFileHandle(
            AbstractDownloader.getStandardDownloadDir(), fileName
        );
        if (output == null)
            return null;

        try (OutputStream fos = fileHandler.getOutputStream(output)) {
            IO.copyStream(u.openStream(), fos);
        } catch (Exception e) {
            fileHandler.delete(output);
            return null;
        }
        return output;
    }

    public static Map<String, String> mapURLsToFileNames(List<String> urls) {
        HashMap<String, String> result = new HashMap<String, String>(
                urls.size());

        for (String url : urls) {
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            result.put(url, fileName);
        }

        return result;
    }

    public static List<String> puzzleRelativeURLs(String baseUrl, String input)
            throws MalformedURLException {
        URL base = new URL(baseUrl);
        ArrayList<String> result = new ArrayList<String>();
        Matcher matcher = REL_PAT.matcher(input);

        while (matcher.find()) {
            result.add(new URL(base, matcher.group(1)).toString());
        }

        return result;
    }

    public static List<String> puzzleURLs(String input) {
        ArrayList<String> result = new ArrayList<String>();
        Matcher matcher = PAT.matcher(input);

        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    public String getSourceName() {
        return this.sourceName;
    }

    private boolean processFile(FileHandle file, String sourceUrl) {
        final FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        try {
            final Puzzle puz = fileHandler.load(file);
            // MATT: changed this from true to false
            // I'm not sure what purpose it has
            // Doesn't seem to be changeable from UI
            puz.setUpdatable(false);
            puz.setSource(this.sourceName);
            puz.setSourceUrl(sourceUrl);
            puz.setDate(LocalDate.now());

            DirHandle dir = AbstractDownloader.getStandardDownloadDir();

            fileHandler.saveCreateMeta(puz, dir, file);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            fileHandler.delete(file);

            return false;
        }
    }

    public List<FileHandle> scrape() {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        ArrayList<FileHandle> scrapedFiles = new ArrayList<>();

        try {
            String content = this.getContent();
            List<String> urls = puzzleURLs(content);

            try {
                urls.addAll(puzzleRelativeURLs(url, content));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Found puzzles: " + urls);

            Map<String, String> urlsToFilenames = mapURLsToFileNames(urls);
            System.out.println("Mapped: " + urlsToFilenames.size());

            Set<String> existingFiles = fileHandler.getFileNames(
                AbstractDownloader.getStandardDownloadDir(),
                AbstractDownloader.getStandardArchiveDir()
            );

            for (String url : urls) {
                String filename = urlsToFilenames.get(url);

                boolean exists = existingFiles.contains(filename);

                if (!exists && (scrapedFiles.size() < 3)) {
                    System.out.println("Attempting " + url + "  scraped "
                            + scrapedFiles.size());

                    try {
                        FileHandle file = download(url, filename);

                        if (file != null && this.processFile(file, url)) {
                            scrapedFiles.add(file);
                            System.out.println("SCRAPED.");
                        }
                    } catch (Exception e) {
                        System.err.println("Exception downloading " + url
                                + " for " + this.sourceName);
                        e.printStackTrace();
                    }
                } else {
                    System.out.println(filename + " exists.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return scrapedFiles;
    }
}
