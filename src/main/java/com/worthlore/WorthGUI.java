package com.worthlore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorthGUI implements InventoryHolder {

    public static final int PER_PAGE = 45;
    public static final int PREV_SLOT = 45;
    public static final int INFO_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final WorthLore plugin;
    private final int page;
    private final int pages;
    private final Inventory inventory;

    public WorthGUI(WorthLore plugin, int page) {
        this.plugin = plugin;

        List<Map.Entry<Material, Double>> entries = new ArrayList<>(plugin.getPrices().entrySet());
        this.pages = Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
        this.page = Math.max(0, Math.min(page, pages - 1));

        String title = plugin.color(plugin.getConfig().getString("gui.title", "&8Item Worth"))
                .replace("%page%", String.valueOf(this.page + 1))
                .replace("%pages%", String.valueOf(pages));
        this.inventory = Bukkit.createInventory(this, 54, title);

        int start = this.page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, entries.size());
        for (int i = start; i < end; i++) {
            ItemStack item = plugin.withWorth(new ItemStack(entries.get(i).getKey()));
            inventory.setItem(i - start, item);
        }

        if (this.page > 0) inventory.setItem(PREV_SLOT, button(plugin.getConfig().getString("gui.previous-page")));
        if (this.page < pages - 1) inventory.setItem(NEXT_SLOT, button(plugin.getConfig().getString("gui.next-page")));
        inventory.setItem(INFO_SLOT, button(plugin.getConfig().getString("gui.page-item", "&ePage %page%/%pages%")
                .replace("%page%", String.valueOf(this.page + 1))
                .replace("%pages%", String.valueOf(pages))), Material.PAPER);
    }

    private ItemStack button(String name) {
        return button(name, Material.ARROW);
    }

    private ItemStack button(String name, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color(name == null ? "" : name));
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public int getPage() {
        return page;
    }

    public int getPages() {
        return pages;
    }

    public WorthLore getPlugin() {
        return plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
