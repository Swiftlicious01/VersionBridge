package com.swiftlicious.versionbridge.paper.r26_3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.versionbridge.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.versionbridge.nms.util.SelfIncreaseEntityID;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

public class ItemDisplayInstance implements FakeItemDisplay {

	private final Location location;
	private ItemStack item;
	private final int entityID = SelfIncreaseEntityID.getAndIncrease();
	private final UUID uuid = UUID.randomUUID();

	public ItemDisplayInstance(Location location) {
		this.location = location;
	}

	@Override
	public void destroy(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityID);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void spawn(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundAddEntityPacket entityPacket = new ClientboundAddEntityPacket(entityID, uuid, location.getX(),
				location.getY(), location.getZ(), location.getPitch(), location.getYaw(), EntityTypes.ITEM_DISPLAY, 0,
				Vec3.ZERO, 0);

		ArrayList<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(
				List.of(entityPacket, getMetaPacket()));
		ClientboundBundlePacket packet = new ClientboundBundlePacket(packets);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void updateMetaData(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.connection.send(getMetaPacket());
	}

	private ClientboundSetEntityDataPacket getMetaPacket() {
		ArrayList<SynchedEntityData.DataValue<?>> values = new ArrayList<>();
		values.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(23, EntityDataSerializers.ITEM_STACK),
				CraftItemStack.asNMSCopy(item)));
		return new ClientboundSetEntityDataPacket(entityID, values);
	}

	@Override
	public void item(ItemStack itemStack) {
		this.item = itemStack;
	}

	@Override
	public int entityID() {
		return entityID;
	}
}