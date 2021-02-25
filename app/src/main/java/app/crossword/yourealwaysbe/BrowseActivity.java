package app.crossword.yourealwaysbe;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.net.Downloader;
import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.net.Scrapers;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.forkyz.BuildConfig;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.view.CircleProgressBar;
import app.crossword.yourealwaysbe.view.recycler.RecyclerItemClickListener;
import app.crossword.yourealwaysbe.view.recycler.RemovableRecyclerViewAdapter;
import app.crossword.yourealwaysbe.view.recycler.SeparatedRecyclerViewAdapter;
import app.crossword.yourealwaysbe.view.recycler.ShowHideOnScroll;
import app.crossword.yourealwaysbe.view.StoragePermissionDialog;
import app.crossword.yourealwaysbe.util.files.Accessor;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BrowseActivity extends ForkyzActivity implements RecyclerItemClickListener.OnItemClickListener{
    private static final String MENU_ARCHIVES = "Archives";
    private static final int REQUEST_WRITE_STORAGE = 1002;
    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final Logger LOGGER = Logger.getLogger(BrowseActivity.class.getCanonicalName());
    private Accessor accessor = Accessor.DATE_DESC;
    private SeparatedRecyclerViewAdapter<FileViewHolder> currentAdapter = null;
    private DirHandle archiveFolder = getFileHandler().getArchiveDirectory();
    private DirHandle crosswordsFolder = getFileHandler().getCrosswordsDirectory();
    private FileHandle lastOpenedHandle = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView puzzleList;
    private ListView sources;
    private NotificationManager nm;
    private View lastOpenedView = null;
    private boolean viewArchive;
    private MenuItem gamesItem;
    private boolean signedIn;
    private boolean hasWritePermissions;
    private FloatingActionButton download;
    private int highlightColor;
    private int normalColor;
    private HashSet<FileHandle> selected = new HashSet<>();
    private ActionMode actionMode;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //4
            actionMode = mode;
            MenuItem item = menu.add("Delete");
            item.setIcon(android.R.drawable.ic_menu_delete);
            utils.onActionBarWithText(item);
            item = menu.add(viewArchive ? "Un-archive" : "Archive");
            utils.onActionBarWithText(item);
            item.setIcon(R.drawable.ic_action_remove);
            download.setVisibility(View.GONE);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if(menuItem.getTitle().equals("Delete")){
                for(FileHandle handle : selected){
                    handle.delete();
                }
                puzzleList.invalidate();
                actionMode.finish();
            } else if(menuItem.getTitle().equals("Archive")){
                for(FileHandle handle : selected){
                    handle.moveTo(archiveFolder);
                }
                puzzleList.invalidate();
                actionMode.finish();
            } else if(menuItem.getTitle().equals("Un-archive")){
                for(FileHandle handle : selected){
                    handle.moveTo(crosswordsFolder);
                }
                puzzleList.invalidate();
                actionMode.finish();
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selected.clear();
            render();
            download.setVisibility(View.VISIBLE);
            actionMode = null;
        }
    };
    private int primaryTextColor;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browse_menu, menu);

        if (utils.isNightModeAvailable()) {
            MenuItem item = menu.findItem(R.id.browse_menu_app_theme);
            if (item != null) item.setIcon(getNightModeIcon());
        } else {
            menu.removeItem(R.id.browse_menu_app_theme);
        }

        return true;
    }

    private int getNightModeIcon() {
        switch (nightMode.getCurrentMode()) {
        case DAY: return R.drawable.day_mode;
        case NIGHT: return R.drawable.night_mode;
        case SYSTEM: return R.drawable.system_daynight_mode;
        }
        return R.drawable.day_mode;
    }

    private void setListItemColor(View v, boolean selected){
        if(selected) {
            v.setBackgroundColor(highlightColor);
            ((TextView) v.findViewById(R.id.puzzle_name)).setTextColor(Color.WHITE);
        } else {
            v.setBackgroundColor(normalColor);
            ((TextView) v.findViewById(R.id.puzzle_name)).setTextColor(primaryTextColor);
        }
    }


	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.browse_menu_app_theme:
            this.utils.nextNightMode(this);
            item.setIcon(getNightModeIcon());
            return true;
        case R.id.browse_menu_settings:
            Intent settingsIntent = new Intent(this, PreferencesActivity.class);
            this.startActivity(settingsIntent);

            return true;
        case R.id.browse_menu_archives:
            this.viewArchive = !viewArchive;
            item.setTitle(viewArchive ? "Crosswords" : MENU_ARCHIVES); //menu item title
            this.setTitle(!viewArchive ? "Puzzles" : MENU_ARCHIVES); //activity title

            render();

            return true;
        case R.id.browse_menu_cleanup:
            this.cleanup();

            return true;
        case R.id.browse_menu_help:
            Intent helpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/filescreen.html"), this,
                    HTMLActivity.class);
            this.startActivity(helpIntent);
            return true;
        case R.id.browse_menu_sort_source:
            this.accessor = Accessor.SOURCE;
            prefs.edit()
                 .putInt("sort", 2)
                 .apply();
            this.render();
            return true;
        case R.id.browse_menu_sort_date_asc:
            this.accessor = Accessor.DATE_ASC;
            prefs.edit()
                 .putInt("sort", 1)
                 .apply();
            this.render();
            return true;
        case R.id.browse_menu_sort_date_desc:
            this.accessor = Accessor.DATE_DESC;
            prefs.edit()
                 .putInt("sort", 0)
                 .apply();
            this.render();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle("Puzzles");
        //this.utils.hideTitleOnPortrait(this);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        this.setContentView(R.layout.browse);
        this.showMenuAlways();
        this.puzzleList = (RecyclerView) this.findViewById(R.id.puzzleList);
        this.puzzleList.setLayoutManager(new LinearLayoutManager(this));
        this.puzzleList.addOnItemTouchListener(new RecyclerItemClickListener(this, this.puzzleList, this));
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END,
                ItemTouchHelper.START | ItemTouchHelper.END) {

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (!(viewHolder instanceof FileViewHolder) || prefs.getBoolean("disableSwipe", false)) {
                    return 0; // Don't swipe the headers.
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                if(!selected.isEmpty()){
                    return;
                }
                if(!(viewHolder instanceof FileViewHolder)){
                    return;
                }
                FileHandle handle = (FileHandle) ((FileViewHolder) viewHolder).itemView.getTag();
                System.out.println(" SWIPED "+handle.file.getAbsolutePath());
                if("DELETE".equals(prefs.getString("swipeAction", "DELETE"))) {
                    handle.delete();
                } else {
                    if (viewArchive) {
                        handle.moveTo(crosswordsFolder);
                    } else {
                        handle.moveTo(archiveFolder);
                    }
                }
                currentAdapter.onItemDismiss(viewHolder.getAdapterPosition());
                puzzleList.invalidate();
            }
        });
        helper.attachToRecyclerView(this.puzzleList);
        upgradePreferences();
        this.nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        switch (prefs.getInt("sort", 0)) {
        case 2:
            this.accessor = Accessor.SOURCE;

            break;

        case 1:
            this.accessor = Accessor.DATE_ASC;

            break;

        default:
            this.accessor = Accessor.DATE_DESC;
        }


        download = (FloatingActionButton) this.findViewById(R.id.button_floating_action);
        if(download != null) {
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogFragment dialog = new DownloadDialog();
                    dialog.show(getSupportFragmentManager(), "DownloadDialog");
                }
            });
            download.setImageBitmap(createBitmap("icons1.ttf", ","));
            this.puzzleList.setOnTouchListener(new ShowHideOnScroll(download));

        }

        highlightColor = ContextCompat.getColor(this, R.color.accent);
        normalColor = ContextCompat.getColor(this, R.color.background_light);
        primaryTextColor = ContextCompat.getColor(this, R.color.textColorPrimary);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                DialogFragment dialog = new StoragePermissionDialog();
                Bundle args = new Bundle();
                args.putInt(
                    StoragePermissionDialog.RESULT_CODE_KEY,
                    REQUEST_WRITE_STORAGE
                );
                dialog.setArguments(args);
                dialog.show(
                    getSupportFragmentManager(), "StoragePermissionDialog"
                );
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_WRITE_STORAGE);
            }

            return;
        } else {
            hasWritePermissions = true;
        }

        startInitialActivityOrFinishLoading();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasWritePermissions = true;
                    startInitialActivityOrFinishLoading();
                }
        }
    }

    private void startInitialActivityOrFinishLoading() {
        if (!crosswordsFolder.exists()) {
            this.downloadTen();

            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/welcome.html"), this,
                    HTMLActivity.class);
            this.startActivity(i);

            return;
        } else if (prefs.getBoolean("release_" + BuildConfig.VERSION_NAME, true)) {
            prefs.edit()
                    .putBoolean("release_"+BuildConfig.VERSION_NAME, false)
                    .apply();

            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/release.html"), this,
                    HTMLActivity.class);
            this.startActivity(i);

            return;
        }

        render();
        this.checkDownload();
        puzzleList.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.currentAdapter == null) {
            this.render();
        } else {
            if (lastOpenedHandle != null) {
                try {
                    lastOpenedHandle.reloadMeta();

                    CircleProgressBar bar = (CircleProgressBar) lastOpenedView.findViewById(R.id.puzzle_progress);

                    if (lastOpenedHandle.isUpdatable()) {
                        bar.setPercentFilled(-1);
                    } else {
                        bar.setPercentFilled(lastOpenedHandle.getFilled());
                        bar.setComplete(lastOpenedHandle.getComplete() == 100);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }



        // A background update will commonly happen when the user turns on the preference for the
        // first time, so check here to ensure the UI is re-rendered when they exit the settings
        // dialog.
        if (utils.checkBackgroundDownload(prefs, hasWritePermissions)) {
            render();
        }

        this.checkDownload();
    }

    private SeparatedRecyclerViewAdapter<FileViewHolder> buildList(DirHandle directory, Accessor accessor) {
        long incept = System.currentTimeMillis();

        if (!directory.exists()) {
            showSDCardHelp();
            return new SeparatedRecyclerViewAdapter<FileViewHolder>(
                R.layout.puzzle_list_header,
                FileViewHolder.class
            );
        }

        String sourceMatch = null;

        if (this.sources != null) {
            sourceMatch = ((SourceListAdapter) sources.getAdapter()).current;

            if (SourceListAdapter.ALL_SOURCES.equals(sourceMatch)) {
                sourceMatch = null;
            }
        }

        FileHandle[] puzFiles = directory.getPuzFiles(sourceMatch);

        try {
            Arrays.sort(puzFiles, accessor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SeparatedRecyclerViewAdapter<FileViewHolder> adapter
            = new SeparatedRecyclerViewAdapter<>(
                R.layout.puzzle_list_header,
                FileViewHolder.class
            );
        String lastHeader = null;
        ArrayList<FileHandle> current = new ArrayList<FileHandle>();

        for (FileHandle handle : puzFiles) {
            String check = accessor.getLabel(handle);

            if (!((lastHeader == null) || lastHeader.equals(check))) {
                FileAdapter fa = new FileAdapter(current);
                adapter.addSection(lastHeader, fa);
                current = new ArrayList<FileHandle>();
            }

            lastHeader = check;
            current.add(handle);
        }

        if (lastHeader != null) {
            FileAdapter fa = new FileAdapter(current);
            adapter.addSection(lastHeader, fa);
            current = new ArrayList<FileHandle>();
        }

        return adapter;
    }

    private void checkDownload() {
        if (!hasWritePermissions) return;

        long lastDL = prefs.getLong("dlLast", 0);

        if (prefs.getBoolean("dlOnStartup", false) &&
                ((System.currentTimeMillis() - (long) (12 * 60 * 60 * 1000)) > lastDL)) {
            this.download(LocalDate.now(), null, true);
            prefs.edit()
                    .putLong("dlLast", System.currentTimeMillis())
                    .apply();
        }
    }

    private LocalDate getMaxAge(String preferenceValue) {
        int cleanupValue = Integer.parseInt(preferenceValue) + 1;
        if (cleanupValue > 0)
            return LocalDate.now().minus(Period.ofDays(cleanupValue));
        else
            return null;
    }

    private void cleanup() {
        boolean deleteOnCleanup = prefs.getBoolean("deleteOnCleanup", false);
        LocalDate maxAge = getMaxAge(prefs.getString("cleanupAge", "2"));
        LocalDate archiveMaxAge = getMaxAge(prefs.getString("archiveCleanupAge", "-1"));

        ArrayList<FileHandle> toArchive = new ArrayList<FileHandle>();
        ArrayList<FileHandle> toDelete = new ArrayList<FileHandle>();

        if (maxAge != null) {
            FileHandle[] puzFiles = crosswordsFolder.getPuzFiles();
            Arrays.sort(puzFiles);
            for (FileHandle h : puzFiles) {
                if ((h.getComplete() == 100) || (h.getDate().isBefore(maxAge))) {
                    if (deleteOnCleanup) {
                        toDelete.add(h);
                    } else {
                        toArchive.add(h);
                    }
                }
            }
        }

        if (archiveMaxAge != null) {
            FileHandle[] puzFiles = archiveFolder.getPuzFiles();
            Arrays.sort(puzFiles);
            for (FileHandle h : puzFiles) {
                if (h.getDate().isBefore(archiveMaxAge)) {
                    toDelete.add(h);
                }
            }
        }

        for (FileHandle h : toDelete) {
            h.delete();
        }

        for (FileHandle h : toArchive) {
            h.moveTo(this.archiveFolder);
        }

        render();
    }

    private void download(final LocalDate d, final List<Downloader> downloaders, final boolean scrape) {
        if (!hasWritePermissions) return;

        final Downloaders dls = new Downloaders(prefs, nm, this);
        LOGGER.info("Downloading from "+downloaders);
        new Thread(new Runnable() {
                public void run() {
                    dls.download(d, downloaders);

                    if (scrape) {
                        Scrapers scrapes = new Scrapers(prefs, nm, BrowseActivity.this);
                        scrapes.scrape();
                    }

                    handler.post(new Runnable() {
                            public void run() {
                                BrowseActivity.this.render();
                            }
                        });
                }
            }).start();
    }

    private void downloadTen() {
        if (!hasWritePermissions) return;

        new Thread(new Runnable() {
                public void run() {
                    Downloaders dls = new Downloaders(prefs, nm, BrowseActivity.this);
                    dls.supressMessages(true);

                    Scrapers scrapes = new Scrapers(prefs, nm, BrowseActivity.this);
                    scrapes.supressMessages(true);
                    scrapes.scrape();

                    LocalDate d = LocalDate.now();

                    for (int i = 0; i < 5; i++) {
                        d = d.minus(Period.ofDays(1));
                        dls.download(d);
                        handler.post(new Runnable() {
                                public void run() {
                                    BrowseActivity.this.render();
                                }
                            });
                    }
                }
            }).start();
    }

    private void render() {
        if (!hasWritePermissions) return;

        utils.clearBackgroundDownload(prefs);

        final DirHandle directory = viewArchive ? BrowseActivity.this.archiveFolder : BrowseActivity.this.crosswordsFolder;
        //Only spawn a thread if there are a lot of puzzles.
        // Using SDK rev as a proxy to decide whether you have a slow processor or not.

        if (((android.os.Build.VERSION.SDK_INT >= 5) && directory.exists() && (directory.numFiles() > 500)) ||
                ((android.os.Build.VERSION.SDK_INT < 5) && directory.exists() && (directory.numFiles() > 160))) {

            final View progressBar
                = BrowseActivity.this.findViewById( R.id.please_wait_notice);
            progressBar.setVisibility(View.VISIBLE);

            Runnable r = new Runnable() {
                public void run() {
                    currentAdapter = BrowseActivity.this.buildList(directory, BrowseActivity.this.accessor);
                    BrowseActivity.this.handler.post(new Runnable() {
                        public void run() {
                            BrowseActivity.this.puzzleList.setAdapter(currentAdapter);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            };

            new Thread(r).start();
        } else {
            this.puzzleList.setAdapter(currentAdapter = this.buildList(directory, accessor));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void upgradePreferences() {
        // do nothing now no keyboard
    }

    @Override
    public void onItemClick(final View v, int position) {
        if (!(v.getTag() instanceof FileHandle)) {
            return;
        }
        if (!selected.isEmpty()) {
            updateSelection(v);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lastOpenedView = v;
                    lastOpenedHandle = ((FileHandle) v.getTag());
                    if (lastOpenedHandle == null) {
                        return;
                    }
                    Intent i = new Intent(
                        Intent.ACTION_EDIT,
                        lastOpenedHandle.getUri(),
                        BrowseActivity.this,
                        PlayActivity.class
                    );
                    startActivity(i);
                }
            }, 450);
        }
    }

    @Override
    public void onItemLongClick(View v, int position) {
        if (!(v.getTag() instanceof FileHandle)) {
            return;
        }
        if (actionMode == null) {
            getSupportActionBar().startActionMode(actionModeCallback);
        }
        updateSelection(v);
    }

    private void updateSelection(View v) {
        Object oTag = v.getTag();
        if(oTag == null || !(oTag instanceof FileHandle)){
            return;
        }
        FileHandle tag = (FileHandle) oTag;
        if (selected.contains(tag)) {
            setListItemColor(v, false);
            selected.remove(tag);
        } else {
            setListItemColor(v, true);
            selected.add(tag);
        }
        if (selected.isEmpty()) {
            actionMode.finish();
        }
    }

    public static interface Provider<T> {
        T get();
    }

    private static ArrayList<FileHandle> toArrayList(FileHandle[] o){
        ArrayList<FileHandle> result = new ArrayList<>();
        result.addAll(Arrays.asList(o));
        return result;
    }

    private class FileAdapter extends RemovableRecyclerViewAdapter<FileViewHolder> {
        final DateTimeFormatter df
            = DateTimeFormatter.ofPattern("EEEE\n MMM dd, yyyy");
        final ArrayList<FileHandle> objects;

        public FileAdapter(ArrayList<FileHandle> objects) {
            this.objects = objects;
        }

        @Override
        public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.puzzle_list_item, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FileViewHolder holder, int position) {
            View view = holder.itemView;
            FileHandle handle = objects.get(position);
            view.setTag(handle);

            TextView date = (TextView) view.findViewById(R.id.puzzle_date);

            date.setText(df.format(handle.getDate()));

            if (accessor == Accessor.SOURCE) {
                date.setVisibility(View.VISIBLE);
            } else {
                date.setVisibility(View.GONE);
            }

            TextView title = (TextView) view.findViewById(R.id.puzzle_name);

            title.setText(handle.getTitle());

            CircleProgressBar bar = (CircleProgressBar) view.findViewById(R.id.puzzle_progress);

            bar.setPercentFilled(handle.getFilled());
            bar.setComplete(handle.getComplete() == 100);

            TextView caption = (TextView) view.findViewById(R.id.puzzle_caption);

            caption.setText(handle.getCaption());

            setListItemColor(view, selected.contains(handle));
        }

        @Override
        public int getItemCount() {
            return objects.size();
        }

        @Override
        public void remove(int position) {
            objects.remove(position);
        }
    }

    private class FileViewHolder extends RecyclerView.ViewHolder {
        public FileViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class DownloadDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            DownloadPickerDialogBuilder.OnDownloadSelectedListener downloadButtonListener = new DownloadPickerDialogBuilder.OnDownloadSelectedListener() {
                public void onDownloadSelected(
                    LocalDate d,
                    List<Downloader> downloaders,
                    int selected
                ) {
                    List<Downloader> toDownload
                        = new LinkedList<Downloader>();
                    boolean scrape;
                    LOGGER.info(
                        "Downloaders: " + selected + " of " + downloaders
                    );

                    if (selected == 0) {
                        // Download all available.
                        toDownload.addAll(downloaders);
                        toDownload.remove(0);
                        scrape = true;
                    } else {
                        // Only download selected.
                        toDownload.add(downloaders.get(selected));
                        scrape = false;
                    }

                    BrowseActivity activity = (BrowseActivity) getActivity();
                    activity.download(d, toDownload, scrape);
                }
            };

            LocalDate d = LocalDate.now();
            BrowseActivity activity = (BrowseActivity) getActivity();

            DownloadPickerDialogBuilder dpd
                = new DownloadPickerDialogBuilder(
                    activity,
                    downloadButtonListener,
                    d.getYear(),
                    d.getMonthValue(),
                    d.getDayOfMonth(),
                    new Downloaders(activity.prefs, activity.nm, activity)
            );

            return dpd.getInstance();
        }
    }
}
