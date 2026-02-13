package com.quickarrive.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quickarrive.QuickArrivePlugin;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Updater implements Listener {
  private static final String API_URL = "https://api.github.com/repos/junginlau/QuickArrive/releases/latest";

  private final QuickArrivePlugin plugin;
  private String latestVersion;
  private String latestUrl;
  private boolean updateAvailable;

  public Updater(QuickArrivePlugin plugin) {
    this.plugin = plugin;
  }

  public void start() {
    if (!plugin.getConfig().getBoolean("updater.enabled", true)) {
      return;
    }
    int intervalMinutes = Math.max(5, plugin.getConfig().getInt("updater.check-interval-minutes", 60));
    new BukkitRunnable() {
      @Override
      public void run() {
        checkNow();
      }
    }.runTaskTimerAsynchronously(plugin, 20L, intervalMinutes * 60L * 20L);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!updateAvailable) {
      return;
    }
    Player player = event.getPlayer();
    if (!player.hasPermission("quickarrive.admin")) {
      return;
    }
    player.sendMessage(plugin.withPrefix(plugin.message("update-available", java.util.Map.of(
        "version", latestVersion != null ? latestVersion : "?",
        "url", latestUrl != null ? latestUrl : API_URL))));
  }

  public void checkNow() {
    try {
      ReleaseInfo info = fetchLatestRelease();
      if (info == null) {
        return;
      }
      String current = normalizeVersion(plugin.getDescription().getVersion());
      String latest = normalizeVersion(info.version());
      updateAvailable = compareVersions(latest, current) > 0;
      latestVersion = info.version();
      latestUrl = info.url();
      if (updateAvailable) {
        notifyOnlineAdmins();
      }
    } catch (Exception ignored) {
    }
  }

  private void notifyOnlineAdmins() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.hasPermission("quickarrive.admin")) {
        continue;
      }
      player.sendMessage(plugin.withPrefix(plugin.message("update-available", java.util.Map.of(
          "version", latestVersion != null ? latestVersion : "?",
          "url", latestUrl != null ? latestUrl : API_URL))));
    }
  }

  private ReleaseInfo fetchLatestRelease() {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(API_URL);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      connection.setRequestProperty("Accept", "application/vnd.github+json");
      connection.setRequestProperty("User-Agent", "QuickArrive-Updater");
      if (connection.getResponseCode() != 200) {
        return null;
      }
      JsonObject root = JsonParser.parseReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
      String tag = root.has("tag_name") ? root.get("tag_name").getAsString() : null;
      String htmlUrl = root.has("html_url") ? root.get("html_url").getAsString() : API_URL;
      if (tag == null || tag.isEmpty()) {
        return null;
      }
      return new ReleaseInfo(tag, htmlUrl);
    } catch (Exception ignored) {
      return null;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private String normalizeVersion(String value) {
    if (value == null) {
      return "";
    }
    String cleaned = value.trim().toLowerCase(Locale.ROOT);
    if (cleaned.startsWith("v")) {
      cleaned = cleaned.substring(1);
    }
    return cleaned;
  }

  private int compareVersions(String left, String right) {
    String[] leftParts = left.split("\\.");
    String[] rightParts = right.split("\\.");
    int max = Math.max(leftParts.length, rightParts.length);
    for (int i = 0; i < max; i++) {
      int l = i < leftParts.length ? parseInt(leftParts[i]) : 0;
      int r = i < rightParts.length ? parseInt(rightParts[i]) : 0;
      if (l != r) {
        return Integer.compare(l, r);
      }
    }
    return 0;
  }

  private int parseInt(String value) {
    try {
      return Integer.parseInt(value.replaceAll("[^0-9]", ""));
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  private record ReleaseInfo(String version, String url) {}
}
