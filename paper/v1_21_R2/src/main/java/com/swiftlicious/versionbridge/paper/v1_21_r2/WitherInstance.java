package com.swiftlicious.versionbridge.paper.v1_21_r2;

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
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
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

		this.wither = new WitherBoss(EntityType.WITHER, world);
		this.wither.setNoAi(true);
		this.wither.setNoGravity(true);
		this.wither.setSilent(true);
		this.wither.setInvulnerable(true);

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

	private static long packedPosition(double coordinate) {
		return Math.round(coordinate * 4096.0);
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

		ViewerState previous = viewers.get(player.getUniqueId());

		if (previous == null || !previous.player().getUniqueId().equals(player.getUniqueId())) {
			sendSpawn(player);
			return;
		}

		long dx = packedPosition(target.getX()) - packedPosition(previous.location().getX());
		long dy = packedPosition(target.getY()) - packedPosition(previous.location().getY());
		long dz = packedPosition(target.getZ()) - packedPosition(previous.location().getZ());

		// Relative packets have a limited range; respawn for a large jump.
		if (dx < Short.MIN_VALUE || dx > Short.MAX_VALUE || dy < Short.MIN_VALUE || dy > Short.MAX_VALUE
				|| dz < Short.MIN_VALUE || dz > Short.MAX_VALUE) {
			destroy(player);
			sendSpawn(player);
			return;
		}

		position(target);

		ServerPlayer handle = ((CraftPlayer) player).getHandle();

		handle.connection.send(new ClientboundMoveEntityPacket.PosRot(wither.getId(), (short) dx, (short) dy,
				(short) dz, packedAngle(target.getYaw()), packedAngle(target.getPitch()), false));

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