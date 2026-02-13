package com.quickarrive;

import com.quickarrive.command.TeleportDecisionCommand;
import com.quickarrive.command.TpmenuCommand;
import com.quickarrive.gate.GateSettingsStore;
import com.quickarrive.gate.GateStore;
import com.quickarrive.home.EssentialsHomeProvider;
import com.quickarrive.menu.ChatInputManager;
import com.quickarrive.menu.MenuManager;
import com.quickarrive.store.LocationStore;
import com.quickarrive.store.PlayerDataStore;
import com.quickarrive.teleport.EssentialsCommandInterceptor;
import com.quickarrive.teleport.TeleportManager;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class QuickArrivePlugin extends JavaPlugin {
  private LocationStore locationStore;
  private TeleportManager teleportManager;
  private MenuManager menuManager;
  private PlayerDataStore playerDataStore;
  private GateStore gateStore;
  private GateSettingsStore gateSettingsStore;
  private EssentialsHomeProvider essentialsHomeProvider;
  private ChatInputManager chatInputManager;
  private EssentialsCommandInterceptor essentialsCommandInterceptor;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    locationStore = new LocationStore(this);
    playerDataStore = new PlayerDataStore(this);
    gateStore = new GateStore(this);
    gateSettingsStore = new GateSettingsStore(this);
    essentialsHomeProvider = new EssentialsHomeProvider(this);
    chatInputManager = new ChatInputManager(this);
    teleportManager = new TeleportManager(this, playerDataStore);
    menuManager = new MenuManager(this, teleportManager, playerDataStore, gateStore, gateSettingsStore, essentialsHomeProvider, chatInputManager);
    essentialsCommandInterceptor = new EssentialsCommandInterceptor(this, teleportManager);

    getServer().getPluginManager().registerEvents(teleportManager, this);
    getServer().getPluginManager().registerEvents(menuManager, this);
    getServer().getPluginManager().registerEvents(chatInputManager, this);
    getServer().getPluginManager().registerEvents(essentialsCommandInterceptor, this);

    TpmenuCommand tpmenuCommand = new TpmenuCommand(this, menuManager, locationStore);
    TeleportDecisionCommand decisionCommand = new TeleportDecisionCommand(this, teleportManager);

    if (getCommand("tpmenu") != null) {
      getCommand("tpmenu").setExecutor(tpmenuCommand);
      getCommand("tpmenu").setTabCompleter(tpmenuCommand);
    }
    if (getCommand("tpaccept") != null) {
      getCommand("tpaccept").setExecutor(decisionCommand);
    }
    if (getCommand("tpdeny") != null) {
      getCommand("tpdeny").setExecutor(decisionCommand);
    }
  }

  public String colorize(String text) {
    return ChatColor.translateAlternateColorCodes('&', text);
  }

  public String message(String path) {
    String raw = getConfig().getString("messages." + path, "");
    return colorize(raw);
  }

  public String message(String path, Map<String, String> replacements) {
    String raw = getConfig().getString("messages." + path, "");
    for (Map.Entry<String, String> entry : replacements.entrySet()) {
      raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return colorize(raw);
  }

  public String withPrefix(String message) {
    String prefix = message("prefix");
    return prefix + message;
  }
}
