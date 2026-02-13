package com.quickarrive.store;

import com.quickarrive.QuickArrivePlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class LocationStore {
  private final QuickArrivePlugin plugin;

  public LocationStore(QuickArrivePlugin plugin) {
    this.plugin = plugin;
  }

  public List<StoredLocation> getEnabledLocations() {
    List<StoredLocation> results = new ArrayList<>();
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("locations");
    if (section == null) {
      return results;
    }
    for (String key : section.getKeys(false)) {
      StoredLocation location = getLocation(key);
      if (location != null && location.enabled()) {
        results.add(location);
      }
    }
    return results;
  }

  public List<StoredLocation> getAllLocations() {
    List<StoredLocation> results = new ArrayList<>();
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("locations");
    if (section == null) {
      return results;
    }
    for (String key : section.getKeys(false)) {
      StoredLocation location = getLocation(key);
      if (location != null) {
        results.add(location);
      }
    }
    return results;
  }

  public StoredLocation getLocation(String name) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("locations." + name);
    if (section == null) {
      return null;
    }
    String world = section.getString("world", "world");
    double x = section.getDouble("x");
    double y = section.getDouble("y");
    double z = section.getDouble("z");
    float yaw = (float) section.getDouble("yaw");
    float pitch = (float) section.getDouble("pitch");
    boolean enabled = section.getBoolean("enabled", false);
    return new StoredLocation(name, world, x, y, z, yaw, pitch, enabled);
  }

  public void setLocation(String name, Location location, boolean enabled) {
    plugin.getConfig().set("locations." + name + ".world", location.getWorld() != null ? location.getWorld().getName() : "world");
    plugin.getConfig().set("locations." + name + ".x", location.getX());
    plugin.getConfig().set("locations." + name + ".y", location.getY());
    plugin.getConfig().set("locations." + name + ".z", location.getZ());
    plugin.getConfig().set("locations." + name + ".yaw", location.getYaw());
    plugin.getConfig().set("locations." + name + ".pitch", location.getPitch());
    plugin.getConfig().set("locations." + name + ".enabled", enabled);
    plugin.saveConfig();
  }

  public boolean removeLocation(String name) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("locations");
    if (section == null || !section.contains(name)) {
      return false;
    }
    plugin.getConfig().set("locations." + name, null);
    plugin.saveConfig();
    return true;
  }

  public boolean toggleLocation(String name) {
    StoredLocation location = getLocation(name);
    if (location == null) {
      return false;
    }
    boolean enabled = !location.enabled();
    plugin.getConfig().set("locations." + name + ".enabled", enabled);
    plugin.saveConfig();
    return enabled;
  }

  public Set<String> getLocationNames() {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("locations");
    if (section == null) {
      return Collections.emptySet();
    }
    return new TreeSet<>(section.getKeys(false));
  }

  public String describeLocations(QuickArrivePlugin plugin) {
    Set<String> names = getLocationNames();
    if (names.isEmpty()) {
      return plugin.withPrefix(plugin.message("menu-empty"));
    }
    StringBuilder builder = new StringBuilder();
    builder.append(plugin.withPrefix(plugin.colorize("&e目前傳送點: ")));
    boolean first = true;
    for (String name : names) {
      if (!first) {
        builder.append(", ");
      }
      StoredLocation location = getLocation(name);
      if (location != null && location.enabled()) {
        builder.append(plugin.colorize("&a" + name));
      } else {
        builder.append(plugin.colorize("&c" + name));
      }
      first = false;
    }
    return builder.toString();
  }
}
