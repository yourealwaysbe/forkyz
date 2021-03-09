package app.crossword.yourealwaysbe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree;
import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.service.BackgroundDownloadService;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

public class PreferencesFragment
       extends PreferenceFragmentCompat
       implements SharedPreferences.OnSharedPreferenceChangeListener {

    ActivityResultLauncher<Uri> getSAFURI = registerForActivityResult(
        new OpenDocumentTree(),
        new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri uri) {
                onNewExternalStorageSAFURI(uri);
            }
        }
    );

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference(ForkyzApplication.STORAGE_LOC_PREF)
            .setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                        Preference preference, Object newValue
                    ) {
                        return onStorageLocationChange(newValue);
                    }
                }
            );

        setExternalSAFURIDisplayValue();

        if (!AndroidVersionUtils.Factory.getInstance().isBackgroundDownloadAvaliable()) {
            Preference backgroundDownload = findPreference("backgroundDownload");
            backgroundDownload.setSelectable(false);
            backgroundDownload.setEnabled(false);
            backgroundDownload.setSummary("Requires Android Lollipop or later");
        }

        findPreference("releaseNotes")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/release.html"),
                            getActivity(), HTMLActivity.class);
                    getActivity().startActivity(i);

                    return true;
                }
            });

        findPreference("license")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/license.html"),
                            getActivity(), HTMLActivity.class);
                    getActivity().startActivity(i);

                    return true;
                }
            });
    }

    protected void onResumePreferences() {
        PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext())
                .registerOnSharedPreferenceChangeListener(this);

        setExternalSAFURIDisplayValue();

        super.onResume();
    }

    protected void onPausePreferences() {
        PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String pref) {
        if (pref.equals("backgroundDownload") ||
                pref.equals("backgroundDownloadRequireUnmetered") ||
                pref.equals("backgroundDownloadAllowRoaming") ||
                pref.equals("backgroundDownloadRequireCharging")) {
            Context context = getActivity().getApplicationContext();
            BackgroundDownloadService.updateJob(context);
        }
    }

    /**
     * Called when the use selects a storage location
     *
     * If they selected external storage (Storage Access Framework),
     * then they may be prompted to select a directory. This happens if
     * they have not already set the directory up, or they are
     * reselecting the same option, suggesting they want to change it.
     *
     * @return true if change should be committed to prefs
     */
    private boolean onStorageLocationChange(Object newValue) {
        SharedPreferences prefs
            = PreferenceManager .getDefaultSharedPreferences(
                getActivity().getApplicationContext()
            );

        String storageLocation
            = prefs.getString(ForkyzApplication.STORAGE_LOC_PREF, null);
        String storageLocationSAFURI
            = prefs.getString(ForkyzApplication.STORAGE_LOC_SAF_URI, null);

        boolean selectURI
            = newValue.equals(getString(R.string.external_storage_saf))
                && (newValue.equals(storageLocation)
                    || storageLocationSAFURI == null);

        if (selectURI) {
            getSAFURI.launch(null);
            return false;
        } else {
            return true;
        }
    }

    private void onNewExternalStorageSAFURI(Uri uri) {
        SharedPreferences prefs
            = PreferenceManager .getDefaultSharedPreferences(
                getActivity().getApplicationContext()
            );
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(
            ForkyzApplication.STORAGE_LOC_PREF,
            getString(R.string.external_storage_saf)
        );
        editor.putString(
            ForkyzApplication.STORAGE_LOC_SAF_URI,
            uri.toString()
        );
        editor.apply();

        setExternalSAFURIDisplayValue();
    }

    private void setExternalSAFURIDisplayValue() {
        SharedPreferences prefs
            = PreferenceManager .getDefaultSharedPreferences(
                getActivity().getApplicationContext()
            );

        String storageLocationSAFURI
            = prefs.getString(
                ForkyzApplication.STORAGE_LOC_SAF_URI,
                getString(R.string.external_storage_saf_none_selected)
            );

        ListPreference storageOptions
            = findPreference(ForkyzApplication.STORAGE_LOC_PREF);

        CharSequence[] entries = storageOptions.getEntries();
        int index
            = storageOptions.findIndexOfValue(getString(
                R.string.external_storage_saf
            ));

        entries[index] =
            getString(R.string.external_storage_saf) + " "
                + getString(
                    R.string.external_storage_saf_current_uri,
                    storageLocationSAFURI
                );

        storageOptions.setEntries(entries);
    }
}
