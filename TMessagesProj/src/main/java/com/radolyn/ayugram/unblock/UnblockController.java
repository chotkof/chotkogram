package com.radolyn.ayugram.unblock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.preference.PreferenceManager;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;

import java.util.Random;

public final class UnblockController {

    public static final String KEY_ENABLED = "unblock_enabled";
    public static final String KEY_AUTOSTART = "autostart_boot";
    public static final String KEY_DYNAMIC_PORT = "dynamic_port";
    public static final String KEY_CUSTOM_PORT = "custom_port";
    public static final String KEY_CUSTOM_IP = "custom_ip";
    public static final String KEY_MODE = "proxy_mode";
    public static final String KEY_VLESS_URI = "vless_uri";
    public static final String KEY_SMART_SLEEP = "smart_sleep";
    public static final String KEY_SOCKS5_USER = "socks5_user";
    public static final String KEY_SOCKS5_PASS = "socks5_pass";
    public static final String KEY_LAST_PORT = "last_port";

    private static final String PREV_SAVED = "unblock_prev_saved";
    private static final String PREV_ENABLED = "unblock_prev_proxy_enabled";
    private static final String PREV_CALLS = "unblock_prev_proxy_calls";
    private static final String PREV_IP = "unblock_prev_proxy_ip";
    private static final String PREV_PORT = "unblock_prev_proxy_port";
    private static final String PREV_USER = "unblock_prev_proxy_user";
    private static final String PREV_PASS = "unblock_prev_proxy_pass";
    private static final String PREV_SECRET = "unblock_prev_proxy_secret";

    private static final Random random = new Random();

    public static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static int getPort(Context context) {
        SharedPreferences preferences = prefs(context);
        int port = preferences.getInt(KEY_CUSTOM_PORT, 1080);
        return sanitizePort(port);
    }

