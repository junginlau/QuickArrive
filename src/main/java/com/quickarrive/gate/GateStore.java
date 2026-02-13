package com.quickarrive.gate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quickarrive.QuickArrivePlugin;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GateStore {
  private final QuickArrivePlugin plugin;
  private final File gateFile;

  public GateStore(QuickArrivePlugin plugin) {
    this.plugin = plugin;
    File pluginsDir = plugin.getDataFolder().getParentFile();
    this.gateFile = new File(new File(pluginsDir, "AncientGates"), "gates.json");
  }

  public List<GateDefinition> loadGates() {
    if (!gateFile.exists()) {
      return Collections.emptyList();
    }
    List<GateDefinition> results = new ArrayList<>();
    try (FileReader reader = new FileReader(gateFile)) {
      JsonElement element = JsonParser.parseReader(reader);
      if (!element.isJsonObject()) {
        return results;
      }
      JsonObject root = element.getAsJsonObject();
      for (String key : root.keySet()) {
        JsonObject gate = root.getAsJsonObject(key);
        GateLocation from = readFirstLocation(gate.getAsJsonArray("froms"));
        GateLocation to = readFirstLocation(gate.getAsJsonArray("tos"));
        String material = gate.has("material") ? gate.get("material").getAsString() : "PORTAL";
        if (to != null) {
          results.add(new GateDefinition(key, from, to, material));
        }
      }
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
    return results;
  }

  public boolean gatesFileExists() {
    return gateFile.exists();
  }

  private GateLocation readFirstLocation(JsonArray array) {
    if (array == null || array.isEmpty()) {
      return null;
    }
    JsonObject obj = array.get(0).getAsJsonObject();
    String world = obj.has("world") ? obj.get("world").getAsString() : "world";
    double x = obj.has("x") ? obj.get("x").getAsDouble() : 0.0;
    double y = obj.has("y") ? obj.get("y").getAsDouble() : 0.0;
    double z = obj.has("z") ? obj.get("z").getAsDouble() : 0.0;
    float yaw = obj.has("yaw") ? obj.get("yaw").getAsFloat() : 0.0f;
    float pitch = obj.has("pitch") ? obj.get("pitch").getAsFloat() : 0.0f;
    return new GateLocation(world, x, y, z, yaw, pitch);
  }
}
