package com.quickarrive.store;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record StoredLocation(String name, String world, double x, double y, double z, float yaw, float pitch, boolean enabled) {
  public Location toLocation() {
    World targetWorld = Bukkit.getWorld(world);
    if (targetWorld == null) {
      return null;
    }
    Location location = new Location(targetWorld, x, y, z, yaw, pitch);
    return location;
  }
}