    public static String getIp(Context context) {
        String ip = prefs(context).getString(KEY_CUSTOM_IP, "127.0.0.1");
        if (ip == null || ip.trim().isEmpty()) {
            return "127.0.0.1";
        }
        return ip.trim();
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) {
            start(context);
        } else {
            stop(context);
        }
    }

    public static void restartIfEnabled(Context context) {
        if (!isEnabled(context)) {
            return;
        }
        start(context);
    }

    public static void start(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = prefs(appContext);
        int port = resolveRuntimePort(preferences);
        String ip = getIp(appContext);
        preferences.edit().putInt(KEY_LAST_PORT, port).apply();

        savePreviousProxy();
        applyTelegramSocksProxy(ip, port,
                preferences.getString(KEY_SOCKS5_USER, ""),
                preferences.getString(KEY_SOCKS5_PASS, ""));

        Intent intent = new Intent(appContext, ProxyService.class);
        intent.putExtra(ProxyService.EXTRA_PORT, port);
        intent.putExtra(ProxyService.EXTRA_IP, ip);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    public static void stop(Context context) {
        Context appContext = context.getApplicationContext();
        appContext.stopService(new Intent(appContext, ProxyService.class));
        restorePreviousProxy();
    }

    public static void applyMtProxy(String server, int port, String secret) {
        savePreviousProxy();
        SharedConfig.ProxyInfo info = new SharedConfig.ProxyInfo(server, sanitizePort(port), "", "", secret);
        SharedConfig.currentProxy = SharedConfig.addProxy(info);

        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean("proxy_enabled", true);
        editor.putBoolean("proxy_enabled_calls", false);
        editor.putString("proxy_ip", server);
        editor.putInt("proxy_port", sanitizePort(port));
        editor.putString("proxy_user", "");
        editor.putString("proxy_pass", "");
        editor.putString("proxy_secret", secret);
        editor.apply();

        ConnectionsManager.setProxySettings(true, server, sanitizePort(port), "", "", secret);
    }

    private static int resolveRuntimePort(SharedPreferences preferences) {
        if (preferences.getBoolean(KEY_DYNAMIC_PORT, false)) {
            return 10000 + random.nextInt(50000);
        }
        return sanitizePort(preferences.getInt(KEY_CUSTOM_PORT, 1080));
    }

    private static int sanitizePort(int port) {
        if (port < 1 || port > 65535) {
            return 1080;
        }
        return port;
    }

    private static void savePreviousProxy() {
        SharedPreferences main = MessagesController.getGlobalMainSettings();
        SharedPreferences preferences = prefs(org.telegram.messenger.ApplicationLoader.applicationContext);
        if (preferences.getBoolean(PREV_SAVED, false)) {
            return;
        }
        preferences.edit()
                .putBoolean(PREV_SAVED, true)
                .putBoolean(PREV_ENABLED, main.getBoolean("proxy_enabled", false))
                .putBoolean(PREV_CALLS, main.getBoolean("proxy_enabled_calls", false))
                .putString(PREV_IP, main.getString("proxy_ip", ""))
                .putInt(PREV_PORT, main.getInt("proxy_port", 1080))
                .putString(PREV_USER, main.getString("proxy_user", ""))
                .putString(PREV_PASS, main.getString("proxy_pass", ""))
                .putString(PREV_SECRET, main.getString("proxy_secret", ""))
                .apply();
    }

    private static void applyTelegramSocksProxy(String ip, int port, String user, String pass) {
        if (user == null) user = "";
        if (pass == null) pass = "";
        SharedConfig.ProxyInfo info = new SharedConfig.ProxyInfo(ip, port, user, pass, "");
        SharedConfig.currentProxy = SharedConfig.addProxy(info);

        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean("proxy_enabled", true);
        editor.putBoolean("proxy_enabled_calls", false);
        editor.putString("proxy_ip", ip);
        editor.putInt("proxy_port", port);
        editor.putString("proxy_user", user);
        editor.putString("proxy_pass", pass);
        editor.putString("proxy_secret", "");
        editor.apply();

        ConnectionsManager.setProxySettings(true, ip, port, user, pass, "");
    }

    private static void restorePreviousProxy() {
        SharedPreferences preferences = prefs(org.telegram.messenger.ApplicationLoader.applicationContext);
        if (!preferences.getBoolean(PREV_SAVED, false)) {
            ConnectionsManager.setProxySettings(false, "", 1080, "", "", "");
            return;
        }

        boolean enabled = preferences.getBoolean(PREV_ENABLED, false);
        boolean calls = preferences.getBoolean(PREV_CALLS, false);
        String ip = preferences.getString(PREV_IP, "");
        int port = sanitizePort(preferences.getInt(PREV_PORT, 1080));
        String user = preferences.getString(PREV_USER, "");
        String pass = preferences.getString(PREV_PASS, "");
        String secret = preferences.getString(PREV_SECRET, "");

        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean("proxy_enabled", enabled);
        editor.putBoolean("proxy_enabled_calls", calls);
        editor.putString("proxy_ip", ip);
        editor.putInt("proxy_port", port);
        editor.putString("proxy_user", user);
        editor.putString("proxy_pass", pass);
        editor.putString("proxy_secret", secret);
        editor.apply();

        if (enabled && ip != null && !ip.isEmpty()) {
            SharedConfig.currentProxy = SharedConfig.addProxy(new SharedConfig.ProxyInfo(ip, port, user, pass, secret));
            ConnectionsManager.setProxySettings(true, ip, port, user, pass, secret);
        } else {
            SharedConfig.currentProxy = null;
            ConnectionsManager.setProxySettings(false, "", 1080, "", "", "");
        }

        preferences.edit()
                .remove(PREV_SAVED)
                .remove(PREV_ENABLED)
                .remove(PREV_CALLS)
                .remove(PREV_IP)
                .remove(PREV_PORT)
                .remove(PREV_USER)
                .remove(PREV_PASS)
                .remove(PREV_SECRET)
                .apply();
    }

    private UnblockController() {
    }
}
