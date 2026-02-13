package com.quickarrive.teleport;

import org.bukkit.Location;

public record PendingSelfTeleport(Location destination, long createdAt, String cause) {}
