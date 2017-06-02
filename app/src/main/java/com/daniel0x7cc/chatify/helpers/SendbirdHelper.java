package com.daniel0x7cc.chatify.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.daniel0x7cc.chatify.App;
import com.daniel0x7cc.chatify.R;
import com.daniel0x7cc.chatify.interfaces.SendBirdEventListener;
import com.daniel0x7cc.chatify.utils.Consts;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.daniel0x7cc.chatify.interfaces.OnSendbirdConnectedListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SendbirdHelper {

    private static SendbirdHelper instance;
    private FirebaseStorage mFirebaseStorage;
    String userPhoto;

    private SendbirdHelper() {
    }

    public static SendbirdHelper getInstance() {
        if (instance == null) {
            instance = new SendbirdHelper();
        }
        return instance;
    }

    public static void init(Context context) {
        SendBird.init(App.getStr(R.string.sendbird_app_id), context);
    }

    public void login(){
        SendBird.ConnectionState connectionState = SendBird.getConnectionState();

        if (connectionState == SendBird.ConnectionState.OPEN
                || connectionState == SendBird.ConnectionState.CONNECTING) {
            return;
        }

        final String userId = PreferenceManager.getInstance().getUserId();
        final String userName = PreferenceManager.getInstance().getUsername();

        SendBird.connect(userId, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                // Updates username
                SendBird.updateCurrentUserInfo(userName, "", new SendBird.UserInfoUpdateHandler() {
                    @Override
                    public void onUpdated(SendBirdException e) {
                        if (e != null) {
                            LogUtils.e(e.getMessage());
                        } else {
                            notifyOnConnectSucceeded();
                        }

                        // Registra o token do GCM
                        if (FirebaseInstanceId.getInstance().getToken() != null) {
                            LogUtils.i("SendBirdHelper.connect >> registerPushTokenForCurrentUser token: " + FirebaseInstanceId.getInstance().getToken());
                            SendBird.registerPushTokenForCurrentUser(FirebaseInstanceId.getInstance().getToken(),
                                    new SendBird.RegisterPushTokenWithStatusHandler() {
                                        @Override
                                        public void onRegistered(SendBird.PushTokenRegistrationStatus status, SendBirdException e) {
                                            LogUtils.i("SendBirdHelper.connect >> registerPushTokenForCurrentUser.onRegistered");
                                            if (e != null) {
                                                LogUtils.e("SendBirdHelper.connect >> Erro ao registrar push token do usuário. Erro " + e.getCode() + ": " + e.getMessage(), e);
                                            }
                                        }
                                    });
                        }
                    }
                });
            }
        });
    }

    public static void logout() {
        SendBird.unregisterPushTokenForCurrentUser(FirebaseInstanceId.getInstance().getToken(),
                new SendBird.UnregisterPushTokenHandler() {
                    @Override
                    public void onUnregistered(SendBirdException e) {
                        disconnect();
                    }
                });
    }

    public static void disconnect() {
        LogUtils.e("SendBirdHelper.disconnect");
        SendBird.disconnect(null);
    }

    public static String getOpponentId(GroupChannel channel) {
        if (channel == null || channel.getMemberCount() < 2 || SendBird.getCurrentUser() == null) {
            return "";
        }

        String opponentIdStr;
        if (channel.getMembers().get(0).getUserId().equals(PreferenceManager.getInstance().getUserId())) {
            opponentIdStr = channel.getMembers().get(1).getUserId();
        } else {
            opponentIdStr = channel.getMembers().get(0).getUserId();
        }

        try {
            return opponentIdStr;
        } catch (NumberFormatException | NullPointerException e) {
            LogUtils.e("Erro ao formatar ID do oponente no chat.", e);
        }

        return "";
    }

    public User.ConnectionStatus getOpponentStatus(GroupChannel channel) {
        if (channel == null || channel.getMemberCount() < 2 || SendBird.getCurrentUser() == null) {
            return User.ConnectionStatus.NON_AVAILABLE;
        }

        User.ConnectionStatus connectionStatus;

        if (channel.getMembers().get(0).getUserId().equals(PreferenceManager.getInstance().getUserId())) {
            connectionStatus = channel.getMembers().get(1).getConnectionStatus();
        } else {
            connectionStatus = channel.getMembers().get(0).getConnectionStatus();
        }

        return connectionStatus;
    }

    public long getOpponentLastSeenAt(GroupChannel channel) {
        if (channel == null || channel.getMemberCount() < 2 || SendBird.getCurrentUser() == null) {
            return 0;
        }

        long opponentLastSeen;

        if (channel.getMembers().get(0).getUserId().equals(PreferenceManager.getInstance().getUserId())) {
            opponentLastSeen = channel.getMembers().get(1).getLastSeenAt();
        } else {
            opponentLastSeen = channel.getMembers().get(0).getLastSeenAt();
        }

        return opponentLastSeen;
    }

    public String getAvatar(String userId){
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseStorage.getReference().child(Consts.USER_AVATAR_PATH).child(userId).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.e("photo", uri.toString());
                userPhoto = uri.toString();
            }
        });

        return userPhoto;
    }

    public static String getOpponentNickname(GroupChannel groupChannel) {
        if (groupChannel == null || groupChannel.getMemberCount() < 2) {
            return App.getStr(R.string.user);
        }

        List<User> members = groupChannel.getMembers();
        if (members.get(0).getUserId().equals(PreferenceManager.getInstance().getUserId())) {
            return members.get(1).getNickname();
        } else {
            return members.get(0).getNickname();
        }
    }

    private static final List<SendBirdEventListener> connectListener = new ArrayList<>();

    public static void addConnectListener(SendBirdEventListener listener) {
        synchronized (connectListener) {
            if (listener != null && !connectListener.contains(listener)) {
                connectListener.add(listener);
            }
        }
    }

    public static void removeConnectListener(SendBirdEventListener listener) {
        synchronized (connectListener) {
            if (listener != null && connectListener.contains(listener)) {
                connectListener.remove(listener);
            }
        }
    }

    private static void notifyOnConnectSucceeded() {
        synchronized (connectListener) {
            if (!connectListener.isEmpty()) {
                Iterator<SendBirdEventListener> it = connectListener.iterator();
                while (it.hasNext()) {
                    SendBirdEventListener listener = it.next();
                    listener.onConnectSucceeded();
                }
            }
        }
    }

    private static void notifyOnConnectFailed() {
        synchronized (connectListener) {
            if (!connectListener.isEmpty()) {
                Iterator<SendBirdEventListener> it = connectListener.iterator();
                while (it.hasNext()) {
                    SendBirdEventListener listener = it.next();
                    listener.onConnectFailed();
                }
            }
        }
    }

    private static void notifyAvatarsUrlLoaded() {
        synchronized (connectListener) {
            if (!connectListener.isEmpty()) {
                Iterator<SendBirdEventListener> it = connectListener.iterator();
                while (it.hasNext()) {
                    SendBirdEventListener listener = it.next();
                    listener.onAvatarsUrlLoaded();
                }
            }
        }
    }

    public static String getDisplayMemberNames(List<User> members, boolean condense) {
        if(condense) {
            if (members.size() < 2) {
                return "No Members";
            } else if (members.size() == 2) {
                StringBuffer names = new StringBuffer();
                for (User member : members) {
                    if (member.getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                        continue;
                    }

                    names.append(", " + member.getNickname());
                }
                return names.delete(0, 2).toString();
            } else {
                return "Group " + members.size();
            }
        } else {
            int count = 0;
            StringBuffer names = new StringBuffer();
            for (User member : members) {
                if (member.getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                    continue;
                }

                count++;
                names.append(", " + member.getNickname());

                if(count >= 10) {
                    break;
                }
            }
            return names.delete(0, 2).toString();
        }
    }

    private static class UrlDownloadAsyncTask extends AsyncTask<Void, Void, Object> {
        private static LRUCache cache = new LRUCache((int) (Runtime.getRuntime().maxMemory() / 16)); // 1/16th of the maximum memory.
        private final UrlDownloadAsyncTaskHandler handler;
        private String url;


        public static void download(String url, final File downloadFile, final Context context) {
            UrlDownloadAsyncTask task = new UrlDownloadAsyncTask(url, new UrlDownloadAsyncTaskHandler() {
                @Override
                public void onPreExecute() {
                    Toast.makeText(context, "Start downloading", Toast.LENGTH_SHORT).show();
                }

                @Override
                public Object doInBackground(File file) {
                    if (file == null) {
                        return null;
                    }

                    try {
                        BufferedInputStream in = null;
                        BufferedOutputStream out = null;

                        //create output directory if it doesn't exist
                        File dir = downloadFile.getParentFile();
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        in = new BufferedInputStream(new FileInputStream(file));
                        out = new BufferedOutputStream(new FileOutputStream(downloadFile));

                        byte[] buffer = new byte[1024 * 100];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        in.close();
                        out.flush();
                        out.close();

                        return downloadFile;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return null;
                }

                @Override
                public void onPostExecute(Object object, UrlDownloadAsyncTask task) {
                    if (object != null && object instanceof File) {
                        Toast.makeText(context, "Finish downloading: " + ((File) object).getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, "Error downloading", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                task.execute();
            }
        }

        public static void display(String url, final ImageView imageView, final boolean circle) {
            UrlDownloadAsyncTask task = null;

            if (imageView.getTag() != null && imageView.getTag() instanceof UrlDownloadAsyncTask) {
                try {
                    task = (UrlDownloadAsyncTask) imageView.getTag();
                    task.cancel(true);
                } catch (Exception e) {
                }

                imageView.setTag(null);
            }

            task = new UrlDownloadAsyncTask(url, new UrlDownloadAsyncTaskHandler() {
                @Override
                public void onPreExecute() {
                }

                @Override
                public Object doInBackground(File file) {
                    if (file == null) {
                        return null;
                    }

                    Bitmap bm = null;
                    try {
                        int targetHeight = 256;
                        int targetWidth = 256;

                        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
                        bin.mark(bin.available());

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(bin, null, options);

                        Boolean scaleByHeight = Math.abs(options.outHeight - targetHeight) >= Math.abs(options.outWidth - targetWidth);

                        if (options.outHeight * options.outWidth >= targetHeight * targetWidth) {
                            double sampleSize = scaleByHeight
                                    ? options.outHeight / targetHeight
                                    : options.outWidth / targetWidth;
                            options.inSampleSize = (int) Math.pow(2d, Math.floor(Math.log(sampleSize) / Math.log(2d)));
                        }

                        try {
                            bin.reset();
                        } catch (IOException e) {
                            bin = new BufferedInputStream(new FileInputStream(file));
                        }

                        // Do the actual decoding
                        options.inJustDecodeBounds = false;
                        bm = BitmapFactory.decodeStream(bin, null, options);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return bm;
                }

                @Override
                public void onPostExecute(Object object, UrlDownloadAsyncTask task) {
                    if (object != null && object instanceof Bitmap && imageView.getTag() == task) {
                        if (circle) {
                            imageView.setImageDrawable(new RoundedDrawable((Bitmap) object));
                        } else {
                            imageView.setImageBitmap((Bitmap) object);
                        }
                    } else {
                        //imageView.setImageResource(R.drawable.sendbird_img_placeholder);
                    }
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                task.execute();
            }

            imageView.setTag(task);
        }

        public UrlDownloadAsyncTask(String url, UrlDownloadAsyncTaskHandler handler) {
            this.handler = handler;
            this.url = url;
        }

        public interface UrlDownloadAsyncTaskHandler {
            public void onPreExecute();

            public Object doInBackground(File file);

            public void onPostExecute(Object object, UrlDownloadAsyncTask task);
        }

        @Override
        protected void onPreExecute() {
            if (handler != null) {
                handler.onPreExecute();
            }
        }

        protected Object doInBackground(Void... args) {
            File outFile = null;
            try {
                if (cache.get(url) != null && new File(cache.get(url)).exists()) { // Cache Hit
                    outFile = new File(cache.get(url));
                } else { // Cache Miss, Downloading a file from the url.
                    outFile = File.createTempFile("sendbird-download", ".tmp");
                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outFile));

                    InputStream input = new BufferedInputStream(new URL(url).openStream());
                    byte[] buf = new byte[1024 * 100];
                    int read = 0;
                    while ((read = input.read(buf, 0, buf.length)) >= 0) {
                        outputStream.write(buf, 0, read);
                    }

                    outputStream.flush();
                    outputStream.close();
                    cache.put(url, outFile.getAbsolutePath());
                }
            } catch (IOException e) {
                if (outFile != null) {
                    outFile.delete();
                }

                outFile = null;
            }


            if (handler != null) {
                return handler.doInBackground(outFile);
            }

            return outFile;
        }

        protected void onPostExecute(Object result) {
            if (handler != null) {
                handler.onPostExecute(result, this);
            }
        }

        private static class LRUCache {
            private final int maxSize;
            private int totalSize;
            private ConcurrentLinkedQueue<String> queue;
            private ConcurrentHashMap<String, String> map;

            public LRUCache(final int maxSize) {
                this.maxSize = maxSize;
                this.queue = new ConcurrentLinkedQueue<String>();
                this.map = new ConcurrentHashMap<String, String>();
            }

            public String get(final String key) {
                if (map.containsKey(key)) {
                    queue.remove(key);
                    queue.add(key);
                }

                return map.get(key);
            }

            public synchronized void put(final String key, final String value) {
                if (key == null || value == null) {
                    throw new NullPointerException();
                }

                if (map.containsKey(key)) {
                    queue.remove(key);
                }

                queue.add(key);
                map.put(key, value);
                totalSize = totalSize + getSize(value);

                while (totalSize >= maxSize) {
                    String expiredKey = queue.poll();
                    if (expiredKey != null) {
                        totalSize = totalSize - getSize(map.remove(expiredKey));
                    }
                }
            }

            private int getSize(String value) {
                return value.length();
            }
        }
    }
    public static void displayUrlImage(ImageView imageView, String url) {
        displayUrlImage(imageView, url, false);
    }

    public static void displayUrlImage(ImageView imageView, String url, boolean circle) {
        UrlDownloadAsyncTask.display(url, imageView, circle);
    }

    public static void downloadUrl(String url, String name, Context context) throws IOException {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File downloadFile = File.createTempFile("SendBird", name.substring(name.lastIndexOf(".")), downloadDir);
        UrlDownloadAsyncTask.download(url, downloadFile, context);
    }

    public static class RoundedDrawable extends Drawable {
        private final Bitmap mBitmap;
        private final Paint mPaint;
        private final RectF mRectF;
        private final int mBitmapWidth;
        private final int mBitmapHeight;

        public RoundedDrawable(Bitmap bitmap) {
            mBitmap = bitmap;
            mRectF = new RectF();
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            final BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);

            mBitmapWidth = mBitmap.getWidth();
            mBitmapHeight = mBitmap.getHeight();
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawOval(mRectF, mPaint);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);

            mRectF.set(bounds);
        }

        @Override
        public void setAlpha(int alpha) {
            if (mPaint.getAlpha() != alpha) {
                mPaint.setAlpha(alpha);
                invalidateSelf();
            }
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            mPaint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return mBitmapWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mBitmapHeight;
        }

        public void setAntiAlias(boolean aa) {
            mPaint.setAntiAlias(aa);
            invalidateSelf();
        }

        @Override
        public void setFilterBitmap(boolean filter) {
            mPaint.setFilterBitmap(filter);
            invalidateSelf();
        }

        @Override
        public void setDither(boolean dither) {
            mPaint.setDither(dither);
            invalidateSelf();
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }
    }

}
