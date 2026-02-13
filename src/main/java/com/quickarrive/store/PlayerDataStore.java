package com.quickarrive.store;

import com.quickarrive.QuickArrivePlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerDataStore {
  private final QuickArrivePlugin plugin;
  private final File file;
  private YamlConfiguration config;

  public PlayerDataStore(QuickArrivePlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "players.yml");
    reload();
  }

  public void reload() {
    if (!file.exists()) {
      try {
        plugin.getDataFolder().mkdirs();
        file.createNewFile();
      } catch (IOException ignored) {
      }
    }
    config = YamlConfiguration.loadConfiguration(file);
  }

  public void save() {
    try {
      config.save(file);
    } catch (IOException ignored) {
    }
  }

  public List<UUID> getBlacklist(UUID playerId) {
    List<String> list = config.getStringList("players." + playerId + ".blacklist");
    if (list == null) {
      return Collections.emptyList();
    }
    List<UUID> results = new ArrayList<>();
    for (String value : list) {
      try {
        results.add(UUID.fromString(value));
      } catch (IllegalArgumentException ignored) {
      }
    }
    return results;
  }

  public boolean isBlacklisted(UUID playerId, UUID targetId) {
    return getBlacklist(playerId).contains(targetId);
  }

  public void addBlacklist(UUID playerId, UUID targetId) {
    List<String> list = new ArrayList<>(config.getStringList("players." + playerId + ".blacklist"));
    String value = targetId.toString();
    if (!list.contains(value)) {
      list.add(value);
      config.set("players." + playerId + ".blacklist", list);
      save();
    }
  }

  public void removeBlacklist(UUID playerId, UUID targetId) {
    List<String> list = new ArrayList<>(config.getStringList("players." + playerId + ".blacklist"));
    String value = targetId.toString();
    if (list.remove(value)) {
      config.set("players." + playerId + ".blacklist", list);
      save();
    }
  }

  public List<PlayerHome> getHomes(UUID playerId) {
    ConfigurationSection section = config.getConfigurationSection("players." + playerId + ".homes");
    if (section == null) {
      return Collections.emptyList();
    }
    List<PlayerHome> results = new ArrayList<>();
    for (String key : section.getKeys(false)) {
      ConfigurationSection home = section.getConfigurationSection(key);
      if (home == null) {
        continue;
      }
      String name = home.getString("name", key);
      String world = home.getString("world", "world");
      double x = home.getDouble("x");
      double y = home.getDouble("y");
      double z = home.getDouble("z");
      float yaw = (float) home.getDouble("yaw");
      float pitch = (float) home.getDouble("pitch");
      if (plugin.getServer().getWorld(world) == null) {
        continue;
      }
      Location location = new Location(plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
      results.add(new PlayerHome(key, name, location, true));
    }
    return results;
  }

  public PlayerHome getHome(UUID playerId, String slotId) {
    ConfigurationSection home = config.getConfigurationSection("players." + playerId + ".homes." + slotId);
    if (home == null) {
      return null;
    }
    String name = home.getString("name", slotId);
    String world = home.getString("world", "world");
    double x = home.getDouble("x");
    double y = home.getDouble("y");
    double z = home.getDouble("z");
    float yaw = (float) home.getDouble("yaw");
    float pitch = (float) home.getDouble("pitch");
    if (plugin.getServer().getWorld(world) == null) {
      return null;
    }
    Location location = new Location(plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
    return new PlayerHome(slotId, name, location, true);
  }

  public void setHome(UUID playerId, String slotId, String name, Location location) {
    String base = "players." + playerId + ".homes." + slotId;
    config.set(base + ".name", name);
    config.set(base + ".world", location.getWorld() != null ? location.getWorld().getName() : "world");
    config.set(base + ".x", location.getX());
    config.set(base + ".y", location.getY());
    config.set(base + ".z", location.getZ());
    config.set(base + ".yaw", location.getYaw());
    config.set(base + ".pitch", location.getPitch());
    save();
  }

  public void removeHome(UUID playerId, String slotId) {
    config.set("players." + playerId + ".homes." + slotId, null);
    save();
  }
}
