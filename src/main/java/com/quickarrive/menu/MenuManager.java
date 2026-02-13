package com.quickarrive.menu;

import com.quickarrive.QuickArrivePlugin;
import com.quickarrive.gate.GateDefinition;
import com.quickarrive.gate.GateLocation;
import com.quickarrive.gate.GateSettings;
import com.quickarrive.gate.GateSettingsStore;
import com.quickarrive.gate.GateStore;
import com.quickarrive.home.EssentialsHomeProvider;
import com.quickarrive.store.PlayerDataStore;
import com.quickarrive.store.PlayerHome;
import com.quickarrive.teleport.TeleportManager;
import com.quickarrive.teleport.TeleportRequestType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class MenuManager implements Listener {
  private static final int SLOT_BACK = 45;
  private static final int SLOT_CLOSE = 49;
  private static final int SLOT_ADMIN = 53;
  private static final int SLOT_PREV = 0;
  private static final int SLOT_NEXT = 7;
  private static final int SLOT_BLACKLIST = 8;
  private static final int SLOT_WORLDS = 8;

  private final QuickArrivePlugin plugin;
  private final TeleportManager teleportManager;
  private final PlayerDataStore playerDataStore;
  private final GateStore gateStore;
  private final GateSettingsStore gateSettingsStore;
  private final EssentialsHomeProvider essentialsHomeProvider;
  private final ChatInputManager chatInputManager;
  private final NamespacedKey toolKey;

  public MenuManager(
      QuickArrivePlugin plugin,
      TeleportManager teleportManager,
      PlayerDataStore playerDataStore,
      GateStore gateStore,
      GateSettingsStore gateSettingsStore,
      EssentialsHomeProvider essentialsHomeProvider,
      ChatInputManager chatInputManager) {
    this.plugin = plugin;
    this.teleportManager = teleportManager;
    this.playerDataStore = playerDataStore;
    this.gateStore = gateStore;
    this.gateSettingsStore = gateSettingsStore;
    this.essentialsHomeProvider = essentialsHomeProvider;
    this.chatInputManager = chatInputManager;
    this.toolKey = new NamespacedKey(plugin, "menu_tool");
  }

  public void openMainMenu(Player player) {
    player.openInventory(buildMainMenu(player));
  }

  public void openAdminMenu(Player player, int page) {
    player.openInventory(buildGateAdminMenu(page));
  }

  public void openWorldsMenu(Player player, int page) {
    player.openInventory(buildWorldsMenu(page));
  }

  public void giveMenuTool(Player player) {
    ItemStack tool = createMenuTool();
    player.getInventory().addItem(tool);
  }

  private Inventory buildMainMenu(Player player) {
    int size = plugin.getConfig().getInt("menu.size", 54);
    String title = plugin.colorize(plugin.getConfig().getString("menu.title", "QuickArrive"));
    MainMenuHolder holder = new MainMenuHolder();
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.setInventory(inventory);

    fillBackground(inventory);

    inventory.setItem(20, createButton(Material.PLAYER_HEAD, plugin.getConfig().getString("menu.buttons.players", "Players")));
    inventory.setItem(22, createButton(Material.ENDER_PEARL, plugin.getConfig().getString("menu.buttons.homes", "Homes")));
    inventory.setItem(24, createButton(Material.NETHER_STAR, plugin.getConfig().getString("menu.buttons.gates", "Gates")));

    inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.close", "Close")));
    if (player.hasPermission("quickarrive.menu.admin")) {
      inventory.setItem(SLOT_ADMIN, createButton(Material.COMPARATOR, plugin.getConfig().getString("menu.buttons.admin", "Admin")));
    }

    return inventory;
  }

  private Inventory buildPlayersMenu(Player viewer, int page) {
    int size = plugin.getConfig().getInt("menu.size", 54);
    String title = plugin.colorize(plugin.getConfig().getString("menu.players-title", "Players"));
    PlayersMenuHolder holder = new PlayersMenuHolder(page);
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.setInventory(inventory);

    fillBackground(inventory);

    List<Player> players = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.getUniqueId().equals(viewer.getUniqueId())) {
        players.add(player);
      }
    }

    int itemsPerPage = 36;
    int maxPage = players.isEmpty() ? 0 : (players.size() - 1) / itemsPerPage;
    int currentPage = Math.max(0, Math.min(page, maxPage));

    int start = currentPage * itemsPerPage;
    int end = Math.min(players.size(), start + itemsPerPage);
    for (int i = start; i < end; i++) {
      int slot = 9 + (i - start);
      Player target = players.get(i);
      ItemStack head = createPlayerItem(target, viewer);
      inventory.setItem(slot, head);
      holder.entries.put(slot, target.getUniqueId());
    }

    if (currentPage > 0) {
      inventory.setItem(SLOT_PREV, createButton(Material.ARROW, "&a上一頁"));
    }
    if (currentPage < maxPage) {
      inventory.setItem(SLOT_NEXT, createButton(Material.ARROW, "&a下一頁"));
    }
    inventory.setItem(SLOT_BLACKLIST, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.blacklist", "Blacklist")));
    inventory.setItem(SLOT_BACK, createButton(Material.ARROW, plugin.getConfig().getString("menu.buttons.back", "Back")));
    inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.close", "Close")));

    return inventory;
  }

  private Inventory buildBlacklistMenu(Player viewer, int page) {
    int size = plugin.getConfig().getInt("menu.size", 54);
    String title = plugin.colorize(plugin.getConfig().getString("menu.blacklist-title", "Blacklist"));
    BlacklistMenuHolder holder = new BlacklistMenuHolder(page);
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.setInventory(inventory);

    fillBackground(inventory);

    List<Player> players = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.getUniqueId().equals(viewer.getUniqueId())) {
        players.add(player);
      }
    }

    int itemsPerPage = 36;
    int maxPage = players.isEmpty() ? 0 : (players.size() - 1) / itemsPerPage;
    int currentPage = Math.max(0, Math.min(page, maxPage));

    int start = currentPage * itemsPerPage;
    int end = Math.min(players.size(), start + itemsPerPage);
    for (int i = start; i < end; i++) {
      int slot = 9 + (i - start);
      Player target = players.get(i);
      ItemStack head = createBlacklistItem(target, viewer);
      inventory.setItem(slot, head);
      holder.entries.put(slot, target.getUniqueId());
    }

    if (currentPage > 0) {
      inventory.setItem(SLOT_PREV, createButton(Material.ARROW, "&a上一頁"));
    }
    if (currentPage < maxPage) {
      inventory.setItem(SLOT_NEXT, createButton(Material.ARROW, "&a下一頁"));
    }
    inventory.setItem(SLOT_BACK, createButton(Material.ARROW, plugin.getConfig().getString("menu.buttons.back", "Back")));
    inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.close", "Close")));

    return inventory;
  }

  private Inventory buildHomesMenu(Player player) {
    int size = plugin.getConfig().getInt("menu.size", 54);
    String title = plugin.colorize(plugin.getConfig().getString("menu.homes-title", "Homes"));
    HomesMenuHolder holder = new HomesMenuHolder();
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.setInventory(inventory);

    fillBackground(inventory);

    List<PlayerHome> essentialsHomes = essentialsHomeProvider.getHomes(player.getUniqueId(), 2);

    List<Integer> slots = List.of(20, 24);
    int slotIndex = 0;
    for (PlayerHome home : essentialsHomes) {
      if (slotIndex >= slots.size()) {
        break;
      }
      int slot = slots.get(slotIndex++);
      inventory.setItem(slot, createHomeItem(home, true));
      holder.entries.put(slot, home);
    }

    List<String> quickSlots = new ArrayList<>();
    quickSlots.add("slot1");
    quickSlots.add("slot2");

    for (String slotId : quickSlots) {
      if (slotIndex >= slots.size()) {
        break;
      }
      int slot = slots.get(slotIndex++);
      PlayerHome home = playerDataStore.getHome(player.getUniqueId(), slotId);
      if (home != null) {
        inventory.setItem(slot, createHomeItem(home, false));
        holder.entries.put(slot, home);
      } else {
        inventory.setItem(slot, createEmptyHomeItem(slotId));
        holder.emptySlots.put(slot, slotId);
      }
    }

    inventory.setItem(SLOT_BACK, createButton(Material.ARROW, plugin.getConfig().getString("menu.buttons.back", "Back")));
    inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.close", "Close")));

    return inventory;
  }

  private Inventory buildGatesMenu(int page) {
    int size = plugin.getConfig().getInt("menu.size", 54);
    String title = plugin.colorize(plugin.getConfig().getString("menu.gates-title", "Gates"));
    GatesMenuHolder holder = new GatesMenuHolder(page);
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.setInventory(inventory);

    fillBackground(inventory);

    List<GateDefinition> allGates = gateStore.loadGates();
    List<GateDefinition> enabledGates = new ArrayList<>();
    for (GateDefinition gate : allGates) {
      if (gateSettingsStore.getSettings(gate.id()).enabled()) {
        enabledGates.add(gate);
      }
    }

    int itemsPerPage = 36;
    int maxPage = enabledGates.isEmpty() ? 0 : (enabledGates.size() - 1) / itemsPerPage;
    int currentPage = Math.max(0, Math.min(page, maxPage));

    int start = currentPage * itemsPerPage;
    int end = Math.min(enabledGates.size(), start + itemsPerPage);
    for (int i = start; i < end; i++) {
      int slot = 9 + (i - start);
      GateDefinition gate = enabledGates.get(i);
      inventory.setItem(slot, createGateItem(gate));
      holder.entries.put(slot, gate.id());
    }

    if (currentPage > 0) {
      inventory.setItem(SLOT_PREV, createButton(Material.ARROW, "&a上一頁"));
    }
    if (currentPage < maxPage) {
      inventory.setItem(SLOT_NEXT, createButton(Material.ARROW, "&a下一頁"));
    }
    inventory.setItem(SLOT_BACK, createButton(Material.ARROW, plugin.getConfig().getString("menu.buttons.back", "Back")));
    inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.close", "Close")));

    return inventory;
  }

  private Inventory buildGateAdminMenu(int page) {
    int size = plugin.getConfig().getInt("menu.size", 54);
    String title = plugin.colorize(plugin.getConfig().getString("menu.gate-admin-title", "Gate Admin"));
    GateAdminMenuHolder holder = new GateAdminMenuHolder(page);
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.setInventory(inventory);

    fillBackground(inventory);

    List<GateDefinition> gates = gateStore.loadGates();

    int itemsPerPage = 36;
    int maxPage = gates.isEmpty() ? 0 : (gates.size() - 1) / itemsPerPage;
    int currentPage = Math.max(0, Math.min(page, maxPage));

    int start = currentPage * itemsPerPage;
    int end = Math.min(gates.size(), start + itemsPerPage);
    for (int i = start; i < end; i++) {
      int slot = 9 + (i - start);
      GateDefinition gate = gates.get(i);
      inventory.setItem(slot, createGateAdminItem(gate));
      holder.entries.put(slot, gate.id());
    }

    if (currentPage > 0) {
      inventory.setItem(SLOT_PREV, createButton(Material.ARROW, "&a上一頁"));
    }
    if (currentPage < maxPage) {
      inventory.setItem(SLOT_NEXT, createButton(Material.ARROW, "&a下一頁"));
    }
    inventory.setItem(SLOT_WORLDS, createButton(Material.GRASS_BLOCK, plugin.getConfig().getString("menu.buttons.worlds", "Worlds")));
    inventory.setItem(SLOT_BACK, createButton(Material.ARROW, plugin.getConfig().getString("menu.buttons.back", "Back")));
    inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.close", "Close")));

    return inventory;
  }

  private Inventory buildWorldsMenu(int page) {
    int size = plugin.getConfig().getInt("menu.size", 54);
    String title = plugin.colorize(plugin.getConfig().getString("menu.worlds-title", "Worlds"));
    WorldsMenuHolder holder = new WorldsMenuHolder(page);
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.setInventory(inventory);

    fillBackground(inventory);

    List<World> worlds = new ArrayList<>(Bukkit.getWorlds());
    int itemsPerPage = 36;
    int maxPage = worlds.isEmpty() ? 0 : (worlds.size() - 1) / itemsPerPage;
    int currentPage = Math.max(0, Math.min(page, maxPage));

    int start = currentPage * itemsPerPage;
    int end = Math.min(worlds.size(), start + itemsPerPage);
    for (int i = start; i < end; i++) {
      int slot = 9 + (i - start);
      World world = worlds.get(i);
      inventory.setItem(slot, createWorldItem(world));
      holder.entries.put(slot, world.getName());
    }

    if (currentPage > 0) {
      inventory.setItem(SLOT_PREV, createButton(Material.ARROW, "&a上一頁"));
    }
    if (currentPage < maxPage) {
      inventory.setItem(SLOT_NEXT, createButton(Material.ARROW, "&a下一頁"));
    }
    inventory.setItem(SLOT_BACK, createButton(Material.ARROW, plugin.getConfig().getString("menu.buttons.back", "Back")));
    inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.close", "Close")));

    return inventory;
  }

  private Inventory buildGateEditMenu(String gateId) {
    int size = plugin.getConfig().getInt("menu.size", 54);
    String title = plugin.colorize(plugin.getConfig().getString("menu.gate-edit-title", "Edit Gate"));
    GateEditMenuHolder holder = new GateEditMenuHolder(gateId);
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.setInventory(inventory);

    fillBackground(inventory);

    GateSettings settings = gateSettingsStore.getSettings(gateId);
    GateDefinition gate = findGate(gateId);
    GateLocation location = settings.overrideTo() != null ? settings.overrideTo() : (gate != null ? gate.to() : null);

    inventory.setItem(20, createButton(Material.NAME_TAG, "&e名稱: " + (settings.displayName() != null ? settings.displayName() : gateId)));
    inventory.setItem(22, createButton(Material.ITEM_FRAME, "&e圖示材質: " + (settings.displayMaterial() != null ? settings.displayMaterial() : "NETHER_STAR")));
    inventory.setItem(24, createButton(Material.PAPER, "&e顯示ID: " + (settings.displayId() != null ? settings.displayId() : gateId)));

    inventory.setItem(29, createButton(Material.PAPER, "&e世界: " + (location != null ? location.world() : "unknown")));
    inventory.setItem(30, createButton(Material.PAPER, "&eX: " + (location != null ? String.format("%.2f", location.x()) : "0")));
    inventory.setItem(31, createButton(Material.PAPER, "&eY: " + (location != null ? String.format("%.2f", location.y()) : "0")));
    inventory.setItem(32, createButton(Material.PAPER, "&eZ: " + (location != null ? String.format("%.2f", location.z()) : "0")));
    inventory.setItem(33, createButton(Material.PAPER, "&eYaw: " + (location != null ? String.format("%.2f", location.yaw()) : "0")));
    inventory.setItem(34, createButton(Material.PAPER, "&ePitch: " + (location != null ? String.format("%.2f", location.pitch()) : "0")));

    inventory.setItem(40, createButton(Material.BARRIER, "&c使用預設傳送點"));

    inventory.setItem(SLOT_BACK, createButton(Material.ARROW, plugin.getConfig().getString("menu.buttons.back", "Back")));
    inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER, plugin.getConfig().getString("menu.buttons.close", "Close")));

    return inventory;
  }

  private void fillBackground(Inventory inventory) {
    Material material = Material.matchMaterial(plugin.getConfig().getString("menu.background-material", "BLACK_STAINED_GLASS_PANE"));
    if (material == null) {
      material = Material.BLACK_STAINED_GLASS_PANE;
    }
    ItemStack filler = new ItemStack(material);
    ItemMeta meta = filler.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(" ");
      filler.setItemMeta(meta);
    }
    for (int i = 0; i < inventory.getSize(); i++) {
      inventory.setItem(i, filler);
    }
  }

  private ItemStack createPlayerItem(Player target, Player viewer) {
    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    SkullMeta meta = (SkullMeta) head.getItemMeta();
    if (meta != null) {
      meta.setOwningPlayer(target);
      meta.setDisplayName(plugin.colorize("&b" + target.getName()));
      List<String> lore = new ArrayList<>();
      lore.add(plugin.colorize("&7左鍵: 傳送過去"));
      lore.add(plugin.colorize("&7右鍵: 請他傳送過來"));
      if (playerDataStore.isBlacklisted(viewer.getUniqueId(), target.getUniqueId())) {
        lore.add(plugin.colorize("&c已加入黑名單"));
      }
      meta.setLore(lore);
      head.setItemMeta(meta);
    }
    return head;
  }

  private ItemStack createBlacklistItem(Player target, Player viewer) {
    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    SkullMeta meta = (SkullMeta) head.getItemMeta();
    if (meta != null) {
      meta.setOwningPlayer(target);
      meta.setDisplayName(plugin.colorize("&b" + target.getName()));
      boolean blocked = playerDataStore.isBlacklisted(viewer.getUniqueId(), target.getUniqueId());
      List<String> lore = new ArrayList<>();
      lore.add(plugin.colorize(blocked ? "&c點擊移除黑名單" : "&a點擊加入黑名單"));
      meta.setLore(lore);
      head.setItemMeta(meta);
    }
    return head;
  }

  private ItemStack createHomeItem(PlayerHome home, boolean readonly) {
    ItemStack item = new ItemStack(Material.RED_BED);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(plugin.colorize("&a" + home.name()));
      List<String> lore = new ArrayList<>();
      lore.add(plugin.colorize("&7左鍵: 傳送"));
      if (!readonly) {
        lore.add(plugin.colorize("&7右鍵: 編輯名稱"));
        lore.add(plugin.colorize("&7Shift+右鍵: 刪除"));
      }
      meta.setLore(lore);
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack createEmptyHomeItem(String slotId) {
    ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(plugin.colorize("&7建立傳點"));
      List<String> lore = new ArrayList<>();
      lore.add(plugin.colorize("&7點擊建立此傳點"));
      meta.setLore(lore);
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack createGateItem(GateDefinition gate) {
    GateSettings settings = gateSettingsStore.getSettings(gate.id());
    String name = settings.displayName() != null ? settings.displayName() : gate.id();
    String displayId = settings.displayId() != null ? settings.displayId() : gate.id();
    String materialName = settings.displayMaterial() != null ? settings.displayMaterial() : "NETHER_STAR";
    Material material = Material.matchMaterial(materialName);
    if (material == null) {
      material = Material.NETHER_STAR;
    }
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(plugin.colorize("&d" + name));
      GateLocation location = settings.overrideTo() != null ? settings.overrideTo() : gate.to();
      List<String> lore = new ArrayList<>();
      lore.add(plugin.colorize("&7ID: &f" + displayId));
      if (!displayId.equalsIgnoreCase(gate.id())) {
        lore.add(plugin.colorize("&7原始ID: &f" + gate.id()));
      }
      if (location != null) {
        lore.add(plugin.colorize("&7世界: &f" + location.world()));
        lore.add(plugin.colorize(String.format("&7座標: &f%.2f, %.2f, %.2f", location.x(), location.y(), location.z())));
      }
      meta.setLore(lore);
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack createGateAdminItem(GateDefinition gate) {
    GateSettings settings = gateSettingsStore.getSettings(gate.id());
    String displayId = settings.displayId() != null ? settings.displayId() : gate.id();
    boolean enabled = settings.enabled();
    Material material = enabled ? Material.LIME_DYE : Material.RED_DYE;
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(plugin.colorize("&e" + gate.id()));
      List<String> lore = new ArrayList<>();
      lore.add(plugin.colorize(enabled ? "&a已開放" : "&c未開放"));
      lore.add(plugin.colorize("&7ID: &f" + displayId));
      if (!displayId.equalsIgnoreCase(gate.id())) {
        lore.add(plugin.colorize("&7原始ID: &f" + gate.id()));
      }
      GateLocation location = settings.overrideTo() != null ? settings.overrideTo() : gate.to();
      if (location != null) {
        lore.add(plugin.colorize("&7世界: &f" + location.world()));
        lore.add(plugin.colorize(String.format("&7座標: &f%.2f, %.2f, %.2f", location.x(), location.y(), location.z())));
      }
      lore.add(plugin.colorize("&7左鍵切換開放"));
      lore.add(plugin.colorize("&7右鍵編輯資料"));
      meta.setLore(lore);
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack createWorldItem(World world) {
    Material material = switch (world.getEnvironment()) {
      case NETHER -> Material.NETHERRACK;
      case THE_END -> Material.END_STONE;
      default -> Material.GRASS_BLOCK;
    };
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(plugin.colorize("&b" + world.getName()));
      List<String> lore = new ArrayList<>();
      lore.add(plugin.colorize("&7環境: &f" + world.getEnvironment().name()));
      lore.add(plugin.colorize("&7左鍵傳送至世界重生點"));
      meta.setLore(lore);
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack createButton(Material material, String name) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(plugin.colorize(name));
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack createMenuTool() {
    String materialName = plugin.getConfig().getString("menu.tool.material", "COMPASS");
    Material material = Material.matchMaterial(materialName);
    if (material == null) {
      material = Material.COMPASS;
    }
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(plugin.colorize(plugin.getConfig().getString("menu.tool.name", "QuickArrive Menu")));
      List<String> loreRaw = plugin.getConfig().getStringList("menu.tool.lore");
      List<String> lore = new ArrayList<>();
      for (String line : loreRaw) {
        lore.add(plugin.colorize(line));
      }
      meta.setLore(lore);
      meta.getPersistentDataContainer().set(toolKey, PersistentDataType.INTEGER, 1);
      item.setItemMeta(meta);
    }
    return item;
  }

  private boolean isMenuTool(ItemStack item) {
    if (item == null || item.getType() == Material.AIR) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }
    Integer flag = meta.getPersistentDataContainer().get(toolKey, PersistentDataType.INTEGER);
    return flag != null && flag == 1;
  }

  @EventHandler
  public void onPlayerUseTool(PlayerInteractEvent event) {
    if (!event.getAction().isRightClick()) {
      return;
    }
    ItemStack item = event.getItem();
    if (!isMenuTool(item)) {
      return;
    }
    Player player = event.getPlayer();
    if (!player.hasPermission("quickarrive.use")) {
      player.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
      return;
    }
    openMainMenu(player);
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getClickedInventory() == null || event.getView().getTopInventory().getType() == InventoryType.PLAYER) {
      return;
    }
    InventoryHolder holder = event.getView().getTopInventory().getHolder();
    if (holder instanceof MainMenuHolder) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      int slot = event.getSlot();
      if (slot == 20) {
        player.openInventory(buildPlayersMenu(player, 0));
      } else if (slot == 22) {
        player.openInventory(buildHomesMenu(player));
      } else if (slot == 24) {
        if (!gateStore.gatesFileExists()) {
          player.sendMessage(plugin.withPrefix(plugin.message("gates-missing")));
          return;
        }
        player.openInventory(buildGatesMenu(0));
      } else if (slot == SLOT_CLOSE) {
        player.closeInventory();
      } else if (slot == SLOT_ADMIN && player.hasPermission("quickarrive.menu.admin")) {
        player.openInventory(buildGateAdminMenu(0));
      }
    } else if (holder instanceof PlayersMenuHolder playersHolder) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      int slot = event.getSlot();
      if (slot == SLOT_PREV) {
        player.openInventory(buildPlayersMenu(player, playersHolder.page - 1));
        return;
      }
      if (slot == SLOT_NEXT) {
        player.openInventory(buildPlayersMenu(player, playersHolder.page + 1));
        return;
      }
      if (slot == SLOT_BLACKLIST) {
        player.openInventory(buildBlacklistMenu(player, 0));
        return;
      }
      if (slot == SLOT_BACK) {
        openMainMenu(player);
        return;
      }
      if (slot == SLOT_CLOSE) {
        player.closeInventory();
        return;
      }
      UUID targetId = playersHolder.entries.get(slot);
      if (targetId == null) {
        return;
      }
      Player target = Bukkit.getPlayer(targetId);
      if (target == null) {
        return;
      }
      TeleportRequestType type = event.getClick() == ClickType.RIGHT ? TeleportRequestType.TPHERE : TeleportRequestType.TPA;
      teleportManager.sendPlayerRequest(player, target, type);
      player.closeInventory();
    } else if (holder instanceof BlacklistMenuHolder blacklistHolder) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      int slot = event.getSlot();
      if (slot == SLOT_PREV) {
        player.openInventory(buildBlacklistMenu(player, blacklistHolder.page - 1));
        return;
      }
      if (slot == SLOT_NEXT) {
        player.openInventory(buildBlacklistMenu(player, blacklistHolder.page + 1));
        return;
      }
      if (slot == SLOT_BACK) {
        player.openInventory(buildPlayersMenu(player, 0));
        return;
      }
      if (slot == SLOT_CLOSE) {
        player.closeInventory();
        return;
      }
      UUID targetId = blacklistHolder.entries.get(slot);
      if (targetId == null) {
        return;
      }
      boolean blocked = playerDataStore.isBlacklisted(player.getUniqueId(), targetId);
      if (blocked) {
        playerDataStore.removeBlacklist(player.getUniqueId(), targetId);
        player.sendMessage(plugin.withPrefix(plugin.message("blacklist-removed", Map.of("player", getName(targetId)))));
      } else {
        playerDataStore.addBlacklist(player.getUniqueId(), targetId);
        player.sendMessage(plugin.withPrefix(plugin.message("blacklist-added", Map.of("player", getName(targetId)))));
      }
      player.openInventory(buildBlacklistMenu(player, blacklistHolder.page));
    } else if (holder instanceof HomesMenuHolder homesHolder) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      int slot = event.getSlot();
      if (slot == SLOT_BACK) {
        openMainMenu(player);
        return;
      }
      if (slot == SLOT_CLOSE) {
        player.closeInventory();
        return;
      }
      PlayerHome home = homesHolder.entries.get(slot);
      if (home != null) {
        if (event.getClick() == ClickType.LEFT) {
          teleportManager.teleportNow(player, home.location());
          player.sendMessage(plugin.withPrefix(plugin.message("location-teleport", Map.of("location", home.name()))));
          player.closeInventory();
          return;
        }
        if (!home.editable()) {
          return;
        }
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
          playerDataStore.removeHome(player.getUniqueId(), home.slotId());
          player.sendMessage(plugin.withPrefix(plugin.message("home-removed", Map.of("home", home.name()))));
          player.openInventory(buildHomesMenu(player));
          return;
        }
        if (event.getClick() == ClickType.RIGHT) {
          requestInput(player, "&e請輸入新的傳點名稱 (輸入 cancel 取消)", input -> {
            playerDataStore.setHome(player.getUniqueId(), home.slotId(), input, home.location());
            player.sendMessage(plugin.withPrefix(plugin.message("home-renamed", Map.of("home", input))));
            player.openInventory(buildHomesMenu(player));
          });
          return;
        }
        return;
      }
      String slotId = homesHolder.emptySlots.get(slot);
      if (slotId == null) {
        return;
      }
      int essentialsCount = essentialsHomeProvider.getHomes(player.getUniqueId(), 2).size();
      int quickCount = playerDataStore.getHomes(player.getUniqueId()).size();
      if (essentialsCount + quickCount >= 2) {
        player.sendMessage(plugin.withPrefix(plugin.message("home-limit")));
        return;
      }
      playerDataStore.setHome(player.getUniqueId(), slotId, slotId, player.getLocation());
      player.sendMessage(plugin.withPrefix(plugin.message("home-set", Map.of("home", slotId))));
      player.openInventory(buildHomesMenu(player));
    } else if (holder instanceof GatesMenuHolder gatesHolder) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      int slot = event.getSlot();
      if (slot == SLOT_PREV) {
        player.openInventory(buildGatesMenu(gatesHolder.page - 1));
        return;
      }
      if (slot == SLOT_NEXT) {
        player.openInventory(buildGatesMenu(gatesHolder.page + 1));
        return;
      }
      if (slot == SLOT_BACK) {
        openMainMenu(player);
        return;
      }
      if (slot == SLOT_CLOSE) {
        player.closeInventory();
        return;
      }
      String gateId = gatesHolder.entries.get(slot);
      if (gateId == null) {
        return;
      }
      GateDefinition gate = findGate(gateId);
      if (gate == null) {
        return;
      }
      GateSettings settings = gateSettingsStore.getSettings(gateId);
      GateLocation location = settings.overrideTo() != null ? settings.overrideTo() : gate.to();
      if (location == null) {
        return;
      }
      teleportManager.teleportNow(player, toLocation(location));
      player.sendMessage(plugin.withPrefix(plugin.message("gate-teleport", Map.of("gate", gateId))));
      player.closeInventory();
    } else if (holder instanceof GateAdminMenuHolder adminHolder) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      if (!player.hasPermission("quickarrive.menu.admin")) {
        player.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
        return;
      }
      int slot = event.getSlot();
      if (slot == SLOT_PREV) {
        player.openInventory(buildGateAdminMenu(adminHolder.page - 1));
        return;
      }
      if (slot == SLOT_NEXT) {
        player.openInventory(buildGateAdminMenu(adminHolder.page + 1));
        return;
      }
      if (slot == SLOT_WORLDS) {
        player.openInventory(buildWorldsMenu(0));
        return;
      }
      if (slot == SLOT_BACK) {
        openMainMenu(player);
        return;
      }
      if (slot == SLOT_CLOSE) {
        player.closeInventory();
        return;
      }
      String gateId = adminHolder.entries.get(slot);
      if (gateId == null) {
        return;
      }
      if (event.getClick() == ClickType.RIGHT) {
        player.openInventory(buildGateEditMenu(gateId));
        return;
      }
      boolean enabled = !gateSettingsStore.getSettings(gateId).enabled();
      gateSettingsStore.setEnabled(gateId, enabled);
      String msg = enabled ? "gate-enabled" : "gate-disabled";
      player.sendMessage(plugin.withPrefix(plugin.message(msg, Map.of("gate", gateId))));
      player.openInventory(buildGateAdminMenu(adminHolder.page));
    } else if (holder instanceof WorldsMenuHolder worldsHolder) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      if (!player.hasPermission("quickarrive.menu.admin")) {
        player.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
        return;
      }
      int slot = event.getSlot();
      if (slot == SLOT_PREV) {
        player.openInventory(buildWorldsMenu(worldsHolder.page - 1));
        return;
      }
      if (slot == SLOT_NEXT) {
        player.openInventory(buildWorldsMenu(worldsHolder.page + 1));
        return;
      }
      if (slot == SLOT_BACK) {
        player.openInventory(buildGateAdminMenu(0));
        return;
      }
      if (slot == SLOT_CLOSE) {
        player.closeInventory();
        return;
      }
      String worldName = worldsHolder.entries.get(slot);
      if (worldName == null) {
        return;
      }
      World world = Bukkit.getWorld(worldName);
      if (world == null) {
        player.sendMessage(plugin.withPrefix(plugin.message("input-invalid")));
        return;
      }
      teleportManager.teleportNow(player, world.getSpawnLocation());
      player.sendMessage(plugin.withPrefix(plugin.message("world-teleport", Map.of("world", world.getName()))));
      player.closeInventory();
    } else if (holder instanceof GateEditMenuHolder editHolder) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      if (!player.hasPermission("quickarrive.menu.admin")) {
        player.sendMessage(plugin.withPrefix(plugin.message("no-permission")));
        return;
      }
      int slot = event.getSlot();
      String gateId = editHolder.gateId;
      if (slot == SLOT_BACK) {
        player.openInventory(buildGateAdminMenu(0));
        return;
      }
      if (slot == SLOT_CLOSE) {
        player.closeInventory();
        return;
      }
      if (slot == 20) {
        requestInput(player, "&e請輸入顯示名稱 (輸入 cancel 取消)", input -> {
          gateSettingsStore.setDisplayName(gateId, input);
          player.openInventory(buildGateEditMenu(gateId));
        });
        return;
      }
      if (slot == 22) {
        requestInput(player, "&e請輸入材質名稱 (例如 NETHER_STAR)", input -> {
          Material material = Material.matchMaterial(input);
          if (material == null) {
            player.sendMessage(plugin.withPrefix(plugin.message("input-invalid")));
            player.openInventory(buildGateEditMenu(gateId));
            return;
          }
          gateSettingsStore.setDisplayMaterial(gateId, material.name());
          player.openInventory(buildGateEditMenu(gateId));
        });
        return;
      }
      if (slot == 24) {
        requestInput(player, "&e請輸入顯示ID (輸入 cancel 取消)", input -> {
          gateSettingsStore.setDisplayId(gateId, input);
          player.openInventory(buildGateEditMenu(gateId));
        });
        return;
      }
      if (slot == 29) {
        requestInput(player, "&e請輸入世界名稱", input -> {
          World world = Bukkit.getWorld(input);
          if (world == null) {
            player.sendMessage(plugin.withPrefix(plugin.message("input-invalid")));
            player.openInventory(buildGateEditMenu(gateId));
            return;
          }
          GateLocation current = getGateLocation(gateId);
          double x = current != null ? current.x() : 0.0;
          double y = current != null ? current.y() : 0.0;
          double z = current != null ? current.z() : 0.0;
          float yaw = current != null ? current.yaw() : 0.0f;
          float pitch = current != null ? current.pitch() : 0.0f;
          gateSettingsStore.setOverride(gateId, new GateLocation(world.getName(), x, y, z, yaw, pitch));
          player.openInventory(buildGateEditMenu(gateId));
        });
        return;
      }
      if (slot >= 30 && slot <= 34) {
        handleCoordinateEdit(player, gateId, slot);
        return;
      }
      if (slot == 40) {
        gateSettingsStore.clearOverride(gateId);
        player.openInventory(buildGateEditMenu(gateId));
      }
    }
  }

  private void handleCoordinateEdit(Player player, String gateId, int slot) {
    String field;
    if (slot == 30) {
      field = "x";
    } else if (slot == 31) {
      field = "y";
    } else if (slot == 32) {
      field = "z";
    } else if (slot == 33) {
      field = "yaw";
    } else {
      field = "pitch";
    }
    requestInput(player, "&e請輸入新的 " + field + "", input -> {
      try {
        double value = Double.parseDouble(input);
        GateLocation current = getGateLocation(gateId);
        if (current == null) {
          player.sendMessage(plugin.withPrefix(plugin.message("input-invalid")));
          player.openInventory(buildGateEditMenu(gateId));
          return;
        }
        GateLocation updated = switch (field) {
          case "x" -> new GateLocation(current.world(), value, current.y(), current.z(), current.yaw(), current.pitch());
          case "y" -> new GateLocation(current.world(), current.x(), value, current.z(), current.yaw(), current.pitch());
          case "z" -> new GateLocation(current.world(), current.x(), current.y(), value, current.yaw(), current.pitch());
          case "yaw" -> new GateLocation(current.world(), current.x(), current.y(), current.z(), (float) value, current.pitch());
          default -> new GateLocation(current.world(), current.x(), current.y(), current.z(), current.yaw(), (float) value);
        };
        gateSettingsStore.setOverride(gateId, updated);
      } catch (NumberFormatException ignored) {
        player.sendMessage(plugin.withPrefix(plugin.message("input-invalid")));
      }
      player.openInventory(buildGateEditMenu(gateId));
    });
  }

  private GateLocation getGateLocation(String gateId) {
    GateSettings settings = gateSettingsStore.getSettings(gateId);
    if (settings.overrideTo() != null) {
      return settings.overrideTo();
    }
    GateDefinition gate = findGate(gateId);
    if (gate == null) {
      return null;
    }
    return gate.to();
  }

  private GateDefinition findGate(String gateId) {
    for (GateDefinition gate : gateStore.loadGates()) {
      if (gate.id().equalsIgnoreCase(gateId)) {
        return gate;
      }
    }
    return null;
  }

  private org.bukkit.Location toLocation(GateLocation location) {
    World world = Bukkit.getWorld(location.world());
    if (world == null) {
      return null;
    }
    return new org.bukkit.Location(world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
  }

  private String getName(UUID uuid) {
    Player player = Bukkit.getPlayer(uuid);
    if (player != null) {
      return player.getName();
    }
    return uuid.toString();
  }

  private void requestInput(Player player, String prompt, Consumer<String> handler) {
    chatInputManager.request(player, plugin.colorize(prompt), handler);
  }

  private static class MainMenuHolder implements InventoryHolder {
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }

  private static class PlayersMenuHolder implements InventoryHolder {
    private final Map<Integer, UUID> entries = new HashMap<>();
    private final int page;
    private Inventory inventory;

    private PlayersMenuHolder(int page) {
      this.page = page;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }

  private static class BlacklistMenuHolder implements InventoryHolder {
    private final Map<Integer, UUID> entries = new HashMap<>();
    private final int page;
    private Inventory inventory;

    private BlacklistMenuHolder(int page) {
      this.page = page;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }

  private static class HomesMenuHolder implements InventoryHolder {
    private final Map<Integer, PlayerHome> entries = new HashMap<>();
    private final Map<Integer, String> emptySlots = new HashMap<>();
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }

  private static class GatesMenuHolder implements InventoryHolder {
    private final Map<Integer, String> entries = new HashMap<>();
    private final int page;
    private Inventory inventory;

    private GatesMenuHolder(int page) {
      this.page = page;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }

  private static class GateAdminMenuHolder implements InventoryHolder {
    private final Map<Integer, String> entries = new HashMap<>();
    private final int page;
    private Inventory inventory;

    private GateAdminMenuHolder(int page) {
      this.page = page;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }

  private static class GateEditMenuHolder implements InventoryHolder {
    private final String gateId;
    private Inventory inventory;

    private GateEditMenuHolder(String gateId) {
      this.gateId = gateId;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }

  private static class WorldsMenuHolder implements InventoryHolder {
    private final Map<Integer, String> entries = new HashMap<>();
    private final int page;
    private Inventory inventory;

    private WorldsMenuHolder(int page) {
      this.page = page;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }
}
