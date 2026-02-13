package com.quickarrive.home;

import com.quickarrive.QuickArrivePlugin;
import com.quickarrive.store.PlayerHome;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class EssentialsHomeProvider {
  private final QuickArrivePlugin plugin;

  public EssentialsHomeProvider(QuickArrivePlugin plugin) {
    this.plugin = plugin;
  }

  public List<PlayerHome> getHomes(UUID playerId, int max) {
    File dataFile = new File(new File(plugin.getDataFolder().getParentFile(), "Essentials"), "userdata" + File.separator + playerId + ".yml");
    if (!dataFile.exists()) {
      return Collections.emptyList();
    }
    YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
    ConfigurationSection homes = config.getConfigurationSection("homes");
    if (homes == null) {
      return Collections.emptyList();
    }
    List<PlayerHome> results = new ArrayList<>();
    for (String key : homes.getKeys(false)) {
      if (results.size() >= max) {
        break;
      }
      ConfigurationSection home = homes.getConfigurationSection(key);
      if (home == null) {
        continue;
      }
      String worldName = home.getString("world-name");
      World world = worldName != null ? Bukkit.getWorld(worldName) : null;
      if (world == null) {
        String worldId = home.getString("world");
        if (worldId != null) {
          try {
            world = Bukkit.getWorld(UUID.fromString(worldId));
          } catch (IllegalArgumentException ignored) {
          }
        }
      }
      if (world == null) {
        continue;
      }
      double x = home.getDouble("x");
      double y = home.getDouble("y");
      double z = home.getDouble("z");
      float yaw = (float) home.getDouble("yaw");
      float pitch = (float) home.getDouble("pitch");
      Location location = new Location(world, x, y, z, yaw, pitch);
      results.add(new PlayerHome("essentials:" + key, key, location, false));
    }
    return results;
  }
}
