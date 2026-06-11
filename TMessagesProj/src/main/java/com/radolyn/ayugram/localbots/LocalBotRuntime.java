package com.radolyn.ayugram.localbots;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class LocalBotRuntime implements NotificationCenter.NotificationCenterDelegate {
    private static final LocalBotRuntime INSTANCE = new LocalBotRuntime();
    private static final String VAR_PREFS = "chotkogram_local_bot_vars";
    private static final int MAX_DEPTH = 64;

    private static boolean inited;
    private final Set<String> processed = new HashSet<>();

    public static void init() {
        if (inited) {
            return;
        }
        inited = true;
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            NotificationCenter.getInstance(account).addObserver(INSTANCE, NotificationCenter.didReceiveNewMessages);
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
        for (MessageObject message : messages) {
            if (!canHandle(message)) {
                continue;
            }
            long dialogId = message.getDialogId();
            ArrayList<LocalBot> bots = LocalBotController.getSelectedBots(dialogId);
            if (!bots.isEmpty()) {
                logReceive(account, dialogId, message, bots.size());
            }
            for (LocalBot bot : bots) {
                if (bot.blocks == null || bot.blocks.isEmpty() || !markProcessed(account, bot, message)) {
                    continue;
                }
                executeBot(account, bot, message);
            }
        }
    }

    private void logReceive(int account, long dialogId, MessageObject message, int botsCount) {
        int currentTime = ConnectionsManager.getInstance(account).getCurrentTime();
        int messageDate = message.messageOwner != null ? message.messageOwner.date : currentTime;
        int delay = Math.max(0, currentTime - messageDate);
        FileLog.d("LocalBotRuntime receive dialog=" + dialogId + " messageId=" + message.getId() + " bots=" + botsCount + " deliveryDelaySec=" + delay);
    }

    private boolean canHandle(MessageObject message) {
        return message != null
                && message.messageOwner != null
                && !message.isOutOwner()
                && message.getDialogId() != 0;
    }

    private boolean markProcessed(int account, LocalBot bot, MessageObject message) {
        String key = account + ":" + bot.id + ":" + message.getDialogId() + ":" + message.getId();
        if (processed.contains(key)) {
            return false;
        }
        if (processed.size() > 1024) {
            processed.clear();
        }
        processed.add(key);
        return true;
    }

    private void executeBot(int account, LocalBot bot, MessageObject message) {
        LocalBot.Block entry = findEntryBlock(account, bot, message);
        if (entry == null || !eventMatches(account, entry, message)) {
            return;
        }
        executeBlock(account, bot, entry, message, 0);
    }

    private void executeBlock(int account, LocalBot bot, LocalBot.Block block, MessageObject message, int depth) {
        if (block == null || depth > MAX_DEPTH) {
            return;
        }
        if (isCondition(block) && !conditionMatches(account, bot, block, message)) {
            return;
        }
        if ("action_delay".equals(block.type)) {
            long delay = parseDelayMillis(block.param1);
            AndroidUtilities.runOnUIThread(() -> executeNext(account, bot, block, message, depth + 1), delay);
            return;
        }
        if (isAction(block)) {
            runAction(account, bot, block, message);
        }
        executeNext(account, bot, block, message, depth + 1);
    }

    private void executeNext(int account, LocalBot bot, LocalBot.Block block, MessageObject message, int depth) {
        if (!TextUtils.isEmpty(block.nextBlockId)) {
            executeBlock(account, bot, findBlock(bot, block.nextBlockId), message, depth);
        }
    }

    private LocalBot.Block findEntryBlock(int account, LocalBot bot, MessageObject message) {
        LocalBot.Block fallback = null;
        for (LocalBot.Block block : bot.blocks) {
            if (block == null || block.type == null || !block.type.startsWith("event_")) {
                continue;
            }
            if (fallback == null) {
                fallback = block;
            }
            if (eventMatches(account, block, message)) {
                return block;
            }
        }
        return fallback;
    }

    private LocalBot.Block findBlock(LocalBot bot, String blockId) {
        if (bot == null || bot.blocks == null || blockId == null) {
            return null;
        }
        for (LocalBot.Block block : bot.blocks) {
            if (block != null && blockId.equals(block.blockId)) {
                return block;
            }
        }
        return null;
    }

    private boolean eventMatches(int account, LocalBot.Block block, MessageObject message) {
        long dialogId = message.getDialogId();
        if ("event_private_message".equals(block.type)) {
            return dialogId > 0 && senderMatches(account, block.param2, message);
        } else if ("event_message".equals(block.type)) {
            return !message.messageOwner.post && senderMatches(account, block.param2, message);
        } else if ("event_post".equals(block.type)) {
            return dialogId < 0 && message.messageOwner.post;
        } else if ("event_command".equals(block.type)) {
            String command = normalizeCommand(block.param1);
            return !TextUtils.isEmpty(command) && getText(message).trim().startsWith(command);
        }
        return true;
    }

    private boolean conditionMatches(int account, LocalBot bot, LocalBot.Block block, MessageObject message) {
        if ("condition_text".equals(block.type)) {
            String text = getText(message);
            String needle = value(block.param1);
            if (TextUtils.isEmpty(needle)) {
                return true;
            }
            if ("ignore_case".equals(block.param2)) {
                return text.toLowerCase(Locale.US).contains(needle.toLowerCase(Locale.US));
            }
            return text.contains(needle);
        } else if ("condition_regex".equals(block.type)) {
            String pattern = value(block.param1);
            if (TextUtils.isEmpty(pattern)) {
                return true;
            }
            try {
                int flags = "i".equals(block.param2) ? Pattern.CASE_INSENSITIVE : 0;
                return Pattern.compile(pattern, flags).matcher(getText(message)).find();
            } catch (PatternSyntaxException e) {
                FileLog.e(e);
                return "pass".equals(block.param3);
            }
        } else if ("condition_sender".equals(block.type)) {
            boolean matched = senderMatches(account, block.param1, message);
            return "not".equals(block.param2) ? !matched : matched;
        } else if ("condition_admin".equals(block.type)) {
            return true;
        } else if ("var_check".equals(block.type)) {
            String current = getVar(bot, message.getDialogId(), block.param1, "chat");
            return compareValues(current, block.param2, resolveValue(block.param3, bot, message));
        }
        return true;
    }

    private boolean senderMatches(int account, String expected, MessageObject message) {
        String value = value(expected).trim();
        if (TextUtils.isEmpty(value) || "any".equals(value)) {
            return true;
        }
        long senderId = getSenderId(message);
        if (value.startsWith("@")) {
            TLRPC.User user = MessagesController.getInstance(account).getUser(senderId);
            return user != null && user.username != null && value.equalsIgnoreCase("@" + user.username);
        }
        try {
            return senderId == Long.parseLong(value);
        } catch (Exception ignore) {
            return false;
        }
    }

    private void runAction(int account, LocalBot bot, LocalBot.Block block, MessageObject message) {
        if ("action_reply".equals(block.type)) {
            sendReply(account, bot, block, message);
        } else if ("action_delete".equals(block.type)) {
            deleteMessage(account, block, message);
        } else if ("action_mute".equals(block.type)) {
            muteDialog(account, block, message);
        } else if ("action_pin".equals(block.type)) {
            pinMessage(account, block, message);
        } else if ("var_set".equals(block.type)) {
            setVar(bot, message, block.param1, block.param3, resolveValue(block.param2, bot, message));
        }
    }

    private void sendReply(int account, LocalBot bot, LocalBot.Block block, MessageObject message) {
        String text = resolveValue(block.param1, bot, message);
        if (TextUtils.isEmpty(text)) {
            return;
        }
        boolean withQuote = "quote".equals(block.param3);
        AndroidUtilities.runOnUIThread(() -> SendMessagesHelper.getInstance(account).sendMessage(
                SendMessagesHelper.SendMessageParams.of(text, message.getDialogId(), withQuote ? message : null, null, null, false, null, null, null, true, 0, 0, null, false)
        ));
    }

    private void deleteMessage(int account, LocalBot.Block block, MessageObject message) {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(message.getId());
        boolean forAll = "for_all".equals(block.param1);
        int topicId = (int) MessageObject.getTopicId(account, message.messageOwner, false);
        AndroidUtilities.runOnUIThread(() -> MessagesController.getInstance(account).deleteMessages(ids, null, null, message.getDialogId(), topicId, forAll, 0));
    }

    private void muteDialog(int account, LocalBot.Block block, MessageObject message) {
        int seconds = parseMuteSeconds(block.param1);
        int topicId = (int) MessageObject.getTopicId(account, message.messageOwner, false);
        AndroidUtilities.runOnUIThread(() -> NotificationsController.getInstance(account).muteUntil(message.getDialogId(), topicId, seconds));
    }

    private void pinMessage(int account, LocalBot.Block block, MessageObject message) {
        MessagesController controller = MessagesController.getInstance(account);
        long dialogId = message.getDialogId();
        TLRPC.Chat chat = dialogId < 0 ? controller.getChat(-dialogId) : null;
        TLRPC.User user = dialogId > 0 ? controller.getUser(dialogId) : null;
        boolean notify = "notify".equals(block.param1);
        AndroidUtilities.runOnUIThread(() -> controller.pinMessage(chat, user, message.getId(), false, false, notify));
    }

    private boolean isCondition(LocalBot.Block block) {
        return block.type != null && (block.type.startsWith("condition_") || "var_check".equals(block.type));
    }

    private boolean isAction(LocalBot.Block block) {
        return block.type != null && (block.type.startsWith("action_") || "var_set".equals(block.type));
    }

    private boolean compareValues(String left, String operator, String right) {
        if ("not_equals".equals(operator)) {
            return !TextUtils.equals(left, right);
        } else if ("contains".equals(operator)) {
            return value(left).contains(value(right));
        }
        return TextUtils.equals(left, right);
    }

    private String resolveValue(String raw, LocalBot bot, MessageObject message) {
        String result = value(raw);
        result = result.replace("{text}", getText(message));
        result = result.replace("{sender_id}", String.valueOf(getSenderId(message)));
        result = result.replace("{chat_id}", String.valueOf(message.getDialogId()));
        result = result.replace("{message_id}", String.valueOf(message.getId()));
        if (result.startsWith("{var:") && result.endsWith("}")) {
            String name = result.substring(5, result.length() - 1);
            return getVar(bot, message.getDialogId(), name, "chat");
        }
        return result;
    }

    private String getText(MessageObject message) {
        if (message.messageText != null) {
            return message.messageText.toString();
        }
        if (message.messageOwner != null && message.messageOwner.message != null) {
            return message.messageOwner.message;
        }
        return "";
    }

    private long getSenderId(MessageObject message) {
        if (message.messageOwner == null || message.messageOwner.from_id == null) {
            return 0;
        }
        if (message.messageOwner.from_id.user_id != 0) {
            return message.messageOwner.from_id.user_id;
        }
        if (message.messageOwner.from_id.chat_id != 0) {
            return -message.messageOwner.from_id.chat_id;
        }
        if (message.messageOwner.from_id.channel_id != 0) {
            return -message.messageOwner.from_id.channel_id;
        }
        return 0;
    }

    private String normalizeCommand(String command) {
        String result = value(command).trim();
        if (TextUtils.isEmpty(result)) {
            return "";
        }
        return result.startsWith("/") ? result : "/" + result;
    }

    private long parseDelayMillis(String raw) {
        String value = value(raw).toLowerCase(Locale.US);
        if ("1m".equals(value)) return 60_000L;
        if ("30s".equals(value)) return 30_000L;
        if ("10s".equals(value)) return 10_000L;
        if ("5s".equals(value)) return 5_000L;
        return 1_000L;
    }

    private int parseMuteSeconds(String raw) {
        String value = value(raw).toLowerCase(Locale.US);
        if ("forever".equals(value)) return Integer.MAX_VALUE;
        if ("1w".equals(value)) return 7 * 24 * 60 * 60;
        if ("1d".equals(value)) return 24 * 60 * 60;
        if ("8h".equals(value)) return 8 * 60 * 60;
        if ("10m".equals(value)) return 10 * 60;
        return 60 * 60;
    }

    private String getVar(LocalBot bot, long dialogId, String name, String scope) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }
        return vars().getString(varKey(bot, dialogId, name, scope), "");
    }

    private void setVar(LocalBot bot, MessageObject message, String name, String scope, String value) {
        if (TextUtils.isEmpty(name)) {
            return;
        }
        vars().edit().putString(varKey(bot, message.getDialogId(), name, scope), value == null ? "" : value).apply();
    }

    private SharedPreferences vars() {
        return ApplicationLoader.applicationContext.getSharedPreferences(VAR_PREFS, 0);
    }

    private String varKey(LocalBot bot, long dialogId, String name, String scope) {
        String actualScope = "global".equals(scope) ? "global" : String.valueOf(dialogId);
        return bot.id + ":" + actualScope + ":" + name.trim();
    }

    private String value(String text) {
        return text == null ? "" : text;
    }
}
