package com.swiftlicious.versionbridge.paper.v1_18_r1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;

import com.mojang.datafixers.util.Pair;
import com.swiftlicious.versionbridge.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.versionbridge.nms.util.SelfIncreaseEntityID;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ArmorStandInstance implements FakeArmorStand {

	private Location location;
	private boolean small = false;
	private boolean invisible = false;
	private boolean gravity = false;
	private boolean basePlate = false;
	private boolean marker = false;
	private String name;
	private final List<Pair<EquipmentSlot, ItemStack>> equipments = new ArrayList<>();
	private final int entityID = SelfIncreaseEntityID.getAndIncrease();
	private final UUID uuid = UUID.randomUUID();
	private boolean isDestroyed = false;
	private ArmorStand armorStand;

	public ArmorStandInstance(Location location) {
		this.location = location.clone();
	}

	@Override
	public void small(boolean small) {
		this.small = small;
	}

	@Override
	public void invisible(boolean invisible) {
		this.invisible = invisible;
	}

	@Override
	public void gravity(boolean gravity) {
		this.gravity = gravity;
	}

	@Override
	public void basePlate(boolean basePlate) {
		this.basePlate = basePlate;
	}

	@Override
	public void marker(boolean marker) {
		this.marker = marker;
	}

	@Override
	public void name(String json) {
		this.name = json;
	}

	@Override
	public void equipment(org.bukkit.inventory.EquipmentSlot slot, org.bukkit.inventory.ItemStack itemStack) {
		this.equipments.add(Pair.of(CraftEquipmentSlot.getNMS(slot), CraftItemStack.asNMSCopy(itemStack)));
	}

	@Override
	public void spawn(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		if (armorStand == null) {
			armorStand = new ArmorStand(EntityType.ARMOR_STAND, serverPlayer.level);
			armorStand.setId(entityID);
			armorStand.setUUID(uuid);
		}

		ClientboundAddEntityPacket entityPacket = new ClientboundAddEntityPacket(entityID, uuid, location.getX(),
				location.getY(), location.getZ(), location.getPitch(), location.getYaw(), EntityType.ARMOR_STAND, 0,
				Vec3.ZERO);

		serverPlayer.connection.send(entityPacket);
		serverPlayer.connection.send(getMetaPacket(serverPlayer));

		if (!equipments.isEmpty()) {
			serverPlayer.connection.send(getEquipmentPacket());
		}
	}

	@Override
	public void destroy(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityID);
		serverPlayer.connection.send(packet);
		isDestroyed = true;
	}

	@Override
	public void teleport(Player player, Location newLocation) {
		if (isDestroyed || player == null || !player.isOnline())
			return;

		this.location = newLocation.clone();

		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

		// If the fake armor stand is gone, there’s nothing to move
		if (this.armorStand == null || this.armorStand.isRemoved())
			return;

		// Update the internal entity location to match the new position
		this.armorStand.setPos(newLocation.getX(), newLocation.getY(), newLocation.getZ());
		this.armorStand.setYRot(newLocation.getYaw());
		this.armorStand.setXRot(newLocation.getPitch());

		// Send teleport packet directly for this entity
		ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(this.armorStand);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void updateMetaData(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.connection.send(getMetaPacket(serverPlayer));
	}

	@Override
	public void updateEquipment(Player player) {
		if (equipments.isEmpty())
			return;
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.connection.send(getEquipmentPacket());
	}

	private ClientboundSetEntityDataPacket getMetaPacket(ServerPlayer player) {
		if (armorStand == null) {
			armorStand = new ArmorStand(EntityType.ARMOR_STAND, player.level);
			armorStand.setId(entityID);
			armorStand.setUUID(uuid);
		}
		SynchedEntityData entityData = new SynchedEntityData(armorStand);

		byte flags = 0x00;
		if (invisible)
			flags |= 0x20; // invisible flag

		entityData.define(new EntityDataAccessor<>(0, EntityDataSerializers.BYTE), flags);

		if (name != null) {
			entityData.define(new EntityDataAccessor<>(3, EntityDataSerializers.BOOLEAN), true);
			entityData.define(new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT),
					Optional.of(CraftChatMessage.fromJSON(name)));
		}

		byte armorStandFlags = 0x00;

		if (small) {
			armorStandFlags |= 0x01; // Small
		}
		if (!gravity) {
			armorStandFlags |= 0x02; // No gravity
		}
		if (!basePlate) {
			armorStandFlags |= 0x08; // No baseplate
		}
		if (marker) {
			armorStandFlags |= 0x10; // Marker (no collision box)
		}

		entityData.define(new EntityDataAccessor<>(15, EntityDataSerializers.BYTE), armorStandFlags);

		return new ClientboundSetEntityDataPacket(entityID, entityData, true);
	}

	private ClientboundSetEquipmentPacket getEquipmentPacket() {
		return new ClientboundSetEquipmentPacket(entityID, equipments);
	}

	@Override
	public Location getLocation() {
		return location.clone();
	}

	@Override
	public boolean isDead() {
		return isDestroyed;
	}

	@Override
	public int entityID() {
		return entityID;
	}

	@Override
	public void setCamera(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		if (armorStand == null)
			return; // safety
		armorStand.setId(entityID); // ensure synced
		armorStand.setUUID(uuid);
		ClientboundSetCameraPacket packet = new ClientboundSetCameraPacket(armorStand);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void resetCamera(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundSetCameraPacket packet = new ClientboundSetCameraPacket(serverPlayer);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void keepClientCameraStable(Player player) {
		setCamera(player);
	}
}