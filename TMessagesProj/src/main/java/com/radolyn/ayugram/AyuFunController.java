/*
 * This is the source code of ChotkoGram for Android.
 */

package com.radolyn.ayugram;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

public class AyuFunController implements NotificationCenter.NotificationCenterDelegate {
    private static final String COMMAND_PREFIX = "/x.";
    private static final String ASSET_ROOT = "fun/";
    private static final AyuFunController INSTANCE = new AyuFunController();

    private static boolean inited;
    private static Dialog currentDialog;
    private static MediaPlayer currentPlayer;

    public static void init() {
        if (inited) {
            return;
        }
        inited = true;

        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            NotificationCenter.getInstance(account).addObserver(INSTANCE, NotificationCenter.didReceiveNewMessages);
        }
    }

    public static boolean isFunCommand(CharSequence text) {
        return text != null && getCommandName(text.toString()) != null;
    }

    public static void playOutgoing(CharSequence text) {
        String command = text != null ? getCommandName(text.toString()) : null;
        if (command != null) {
            play(command);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id != NotificationCenter.didReceiveNewMessages || args.length < 3 || Boolean.TRUE.equals(args[2])) {
            return;
        }

        ArrayList<MessageObject> messages = (ArrayList<MessageObject>) args[1];
        if (messages == null || messages.isEmpty()) {
            return;
        }

        HashSet<String> playedCommands = new HashSet<>();
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            if (messageObject == null || messageObject.isOutOwner() || TextUtils.isEmpty(messageObject.messageText)) {
                continue;
            }

            String command = getCommandName(messageObject.messageText.toString());
            if (command != null && playedCommands.add(command)) {
                play(command);
            }
        }
    }

    private static String getCommandName(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith(COMMAND_PREFIX)) {
            return null;
        }

        int end = trimmed.length();
        for (int i = COMMAND_PREFIX.length(); i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isWhitespace(c)) {
                end = i;
                break;
            }
        }

        String command = trimmed.substring(COMMAND_PREFIX.length(), end).toLowerCase();
        if (command.isEmpty()) {
            return null;
        }
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if ((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '_' && c != '-') {
                return null;
            }
        }
        return command;
    }

    private static void play(String command) {
        AndroidUtilities.runOnUIThread(() -> {
            Activity activity = LaunchActivity.instance;
            if (activity == null || ApplicationLoader.mainInterfacePaused) {
                return;
            }

            String imageAsset = findImageAsset(command);
            if (imageAsset == null || !assetExists(assetPath(command, "sound.mp3"))) {
                return;
            }

            showOverlay(activity, imageAsset, assetPath(command, "sound.mp3"));
        });
    }

    private static void showOverlay(Activity activity, String imageAsset, String soundAsset) {
        dismissCurrent();

        Bitmap bitmap = loadBitmap(imageAsset);
        if (bitmap == null) {
            return;
        }

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(Color.BLACK);

        ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bitmap);
        root.addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        root.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> {
            stopPlayer();
            bitmap.recycle();
            if (currentDialog == dialog) {
                currentDialog = null;
            }
        });

        try {
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(0f);
            }
            currentDialog = dialog;
            playSound(soundAsset);
            AndroidUtilities.runOnUIThread(() -> {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }, 6000);
        } catch (Exception e) {
            FileLog.e(e);
            bitmap.recycle();
            stopPlayer();
        }
    }

    private static void playSound(String soundAsset) {
        stopPlayer();
        AssetFileDescriptor descriptor = null;
        MediaPlayer player = null;
        try {
            descriptor = ApplicationLoader.applicationContext.getAssets().openFd(soundAsset);
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            descriptor = null;
            player.setOnCompletionListener(completedPlayer -> {
                completedPlayer.release();
                if (currentPlayer == completedPlayer) {
                    currentPlayer = null;
                }
            });
            player.prepare();
            player.start();
            currentPlayer = player;
        } catch (Exception e) {
            FileLog.e(e);
            if (player != null) {
                player.release();
            }
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (IOException closeException) {
                    FileLog.e(closeException);
                }
            }
        }
    }

    private static void dismissCurrent() {
        if (currentDialog != null) {
            try {
                currentDialog.dismiss();
            } catch (Exception e) {
                FileLog.e(e);
            }
            currentDialog = null;
        }
        stopPlayer();
    }

    private static void stopPlayer() {
        if (currentPlayer == null) {
            return;
        }
        try {
            currentPlayer.stop();
        } catch (Exception ignored) {
        }
        currentPlayer.release();
        currentPlayer = null;
    }

    private static Bitmap loadBitmap(String assetPath) {
        try (InputStream stream = ApplicationLoader.applicationContext.getAssets().open(assetPath)) {
            return BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            FileLog.e(e);
        }
        return null;
    }

    private static String findImageAsset(String command) {
        String[] extensions = new String[]{"jpg", "jpeg", "png", "webp"};
        for (int i = 0; i < extensions.length; i++) {
            String path = assetPath(command, "image." + extensions[i]);
            if (assetExists(path)) {
                return path;
            }
        }
        return null;
    }

    private static boolean assetExists(String assetPath) {
        try (InputStream ignored = ApplicationLoader.applicationContext.getAssets().open(assetPath)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String assetPath(String command, String fileName) {
        return ASSET_ROOT + command + "/" + fileName;
    }
}
