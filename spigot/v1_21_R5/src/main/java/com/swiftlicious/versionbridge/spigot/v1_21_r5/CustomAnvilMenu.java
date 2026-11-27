package com.swiftlicious.versionbridge.spigot.v1_21_r5;

import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R5.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R5.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.versionbridge.nms.anvil.AnvilInputMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class CustomAnvilMenu extends AnvilInputMenu {

	@Nullable
	private AnvilMenu menu;

	public CustomAnvilMenu(Player player, Plugin plugin) {
		super(player, plugin);
	}

	@Override
	public void open(String jsonTitle) {
		ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

		int containerId = nmsPlayer.nextContainerCounter();
		Inventory playerInventory = nmsPlayer.getInventory();
		BlockPos pos = BlockPos.ZERO;
		ContainerLevelAccess access = ContainerLevelAccess.create(nmsPlayer.level(), pos);

		menu = new AnvilMenu(containerId, playerInventory, access) {
			@Override
			public void removed(net.minecraft.world.entity.player.Player entityHuman) {
				this.inputSlots.clearContent();
				this.resultSlots.clearContent();
				super.removed(entityHuman);

				// Force reset container to avoid showing ghost screen
				if (entityHuman instanceof ServerPlayer serverPlayer) {
					serverPlayer.containerMenu = serverPlayer.inventoryMenu;
					triggerCloseCallback(); // ← safe handoff to plugin
				}
			}
		};

		menu.checkReachable = false;

		Component titleComponent = CraftChatMessage.fromJSON(jsonTitle);
		ClientboundOpenScreenPacket openPacket = new ClientboundOpenScreenPacket(containerId, MenuType.ANVIL,
				titleComponent);
		nmsPlayer.connection.send(openPacket);

		nmsPlayer.containerMenu = menu;
		nmsPlayer.initMenu(menu);
	}

	@Override
	public void close() {
		ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

		CraftEventFactory.handleInventoryCloseEvent(nmsPlayer);
		// Force close container
		nmsPlayer.doCloseContainer();

		nmsPlayer.containerMenu = nmsPlayer.inventoryMenu; // reset to default container
	}

	@Override
	public org.bukkit.inventory.Inventory getTopInventory() {
		return menu != null ? menu.getBukkitView().getTopInventory() : null;
	}

	@Override
	public String getRenameText() {
		return menu != null ? menu.getBukkitView().getRenameText() : null;
	}

	@Override
	public void setRenameText(String text) {
		if (menu != null) {
			menu.setItemName(text);
			menu.broadcastChanges();
		}
	}

	@Override
	public void setResultItem(org.bukkit.inventory.ItemStack itemStack) {
		if (menu == null)
			return;

		ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		int containerId = menu.containerId;
		int stateId = menu.getStateId();

		ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
		ClientboundContainerSetSlotPacket slotPacket = new ClientboundContainerSetSlotPacket(containerId, stateId, 2,
				nmsItem);
		nmsPlayer.connection.send(slotPacket);
	}

	@Override
	public InventoryView getBukkitView() {
		return menu != null ? menu.getBukkitView() : null;
	}
}