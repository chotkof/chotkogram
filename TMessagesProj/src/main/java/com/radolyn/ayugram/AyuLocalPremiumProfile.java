/*
 * This is the source code of ChotkoGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import android.text.TextUtils;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

public class AyuLocalPremiumProfile {
    private static final char MARKER = '\u2063';
    private static final String PREFIX = "" + MARKER + "CGPC|";
    private static final String SUFFIX = "" + MARKER;

    public static String stripMarker(String about) {
        int start = findStart(about);
        if (start < 0) {
            return about;
        }
        int end = about.indexOf(SUFFIX, start + PREFIX.length());
        if (end < 0) {
            return about;
        }
        String stripped = about.substring(0, start) + about.substring(end + SUFFIX.length());
        return stripped.trim();
    }

    public static void applyFromAbout(TLRPC.User user, TLRPC.UserFull userFull) {
        if (user == null || userFull == null || TextUtils.isEmpty(userFull.about)) {
            return;
        }
        String payload = getPayload(userFull.about);
        if (payload == null) {
            return;
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 4 && parts.length != 5) {
            return;
        }
        int nameColor = parseInt(parts[0], UserObject.getColorId(user));
        long nameEmoji = parseLong(parts[1], 0);
        int profileColor = parseInt(parts[2], -1);
        long profileEmoji = parseLong(parts[3], 0);
        long emojiStatus = parts.length >= 5 ? parseLong(parts[4], 0) : 0;

        if (user.color == null || !(user.color instanceof TLRPC.TL_peerColor)) {
            user.color = new TLRPC.TL_peerColor();
        }
        user.flags2 |= 256;
        user.color.flags |= 1;
        user.color.color = nameColor;
        if (nameEmoji != 0) {
            user.color.flags |= 2;
            user.color.background_emoji_id = nameEmoji;
        } else {
            user.color.flags &= ~2;
            user.color.background_emoji_id = 0;
        }

        if (user.profile_color == null || !(user.profile_color instanceof TLRPC.TL_peerColor)) {
            user.profile_color = new TLRPC.TL_peerColor();
        }
        user.flags2 |= 512;
        if (profileColor >= 0) {
            user.profile_color.flags |= 1;
            user.profile_color.color = profileColor;
        } else {
            user.profile_color.flags &= ~1;
        }
        if (profileEmoji != 0) {
            user.profile_color.flags |= 2;
            user.profile_color.background_emoji_id = profileEmoji;
        } else {
            user.profile_color.flags &= ~2;
            user.profile_color.background_emoji_id = 0;
        }
        if (!(user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible)) {
            if (emojiStatus != 0) {
                TLRPC.TL_emojiStatus status = new TLRPC.TL_emojiStatus();
                status.document_id = emojiStatus;
                user.emoji_status = status;
            } else if (parts.length >= 5) {
                user.emoji_status = new TLRPC.TL_emojiStatusEmpty();
            }
        }
    }

    public static String withMarker(String about, TLRPC.User user, int limit) {
        String cleanAbout = stripMarker(about == null ? "" : about).replace("\n", "");
        if (user == null) {
            return cleanAbout;
        }
        String marker = PREFIX +
                UserObject.getColorId(user) + "|" +
                UserObject.getEmojiId(user) + "|" +
                UserObject.getProfileColorId(user) + "|" +
                UserObject.getOnlyProfileEmojiId(user) + "|" +
                getRegularEmojiStatusId(user) +
                SUFFIX;
        int maxAboutLength = Math.max(0, limit - marker.length());
        if (cleanAbout.length() > maxAboutLength) {
            cleanAbout = cleanAbout.substring(0, maxAboutLength).trim();
        }
        return cleanAbout + marker;
    }

    public static void syncAboutMarker(int currentAccount, TLRPC.User user) {
        TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(user == null ? 0 : user.id);
        if (userFull == null) {
            return;
        }
        String newAbout = withMarker(userFull.about, user, MessagesController.getInstance(currentAccount).getAboutLimit());
        if (newAbout.equals(userFull.about)) {
            return;
        }
        updateProfileAbout(currentAccount, userFull, newAbout);
    }

    public static void removeAboutMarker(int currentAccount, TLRPC.User user) {
        TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(user == null ? 0 : user.id);
        if (userFull == null) {
            return;
        }
        String cleanAbout = stripMarker(userFull.about);
        if (cleanAbout == null || cleanAbout.equals(userFull.about)) {
            return;
        }
        updateProfileAbout(currentAccount, userFull, cleanAbout);
    }

    private static void updateProfileAbout(int currentAccount, TLRPC.UserFull userFull, String about) {
        TL_account.updateProfile req = new TL_account.updateProfile();
        req.about = about;
        req.flags |= 4;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
        userFull.about = stripMarker(about);
    }

    private static int findStart(String about) {
        return about == null ? -1 : about.indexOf(PREFIX);
    }

    private static String getPayload(String about) {
        int start = findStart(about);
        if (start < 0) {
            return null;
        }
        int payloadStart = start + PREFIX.length();
        int end = about.indexOf(SUFFIX, payloadStart);
        if (end < 0) {
            return null;
        }
        return about.substring(payloadStart, end);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long getRegularEmojiStatusId(TLRPC.User user) {
        if (user != null && user.emoji_status instanceof TLRPC.TL_emojiStatus) {
            return ((TLRPC.TL_emojiStatus) user.emoji_status).document_id;
        }
        return 0;
    }
}
