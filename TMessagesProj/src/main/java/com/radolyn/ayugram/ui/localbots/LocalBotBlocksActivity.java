package com.radolyn.ayugram.ui.localbots;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.radolyn.ayugram.localbots.LocalBot;
import com.radolyn.ayugram.localbots.LocalBotController;
import com.radolyn.ayugram.localbots.LocalBotPurpose;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

import java.util.ArrayList;
import java.util.HashSet;

public class LocalBotBlocksActivity extends BaseFragment {
    private static final String ANY = "any";
    private static final String MANUAL = "__manual__";

    private final String botId;
    private final long dialogId;
    private LocalBot bot;
    private LinearLayout contentView;

    private static final BlockTemplate[] templates = new BlockTemplate[]{
            t("event_private_message", "Событие", "Сообщение в личке", "Запускает сценарий при новом входящем сообщении в личном чате.", 0xfff59e0b, new String[]{LocalBotPurpose.PRIVATE_CHAT},
                    f("Тип сообщения", o("Любой текст", "any"), o("Только с текстом", "text"), o("Команда", "command")),
                    f("От кого", o("Любой пользователь", "any"), o("Точный ID или @username", MANUAL)),
                    f("Запуск", o("Только если бот выбран для чата", "selected"))),
            t("event_message", "Событие", "Сообщение в группе", "Запускает сценарий при новом входящем сообщении в группе.", 0xfff59e0b, new String[]{LocalBotPurpose.GROUP, LocalBotPurpose.OTHER},
                    f("Тип сообщения", o("Любой текст", "any"), o("Только с текстом", "text"), o("Команда", "command")),
                    f("От кого", o("Любой участник", "any"), o("Точный ID или @username", MANUAL)),
                    f("Запуск", o("Только если бот выбран для чата", "selected"))),
            t("event_post", "Событие", "Пост в канале", "Запускает сценарий при новом посте канала.", 0xfff59e0b, new String[]{LocalBotPurpose.CHANNEL},
                    f("Тип поста", o("Любой пост", "any"), o("Только текст", "text")),
                    f("Источник", o("Этот канал", "any")),
                    f("Запуск", o("Только если бот выбран для чата", "selected"))),
            t("event_command", "Событие", "Команда", "Запускает сценарий, когда сообщение начинается с команды.", 0xfff59e0b, new String[]{ANY},
                    f("Команда", o("/start", "/start"), o("/help", "/help"), o("/rules", "/rules"), o("Другая команда", MANUAL)),
                    f("Аргументы", o("Не проверять", "any")),
                    f("Кто может", o("Любой", "any"))),

            t("condition_text", "Условие", "Текст содержит", "Продолжает сценарий, если текст сообщения содержит фразу.", 0xff2563eb, new String[]{ANY},
                    f("Фраза", o("спам", "спам"), o("реклама", "реклама"), o("ссылка", "http"), o("Другая фраза", MANUAL)),
                    f("Регистр", o("Без учета регистра", "ignore_case"), o("Строго как написано", "exact")),
                    f("Где искать", o("Текст сообщения", "text"))),
            t("condition_regex", "Условие", "Regex совпал", "Продолжает сценарий, если регулярное выражение совпало с текстом.", 0xff2563eb, new String[]{ANY},
                    f("Шаблон", o("Ссылка", "https?://\\S+"), o("Шесть цифр", "\\d{6,}"), o("Другой шаблон", MANUAL)),
                    f("Флаги", o("Без учета регистра", "i"), o("Без флагов", "none")),
                    f("Если ошибка", o("Остановить сценарий", "stop"), o("Пропустить блок", "pass"))),
            t("condition_sender", "Условие", "Отправитель", "Проверяет отправителя по ID или @username.", 0xff2563eb, new String[]{ANY},
                    f("Отправитель", o("Любой", "any"), o("ID или @username", MANUAL)),
                    f("Проверка", o("Совпадает", "matches"), o("Не совпадает", "not")),
                    f("Если не найден", o("Остановить", "stop"))),
            t("condition_admin", "Условие", "Админ", "Зарезервировано под проверку прав администратора.", 0xff2563eb, new String[]{LocalBotPurpose.GROUP, LocalBotPurpose.CHANNEL},
                    f("Роль", o("Админ", "admin"), o("Владелец", "owner")),
                    f("Права", o("Любые", "any")),
                    f("Если неизвестно", o("Продолжить", "pass"))),

            t("action_reply", "Действие", "Ответить", "Отправляет сообщение в текущий чат.", 0xff16a34a, new String[]{ANY},
                    f("Текст", o("Готово", "Готово"), o("Привет", "Привет"), o("Текст исходного сообщения", "{text}"), o("Другой текст", MANUAL)),
                    f("Формат", o("Обычный текст", "plain")),
                    f("Ответ", o("С цитатой", "quote"), o("Без цитаты", "no_quote"))),
            t("action_delete", "Действие", "Удалить сообщение", "Удаляет сообщение, которое запустило сценарий.", 0xffdc2626, new String[]{LocalBotPurpose.GROUP, LocalBotPurpose.CHANNEL},
                    f("Удалить", o("Для всех", "for_all"), o("Только локально", "local")),
                    f("Когда", o("Сразу", "now")),
                    f("Лог", o("Без лога", "no_log"))),
            t("action_mute", "Действие", "Замутить чат", "Выключает уведомления в текущем чате на выбранное время.", 0xff7c3aed, new String[]{LocalBotPurpose.GROUP, LocalBotPurpose.OTHER},
                    f("Время", o("10 минут", "10m"), o("1 час", "1h"), o("8 часов", "8h"), o("1 день", "1d"), o("Навсегда", "forever")),
                    f("Что мутить", o("Текущий чат", "chat")),
                    f("Причина", o("spam", "spam"), o("Другая", MANUAL))),
            t("action_pin", "Действие", "Закрепить", "Закрепляет сообщение, которое запустило сценарий.", 0xff0891b2, new String[]{LocalBotPurpose.GROUP, LocalBotPurpose.CHANNEL},
                    f("Уведомление", o("Тихо", "silent"), o("С уведомлением", "notify")),
                    f("Что закрепить", o("Исходное сообщение", "trigger")),
                    f("Срок", o("Без срока", "none"))),
            t("action_delay", "Действие", "Пауза", "Ждет перед следующим блоком.", 0xff64748b, new String[]{ANY},
                    f("Время", o("1 секунда", "1s"), o("5 секунд", "5s"), o("10 секунд", "10s"), o("30 секунд", "30s"), o("1 минута", "1m")),
                    f("Если чат закрыт", o("Продолжить", "continue")),
                    f("Точность", o("Обычная", "normal"))),

            t("var_set", "Переменные", "Записать значение", "Сохраняет значение для следующих сообщений.", 0xff9333ea, new String[]{ANY},
                    f("Переменная", o("last_text", "last_text"), o("last_user", "last_user"), o("Другое имя", MANUAL)),
                    f("Значение", o("Текст сообщения", "{text}"), o("ID отправителя", "{sender_id}"), o("ID чата", "{chat_id}"), o("Другое", MANUAL)),
                    f("Где хранить", o("Этот чат", "chat"), o("Глобально для бота", "global"))),
            t("var_check", "Переменные", "Проверить значение", "Сравнивает сохраненную переменную со значением.", 0xff9333ea, new String[]{ANY},
                    f("Переменная", o("last_text", "last_text"), o("last_user", "last_user"), o("Другое имя", MANUAL)),
                    f("Оператор", o("Равно", "equals"), o("Не равно", "not_equals"), o("Содержит", "contains")),
                    f("Значение", o("Текст сообщения", "{text}"), o("Пусто", ""), o("Другое", MANUAL)))
    };

