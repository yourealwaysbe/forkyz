package app.crossword.yourealwaysbe.net;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.DirHandle;

public class DownloadReceiver extends BroadcastReceiver {

    public static class Metas {
        private PuzzleMeta puzMeta;
        private DirHandle parentDir;

        public Metas(PuzzleMeta puzMeta, DirHandle parentDir) {
            this.puzMeta = puzMeta;
            this.parentDir = parentDir;
        }

        public PuzzleMeta getPuzMeta() { return puzMeta; }
        public DirHandle getParentDir() { return parentDir; }
    }

    public static HashMap<Uri, Metas> metas = new HashMap<>();


    private BroadcastReceiver impl;
    {
        if(android.os.Build.VERSION.SDK_INT >= 9){
            try{
                BroadcastReceiver built = (BroadcastReceiver) Class.forName("app.crossword.yourealwaysbe.net.DownloadReceiverGinger").newInstance();
                impl = built;
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        if(impl == null){
            impl = new DownloadReceiverNoop();
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        impl.onReceive(ctx, intent);
    }
}
