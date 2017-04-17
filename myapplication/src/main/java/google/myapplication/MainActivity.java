package google.myapplication;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private File         mCurrentDir;
    private int          mMaxCount;
    private List<String> imgs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver resolver = getContentResolver();
                String selection = MediaStore.Images.Media.MIME_TYPE + "=? or" +
                        MediaStore.Images.Media.MIME_TYPE + "=?";
                String[] args = new String[]{"image/jpeg", "image/png"};
                String sortOrder = MediaStore.Images.Media.DATE_MODIFIED;
                Cursor cursor = resolver.query(uri, null, selection, args, sortOrder);


                Set<String> mDirpaths = new HashSet<String>();
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images
                            .Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) {
                        continue;
                    }
                    String dirpath = parentFile.getAbsolutePath();
                    FolderBean bean = null;

                    if (mDirpaths.contains(dirpath)) {
                        continue;
                    } else {
                        mDirpaths.add(dirpath);
                        bean = new FolderBean();
                        bean.setDirPath(dirpath);

                    }

                    int picsize = parentFile.list().length;
                    if (picsize > mMaxCount) {
                        mMaxCount = picsize;
                        mCurrentDir = parentFile;
                    }

                }


            }
        }) {
        }.start();

    }

}