    public LocalBotBlocksActivity(String botId, long dialogId) {
        this.botId = botId;
        this.dialogId = dialogId;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        bot = LocalBotController.getBot(botId);
        if (bot == null) {
            bot = LocalBotController.createBot(dialogId, LocalBotPurpose.OTHER);
        }
        relinkBlocks(orderedBlocks());
        LocalBotController.saveBot(bot);
        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(bot.name);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        ScrollView scrollView = new ScrollView(context);
        contentView = new LinearLayout(context);
        contentView.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(contentView, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        frameLayout.addView(scrollView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        buildEditor(context);
        return fragmentView;
    }

    private void buildEditor(Context context) {
        if (context == null || contentView == null) {
            return;
        }
        contentView.removeAllViews();

        TextCell nameCell = new TextCell(context);
        nameCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        nameCell.setTextAndValue("Название", bot.name, true);
        nameCell.setOnClickListener(v -> showEditBotNameDialog());
        contentView.addView(nameCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));

        TextCell enabledCell = new TextCell(context);
        enabledCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        enabledCell.setTextAndValue("Работа бота", bot.enabled ? "включен" : "выключен", true);
        enabledCell.setOnClickListener(v -> {
            bot.enabled = !bot.enabled;
            saveAndRebuild();
        });
        contentView.addView(enabledCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));

        TextCell selectedCell = new TextCell(context);
        selectedCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        boolean selected = LocalBotController.isSelected(dialogId, bot.id);
        selectedCell.setTextAndValue("Этот чат", selected ? "бот выбран" : "бот не выбран", true);
        selectedCell.setOnClickListener(v -> {
            LocalBotController.selectBot(dialogId, bot.id, !selected);
            buildEditor(getParentActivity());
        });
        contentView.addView(selectedCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));

        TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(context);
        infoCell.setText("Сценарий выполняется сверху вниз. Если условие не прошло, выполнение останавливается.");
        contentView.addView(infoCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ArrayList<LocalBot.Block> ordered = orderedBlocks();
        for (int i = 0; i < ordered.size(); i++) {
            LocalBot.Block block = ordered.get(i);
            BlockTemplate template = getTemplate(block.type);
            TextCell blockCell = new TextCell(context);
            blockCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            blockCell.setTextAndValue((i + 1) + ". " + template.title, summarize(template, block), true);
            int index = i;
            blockCell.setOnClickListener(v -> showBlockMenu(index));
            contentView.addView(blockCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(64)));
        }

        TextInfoPrivacyCell addHeader = new TextInfoPrivacyCell(context);
        addHeader.setText("Добавление блока");
        contentView.addView(addHeader, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextCell addCell = new TextCell(context);
        addCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        addCell.setTextAndIcon("Добавить блок", R.drawable.msg_add, true);
        addCell.setOnClickListener(v -> showAddBlockDialog());
        contentView.addView(addCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));
    }

    private void showBlockMenu(int index) {
        ArrayList<LocalBot.Block> ordered = orderedBlocks();
        if (index < 0 || index >= ordered.size()) {
            return;
        }
        LocalBot.Block block = ordered.get(index);
        BlockTemplate template = getTemplate(block.type);
        CharSequence[] items = new CharSequence[]{"Настроить", "Поднять выше", "Опустить ниже", "Дублировать", "Удалить"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(template.category + ": " + template.title);
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                showBlockSettings(block);
            } else if (which == 1 && index > 0) {
                ordered.remove(index);
                ordered.add(index - 1, block);
                relinkAndSave(ordered);
            } else if (which == 2 && index < ordered.size() - 1) {
                ordered.remove(index);
                ordered.add(index + 1, block);
                relinkAndSave(ordered);
            } else if (which == 3) {
                LocalBot.Block copy = copyBlock(block);
                ordered.add(index + 1, copy);
                relinkAndSave(ordered);
            } else if (which == 4) {
                if (block.type != null && block.type.startsWith("event_") && countEvents(ordered) <= 1) {
                    showInfo("Нужен хотя бы один стартовый блок.");
                    return;
                }
                ordered.remove(index);
                relinkAndSave(ordered);
            }
        });
        showDialog(builder.create());
    }

    private void showBlockSettings(LocalBot.Block block) {
        BlockTemplate template = getTemplate(block.type);
        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(8));

        TextView description = new TextView(getParentActivity());
        description.setText(template.description);
        description.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        description.setTextSize(14);
        description.setGravity(Gravity.CENTER_VERTICAL);
        description.setPadding(0, 0, 0, AndroidUtilities.dp(8));
        layout.addView(description, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        addChoiceRow(layout, template.fields[0], block.param1, value -> {
            block.param1 = value;
            saveAndRebuild();
        });
        addChoiceRow(layout, template.fields[1], block.param2, value -> {
            block.param2 = value;
            saveAndRebuild();
        });
        addChoiceRow(layout, template.fields[2], block.param3, value -> {
            block.param3 = value;
            saveAndRebuild();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(template.title);
        builder.setView(layout);
        builder.setPositiveButton("Готово", null);
        showDialog(builder.create());
    }

    private void addChoiceRow(LinearLayout layout, FieldSpec field, String currentValue, ValueCallback callback) {
        TextView row = new TextView(getParentActivity());
        row.setText(field.title + "\n" + field.labelFor(currentValue));
        row.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.setTextSize(15);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        row.setOnClickListener(v -> showChoiceDialog(field, currentValue, callback));
        layout.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(62)));
    }

    private void showChoiceDialog(FieldSpec field, String currentValue, ValueCallback callback) {
        CharSequence[] labels = new CharSequence[field.options.length];
        for (int i = 0; i < field.options.length; i++) {
            labels[i] = field.options[i].label;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(field.title);
        builder.setItems(labels, (dialog, which) -> {
            Option option = field.options[which];
            if (MANUAL.equals(option.value)) {
                showManualValueDialog(field.title, currentValue, callback);
            } else {
                callback.run(option.value);
            }
        });
        showDialog(builder.create());
    }

    private void showAddBlockDialog() {
        ArrayList<BlockTemplate> supported = new ArrayList<>();
        for (BlockTemplate template : templates) {
            if (template.supports(bot.purpose)) {
                supported.add(template);
            }
        }
        CharSequence[] labels = new CharSequence[supported.size()];
        for (int i = 0; i < supported.size(); i++) {
            BlockTemplate template = supported.get(i);
            labels[i] = template.category + ": " + template.title;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Добавить блок");
        builder.setItems(labels, (dialog, which) -> {
            addBlock(supported.get(which));
        });
        showDialog(builder.create());
    }

    private void addBlock(BlockTemplate template) {
        ArrayList<LocalBot.Block> ordered = orderedBlocks();
        LocalBot.Block block = new LocalBot.Block(LocalBotController.createBlockId(), template.type, template.title, 40, 40 + ordered.size() * 104);
        block.param1 = template.fields[0].defaultValue();
        block.param2 = template.fields[1].defaultValue();
        block.param3 = template.fields[2].defaultValue();
        ordered.add(block);
        relinkAndSave(ordered);
    }

    private void showEditBotNameDialog() {
        showManualValueDialog("Название бота", bot.name, value -> {
            bot.name = TextUtils.isEmpty(value) ? "Локальный бот" : value;
            actionBar.setTitle(bot.name);
            saveAndRebuild();
        });
    }

    private void showManualValueDialog(String title, String currentValue, ValueCallback callback) {
        EditText editText = new EditText(getParentActivity());
        editText.setText(currentValue == null ? "" : currentValue);
        editText.setSingleLine(false);
        editText.setMinLines(1);
        editText.setMaxLines(4);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        builder.setView(editText);
        builder.setNegativeButton("Отмена", null);
        builder.setPositiveButton("Сохранить", (dialog, which) -> callback.run(editText.getText().toString()));
        showDialog(builder.create());
    }

    private void showInfo(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Локальный бот");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        showDialog(builder.create());
    }

    private void relinkAndSave(ArrayList<LocalBot.Block> ordered) {
        relinkBlocks(ordered);
        LocalBotController.saveBot(bot);
        buildEditor(getParentActivity());
    }

    private void saveAndRebuild() {
        LocalBotController.saveBot(bot);
        buildEditor(getParentActivity());
    }

    private void relinkBlocks(ArrayList<LocalBot.Block> ordered) {
        bot.blocks.clear();
        for (int i = 0; i < ordered.size(); i++) {
            LocalBot.Block block = ordered.get(i);
            block.nextBlockId = i < ordered.size() - 1 ? ordered.get(i + 1).blockId : null;
            block.x = 40;
            block.y = 40 + i * 104;
            bot.blocks.add(block);
        }
    }

    private ArrayList<LocalBot.Block> orderedBlocks() {
        ArrayList<LocalBot.Block> result = new ArrayList<>();
        if (bot.blocks == null) {
            bot.blocks = new ArrayList<>();
            return result;
        }
        LocalBot.Block start = null;
        for (LocalBot.Block block : bot.blocks) {
            if (block != null && block.type != null && block.type.startsWith("event_")) {
                start = block;
                break;
            }
        }
        if (start == null && !bot.blocks.isEmpty()) {
            start = bot.blocks.get(0);
        }
        HashSet<String> seen = new HashSet<>();
        LocalBot.Block current = start;
        while (current != null && !seen.contains(current.blockId)) {
            result.add(current);
            seen.add(current.blockId);
            current = findBlock(current.nextBlockId);
        }
        for (LocalBot.Block block : bot.blocks) {
            if (block != null && !seen.contains(block.blockId)) {
                result.add(block);
                seen.add(block.blockId);
            }
        }
        return result;
    }

    private LocalBot.Block findBlock(String blockId) {
        if (blockId == null || bot.blocks == null) {
            return null;
        }
        for (LocalBot.Block block : bot.blocks) {
            if (blockId.equals(block.blockId)) {
                return block;
            }
        }
        return null;
    }

    private LocalBot.Block copyBlock(LocalBot.Block block) {
        LocalBot.Block copy = new LocalBot.Block(LocalBotController.createBlockId(), block.type, block.value, block.x, block.y);
        copy.param1 = block.param1;
        copy.param2 = block.param2;
        copy.param3 = block.param3;
        return copy;
    }

    private int countEvents(ArrayList<LocalBot.Block> blocks) {
        int count = 0;
        for (LocalBot.Block block : blocks) {
            if (block.type != null && block.type.startsWith("event_")) {
                count++;
            }
        }
        return count;
    }

    private String summarize(BlockTemplate template, LocalBot.Block block) {
        return template.fields[0].shortLabel(block.param1) + " | "
                + template.fields[1].shortLabel(block.param2) + " | "
                + template.fields[2].shortLabel(block.param3);
    }

    private BlockTemplate getTemplate(String type) {
        for (BlockTemplate template : templates) {
            if (template.type.equals(type)) {
                return template;
            }
        }
        return templates[0];
    }

    private static BlockTemplate t(String type, String category, String title, String description, int color, String[] purposes, FieldSpec field1, FieldSpec field2, FieldSpec field3) {
        return new BlockTemplate(type, category, title, description, color, purposes, new FieldSpec[]{field1, field2, field3});
    }

    private static FieldSpec f(String title, Option... options) {
        return new FieldSpec(title, options);
    }

    private static Option o(String label, String value) {
        return new Option(label, value);
    }

    private static class BlockTemplate {
        final String type;
        final String category;
        final String title;
        final String description;
        final int color;
        final String[] purposes;
        final FieldSpec[] fields;

        BlockTemplate(String type, String category, String title, String description, int color, String[] purposes, FieldSpec[] fields) {
            this.type = type;
            this.category = category;
            this.title = title;
            this.description = description;
            this.color = color;
            this.purposes = purposes;
            this.fields = fields;
        }

        boolean supports(String purpose) {
            for (String item : purposes) {
                if (ANY.equals(item) || item.equals(purpose)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class FieldSpec {
        final String title;
        final Option[] options;

        FieldSpec(String title, Option[] options) {
            this.title = title;
            this.options = options;
        }

        String defaultValue() {
            return options.length == 0 ? "" : (MANUAL.equals(options[0].value) ? "" : options[0].value);
        }

        String labelFor(String value) {
            for (Option option : options) {
                if (option.value.equals(value)) {
                    return option.label;
                }
            }
            return TextUtils.isEmpty(value) ? "не задано" : value;
        }

        String shortLabel(String value) {
            String label = labelFor(value);
            return label.length() > 28 ? label.substring(0, 28) + "..." : label;
        }
    }

    private static class Option {
        final String label;
        final String value;

        Option(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private interface ValueCallback {
        void run(String value);
    }
}
