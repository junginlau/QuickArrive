package com.quickarrive.teleport;

import com.quickarrive.QuickArrivePlugin;
import com.quickarrive.store.PlayerDataStore;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportManager implements Listener {
  private final QuickArrivePlugin plugin;
  private final PlayerDataStore playerDataStore;
  private final Map<UUID, PendingSelfTeleport> selfTeleports = new ConcurrentHashMap<>();
  private final Map<UUID, PendingPlayerRequest> playerRequests = new ConcurrentHashMap<>();
  private final Map<UUID, PendingDelayedTeleport> delayedTeleports = new ConcurrentHashMap<>();
  private final Set<UUID> bypassNextTeleport = ConcurrentHashMap.newKeySet();

  public TeleportManager(QuickArrivePlugin plugin, PlayerDataStore playerDataStore) {
    this.plugin = plugin;
    this.playerDataStore = playerDataStore;
  }

  @EventHandler
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    Player player = event.getPlayer();
    if (bypassNextTeleport.remove(player.getUniqueId())) {
      return;
    }
    if (isBypass(player)) {
      return;
    }
    if (event.getTo() == null) {
      return;
    }

    PendingSelfTeleport pending = new PendingSelfTeleport(event.getTo(), System.currentTimeMillis(), event.getCause().name());
    selfTeleports.put(player.getUniqueId(), pending);
    event.setCancelled(true);

    int timeout = plugin.getConfig().getInt("teleport.timeout-seconds", 60);
    player.sendMessage(plugin.withPrefix(plugin.message("self-confirm", Map.of("seconds", String.valueOf(timeout)))));
    UUID id = player.getUniqueId();
    Bukkit.getScheduler().runTaskLater(plugin, () -> expireSelfTeleport(id, pending.createdAt()), timeout * 20L);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!plugin.getConfig().getBoolean("teleport.cancel-on-move", false)) {
      return;
    }
    if (event.getTo() == null) {
      return;
    }
    if (event.getFrom().getBlockX() == event.getTo().getBlockX()
        && event.getFrom().getBlockY() == event.getTo().getBlockY()
        && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
      return;
    }
    cancelSelfTeleport(event.getPlayer(), "request-canceled");
    cancelDelayedTeleport(event.getPlayer());
  }

  @EventHandler
  public void onDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }
    if (!plugin.getConfig().getBoolean("teleport.cancel-on-damage", false)) {
      return;
    }
    cancelSelfTeleport(player, "request-canceled");
    cancelDelayedTeleport(player);
  }

  public void sendPlayerRequest(Player requester, Player target, TeleportRequestType type) {
    if (requester.getUniqueId().equals(target.getUniqueId())) {
      requester.sendMessage(plugin.withPrefix(plugin.message("cannot-teleport-self")));
      return;
    }
    if (playerRequests.containsKey(target.getUniqueId())) {
      requester.sendMessage(plugin.withPrefix(plugin.message("request-pending")));
      return;
    }
    if (playerDataStore.isBlacklisted(target.getUniqueId(), requester.getUniqueId())) {
      requester.sendMessage(plugin.withPrefix(plugin.message("request-blocked")));
      return;
    }
    int timeout = plugin.getConfig().getInt("teleport.timeout-seconds", 60);
    PendingPlayerRequest request = new PendingPlayerRequest(requester.getUniqueId(), System.currentTimeMillis(), type);
    playerRequests.put(target.getUniqueId(), request);

    requester.sendMessage(plugin.withPrefix(plugin.message("request-sent", Map.of("player", target.getName()))));
    String direction = type == TeleportRequestType.TPA ? "過去" : "過來";
    target.sendMessage(plugin.withPrefix(plugin.message("request-received", Map.of("player", requester.getName(), "direction", direction))));
    sendRequestButtons(target, requester.getName(), direction);
    Bukkit.getScheduler().runTaskLater(plugin, () -> expireRequest(target.getUniqueId()), timeout * 20L);
  }

  public void accept(Player player) {
    if (acceptPlayerRequest(player)) {
      return;
    }
    acceptSelfTeleport(player);
  }

  public void deny(Player player) {
    if (denyPlayerRequest(player)) {
      return;
    }
    denySelfTeleport(player);
  }

  public void teleportNow(Player player, Location location) {
    scheduleTeleport(player, location);
  }

  private boolean acceptPlayerRequest(Player player) {
    PendingPlayerRequest request = playerRequests.remove(player.getUniqueId());
    if (request == null) {
      return false;
    }
    if (isExpired(request.createdAt())) {
      player.sendMessage(plugin.withPrefix(plugin.message("request-expired")));
      return true;
    }
    Player requester = Bukkit.getPlayer(request.requesterId());
    if (requester == null) {
      player.sendMessage(plugin.withPrefix(plugin.message("request-expired")));
      return true;
    }
    if (request.type() == TeleportRequestType.TPA) {
      scheduleTeleport(requester, player.getLocation());
    } else {
      scheduleTeleport(player, requester.getLocation());
    }
    requester.sendMessage(plugin.withPrefix(plugin.message("request-accepted")));
    player.sendMessage(plugin.withPrefix(plugin.message("request-accepted")));
    return true;
  }

  private boolean denyPlayerRequest(Player player) {
    PendingPlayerRequest request = playerRequests.remove(player.getUniqueId());
    if (request == null) {
      return false;
    }
    Player requester = Bukkit.getPlayer(request.requesterId());
    if (requester != null) {
      requester.sendMessage(plugin.withPrefix(plugin.message("request-denied")));
    }
    player.sendMessage(plugin.withPrefix(plugin.message("request-denied")));
    return true;
  }

  private void acceptSelfTeleport(Player player) {
    PendingSelfTeleport pending = selfTeleports.remove(player.getUniqueId());
    if (pending == null) {
      player.sendMessage(plugin.withPrefix(plugin.message("no-pending")));
      return;
    }
    if (isExpired(pending.createdAt())) {
      player.sendMessage(plugin.withPrefix(plugin.message("self-expired")));
      return;
    }
    scheduleTeleport(player, pending.destination());
    player.sendMessage(plugin.withPrefix(plugin.message("self-accepted")));
  }

  private void denySelfTeleport(Player player) {
    PendingSelfTeleport pending = selfTeleports.remove(player.getUniqueId());
    if (pending == null) {
      player.sendMessage(plugin.withPrefix(plugin.message("no-pending")));
      return;
    }
    player.sendMessage(plugin.withPrefix(plugin.message("self-denied")));
  }

  private void cancelSelfTeleport(Player player, String messageKey) {
    if (selfTeleports.remove(player.getUniqueId()) != null) {
      player.sendMessage(plugin.withPrefix(plugin.message(messageKey)));
    }
  }

  private void expireSelfTeleport(UUID playerId, long createdAt) {
    PendingSelfTeleport pending = selfTeleports.get(playerId);
    if (pending == null || pending.createdAt() != createdAt) {
      return;
    }
    if (isExpired(pending.createdAt())) {
      selfTeleports.remove(playerId);
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        player.sendMessage(plugin.withPrefix(plugin.message("self-expired")));
      }
    }
  }

  private void expireRequest(UUID targetId) {
    PendingPlayerRequest request = playerRequests.get(targetId);
    if (request == null) {
      return;
    }
    if (isExpired(request.createdAt())) {
      playerRequests.remove(targetId);
      Player target = Bukkit.getPlayer(targetId);
      if (target != null) {
        target.sendMessage(plugin.withPrefix(plugin.message("request-expired")));
      }
      Player requester = Bukkit.getPlayer(request.requesterId());
      if (requester != null) {
        requester.sendMessage(plugin.withPrefix(plugin.message("request-expired")));
      }
    }
  }

  private boolean isExpired(long createdAt) {
    int timeout = plugin.getConfig().getInt("teleport.timeout-seconds", 60);
    return System.currentTimeMillis() - createdAt > timeout * 1000L;
  }

  private boolean isBypass(Player player) {
    if (player.hasPermission("quickarrive.bypass")) {
      return true;
    }
    for (String name : plugin.getConfig().getStringList("bypass.players")) {
      if (name.equalsIgnoreCase(player.getName())) {
        return true;
      }
    }
    return false;
  }

  private void sendRequestButtons(Player target, String requesterName, String direction) {
    String prompt = plugin.message("request-received-chat", Map.of("player", requesterName, "direction", direction));
    String acceptLabel = plugin.message("request-accept-button");
    String denyLabel = plugin.message("request-deny-button");

    TextComponent header = new TextComponent(ChatColor.translateAlternateColorCodes('&', prompt));
    TextComponent accept = new TextComponent(ChatColor.translateAlternateColorCodes('&', acceptLabel));
    accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
    accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("點擊接受傳送").create()));

    TextComponent deny = new TextComponent(ChatColor.translateAlternateColorCodes('&', denyLabel));
    deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"));
    deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("點擊拒絕傳送").create()));

    TextComponent line = new TextComponent("");
    line.addExtra(accept);
    line.addExtra(" ");
    line.addExtra(deny);

    target.spigot().sendMessage(header);
    target.spigot().sendMessage(line);
  }

  private void scheduleTeleport(Player player, Location location) {
    if (location == null) {
      return;
    }
    int delaySeconds = plugin.getConfig().getInt("teleport.delay-seconds", 0);
    if (delaySeconds <= 0) {
      bypassNextTeleport.add(player.getUniqueId());
      player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
      return;
    }
    cancelDelayedTeleport(player);
    UUID playerId = player.getUniqueId();
    PendingDelayedTeleport pending = new PendingDelayedTeleport(location, delaySeconds);
    delayedTeleports.put(playerId, pending);
    player.sendMessage(plugin.withPrefix(plugin.message("teleport-delay", Map.of("seconds", String.valueOf(delaySeconds)))));

    new BukkitRunnable() {
      private int remaining = delaySeconds;

      @Override
      public void run() {
        PendingDelayedTeleport current = delayedTeleports.get(playerId);
        if (current == null || current != pending) {
          cancel();
          return;
        }
        if (!player.isOnline()) {
          delayedTeleports.remove(playerId);
          cancel();
          return;
        }
        remaining--;
        if (remaining > 0) {
          player.sendMessage(plugin.withPrefix(plugin.message("teleport-delay", Map.of("seconds", String.valueOf(remaining)))));
          return;
        }
        delayedTeleports.remove(playerId);
        bypassNextTeleport.add(playerId);
        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        cancel();
      }
    }.runTaskTimer(plugin, 20L, 20L);
  }

  private void cancelDelayedTeleport(Player player) {
    if (delayedTeleports.remove(player.getUniqueId()) != null) {
      player.sendMessage(plugin.withPrefix(plugin.message("teleport-delay-canceled")));
    }
  }

  private record PendingDelayedTeleport(Location destination, int delaySeconds) {}
}
