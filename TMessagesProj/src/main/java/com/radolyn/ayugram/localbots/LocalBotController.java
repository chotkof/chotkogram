package com.radolyn.ayugram.localbots;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LocalBotController {
    private static final String PREFS = "chotkogram_local_bots";
    private static final String BOTS_KEY = "bots";
    private static final String SELECTED_PREFIX = "selected_";
    private static final Gson gson = new Gson();
    private static ArrayList<LocalBot> botsCache;
    private static final HashMap<Long, ArrayList<LocalBot>> selectedBotsCache = new HashMap<>();

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
    }

    public static synchronized ArrayList<LocalBot> getBots() {
        if (botsCache != null) {
            return new ArrayList<>(botsCache);
        }
        ArrayList<LocalBot> bots = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(prefs().getString(BOTS_KEY, "[]"));
            if (!root.isJsonArray()) {
                return bots;
            }
            JsonArray array = root.getAsJsonArray();
            for (JsonElement botElement : array) {
                if (!botElement.isJsonObject()) {
                    continue;
                }
                LocalBot bot = readBot(botElement.getAsJsonObject());
                if (!TextUtils.isEmpty(bot.id)) {
                    normalizeBot(bot);
                    bots.add(bot);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        botsCache = new ArrayList<>(bots);
        return new ArrayList<>(bots);
    }

    private static LocalBot readBot(JsonObject object) {
        LocalBot bot = new LocalBot();
        bot.id = getString(object, "id", "");
        bot.name = getString(object, "name", "Локальный бот");
        bot.purpose = getString(object, "purpose", LocalBotPurpose.OTHER);
        bot.dialogId = getLong(object, "dialogId", 0);
        bot.enabled = getBoolean(object, "enabled", true);
        bot.blocks = new ArrayList<>();
        JsonElement blocksElement = object.get("blocks");
        if (blocksElement != null && blocksElement.isJsonArray()) {
            for (JsonElement blockElement : blocksElement.getAsJsonArray()) {
                if (!blockElement.isJsonObject()) {
                    continue;
                }
                JsonObject blockObject = blockElement.getAsJsonObject();
                LocalBot.Block block = new LocalBot.Block();
                block.blockId = getString(blockObject, "blockId", createBlockId());
                block.nextBlockId = getString(blockObject, "nextBlockId", null);
                block.type = normalizeType(getString(blockObject, "type", ""));
                block.value = getString(blockObject, "value", "");
                block.param1 = getString(blockObject, "param1", "");
                block.param2 = getString(blockObject, "param2", "");
                block.param3 = getString(blockObject, "param3", "");
                block.x = (int) getLong(blockObject, "x", 0);
                block.y = (int) getLong(blockObject, "y", 0);
                bot.blocks.add(block);
            }
        }
        return bot;
    }

    private static String getString(JsonObject object, String key, String defaultValue) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() ? value.getAsString() : defaultValue;
    }

    private static long getLong(JsonObject object, String key, long defaultValue) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() ? value.getAsLong() : defaultValue;
    }

    private static boolean getBoolean(JsonObject object, String key, boolean defaultValue) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() ? value.getAsBoolean() : defaultValue;
    }

    public static synchronized LocalBot getBot(String id) {
        for (LocalBot bot : getBots()) {
            if (bot.id != null && bot.id.equals(id)) {
                return bot;
            }
        }
        return null;
    }

    public static synchronized LocalBot createBot(long dialogId) {
        return createBot(dialogId, LocalBotPurpose.OTHER);
    }

    public static synchronized LocalBot createBot(long dialogId, String purpose) {
        ArrayList<LocalBot> bots = getBots();
        LocalBot bot = new LocalBot();
        bot.id = System.currentTimeMillis() + "_" + Math.abs(Utilities.random.nextInt());
        bot.purpose = isKnownPurpose(purpose) ? purpose : LocalBotPurpose.OTHER;
        bot.name = getPurposeTitle(bot.purpose) + " " + (bots.size() + 1);
        bot.dialogId = dialogId;
        bot.enabled = true;
        addStarterBlocks(bot);
        bots.add(bot);
        saveBots(bots);
        selectBot(dialogId, bot.id, true);
        return bot;
    }

    private static void addStarterBlocks(LocalBot bot) {
        LocalBot.Block event = new LocalBot.Block(createBlockId(), defaultEventType(bot.purpose), "Старт", 40, 40);
        event.param1 = "any";
        event.param2 = "any";
        event.param3 = "selected";

        LocalBot.Block reply = new LocalBot.Block(createBlockId(), "action_reply", "Ответ", 40, 144);
        reply.param1 = "Готово";
        reply.param2 = "plain";
        reply.param3 = "quote";
        event.nextBlockId = reply.blockId;

        bot.blocks.add(event);
        bot.blocks.add(reply);
    }

    public static synchronized void saveBot(LocalBot bot) {
        if (bot == null) {
            return;
        }
        normalizeBot(bot);
        ArrayList<LocalBot> bots = getBots();
        boolean replaced = false;
        for (int i = 0; i < bots.size(); i++) {
            if (bots.get(i).id != null && bots.get(i).id.equals(bot.id)) {
                bots.set(i, bot);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            bots.add(bot);
        }
        invalidateCache();
        saveBots(bots);
    }

    public static synchronized void deleteBot(String id) {
        ArrayList<LocalBot> bots = getBots();
        for (Iterator<LocalBot> iterator = bots.iterator(); iterator.hasNext();) {
            LocalBot bot = iterator.next();
            if (bot.id != null && bot.id.equals(id)) {
                iterator.remove();
            }
        }
        SharedPreferences.Editor editor = prefs().edit();
        for (String key : prefs().getAll().keySet()) {
            if (key.startsWith(SELECTED_PREFIX)) {
                Set<String> selected = prefs().getStringSet(key, new HashSet<>());
                if (selected.contains(id)) {
                    HashSet<String> updated = new HashSet<>(selected);
                    updated.remove(id);
                    editor.putStringSet(key, updated);
                }
            }
        }
        invalidateCache();
        editor.putString(BOTS_KEY, gson.toJson(bots)).apply();
    }

    private static void saveBots(ArrayList<LocalBot> bots) {
        invalidateCache();
        prefs().edit().putString(BOTS_KEY, gson.toJson(bots)).apply();
    }

    public static String createBlockId() {
        return "block_" + System.currentTimeMillis() + "_" + Math.abs(Utilities.random.nextInt());
    }

    public static String getPurposeTitle(String purpose) {
        if (LocalBotPurpose.PRIVATE_CHAT.equals(purpose)) {
            return "Личный чат";
        } else if (LocalBotPurpose.GROUP.equals(purpose)) {
            return "Группа";
        } else if (LocalBotPurpose.CHANNEL.equals(purpose)) {
            return "Канал";
        }
        return "Любой чат";
    }

    public static String defaultEventType(String purpose) {
        if (LocalBotPurpose.PRIVATE_CHAT.equals(purpose)) {
            return "event_private_message";
        } else if (LocalBotPurpose.CHANNEL.equals(purpose)) {
            return "event_post";
        }
        return "event_message";
    }

    public static synchronized boolean isSelected(long dialogId, String id) {
        return prefs().getStringSet(SELECTED_PREFIX + dialogId, new HashSet<>()).contains(id);
    }

    public static synchronized ArrayList<LocalBot> getSelectedBots(long dialogId) {
        ArrayList<LocalBot> cached = selectedBotsCache.get(dialogId);
        if (cached != null) {
            return new ArrayList<>(cached);
        }
        Set<String> selected = prefs().getStringSet(SELECTED_PREFIX + dialogId, new HashSet<>());
        ArrayList<LocalBot> result = new ArrayList<>();
        if (selected.isEmpty()) {
            selectedBotsCache.put(dialogId, result);
            return new ArrayList<>(result);
        }
        for (LocalBot bot : getBots()) {
            if (bot.enabled && bot.id != null && selected.contains(bot.id)) {
                result.add(bot);
            }
        }
        selectedBotsCache.put(dialogId, new ArrayList<>(result));
        return new ArrayList<>(result);
    }

    public static synchronized void selectBot(long dialogId, String id, boolean selected) {
        Set<String> current = prefs().getStringSet(SELECTED_PREFIX + dialogId, new HashSet<>());
        HashSet<String> updated = new HashSet<>(current);
        if (selected) {
            updated.add(id);
        } else {
            updated.remove(id);
        }
        selectedBotsCache.remove(dialogId);
        prefs().edit().putStringSet(SELECTED_PREFIX + dialogId, updated).apply();
    }

    public static synchronized void invalidateCache() {
        botsCache = null;
        selectedBotsCache.clear();
    }

    private static void normalizeBot(LocalBot bot) {
        if (TextUtils.isEmpty(bot.id)) {
            bot.id = System.currentTimeMillis() + "_" + Math.abs(Utilities.random.nextInt());
        }
        if (TextUtils.isEmpty(bot.name)) {
            bot.name = "Локальный бот";
        }
        if (!isKnownPurpose(bot.purpose)) {
            bot.purpose = LocalBotPurpose.OTHER;
        }
        if (bot.blocks == null) {
            bot.blocks = new ArrayList<>();
        }
        if (bot.blocks.isEmpty()) {
            addStarterBlocks(bot);
            return;
        }
        HashSet<String> ids = new HashSet<>();
        for (int i = 0; i < bot.blocks.size(); i++) {
            LocalBot.Block block = bot.blocks.get(i);
            if (block == null) {
                block = new LocalBot.Block();
                bot.blocks.set(i, block);
            }
            if (TextUtils.isEmpty(block.blockId) || ids.contains(block.blockId)) {
                block.blockId = createBlockId();
            }
            ids.add(block.blockId);
            block.type = normalizeType(block.type);
            if (!blockSupportsPurpose(block.type, bot.purpose)) {
                block.type = defaultEventType(bot.purpose);
            }
            normalizeBlockParams(block);
            if (TextUtils.isEmpty(block.value)) {
                block.value = blockTitle(block.type);
            }
            if (block.x == 0 && block.y == 0) {
                block.x = 40;
                block.y = 40 + i * 104;
            }
        }
        for (LocalBot.Block block : bot.blocks) {
            if (!TextUtils.isEmpty(block.nextBlockId) && !ids.contains(block.nextBlockId)) {
                block.nextBlockId = null;
            }
        }
    }

    private static void normalizeBlockParams(LocalBot.Block block) {
        if (block.param1 == null) block.param1 = "";
        if (block.param2 == null) block.param2 = "";
        if (block.param3 == null) block.param3 = "";

        if (block.type.startsWith("event_")) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "any";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "any";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "selected";
        } else if ("condition_text".equals(block.type)) {
            if (TextUtils.isEmpty(block.param2)) block.param2 = "ignore_case";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "text";
        } else if ("condition_regex".equals(block.type)) {
            if (TextUtils.isEmpty(block.param2)) block.param2 = "i";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "stop";
        } else if ("condition_sender".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "any";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "matches";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "stop";
        } else if ("condition_admin".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "admin";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "any";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "stop";
        } else if ("action_reply".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "Готово";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "plain";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "quote";
        } else if ("action_delete".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "for_all";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "now";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "no_log";
        } else if ("action_mute".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "1h";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "messages";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "spam";
        } else if ("action_pin".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "silent";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "trigger";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "none";
        } else if ("action_delay".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "1s";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "continue";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "normal";
        } else if ("var_set".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "last_text";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "{text}";
            if (TextUtils.isEmpty(block.param3)) block.param3 = "chat";
        } else if ("var_check".equals(block.type)) {
            if (TextUtils.isEmpty(block.param1)) block.param1 = "last_text";
            if (TextUtils.isEmpty(block.param2)) block.param2 = "equals";
        }
    }

    private static boolean isKnownPurpose(String purpose) {
        return LocalBotPurpose.PRIVATE_CHAT.equals(purpose)
                || LocalBotPurpose.GROUP.equals(purpose)
                || LocalBotPurpose.CHANNEL.equals(purpose)
                || LocalBotPurpose.OTHER.equals(purpose);
    }

    private static String normalizeType(String type) {
        if (TextUtils.isEmpty(type) || "reply".equals(type)) {
            return "action_reply";
        }
        return type;
    }

    private static boolean blockSupportsPurpose(String type, String purpose) {
        if ("event_private_message".equals(type)) {
            return LocalBotPurpose.PRIVATE_CHAT.equals(purpose);
        }
        if ("event_post".equals(type)) {
            return LocalBotPurpose.CHANNEL.equals(purpose);
        }
        if ("event_message".equals(type)) {
            return !LocalBotPurpose.PRIVATE_CHAT.equals(purpose) && !LocalBotPurpose.CHANNEL.equals(purpose);
        }
        if ("action_delete".equals(type) || "action_pin".equals(type) || "condition_admin".equals(type)) {
            return LocalBotPurpose.GROUP.equals(purpose) || LocalBotPurpose.CHANNEL.equals(purpose);
        }
        if ("action_mute".equals(type)) {
            return LocalBotPurpose.GROUP.equals(purpose) || LocalBotPurpose.OTHER.equals(purpose);
        }
        return true;
    }

    private static String blockTitle(String type) {
        if ("event_private_message".equals(type)) return "Сообщение в личке";
        if ("event_message".equals(type)) return "Сообщение в группе";
        if ("event_post".equals(type)) return "Пост в канале";
        if ("event_command".equals(type)) return "Команда";
        if ("condition_text".equals(type)) return "Текст содержит";
        if ("condition_regex".equals(type)) return "Regex";
        if ("condition_sender".equals(type)) return "Отправитель";
        if ("condition_admin".equals(type)) return "Админ";
        if ("action_reply".equals(type)) return "Ответить";
        if ("action_delete".equals(type)) return "Удалить";
        if ("action_mute".equals(type)) return "Замутить";
        if ("action_pin".equals(type)) return "Закрепить";
        if ("action_delay".equals(type)) return "Пауза";
        if ("var_set".equals(type)) return "Записать";
        if ("var_check".equals(type)) return "Проверить";
        return "Блок";
    }
}
