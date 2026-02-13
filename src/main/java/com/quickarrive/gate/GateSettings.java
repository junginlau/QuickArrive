package com.quickarrive.gate;

public record GateSettings(boolean enabled, String displayName, String displayMaterial, String displayId, GateLocation overrideTo) {}
