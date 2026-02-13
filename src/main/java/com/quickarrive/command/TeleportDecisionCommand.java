package com.quickarrive.command;

import com.quickarrive.QuickArrivePlugin;
import com.quickarrive.teleport.TeleportManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeleportDecisionCommand implements CommandExecutor {
  private final QuickArrivePlugin plugin;
  private final TeleportManager teleportManager;

  public TeleportDecisionCommand(QuickArrivePlugin plugin, TeleportManager teleportManager) {
    this.plugin = plugin;
    this.teleportManager = teleportManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(plugin.withPrefix(plugin.message("player-only")));
      return true;
    }
    String cmd = command.getName().toLowerCase();
    if (cmd.equals("tpaccept")) {
      teleportManager.accept(player);
      return true;
    }
    if (cmd.equals("tpdeny")) {
      teleportManager.deny(player);
      return true;
    }
    return true;
  }
}
