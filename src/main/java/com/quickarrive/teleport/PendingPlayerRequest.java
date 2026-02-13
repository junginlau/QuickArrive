package com.quickarrive.teleport;

import java.util.UUID;

public record PendingPlayerRequest(UUID requesterId, long createdAt, TeleportRequestType type) {}
