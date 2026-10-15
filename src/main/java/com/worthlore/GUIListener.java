package com.worthlore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof WorthGUI gui)) return;

        // Nothing can be taken, moved or shift-clicked while the GUI is open
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        if (e.getSlot() == WorthGUI.PREV_SLOT && gui.getPage() > 0) {
            new WorthGUI(gui.getPlugin(), gui.getPage() - 1).open(player);
        } else if (e.getSlot() == WorthGUI.NEXT_SLOT && gui.getPage() < gui.getPages() - 1) {
            new WorthGUI(gui.getPlugin(), gui.getPage() + 1).open(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof WorthGUI) {
            e.setCancelled(true);
        }
    }
}
