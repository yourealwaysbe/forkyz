package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.JPZIO;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.versions.DefaultUtil;

public abstract class AbstractJPZDownloader extends AbstractDownloader {

	protected AbstractJPZDownloader(String baseUrl, File downloadDirectory, String downloaderName) {
		super(baseUrl, downloadDirectory, downloaderName);
	}
	
	
	
	protected File download(LocalDate date, String urlSuffix, Map<String, String> headers) {
		File jpzFile = download(date, urlSuffix, headers, false);
		File puzFile = new File(downloadDirectory, this.createFileName(date));
		try {
			FileInputStream is = new FileInputStream(jpzFile);
	        DataOutputStream dos = new DataOutputStream(new FileOutputStream(puzFile));
			JPZIO.convertJPZPuzzle(is, dos , date);
			dos.close();
			jpzFile.delete();
			return puzFile;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String createFileName(LocalDate date) {
        return date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth() + "-" +
        this.getName().replaceAll(" ", "") + ".puz";
    }
	
	protected File download(LocalDate date, String urlSuffix, Map<String, String> headers, boolean canDefer) {
        LOG.info("Mkdirs: " + this.downloadDirectory.mkdirs());
        LOG.info("Exist: " + this.downloadDirectory.exists());

        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            LOG.info("Downloading from "+url);

            File f = new File(downloadDirectory, this.createFileName(date)+".jpz");
            PuzzleMeta meta = new PuzzleMeta();
            meta.date = date;
            meta.source = getName();
            meta.sourceUrl = url.toString();
            meta.updatable = false;
            
            utils.storeMetas(Uri.fromFile(f), meta);
            if( canDefer ){
	            if (utils.downloadFile(url, f, headers, true, this.getName())) {
	                DownloadReceiver.metas.remove(Uri.fromFile(f));
	
	                return f;
	            } else {
	                return Downloader.DEFERRED_FILE;
	            }
            } else {
            	AndroidVersionUtils.Factory.getInstance().downloadFile(url, f, headers, true, this.getName());
            	return f;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
