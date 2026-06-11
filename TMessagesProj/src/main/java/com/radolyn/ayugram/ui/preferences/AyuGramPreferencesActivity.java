/*
 * This is the source code of ChotkoGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.radolyn.ayugram.AyuLocalPremiumProfile;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.messages.AyuMessagesController;
import com.radolyn.ayugram.sync.AyuSyncState;
import com.radolyn.ayugram.unblock.ProxyEngine;
import com.radolyn.ayugram.unblock.TgConstants;
import com.radolyn.ayugram.unblock.UnblockController;
import com.radolyn.ayugram.utils.AyuState;
import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.*;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;

import java.util.Locale;

public class AyuGramPreferencesActivity extends BasePreferencesActivity implements NotificationCenter.NotificationCenterDelegate {

    private static final int TOGGLE_BUTTON_VIEW = 1000;

    private int navigationHeaderRow;
    private int navigationInstructionRow;
    private int navigationUnblockRow;
    private int navigationSpyRow;
    private int navigationDividerRow;

    private int ghostEssentialsHeaderRow;
    private int ghostModeToggleRow;
    private int sendReadPacketsRow;
    private int sendOnlinePacketsRow;
    private int sendUploadProgressRow;
    private int sendOfflinePacketAfterOnlineRow;
    private int markReadAfterSendRow;
    private int useScheduledMessagesRow;
    private int ghostDividerRow;

    private int spyHeaderRow;
    private int saveDeletedMessagesRow;
    private int saveMessagesHistoryRow;
    private int spyDivider1Row;
    private int messageSavingBtnRow;
    private int spyDivider2Row;

    private int qolHeaderRow;
    private int keepAliveServiceRow;
    private int disableAdsRow;
    private int localPremiumRow;
    private int sendFunCommandsRow;
    private int filtersRow;
    private int qolDividerRow;

    private int unblockHeaderRow;
    private int unblockEnabledRow;
    private int unblockStatusRow;
    private int unblockModeRow;
    private int unblockDynamicPortRow;
    private int unblockPortRow;
    private int unblockIpRow;
    private int unblockSmartSleepRow;
    private int unblockAutostartRow;
    private int unblockVlessUriRow;
    private int unblockSocksUserRow;
    private int unblockSocksPassRow;
    private int unblockMtProxyRow;
    private int unblockDividerRow;

    private int ayuSyncHeaderRow;
    private int ayuSyncStatusBtnRow;
    private int ayuSyncDividerRow;

    private int debugHeaderRow;
    private int WALModeRow;
    private int buttonsDividerRow;
    private int clearAyuDatabaseBtnRow;
    private int eraseLocalDatabaseBtnRow;
    private int creatorFooterRow;

    private boolean ghostModeMenuExpanded;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        navigationHeaderRow = newRow();
        navigationInstructionRow = newRow();
        navigationUnblockRow = newRow();
        navigationSpyRow = newRow();
        navigationDividerRow = newRow();

        ghostEssentialsHeaderRow = newRow();
        ghostModeToggleRow = newRow();
        if (ghostModeMenuExpanded) {
            sendReadPacketsRow = newRow();
            sendOnlinePacketsRow = newRow();
            sendUploadProgressRow = newRow();
            sendOfflinePacketAfterOnlineRow = newRow();
        } else {
            sendReadPacketsRow = -1;
            sendOnlinePacketsRow = -1;
            sendUploadProgressRow = -1;
            sendOfflinePacketAfterOnlineRow = -1;
        }
        markReadAfterSendRow = newRow();
        useScheduledMessagesRow = newRow();
        ghostDividerRow = newRow();

        spyHeaderRow = newRow();
        saveDeletedMessagesRow = newRow();
        saveMessagesHistoryRow = newRow();
        spyDivider1Row = newRow();
        messageSavingBtnRow = newRow();
        spyDivider2Row = newRow();

        qolHeaderRow = newRow();
        keepAliveServiceRow = newRow();
        disableAdsRow = newRow();
        localPremiumRow = newRow();
        sendFunCommandsRow = newRow();
        filtersRow = newRow();
        qolDividerRow = newRow();

        unblockHeaderRow = newRow();
        unblockEnabledRow = newRow();
        unblockStatusRow = newRow();
        unblockModeRow = newRow();
        unblockDynamicPortRow = newRow();
        unblockPortRow = newRow();
        unblockIpRow = newRow();
        unblockSmartSleepRow = newRow();
        unblockAutostartRow = newRow();
        unblockVlessUriRow = newRow();
        unblockSocksUserRow = newRow();
        unblockSocksPassRow = newRow();
        unblockMtProxyRow = newRow();
        unblockDividerRow = newRow();

        ayuSyncHeaderRow = newRow();
        ayuSyncStatusBtnRow = newRow();
        ayuSyncDividerRow = newRow();

        debugHeaderRow = newRow();
        WALModeRow = newRow();
        buttonsDividerRow = newRow();
        clearAyuDatabaseBtnRow = newRow();
        eraseLocalDatabaseBtnRow = newRow();
        creatorFooterRow = newRow();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        // todo: register `MESSAGES_DELETED_NOTIFICATION` on all notification centers, not only on the current account

        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, AyuConstants.MESSAGES_DELETED_NOTIFICATION);
        NotificationCenter.getGlobalInstance().addObserver(this, AyuConstants.AYUSYNC_STATE_CHANGED);

        return true;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == AyuConstants.MESSAGES_DELETED_NOTIFICATION) {
            // recalculate database size
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(clearAyuDatabaseBtnRow);
            }
        } else if (id == AyuConstants.AYUSYNC_STATE_CHANGED) {
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(ayuSyncStatusBtnRow);
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, AyuConstants.MESSAGES_DELETED_NOTIFICATION);
        NotificationCenter.getGlobalInstance().removeObserver(this, AyuConstants.AYUSYNC_STATE_CHANGED);
    }

    private void updateGhostViews() {
        var isActive = AyuConfig.isGhostModeActive();

        listAdapter.notifyItemChanged(ghostModeToggleRow, payload);
        listAdapter.notifyItemChanged(sendReadPacketsRow, !isActive);
        listAdapter.notifyItemChanged(sendOnlinePacketsRow, !isActive);
        listAdapter.notifyItemChanged(sendUploadProgressRow, !isActive);
        listAdapter.notifyItemChanged(sendOfflinePacketAfterOnlineRow, isActive);

        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
    }

    private void toggleLocalPremium() {
        var newState = !AyuConfig.localPremium;

        AyuConfig.editor.putBoolean("localPremium", AyuConfig.localPremium = newState).apply();
        listAdapter.notifyItemChanged(localPremiumRow, AyuConfig.localPremium);

        getMessagesController().updatePremium(AyuConfig.localPremium);
        if (AyuConfig.localPremium) {
            AyuLocalPremiumProfile.syncAboutMarker(currentAccount, getUserConfig().getCurrentUser());
        } else {
            AyuLocalPremiumProfile.removeAboutMarker(currentAccount, getUserConfig().getCurrentUser());
        }
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.premiumStatusChangedGlobal);

        getMediaDataController().loadPremiumPromo(false);
        getMediaDataController().loadReactions(false, null);
    }

    private SharedPreferences getUnblockPrefs() {
        return UnblockController.prefs(ApplicationLoader.applicationContext);
    }

    private String getUnblockModeName() {
        int mode = getUnblockPrefs().getInt(UnblockController.KEY_MODE, ProxyEngine.MODE_ORIGINAL);
        if (mode == ProxyEngine.MODE_PYTHON) {
            return "WebSocket + Python init";
        } else if (mode == ProxyEngine.MODE_VLESS) {
            return "VLESS";
        }
        return "WebSocket";
    }

    private void restartUnblockIfEnabled() {
        UnblockController.restartIfEnabled(ApplicationLoader.applicationContext);
        if (listAdapter != null) {
            listAdapter.notifyItemChanged(unblockStatusRow);
        }
    }

    private void showUnblockModeAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Режим обхода");
        builder.setItems(new CharSequence[]{"WebSocket", "WebSocket + Python init", "VLESS"}, (dialog, which) -> {
            int mode = which == 1 ? ProxyEngine.MODE_PYTHON : which == 2 ? ProxyEngine.MODE_VLESS : ProxyEngine.MODE_ORIGINAL;
            getUnblockPrefs().edit().putInt(UnblockController.KEY_MODE, mode).apply();
            listAdapter.notifyItemChanged(unblockModeRow);
            restartUnblockIfEnabled();
        });
        builder.show();
    }

    private void showUnblockEditBox(TextCell view, String title, String key, String defaultValue, boolean restart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        EditTextSettingsCell input = new EditTextSettingsCell(getParentActivity());
        input.setText(getUnblockPrefs().getString(key, defaultValue), true);
        layout.addView(input);
        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String value = input.getText().trim();
            if (UnblockController.KEY_CUSTOM_PORT.equals(key)) {
                int port = Utilities.parseInt(value);
                if (port < 1 || port > 65535) {
                    port = 1080;
                }
                getUnblockPrefs().edit().putInt(key, port).apply();
                view.setTextAndValue(title, String.valueOf(port), true);
            } else {
                getUnblockPrefs().edit().putString(key, value).apply();
                view.setTextAndValue(title, value, true);
            }
            if (restart) {
                restartUnblockIfEnabled();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, which) -> dialog.cancel());
        builder.setNeutralButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            if (UnblockController.KEY_CUSTOM_PORT.equals(key)) {
                int port = Utilities.parseInt(defaultValue);
                getUnblockPrefs().edit().putInt(key, port).apply();
                view.setTextAndValue(title, String.valueOf(port), true);
            } else {
                getUnblockPrefs().edit().putString(key, defaultValue).apply();
                view.setTextAndValue(title, defaultValue, true);
            }
            if (restart) {
                restartUnblockIfEnabled();
            }
        });
        builder.show();
    }

    private void showMtProxyAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Встроенные MTProxy");
        CharSequence[] items = new CharSequence[]{
                TgConstants.PROXY_1_SERVER + ":" + TgConstants.PROXY_1_PORT,
                TgConstants.PROXY_2_SERVER + ":" + TgConstants.PROXY_2_PORT,
                TgConstants.PROXY_3_SERVER + ":" + TgConstants.PROXY_3_PORT,
                TgConstants.PROXY_4_SERVER + ":" + TgConstants.PROXY_4_PORT,
                TgConstants.PROXY_5_SERVER + ":" + TgConstants.PROXY_5_PORT,
                TgConstants.PROXY_6_SERVER + ":" + TgConstants.PROXY_6_PORT
        };
        builder.setItems(items, (dialog, which) -> {
            String server;
            int port;
            String secret;
            switch (which) {
                case 1:
                    server = TgConstants.PROXY_2_SERVER;
                    port = TgConstants.PROXY_2_PORT;
                    secret = TgConstants.PROXY_2_SECRET;
                    break;
                case 2:
                    server = TgConstants.PROXY_3_SERVER;
                    port = TgConstants.PROXY_3_PORT;
                    secret = TgConstants.PROXY_3_SECRET;
                    break;
                case 3:
                    server = TgConstants.PROXY_4_SERVER;
                    port = TgConstants.PROXY_4_PORT;
                    secret = TgConstants.PROXY_4_SECRET;
                    break;
                case 4:
                    server = TgConstants.PROXY_5_SERVER;
                    port = TgConstants.PROXY_5_PORT;
                    secret = TgConstants.PROXY_5_SECRET;
                    break;
                case 5:
                    server = TgConstants.PROXY_6_SERVER;
                    port = TgConstants.PROXY_6_PORT;
                    secret = TgConstants.PROXY_6_SECRET;
                    break;
                default:
                    server = TgConstants.PROXY_1_SERVER;
                    port = TgConstants.PROXY_1_PORT;
                    secret = TgConstants.PROXY_1_SECRET;
                    break;
            }
            UnblockController.setEnabled(ApplicationLoader.applicationContext, false);
            UnblockController.applyMtProxy(server, port, secret);
            listAdapter.notifyItemChanged(unblockEnabledRow);
            listAdapter.notifyItemChanged(unblockStatusRow);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "MTProxy включен").show();
        });
        builder.show();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == navigationInstructionRow) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle("ChotkoGram");
            builder.setMessage("Инструкция: основные функции ChotkoGram находятся в этом разделе настроек. Используй быстрые переходы сверху, чтобы перейти к обходу блокировки и сохранению сообщений.\n\nАвтор: @I_am_Chotko\nTGK: https://t.me/chotko_tg\nРелизы: https://t.me/chotkogram");
            builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
            builder.show();
        } else if (position == navigationUnblockRow) {
            listView.smoothScrollToPosition(unblockHeaderRow);
        } else if (position == navigationSpyRow) {
            listView.smoothScrollToPosition(spyHeaderRow);
        } else if (position == ghostModeToggleRow) {
            ghostModeMenuExpanded ^= true;
            updateRowsId();
            listAdapter.notifyItemChanged(ghostModeToggleRow, payload);
            if (ghostModeMenuExpanded) {
                listAdapter.notifyItemRangeInserted(ghostModeToggleRow + 1, 4);
            } else {
                listAdapter.notifyItemRangeRemoved(ghostModeToggleRow + 1, 4);
            }
        } else if (position == sendReadPacketsRow) {
            AyuConfig.editor.putBoolean("sendReadPackets", AyuConfig.sendReadPackets ^= true).apply();
            ((CheckBoxCell) view).setChecked(AyuConfig.sendReadPackets, true);

            AyuState.setAllowReadPacket(false, -1);
            updateGhostViews();
        } else if (position == sendOnlinePacketsRow) {
            AyuConfig.editor.putBoolean("sendOnlinePackets", AyuConfig.sendOnlinePackets ^= true).apply();
            ((CheckBoxCell) view).setChecked(AyuConfig.sendOnlinePackets, true);

            updateGhostViews();
        } else if (position == sendUploadProgressRow) {
            AyuConfig.editor.putBoolean("sendUploadProgress", AyuConfig.sendUploadProgress ^= true).apply();
            ((CheckBoxCell) view).setChecked(AyuConfig.sendUploadProgress, true);

            updateGhostViews();
        } else if (position == sendOfflinePacketAfterOnlineRow) {
            AyuConfig.editor.putBoolean("sendOfflinePacketAfterOnline", AyuConfig.sendOfflinePacketAfterOnline ^= true).apply();
            ((CheckBoxCell) view).setChecked(AyuConfig.sendOfflinePacketAfterOnline, true);

            updateGhostViews();
        } else if (position == markReadAfterSendRow) {
            AyuConfig.editor.putBoolean("markReadAfterSend", AyuConfig.markReadAfterSend ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.markReadAfterSend);

            AyuState.setAllowReadPacket(false, -1);

            if (AyuConfig.markReadAfterSend && AyuConfig.useScheduledMessages) {
                AyuConfig.editor.putBoolean("useScheduledMessages", AyuConfig.useScheduledMessages ^= true).apply();

                listAdapter.notifyItemChanged(useScheduledMessagesRow, false);
            }
        } else if (position == useScheduledMessagesRow) {
            AyuConfig.editor.putBoolean("useScheduledMessages", AyuConfig.useScheduledMessages ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.useScheduledMessages);

            AyuState.setAutomaticallyScheduled(false, -1);

            if (AyuConfig.useScheduledMessages && AyuConfig.markReadAfterSend) {
                AyuConfig.editor.putBoolean("markReadAfterSend", AyuConfig.markReadAfterSend ^= true).apply();

                listAdapter.notifyItemChanged(markReadAfterSendRow, false);
            }
        } else if (position == saveDeletedMessagesRow) {
            AyuConfig.editor.putBoolean("saveDeletedMessages", AyuConfig.saveDeletedMessages ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveDeletedMessages);
        } else if (position == saveMessagesHistoryRow) {
            AyuConfig.editor.putBoolean("saveMessagesHistory", AyuConfig.saveMessagesHistory ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.saveMessagesHistory);
        } else if (position == messageSavingBtnRow) {
            presentFragment(new MessageSavingPreferencesActivity());
        } else if (position == keepAliveServiceRow) {
            AyuConfig.editor.putBoolean("keepAliveService", AyuConfig.keepAliveService ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.keepAliveService);
        } else if (position == disableAdsRow) {
            AyuConfig.editor.putBoolean("disableAds", AyuConfig.disableAds ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.disableAds);
            getMessagesController().clearSponsoredMessagesCache();
        } else if (position == localPremiumRow) {
            toggleLocalPremium();
        } else if (position == sendFunCommandsRow) {
            AyuConfig.editor.putBoolean("sendFunCommands", AyuConfig.sendFunCommands ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.sendFunCommands);
        } else if (position == filtersRow) {
            NotificationsCheckCell checkCell = (NotificationsCheckCell) view;
            if (LocaleController.isRTL && x <= AndroidUtilities.dp(76) || !LocaleController.isRTL && x >= view.getMeasuredWidth() - AndroidUtilities.dp(76)) {
                AyuConfig.editor.putBoolean("regexFiltersEnabled", AyuConfig.regexFiltersEnabled ^= true).apply();
                checkCell.setChecked(AyuConfig.regexFiltersEnabled, 0);
            } else {
                presentFragment(new RegexFiltersPreferencesActivity());
            }
        } else if (position == unblockEnabledRow) {
            boolean enabled = !UnblockController.isEnabled(ApplicationLoader.applicationContext);
            UnblockController.setEnabled(ApplicationLoader.applicationContext, enabled);
            ((TextCheckCell) view).setChecked(enabled);
            listAdapter.notifyItemChanged(unblockStatusRow);
        } else if (position == unblockModeRow) {
            showUnblockModeAlert();
        } else if (position == unblockDynamicPortRow) {
            boolean enabled = !getUnblockPrefs().getBoolean(UnblockController.KEY_DYNAMIC_PORT, false);
            getUnblockPrefs().edit().putBoolean(UnblockController.KEY_DYNAMIC_PORT, enabled).apply();
            ((TextCheckCell) view).setChecked(enabled);
            restartUnblockIfEnabled();
        } else if (position == unblockPortRow) {
            showUnblockEditBox((TextCell) view, "Порт локального прокси", UnblockController.KEY_CUSTOM_PORT, "1080", true);
        } else if (position == unblockIpRow) {
            showUnblockEditBox((TextCell) view, "IP локального прокси", UnblockController.KEY_CUSTOM_IP, "127.0.0.1", true);
        } else if (position == unblockSmartSleepRow) {
            boolean enabled = !getUnblockPrefs().getBoolean(UnblockController.KEY_SMART_SLEEP, true);
            getUnblockPrefs().edit().putBoolean(UnblockController.KEY_SMART_SLEEP, enabled).apply();
            ((TextCheckCell) view).setChecked(enabled);
            restartUnblockIfEnabled();
        } else if (position == unblockAutostartRow) {
            boolean enabled = !getUnblockPrefs().getBoolean(UnblockController.KEY_AUTOSTART, false);
            getUnblockPrefs().edit().putBoolean(UnblockController.KEY_AUTOSTART, enabled).apply();
            ((TextCheckCell) view).setChecked(enabled);
        } else if (position == unblockVlessUriRow) {
            showUnblockEditBox((TextCell) view, "VLESS URI", UnblockController.KEY_VLESS_URI, "", true);
        } else if (position == unblockSocksUserRow) {
            showUnblockEditBox((TextCell) view, "SOCKS5 логин", UnblockController.KEY_SOCKS5_USER, "", true);
        } else if (position == unblockSocksPassRow) {
            showUnblockEditBox((TextCell) view, "SOCKS5 пароль", UnblockController.KEY_SOCKS5_PASS, "", true);
        } else if (position == unblockMtProxyRow) {
            showMtProxyAlert();
        } else if (position == ayuSyncStatusBtnRow) {
            presentFragment(new AyuSyncPreferencesActivity());
        } else if (position == WALModeRow) {
            AyuConfig.editor.putBoolean("walMode", AyuConfig.WALMode ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.WALMode);
        } else if (position == clearAyuDatabaseBtnRow) {
            AyuMessagesController.getInstance().clean();

            // reset size
            ((TextCell) view).setValue("…");

            BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.ClearAyuDatabaseNotification)).show();
        } else if (position == eraseLocalDatabaseBtnRow) {
            getMessagesStorage().clearLocalDatabase();

            try {
                getMessagesStorage().getDatabase().executeFast("DELETE FROM messages_v2").stepThis().dispose();
            } catch (Exception e) {
                FileLog.e(e);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.ErrorOccurred)).show();
            }

            try {
                getMessagesStorage().getDatabase().executeFast("DELETE FROM dialogs").stepThis().dispose();
            } catch (Exception e) {
                FileLog.e(e);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.ErrorOccurred)).show();
            }

            BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.RestartRequired)).show();
        }
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.AyuPreferences);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private int getGhostModeSelectedCount() {
        int count = 0;
        if (!AyuConfig.sendReadPackets) count++;
        if (!AyuConfig.sendOnlinePackets) count++;
        if (!AyuConfig.sendUploadProgress) count++;
        if (AyuConfig.sendOfflinePacketAfterOnline) count++;

        return count;
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == navigationInstructionRow) {
                        textCell.setTextAndValue("Инструкция и каналы", "@I_am_Chotko | t.me/chotko_tg | t.me/chotkogram", true);
                    } else if (position == navigationUnblockRow) {
                        textCell.setTextAndValue("Перейти к обходу Telegram", "Прокси, VLESS, MTProxy", true);
                    } else if (position == navigationSpyRow) {
                        textCell.setTextAndValue("Перейти к сохранению сообщений", "Удаленные и история", true);
                    } else if (position == messageSavingBtnRow) {
                        textCell.setText(LocaleController.getString(R.string.MessageSavingBtn), false);
                    } else if (position == ayuSyncStatusBtnRow) {
                        var status = AyuSyncState.getConnectionStateString();

                        textCell.setTextAndValue(LocaleController.getString(R.string.AyuSyncStatusTitle), status, false);
                    } else if (position == unblockStatusRow) {
                        boolean enabled = UnblockController.isEnabled(ApplicationLoader.applicationContext);
                        String value = enabled
                                ? UnblockController.getIp(ApplicationLoader.applicationContext) + ":" + getUnblockPrefs().getInt(UnblockController.KEY_LAST_PORT, UnblockController.getPort(ApplicationLoader.applicationContext))
                                : "Выключен";
                        textCell.setTextAndValue("Статус обхода", value, true);
                    } else if (position == unblockModeRow) {
                        textCell.setTextAndValue("Режим обхода", getUnblockModeName(), true);
                    } else if (position == unblockPortRow) {
                        textCell.setTextAndValue("Порт локального прокси", String.valueOf(UnblockController.getPort(ApplicationLoader.applicationContext)), true);
                    } else if (position == unblockIpRow) {
                        textCell.setTextAndValue("IP локального прокси", UnblockController.getIp(ApplicationLoader.applicationContext), true);
                    } else if (position == unblockVlessUriRow) {
                        String value = getUnblockPrefs().getString(UnblockController.KEY_VLESS_URI, "");
                        textCell.setTextAndValue("VLESS URI", value == null || value.isEmpty() ? "Не задан" : value, true);
                    } else if (position == unblockSocksUserRow) {
                        String value = getUnblockPrefs().getString(UnblockController.KEY_SOCKS5_USER, "");
                        textCell.setTextAndValue("SOCKS5 логин", value == null || value.isEmpty() ? "Не задан" : value, true);
                    } else if (position == unblockSocksPassRow) {
                        String value = getUnblockPrefs().getString(UnblockController.KEY_SOCKS5_PASS, "");
                        textCell.setTextAndValue("SOCKS5 пароль", value == null || value.isEmpty() ? "Не задан" : "Сохранен", true);
                    } else if (position == unblockMtProxyRow) {
                        textCell.setTextAndValue("Встроенные MTProxy", "6 серверов", true);
                    } else if (position == clearAyuDatabaseBtnRow) {
                        var file = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE);
                        var size = file.exists() ? file.length() : 0;
                        var walFile = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE + "-wal");
                        if (walFile.exists()) {
                            size += walFile.length();
                        }
                        var shmFile = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE + "-shm");
                        if (shmFile.exists()) {
                            size += shmFile.length();
                        }

                        textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ClearAyuDatabase), AndroidUtilities.formatFileSize(size), R.drawable.msg_clear_solar, true);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    } else if (position == eraseLocalDatabaseBtnRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.EraseLocalDatabase), R.drawable.msg_archive, false);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == navigationHeaderRow) {
                        headerCell.setText("Навигация");
                    } else if (position == ghostEssentialsHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.GhostEssentialsHeader));
                    } else if (position == spyHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.SpyEssentialsHeader));
                    } else if (position == qolHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.QoLTogglesHeader));
                    } else if (position == unblockHeaderRow) {
                        headerCell.setText("Обход блокировки Telegram");
                    } else if (position == ayuSyncHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.AyuSyncHeader));
                    } else if (position == debugHeaderRow) {
                        headerCell.setText(LocaleController.getString("SettingsDebug", R.string.SettingsDebug));
                    }
                    break;
                case 8:
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == creatorFooterRow) {
                        infoCell.setText("@I_am_Chotko");
                    }
                    break;
                case 5:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == markReadAfterSendRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.MarkReadAfterSend), AyuConfig.markReadAfterSend, true);
                    } else if (position == useScheduledMessagesRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.UseScheduledMessages), AyuConfig.useScheduledMessages, false);
                    } else if (position == saveDeletedMessagesRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.SaveDeletedMessages), AyuConfig.saveDeletedMessages, true);
                    } else if (position == saveMessagesHistoryRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.SaveMessagesHistory), AyuConfig.saveMessagesHistory, false);
                    } else if (position == keepAliveServiceRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.KeepAliveService), AyuConfig.keepAliveService, true);
                    } else if (position == disableAdsRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.DisableAds), AyuConfig.disableAds, true);
                    } else if (position == localPremiumRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.LocalPremium) + " β", AyuConfig.localPremium, true);
                    } else if (position == sendFunCommandsRow) {
                        textCheckCell.setTextAndCheck("Send troll commands", AyuConfig.sendFunCommands, true);
                    } else if (position == unblockEnabledRow) {
                        textCheckCell.setTextAndCheck("Включить обход Telegram", UnblockController.isEnabled(ApplicationLoader.applicationContext), true);
                    } else if (position == unblockDynamicPortRow) {
                        textCheckCell.setTextAndCheck("Динамический порт", getUnblockPrefs().getBoolean(UnblockController.KEY_DYNAMIC_PORT, false), true);
                    } else if (position == unblockSmartSleepRow) {
                        textCheckCell.setTextAndCheck("Smart sleep", getUnblockPrefs().getBoolean(UnblockController.KEY_SMART_SLEEP, true), true);
                    } else if (position == unblockAutostartRow) {
                        textCheckCell.setTextAndCheck("Автозапуск после перезагрузки", getUnblockPrefs().getBoolean(UnblockController.KEY_AUTOSTART, false), true);
                    } else if (position == WALModeRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.WALMode), AyuConfig.WALMode, false);
                    }
                    break;
                case 18:
                    TextCheckCell2 checkCell = (TextCheckCell2) holder.itemView;
                    if (position == ghostModeToggleRow) {
                        int selectedCount = getGhostModeSelectedCount();
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.GhostModeToggle), AyuConfig.isGhostModeActive(), true, true);
                        checkCell.setCollapseArrow(String.format(Locale.US, "%d/4", selectedCount), !ghostModeMenuExpanded, () -> {
                            AyuConfig.toggleGhostMode();
                            updateGhostViews();
                        });
                    }
                    checkCell.getCheckBox().setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
                    checkCell.getCheckBox().setDrawIconType(0);
                    break;
                case 19:
                    CheckBoxCell checkBoxCell = (CheckBoxCell) holder.itemView;
                    if (position == sendReadPacketsRow) {
                        checkBoxCell.setText(LocaleController.getString(R.string.DontSendReadPackets), "", !AyuConfig.sendReadPackets, true, true);
                    } else if (position == sendOnlinePacketsRow) {
                        checkBoxCell.setText(LocaleController.getString(R.string.DontSendOnlinePackets), "", !AyuConfig.sendOnlinePackets, true, true);
                    } else if (position == sendUploadProgressRow) {
                        checkBoxCell.setText(LocaleController.getString(R.string.DontSendUploadProgress), "", !AyuConfig.sendUploadProgress, true, true);
                    } else if (position == sendOfflinePacketAfterOnlineRow) {
                        checkBoxCell.setText(LocaleController.getString(R.string.SendOfflinePacketAfterOnline), "", AyuConfig.sendOfflinePacketAfterOnline, true, true);
                    }
                    checkBoxCell.setPad(1);
                    break;
                case TOGGLE_BUTTON_VIEW:
                    NotificationsCheckCell notificationsCheckCell = (NotificationsCheckCell) holder.itemView;
                    if (position == filtersRow) {
                        var count = AyuConfig.getRegexFilters().size();
                        notificationsCheckCell.setTextAndValueAndCheck(LocaleController.getString(R.string.RegexFilters), count + " " + LocaleController.getString(R.string.RegexFiltersAmount), AyuConfig.regexFiltersEnabled, false);
                    }
                    break;
            }
        }

        @NonNull
        @NotNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            if (viewType == TOGGLE_BUTTON_VIEW) {
                var view = new NotificationsCheckCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                return new RecyclerListView.Holder(view);
            }
            return super.onCreateViewHolder(parent, viewType);
        }

        @Override
        public int getItemViewType(int position) {
            if (
                    position == ghostDividerRow ||
                            position == navigationDividerRow ||
                            position == spyDivider1Row ||
                            position == spyDivider2Row ||
                            position == qolDividerRow ||
                            position == unblockDividerRow ||
                            position == ayuSyncDividerRow ||
                            position == buttonsDividerRow
            ) {
                return 1;
            } else if (
                    position == navigationInstructionRow ||
                            position == navigationUnblockRow ||
                            position == navigationSpyRow ||
                            position == messageSavingBtnRow ||
                            position == ayuSyncStatusBtnRow ||
                            position == unblockStatusRow ||
                            position == unblockModeRow ||
                            position == unblockPortRow ||
                            position == unblockIpRow ||
                            position == unblockVlessUriRow ||
                            position == unblockSocksUserRow ||
                            position == unblockSocksPassRow ||
                            position == unblockMtProxyRow ||
                            position == clearAyuDatabaseBtnRow ||
                            position == eraseLocalDatabaseBtnRow
            ) {
                return 2;
            } else if (
                    position == navigationHeaderRow ||
                            position == ghostEssentialsHeaderRow ||
                            position == spyHeaderRow ||
                            position == qolHeaderRow ||
                            position == unblockHeaderRow ||
                            position == ayuSyncHeaderRow ||
                            position == debugHeaderRow
            ) {
                return 3;
            } else if (
                    position == creatorFooterRow
            ) {
                return 8;
            } else if (
                    position == ghostModeToggleRow
            ) {
                return 18;
            } else if (
                    position >= sendReadPacketsRow && position <= sendOfflinePacketAfterOnlineRow
            ) {
                return 19;
            } else if (
                    position == filtersRow
            ) {
                return TOGGLE_BUTTON_VIEW;
            }
            return 5;
        }
    }
}
