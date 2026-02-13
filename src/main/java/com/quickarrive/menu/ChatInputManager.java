package com.quickarrive.menu;

import com.quickarrive.QuickArrivePlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatInputManager implements Listener {
  private final QuickArrivePlugin plugin;
  private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

  public ChatInputManager(QuickArrivePlugin plugin) {
    this.plugin = plugin;
  }

  public void request(Player player, String prompt, Consumer<String> handler) {
    pending.put(player.getUniqueId(), new PendingInput(prompt, handler));
    player.sendMessage(plugin.withPrefix(prompt));
  }

  public void clear(Player player) {
    pending.remove(player.getUniqueId());
  }

  @EventHandler
  public void onChat(AsyncPlayerChatEvent event) {
    PendingInput input = pending.remove(event.getPlayer().getUniqueId());
    if (input == null) {
      return;
    }
    event.setCancelled(true);
    String message = event.getMessage();
    if (message.equalsIgnoreCase("cancel")) {
      event.getPlayer().sendMessage(plugin.withPrefix(plugin.message("input-canceled")));
      return;
    }
    Bukkit.getScheduler().runTask(plugin, () -> input.handler().accept(message));
  }

  private record PendingInput(String prompt, Consumer<String> handler) {}
}
