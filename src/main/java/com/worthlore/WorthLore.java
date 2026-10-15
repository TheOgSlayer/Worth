package com.worthlore;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class WorthLore extends JavaPlugin {

    /** Invisible marker at the start of our lore line so we can detect / strip it later. */
    public static final String MARKER = "\u00A7r\u00A7r\u00A7r";

    private final Map<Material, Double> prices = new TreeMap<>(Comparator.comparing(Material::name));

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "prices.yml").exists()) {
            saveResource("prices.yml", false);
        }
        loadPrices();

        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        new LorePacketListener(this).register();
        getLogger().info("WorthLore enabled with " + prices.size() + " prices.");
    }

    public void loadPrices() {
        prices.clear();
        File file = new File(getDataFolder(), "prices.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        for (String key : yml.getKeys(false)) {
            Material mat = Material.matchMaterial(key.trim());
            if (mat == null || !mat.isItem()) {
                getLogger().warning("prices.yml: '" + key + "' is not a valid item, skipping.");
                continue;
            }
            if (!yml.isInt(key) && !yml.isDouble(key) && !yml.isLong(key)) {
                getLogger().warning("prices.yml: value for '" + key + "' is not a number, skipping.");
                continue;
            }
            prices.put(mat, yml.getDouble(key));
        }
    }

    public Map<Material, Double> getPrices() {
        return Collections.unmodifiableMap(prices);
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public String msg(String path) {
        return color(getConfig().getString("messages." + path, path));
    }

    public static String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.format("%,d", (long) v);
        return String.format("%,.2f", v);
    }

    /** Returns a copy of the item with the worth line added (or the same item if it has no price). */
    public ItemStack withWorth(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        Double worth = prices.get(item.getType());
        if (worth == null) return item;

        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        for (String line : lore) {
            if (line.startsWith(MARKER)) return item; // already has it
        }

        int amount = Math.max(1, item.getAmount());
        String format = amount > 1
                ? getConfig().getString("stack-lore-format", "&7Worth: &a$%worth%")
                : getConfig().getString("lore-format", "&7Worth: &a$%worth%");

        String line = format
                .replace("%worth%", fmt(worth))
                .replace("%total%", fmt(worth * amount))
                .replace("%amount%", String.valueOf(amount));

        lore.add(MARKER + color(line));
        meta.setLore(lore);
        copy.setItemMeta(meta);
        return copy;
    }

    /** Removes our lore line (used when creative clients send items back to the server). */
    public ItemStack stripWorth(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return item;

        List<String> lore = new ArrayList<>(meta.getLore());
        if (!lore.removeIf(l -> l.startsWith(MARKER))) return item;

        ItemStack copy = item.clone();
        ItemMeta m = copy.getItemMeta();
        m.setLore(lore.isEmpty() ? null : lore);
        copy.setItemMeta(m);
        return copy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("worth.reload")) {
                sender.sendMessage(msg("no-permission"));
                return true;
            }
            reloadConfig();
            loadPrices();
            sender.sendMessage(msg("reloaded").replace("%count%", String.valueOf(prices.size())));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("players-only"));
            return true;
        }
        if (!player.hasPermission("worth.use")) {
            player.sendMessage(msg("no-permission"));
            return true;
        }
        if (prices.isEmpty()) {
            player.sendMessage(msg("no-prices"));
            return true;
        }
        new WorthGUI(this, 0).open(player);
        return true;
    }
}
