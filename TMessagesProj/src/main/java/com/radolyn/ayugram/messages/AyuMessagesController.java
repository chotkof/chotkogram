/*
 * This is the source code of ChotkoGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.messages;

import android.os.Environment;
import android.text.TextUtils;
import com.google.android.exoplayer2.util.Log;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.database.entities.DeletedMessageReaction;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.proprietary.AyuMessageUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AyuMessagesController {
    public static final String attachmentsSubfolder = "Saved Attachments";
    public static final File attachmentsPath = new File(
            new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AyuConstants.APP_NAME),
            attachmentsSubfolder
    );
    private static AyuMessagesController instance;
    private static final Object recentMessagesLock = new Object();
    private static final int MAX_RECENT_MESSAGES = 2000;
    private static final LinkedHashMap<String, AyuSavePreferences> recentMessages = new LinkedHashMap<>();
    private final EditedMessageDao editedMessageDao;
    private final DeletedMessageDao deletedMessageDao;

    private AyuMessagesController() {
        initializeAttachmentsFolder();

        editedMessageDao = AyuData.getEditedMessageDao();
        deletedMessageDao = AyuData.getDeletedMessageDao();
    }

    private static void initializeAttachmentsFolder() {
        if (!attachmentsPath.exists()) {
            attachmentsPath.mkdirs();
            try {
                new File(attachmentsPath, ".nomedia").createNewFile();
            } catch (IOException e) {
                // ignored, I hate java
            }
        }
    }

    public static AyuMessagesController getInstance() {
        if (instance == null) {
            instance = new AyuMessagesController();
        }
        return instance;
    }

    public static void rememberMessages(int accountId, long dialogId, ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            if (messageObject == null || messageObject.messageOwner == null) {
                continue;
            }
            long did = dialogId != 0 ? dialogId : messageObject.getDialogId();
            int topicId = (int) MessageObject.getTopicId(accountId, messageObject.messageOwner, false);
            rememberMessage(new AyuSavePreferences(messageObject.messageOwner, accountId, did, topicId, messageObject.getId(), (int) (System.currentTimeMillis() / 1000)));
        }
    }

    public static void rememberMessage(AyuSavePreferences prefs) {
        if (prefs == null || prefs.getMessage() == null || prefs.getDialogId() == 0 || prefs.getMessageId() == 0) {
            return;
        }
        synchronized (recentMessagesLock) {
            recentMessages.put(cacheKey(prefs.getAccountId(), prefs.getDialogId(), prefs.getMessageId()), prefs);
            while (recentMessages.size() > MAX_RECENT_MESSAGES) {
                Iterator<String> iterator = recentMessages.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                iterator.next();
                iterator.remove();
            }
        }
    }

    public Map<Long, ArrayList<Integer>> saveCachedDeletedMessages(int accountId, long dialogId, ArrayList<Integer> messageIds, int requestCatchTime) {
        Map<Long, ArrayList<Integer>> savedMessages = new LinkedHashMap<>();
        if (messageIds == null || messageIds.isEmpty()) {
            return savedMessages;
        }
        ArrayList<AyuSavePreferences> toSave = new ArrayList<>();
        synchronized (recentMessagesLock) {
            for (int i = 0; i < messageIds.size(); i++) {
                int messageId = messageIds.get(i);
                if (dialogId != 0) {
                    AyuSavePreferences prefs = recentMessages.get(cacheKey(accountId, dialogId, messageId));
                    if (prefs != null) {
                        toSave.add(prefs);
                    }
                } else {
                    for (Map.Entry<String, AyuSavePreferences> entry : recentMessages.entrySet()) {
                        AyuSavePreferences prefs = entry.getValue();
                        if (prefs.getAccountId() == accountId && prefs.getMessageId() == messageId) {
                            toSave.add(prefs);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < toSave.size(); i++) {
            AyuSavePreferences prefs = toSave.get(i);
            AyuSavePreferences freshPrefs = new AyuSavePreferences(prefs.getMessage(), accountId, prefs.getDialogId(), prefs.getTopicId(), prefs.getMessageId(), requestCatchTime);
            onMessageDeleted(freshPrefs);
            ArrayList<Integer> ids = savedMessages.get(freshPrefs.getDialogId());
            if (ids == null) {
                ids = new ArrayList<>();
                savedMessages.put(freshPrefs.getDialogId(), ids);
            }
            if (!ids.contains(freshPrefs.getMessageId())) {
                ids.add(freshPrefs.getMessageId());
            }
        }
        return savedMessages;
    }

    private static String cacheKey(int accountId, long dialogId, int messageId) {
        return accountId + ":" + dialogId + ":" + messageId;
    }

    public void onMessageEdited(AyuSavePreferences prefs, TLRPC.Message newMessage) {
        try {
            onMessageEditedInner(prefs, newMessage, false);
        } catch (Exception e) {
            Log.e("ChotkoGram", "error onMessageEdited", e);
            FileLog.e("onMessageEdited", e);
        }
    }

    public void onMessageEditedForce(AyuSavePreferences prefs) {
        try {
            onMessageEditedInner(prefs, prefs.getMessage(), true);
        } catch (Exception e) {
            Log.e("ChotkoGram", "error onMessageEditedForce", e);
            FileLog.e("onMessageEditedForce", e);
        }
    }

    private void onMessageEditedInner(AyuSavePreferences prefs, TLRPC.Message newMessage, boolean force) {
        if (!AyuConfig.saveEditedMessageFor(prefs.getAccountId(), prefs.getDialogId())) {
            return;
        }

        var oldMessage = prefs.getMessage();

        boolean sameMedia = oldMessage.media == newMessage.media ||
                (oldMessage.media != null && newMessage.media != null && oldMessage.media.getClass() == newMessage.media.getClass());
        if (oldMessage.media instanceof TLRPC.TL_messageMediaPhoto && newMessage.media instanceof TLRPC.TL_messageMediaPhoto && oldMessage.media.photo != null && newMessage.media.photo != null) {
            sameMedia = oldMessage.media.photo.id == newMessage.media.photo.id;
        } else if (oldMessage.media instanceof TLRPC.TL_messageMediaDocument && newMessage.media instanceof TLRPC.TL_messageMediaDocument && oldMessage.media.document != null && newMessage.media.document != null) {
            sameMedia = oldMessage.media.document.id == newMessage.media.document.id;
        }

        if (force) {
            sameMedia = false;
        }

        if (sameMedia && TextUtils.equals(oldMessage.message, newMessage.message)) {
            return;
        }

        var revision = new EditedMessage();
        AyuMessageUtils.map(prefs, revision);
        AyuMessageUtils.mapMedia(prefs, revision, !sameMedia);

        if (!sameMedia && !TextUtils.isEmpty(revision.mediaPath)) {
            var lastRevision = editedMessageDao.getLastRevision(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId());

            if (lastRevision != null && !TextUtils.equals(revision.mediaPath, lastRevision.mediaPath) && lastRevision.mediaPath != null && !lastRevision.mediaPath.contains(attachmentsSubfolder)) {
                // update previous revisions to reflect media change
                // like, there's no previous file, so replace it with one we copied before...
                editedMessageDao.updateAttachmentForRevisionsBetweenDates(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId(), lastRevision.mediaPath, revision.mediaPath);
            }
        }

        editedMessageDao.insert(revision);

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(prefs.getAccountId()).postNotificationName(AyuConstants.MESSAGE_EDITED_NOTIFICATION, prefs.getDialogId(), prefs.getMessageId());
        });
    }

    public void onMessageDeleted(AyuSavePreferences prefs) {
        if (prefs.getMessage() == null) {
            Log.w("ChotkoGram", "null msg ?");
            return;
        }

        try {
            onMessageDeletedInner(prefs);
        } catch (Exception e) {
            Log.e("ChotkoGram", "error onMessageDeleted", e);
            FileLog.e("onMessageDeleted", e);
        }
    }

    private void onMessageDeletedInner(AyuSavePreferences prefs) {
        if (!AyuConfig.saveDeletedMessageFor(prefs.getAccountId(), prefs.getDialogId())) {
            return;
        }

        if (deletedMessageDao.exists(prefs.getUserId(), prefs.getDialogId(), prefs.getTopicId(), prefs.getMessageId())) {
            return;
        }

        var deletedMessage = new DeletedMessage();
        deletedMessage.userId = prefs.getUserId();
        deletedMessage.dialogId = prefs.getDialogId();
        deletedMessage.messageId = prefs.getMessageId();
        deletedMessage.entityCreateDate = prefs.getRequestCatchTime();

        var msg = prefs.getMessage();

        Log.d("ChotkoGram", "saving message " + prefs.getMessageId() + " for " + prefs.getDialogId() + " with topic " + prefs.getTopicId());

        AyuMessageUtils.map(prefs, deletedMessage);
        AyuMessageUtils.mapMedia(prefs, deletedMessage, true);

        var fakeMsgId = deletedMessageDao.insert(deletedMessage);

        if (msg != null && msg.reactions != null && AyuConfig.saveReactions) {
            processDeletedReactions(fakeMsgId, msg.reactions);
        }
    }

    private void processDeletedReactions(long fakeMessageId, TLRPC.TL_messageReactions reactions) {
        for (var reaction : reactions.results) {
            if (reaction.reaction instanceof TLRPC.TL_reactionEmpty) {
                continue;
            }

            var deletedReaction = new DeletedMessageReaction();
            deletedReaction.deletedMessageId = fakeMessageId;
            deletedReaction.count = reaction.count;
            deletedReaction.selfSelected = reaction.chosen;

            if (reaction.reaction instanceof TLRPC.TL_reactionEmoji) {
                deletedReaction.emoticon = ((TLRPC.TL_reactionEmoji) reaction.reaction).emoticon;
            } else if (reaction.reaction instanceof TLRPC.TL_reactionCustomEmoji) {
                deletedReaction.documentId = ((TLRPC.TL_reactionCustomEmoji) reaction.reaction).document_id;
                deletedReaction.isCustom = true;
            } else {
                Log.e("ChotkoGram", "fake news emoji");
                continue;
            }

            deletedMessageDao.insertReaction(deletedReaction);
        }
    }

    public boolean hasAnyRevisions(long userId, long dialogId, int messageId) {
        return editedMessageDao.hasAnyRevisions(userId, dialogId, messageId);
    }

    public List<EditedMessage> getRevisions(long userId, long dialogId, int messageId) {
        return editedMessageDao.getAllRevisions(userId, dialogId, messageId);
    }

    public DeletedMessageFull getMessage(long userId, long dialogId, int messageId) {
        return deletedMessageDao.getMessage(userId, dialogId, messageId);
    }

    public List<DeletedMessageFull> getMessages(long userId, long dialogId, long topicId, int startId, int endId, int limit) {
        return deletedMessageDao.getMessages(userId, dialogId, topicId, startId, endId, limit);
    }

    public List<DeletedMessageFull> getMessagesGrouped(long userId, long dialogId, long groupedId) {
        return deletedMessageDao.getMessagesGrouped(userId, dialogId, groupedId);
    }

    public void delete(long userId, long dialogId, int messageId) {
        var msg = getMessage(userId, dialogId, messageId);
        if (msg == null) {
            return;
        }

        deletedMessageDao.delete(userId, dialogId, messageId);

        if (!TextUtils.isEmpty(msg.message.mediaPath)) {
            var p = new File(msg.message.mediaPath);
            if (p.exists()) {
                try {
                    p.delete();
                } catch (Exception e) {
                    Log.e("ChotkoGram", "failed to delete file " + msg.message.mediaPath, e);
                }
            }
        }
    }

    public void clean() {
        AyuData.clean();
        AyuData.create();

        // force to recreate a database to avoid crash
        instance = null;
    }
}
