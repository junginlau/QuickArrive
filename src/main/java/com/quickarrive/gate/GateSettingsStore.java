package com.quickarrive.gate;

import com.quickarrive.QuickArrivePlugin;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class GateSettingsStore {
  private final QuickArrivePlugin plugin;
  private final File file;
  private YamlConfiguration config;

  public GateSettingsStore(QuickArrivePlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "gates.yml");
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

  public GateSettings getSettings(String gateId) {
    boolean enabled = config.getBoolean("gates." + gateId + ".enabled", false);
    String displayName = config.getString("gates." + gateId + ".display.name");
    String displayMaterial = config.getString("gates." + gateId + ".display.material");
    String displayId = config.getString("gates." + gateId + ".display.id");
    GateLocation override = getOverride(gateId);
    return new GateSettings(enabled, displayName, displayMaterial, displayId, override);
  }

  public void setEnabled(String gateId, boolean enabled) {
    config.set("gates." + gateId + ".enabled", enabled);
    save();
  }

  public void setDisplayName(String gateId, String name) {
    config.set("gates." + gateId + ".display.name", name);
    save();
  }

  public void setDisplayMaterial(String gateId, String material) {
    config.set("gates." + gateId + ".display.material", material);
    save();
  }

  public void setDisplayId(String gateId, String displayId) {
    config.set("gates." + gateId + ".display.id", displayId);
    save();
  }

  public GateLocation getOverride(String gateId) {
    ConfigurationSection section = config.getConfigurationSection("gates." + gateId + ".override");
    if (section == null) {
      return null;
    }
    String world = section.getString("world");
    if (world == null || world.isEmpty()) {
      return null;
    }
    double x = section.getDouble("x");
    double y = section.getDouble("y");
    double z = section.getDouble("z");
    float yaw = (float) section.getDouble("yaw");
    float pitch = (float) section.getDouble("pitch");
    return new GateLocation(world, x, y, z, yaw, pitch);
  }

  public void setOverride(String gateId, GateLocation location) {
    String base = "gates." + gateId + ".override";
    config.set(base + ".world", location.world());
    config.set(base + ".x", location.x());
    config.set(base + ".y", location.y());
    config.set(base + ".z", location.z());
    config.set(base + ".yaw", location.yaw());
    config.set(base + ".pitch", location.pitch());
    save();
  }

  public void clearOverride(String gateId) {
    config.set("gates." + gateId + ".override", null);
    save();
  }
}
