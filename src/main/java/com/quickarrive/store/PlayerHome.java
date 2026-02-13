package com.quickarrive.store;

import org.bukkit.Location;

public record PlayerHome(String slotId, String name, Location location, boolean editable) {}
