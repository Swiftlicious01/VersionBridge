package com.swiftlicious.versionbridge.nms.anvil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public abstract class AnvilInputMenu {

	protected final Player player;
	protected final Plugin plugin;

	private Runnable onClose; // safe callback

	public AnvilInputMenu(Player player, Plugin plugin) {
		this.player = player;
		this.plugin = plugin;
	}

	public void setOnClose(Runnable callback) {
		this.onClose = callback;
	}

	protected void triggerCloseCallback() {
		if (onClose != null) {
			Bukkit.getScheduler().runTask(plugin, onClose);
			onClose = null; // ensure only runs once
		}
	}

	/**
	 * Opens the anvil menu to the player.
	 *
	 * @param jsonTitle A JSON string for the anvil's title (e.g. {"text":"Rename"})
	 */
	public abstract void open(String jsonTitle);

	/**
	 * Force closes the menu.
	 */
	public abstract void close();

	/**
	 * @return The top Bukkit inventory for interaction.
	 */
	public abstract Inventory getTopInventory();

	/**
	 * @return The rename text input by the player.
	 */
	public abstract String getRenameText();

	/**
	 * Sets the rename text (client-side).
	 */
	public abstract void setRenameText(String text);

	/**
	 * Sets the result item shown in the result slot (slot 2).
	 */
	public abstract void setResultItem(ItemStack item);

	/**
	 * @return The full Bukkit InventoryView, including player inventory.
	 */
	public abstract InventoryView getBukkitView();
}