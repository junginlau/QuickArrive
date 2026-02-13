package com.quickarrive.teleport;

import com.quickarrive.QuickArrivePlugin;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class EssentialsCommandInterceptor implements Listener {
  private final QuickArrivePlugin plugin;
  private final TeleportManager teleportManager;

  public EssentialsCommandInterceptor(QuickArrivePlugin plugin, TeleportManager teleportManager) {
    this.plugin = plugin;
    this.teleportManager = teleportManager;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    if (!plugin.getConfig().getBoolean("teleport.intercept-essentialsx", true)) {
      return;
    }

    String message = event.getMessage().trim();
    if (!message.startsWith("/")) {
      return;
    }

    String[] parts = message.split("\\s+");
    if (parts.length == 0) {
      return;
    }

    String label = parts[0].substring(1).toLowerCase();
    Player player = event.getPlayer();

    if (label.equals("tpa")) {
      event.setCancelled(true);
      if (!player.hasPermission("quickarrive.use")) {
        player.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
        return;
      }
      if (parts.length < 2) {
        player.sendMessage(plugin.withPrefix(plugin.message("tpa-usage")));
        return;
      }
      Player target = Bukkit.getPlayerExact(parts[1]);
      if (target == null) {
        player.sendMessage(plugin.withPrefix(plugin.message("target-not-found", Map.of("player", parts[1]))));
        return;
      }
      teleportManager.sendPlayerRequest(player, target, TeleportRequestType.TPA);
      return;
    }

    if (label.equals("tpahere")) {
      event.setCancelled(true);
      if (!player.hasPermission("quickarrive.use")) {
        player.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
        return;
      }
      if (parts.length < 2) {
        player.sendMessage(plugin.withPrefix(plugin.message("tpahere-usage")));
        return;
      }
      Player target = Bukkit.getPlayerExact(parts[1]);
      if (target == null) {
        player.sendMessage(plugin.withPrefix(plugin.message("target-not-found", Map.of("player", parts[1]))));
        return;
      }
      teleportManager.sendPlayerRequest(player, target, TeleportRequestType.TPHERE);
      return;
    }

    if (label.equals("tpaccept")) {
      event.setCancelled(true);
      if (!player.hasPermission("quickarrive.use")) {
        player.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
        return;
      }
      teleportManager.accept(player);
      return;
    }

    if (label.equals("tpdeny")) {
      event.setCancelled(true);
      if (!player.hasPermission("quickarrive.use")) {
        player.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
        return;
      }
      teleportManager.deny(player);
    }
  }
}
