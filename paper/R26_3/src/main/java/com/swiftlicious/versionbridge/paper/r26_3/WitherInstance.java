package com.swiftlicious.versionbridge.paper.r26_3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.versionbridge.nms.entity.wither.FakeWither;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public final class WitherInstance implements FakeWither {

	private final UUID worldId;
	private final WitherBoss wither;
	private final Map<UUID, ViewerState> viewers = new HashMap<>();

	private Location location;

	private record ViewerState(Player player, Location location) {
	}

	public WitherInstance(Location location) {
		this.location = location.clone();
		this.worldId = Objects.requireNonNull(location.getWorld()).getUID();

		ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();

		this.wither = new WitherBoss(EntityTypes.WITHER, world);
		this.wither.setNoAi(true);
		this.wither.setNoGravity(true);
		this.wither.setSilent(true);

		position(location);

		// Never add this entity to the server world.
	}

	private void position(Location target) {
		wither.setPos(target.getX(), target.getY(), target.getZ());
		wither.setYRot(target.getYaw());
		wither.setXRot(target.getPitch());
		wither.setYHeadRot(target.getYaw());
	}

	private boolean canView(Player player) {
		return player.isOnline() && player.getWorld().getUID().equals(worldId);
	}

	private static byte packedAngle(float angle) {
		return (byte) Math.floor(angle * 256.0 / 360.0);
	}

	private void sendSpawn(Player player) {
		ServerPlayer handle = ((CraftPlayer) player).getHandle();

		position(location);

		handle.connection.send(new ClientboundAddEntityPacket(wither, 0, wither.blockPosition()));

		viewers.put(player.getUniqueId(), new ViewerState(player, location.clone()));

		// Send the same initial metadata to every viewer.
		updateMetaData(player);

		handle.connection.send(new ClientboundRotateHeadPacket(wither, packedAngle(location.getYaw())));
	}

	@Override
	public void spawn(Player player) {
		if (!canView(player) || viewers.containsKey(player.getUniqueId())) {
			return;
		}

		sendSpawn(player);
	}

	@Override
	public void teleport(Player player, Location target) {
		if (target.getWorld() == null || !target.getWorld().getUID().equals(worldId)) {
			throw new IllegalArgumentException("A fake Wither cannot move between worlds");
		}

		location = target.clone();

		if (!canView(player)) {
			destroy(player);
			return;
		}

		if (!viewers.containsKey(player.getUniqueId())) {
			sendSpawn(player);
			return;
		}

		position(target);

		ServerPlayer handle = ((CraftPlayer) player).getHandle();

		// Send the current absolute position and body rotation.
		handle.connection.send(ClientboundEntityPositionSyncPacket.of(wither));

		handle.connection.send(new ClientboundRotateHeadPacket(wither, packedAngle(target.getYaw())));

		viewers.put(player.getUniqueId(), new ViewerState(player, target.clone()));
	}

	@Override
	public void destroy(Player player) {
		ViewerState removed = viewers.remove(player.getUniqueId());

		if (removed == null || !removed.player().isOnline()) {
			return;
		}

		((CraftPlayer) removed.player()).getHandle().connection
				.send(new ClientboundRemoveEntitiesPacket(wither.getId()));
	}

	@Override
	public void destroyAll() {
		for (ViewerState viewer : new ArrayList<>(viewers.values())) {
			destroy(viewer.player());
		}
	}

	@Override
	public int entityID() {
		return wither.getId();
	}

	@Override
	public void updateMetaData(Player player) {
		if (!canView(player)) {
			return;
		}

		ViewerState viewer = viewers.get(player.getUniqueId());
		
		if (viewer == null) {
			return; // Only update a model already spawned for this connection.
		}

		var metadata = wither.getEntityData().getNonDefaultValues();

		if (metadata == null || metadata.isEmpty()) {
			return;
		}

		((CraftPlayer) player).getHandle().connection
				.send(new ClientboundSetEntityDataPacket(wither.getId(), metadata));
	}
}