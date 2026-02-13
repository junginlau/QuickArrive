package com.quickarrive.command;

import com.quickarrive.QuickArrivePlugin;
import com.quickarrive.menu.MenuManager;
import com.quickarrive.store.LocationStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TpmenuCommand implements CommandExecutor, TabCompleter {
  private final QuickArrivePlugin plugin;
  private final MenuManager menuManager;
  private final LocationStore locationStore;

  public TpmenuCommand(QuickArrivePlugin plugin, MenuManager menuManager, LocationStore locationStore) {
    this.plugin = plugin;
    this.menuManager = menuManager;
    this.locationStore = locationStore;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(plugin.withPrefix(plugin.message("player-only")));
        return true;
      }
      menuManager.openMainMenu(player);
      player.sendMessage(plugin.withPrefix(plugin.message("menu-opened")));
      return true;
    }

    String sub = args[0].toLowerCase();
    switch (sub) {
      case "admin" -> {
        if (!sender.hasPermission("quickarrive.admin")) {
          sender.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
          return true;
        }
        if (!(sender instanceof Player player)) {
          sender.sendMessage(plugin.withPrefix(plugin.message("player-only")));
          return true;
        }
        menuManager.openAdminMenu(player, 0);
        player.sendMessage(plugin.withPrefix(plugin.message("admin-opened")));
        return true;
      }
      case "give" -> {
        if (!sender.hasPermission("quickarrive.menu.give")) {
          sender.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
          return true;
        }
        Player target;
        if (args.length >= 2) {
          target = Bukkit.getPlayerExact(args[1]);
        } else if (sender instanceof Player player) {
          target = player;
        } else {
          sender.sendMessage(plugin.withPrefix(plugin.message("player-only")));
          return true;
        }
        if (target == null) {
          sender.sendMessage(plugin.withPrefix(plugin.message("player-only")));
          return true;
        }
        menuManager.giveMenuTool(target);
        target.sendMessage(plugin.withPrefix(plugin.message("tool-given")));
        if (sender != target) {
          sender.sendMessage(plugin.withPrefix(plugin.message("tool-given")));
        }
        return true;
      }
      case "setpoint" -> {
        if (!sender.hasPermission("quickarrive.admin")) {
          sender.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
          return true;
        }
        if (!(sender instanceof Player player)) {
          sender.sendMessage(plugin.withPrefix(plugin.message("player-only")));
          return true;
        }
        if (args.length < 2) {
          sender.sendMessage("/tpmenu setpoint <name>");
          return true;
        }
        String name = args[1];
        locationStore.setLocation(name, player.getLocation(), false);
        player.sendMessage(plugin.withPrefix(plugin.message("location-set", Collections.singletonMap("location", name))));
        return true;
      }
      case "delpoint" -> {
        if (!sender.hasPermission("quickarrive.admin")) {
          sender.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
          return true;
        }
        if (args.length < 2) {
          sender.sendMessage("/tpmenu delpoint <name>");
          return true;
        }
        String name = args[1];
        if (!locationStore.removeLocation(name)) {
          sender.sendMessage(plugin.withPrefix(plugin.message("location-missing", Collections.singletonMap("location", name))));
          return true;
        }
        sender.sendMessage(plugin.withPrefix(plugin.message("location-removed", Collections.singletonMap("location", name))));
        return true;
      }
      case "points" -> {
        sender.sendMessage(locationStore.describeLocations(plugin));
        return true;
      }
      default -> {
        sender.sendMessage("/tpmenu [admin|give|setpoint|delpoint|points]");
        return true;
      }
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> options = new ArrayList<>();
      options.add("admin");
      options.add("give");
      options.add("setpoint");
      options.add("delpoint");
      options.add("points");
      return options;
    }
    if (args.length == 2 && args[0].equalsIgnoreCase("delpoint")) {
      return new ArrayList<>(locationStore.getLocationNames());
    }
    return Collections.emptyList();
  }
}
