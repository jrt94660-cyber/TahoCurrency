package com.taho.currency;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TahoCurrency extends JavaPlugin implements Listener {
    private CoinsStore store;
    private NamespacedKey shopItemKey;
    private static final String LEADERBOARD_TITLE = "§8Taho Coin Leaderboard";

    @Override public void onEnable() {
        saveDefaultConfig();
        store = new CoinsStore(new File(getDataFolder(), "balances.yml"));
        shopItemKey = new NamespacedKey(this, "shop_item");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TahoCurrency enabled.");
    }

    @Override public void onDisable() {
        if (store != null) store.save();
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String base = color(getConfig().getString("messages.prefix", "&8[&dTahoCurrency&8] "));
        if (command.getName().equalsIgnoreCase("taho")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(base + msg("player-only")); return true;
            }
            player.sendMessage(base + msg("balance")
                .replace("%balance%", format(store.get(player.getUniqueId())))
                .replace("%currency%", currencyName()));
            return true;
        }
        if (command.getName().equalsIgnoreCase("tahobaltop")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(base + msg("player-only")); return true;
            }
            openLeaderboard(player);
            return true;
        }
        if (command.getName().equalsIgnoreCase("tahoshop")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(base + msg("player-only")); return true;
            }
            openShop(player);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("tahocurrency.admin")) {
                sender.sendMessage(base + msg("no-permission")); return true;
            }
            reloadConfig(); sender.sendMessage(base + msg("reloaded")); return true;
        }
        if (command.getName().equalsIgnoreCase("currency")) {
            sender.sendMessage(base + color("&7Use &f/taho &7to check your balance, &f/tahoshop &7for the shop, or &f/tahobaltop &7for the leaderboard."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(base + msg("player-only")); return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal")) {
            player.sendMessage(base + msg("balance")
                .replace("%balance%", format(store.get(player.getUniqueId())))
                .replace("%currency%", currencyName()));
            return true;
        }
        if (args[0].equalsIgnoreCase("shop")) { openShop(player); return true; }
        player.sendMessage(base + color("&7Usage: &f/currency [balance|shop]"));
        return true;
    }

    @EventHandler public void onMobDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        Entity dead = event.getEntity();
        if (dead instanceof Player) return;
        List<String> excluded = getConfig().getStringList("currency.excluded-mobs");
        if (excluded.contains(dead.getType().name())) return;
        int min = Math.max(0, getConfig().getInt("currency.kill-reward.minimum", 200));
        int max = Math.max(min, getConfig().getInt("currency.kill-reward.maximum", 1000));
        int amount = ThreadLocalRandom.current().nextInt(min, max + 1);
        store.add(killer.getUniqueId(), amount);
        killer.sendMessage(color(getConfig().getString("messages.prefix", "&8[&dTahoCurrency&8] "))
            + msg("earned").replace("%amount%", format(amount)).replace("%currency%", currencyName()));
    }

    private void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, color(getConfig().getString("shop.title", "&8Taho Coin Shop")));
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta(); fillerMeta.setDisplayName(" "); filler.setItemMeta(fillerMeta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        for (String id : List.of("elytra", "dragon-head")) {
            String path = "shop.items." + id;
            Material material = Material.matchMaterial(getConfig().getString(path + ".material", "STONE"));
            if (material == null) material = Material.STONE;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(color(getConfig().getString(path + ".name", id)));
            List<String> lore = new ArrayList<>();
            for (String line : getConfig().getStringList(path + ".lore")) lore.add(color(line));
            lore.add(color("&7Your balance: &d" + format(store.get(player.getUniqueId())) + " " + currencySymbol()));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
            inv.setItem(Math.max(0, Math.min(53, getConfig().getInt(path + ".slot", 20))), item);
        }
        player.openInventory(inv);
    }

    private void openLeaderboard(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LEADERBOARD_TITLE);
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        List<Map.Entry<UUID, Long>> leaders = store.getBalances().entrySet().stream()
            .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder()))
            .limit(10)
            .toList();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < leaders.size(); i++) {
            Map.Entry<UUID, Long> entry = leaders.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null || name.isBlank()) name = "Unknown Player";
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            meta.setDisplayName(color("&e#" + (i + 1) + " &f" + name));
            meta.setLore(List.of(color("&7Balance: &d" + format(entry.getValue()) + " " + currencySymbol())));
            head.setItemMeta(meta);
            inv.setItem(slots[i], head);
        }
        ItemStack info = new ItemStack(Material.GOLD_INGOT);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(color("&6Your Balance"));
        infoMeta.setLore(List.of(color("&d" + format(store.get(player.getUniqueId())) + " " + currencySymbol())));
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);
        player.openInventory(inv);
    }

    @EventHandler public void onShopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = color(getConfig().getString("shop.title", "&8Taho Coin Shop"));
        if (event.getView().getTitle().equals(LEADERBOARD_TITLE)) {
            event.setCancelled(true);
            return;
        }
        if (!event.getView().getTitle().equals(title)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String id = clicked.getItemMeta().getPersistentDataContainer().get(shopItemKey, PersistentDataType.STRING);
        if (id == null) return;
        String path = "shop.items." + id;
        int price = Math.max(0, getConfig().getInt(path + ".price", 0));
        UUID uuid = player.getUniqueId();
        if (store.get(uuid) < price) {
            player.sendMessage(prefix() + msg("insufficient").replace("%price%", format(price))
                .replace("%currency%", currencyName()).replace("%balance%", format(store.get(uuid))));
            return;
        }
        Material material = Material.matchMaterial(getConfig().getString(path + ".material", "STONE"));
        if (material == null) return;
        ItemStack reward = new ItemStack(material);
        if (id.equalsIgnoreCase("elytra")) {
            ItemMeta meta = reward.getItemMeta();
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            reward.setItemMeta(meta);
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(prefix() + color("&cYour inventory is full!")); return;
        }
        if (!store.withdraw(uuid, price)) return;
        player.getInventory().addItem(reward);
        player.sendMessage(prefix() + msg("purchased")
            .replace("%item%", color(getConfig().getString(path + ".name", id)))
            .replace("%price%", format(price)).replace("%currency%", currencyName()));
        openShop(player);
    }

    private String currencyName() { return getConfig().getString("currency.name", "Taho Coins"); }
    private String currencySymbol() { return getConfig().getString("currency.symbol", "✦"); }
    private String prefix() { return color(getConfig().getString("messages.prefix", "&8[&dTahoCurrency&8] ")); }
    private String msg(String key) { return color(getConfig().getString("messages." + key, "")); }
    private String format(long n) { return String.format(Locale.US, "%,d", n); }
    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }
}