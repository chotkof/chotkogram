package com.radolyn.ayugram.ui.localbots;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

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

public class LocalBotPickerActivity extends BaseFragment {
    private final long dialogId;
    private LinearLayout contentView;

    public LocalBotPickerActivity(long dialogId) {
        this.dialogId = dialogId;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Локальные боты");
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
        buildList(context);
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (contentView != null) {
            buildList(getParentActivity());
        }
    }

    private void buildList(Context context) {
        if (context == null || contentView == null) {
            return;
        }
        contentView.removeAllViews();

        TextCell createCell = new TextCell(context);
        createCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        createCell.setTextAndIcon("Создать локального бота", R.drawable.msg_addbot, true);
        createCell.setOnClickListener(v -> showCreatePurposeDialog());
        contentView.addView(createCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));

        TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(context);
        infoCell.setText("Боты хранятся только на этом устройстве. Чтобы бот отвечал в этом диалоге, выберите его для текущего чата и оставьте включенным.");
        contentView.addView(infoCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ArrayList<LocalBot> bots = LocalBotController.getBots();
        if (bots.isEmpty()) {
            TextInfoPrivacyCell emptyCell = new TextInfoPrivacyCell(context);
            emptyCell.setText("Локальных ботов пока нет.");
            contentView.addView(emptyCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }

        for (LocalBot bot : bots) {
            TextCell botCell = new TextCell(context);
            botCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            boolean selected = LocalBotController.isSelected(dialogId, bot.id);
            String status = (bot.enabled ? "включен" : "выключен") + ", " + (selected ? "выбран для чата" : "не выбран");
            botCell.setTextAndValue(bot.name, LocalBotController.getPurposeTitle(bot.purpose) + " | " + status, true);
            botCell.setOnClickListener(v -> showBotOptions(bot));
            contentView.addView(botCell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));
        }
    }

    private void showCreatePurposeDialog() {
        CharSequence[] items = new CharSequence[]{
                "Личный чат",
                "Группа",
                "Канал",
                "Любой чат"
        };
        String[] values = new String[]{
                LocalBotPurpose.PRIVATE_CHAT,
                LocalBotPurpose.GROUP,
                LocalBotPurpose.CHANNEL,
                LocalBotPurpose.OTHER
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Где будет работать бот?");
        builder.setItems(items, (dialog, which) -> {
            LocalBot bot = LocalBotController.createBot(dialogId, values[which]);
            presentFragment(new LocalBotBlocksActivity(bot.id, dialogId));
        });
        showDialog(builder.create());
    }

    private void showBotOptions(LocalBot bot) {
        boolean selected = LocalBotController.isSelected(dialogId, bot.id);
        CharSequence[] items = new CharSequence[]{
                "Открыть редактор",
                selected ? "Убрать из этого чата" : "Выбрать для этого чата",
                bot.enabled ? "Выключить" : "Включить",
                "Удалить"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(bot.name);
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                presentFragment(new LocalBotBlocksActivity(bot.id, dialogId));
            } else if (which == 1) {
                LocalBotController.selectBot(dialogId, bot.id, !selected);
                buildList(getParentActivity());
            } else if (which == 2) {
                bot.enabled = !bot.enabled;
                LocalBotController.saveBot(bot);
                buildList(getParentActivity());
            } else if (which == 3) {
                confirmDelete(bot);
            }
        });
        showDialog(builder.create());
    }

    private void confirmDelete(LocalBot bot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Удалить бота?");
        builder.setMessage("Бот \"" + bot.name + "\" и его сценарий будут удалены с устройства.");
        builder.setNegativeButton("Отмена", null);
        builder.setPositiveButton("Удалить", (dialog, which) -> {
            LocalBotController.deleteBot(bot.id);
            buildList(getParentActivity());
        });
        showDialog(builder.create());
    }
}
