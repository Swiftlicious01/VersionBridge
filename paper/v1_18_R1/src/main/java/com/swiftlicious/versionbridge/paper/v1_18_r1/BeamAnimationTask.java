package com.swiftlicious.versionbridge.paper.v1_18_r1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.swiftlicious.versionbridge.nms.beam.BeamAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public final class BeamAnimationTask implements BeamAnimation {

	private final Set<UUID> viewerIds = new LinkedHashSet<>();
	private final Map<UUID, Player> shownTo = new HashMap<>();

	private final World world;
	private final UUID worldId;
	private final Location origin;
	private final EndCrystal crystal;
	private final int durationTicks;

	private int ticks;
	private boolean finished;

	public BeamAnimationTask(Collection<Player> viewers, Location location, int durationTicks) {

		this.world = Objects.requireNonNull(location.getWorld(), "Beam world cannot be null");
		this.worldId = world.getUID();
		this.durationTicks = Math.max(0, durationTicks);

		// Align the crystal with its block-based beam target.
		this.origin = new Location(world, location.getBlockX() + 0.5, location.getY(), location.getBlockZ() + 0.5);

		for (Player viewer : viewers) {
			viewerIds.add(viewer.getUniqueId());
		}

		final ServerLevel level = ((CraftWorld) world).getHandle();

		this.crystal = new EndCrystal(EntityType.END_CRYSTAL, level);
		this.crystal.setShowBottom(false);
		this.crystal.setPos(origin.getX(), origin.getY(), origin.getZ());

		this.crystal.setBeamTarget(new BlockPos(origin.getBlockX(), world.getMaxHeight() - 1, origin.getBlockZ()));

		// Never add the crystal to the server world.
		this.finished = this.durationTicks == 0;
	}

	private boolean canView(Player player) {
		return player != null && player.isOnline() && player.getWorld().getUID().equals(worldId)
				&& player.getLocation().distanceSquared(origin) <= 64.0 * 64.0;
	}

	private void show(Player player) {
		final ServerPlayer handle = ((CraftPlayer) player).getHandle();

		// Track before sending so partial failures can still be cleaned up.
		shownTo.put(player.getUniqueId(), player);

		handle.connection
				.send(new ClientboundAddEntityPacket(crystal, EntityType.END_CRYSTAL, 0, crystal.blockPosition()));

		// Unlike packDirty(), this remains available for every viewer.
		final var metadata = crystal.getEntityData();

		if (metadata != null && !metadata.isEmpty()) {
			handle.connection.send(new ClientboundSetEntityDataPacket(crystal.getId(), metadata, true));
		}
	}

	private void hide(UUID playerId) {
		final Player previous = shownTo.remove(playerId);

		if (previous == null || !previous.isOnline()) {
			return;
		}

		((CraftPlayer) previous).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(crystal.getId()));
	}

	private void renderPulse(Player viewer) {
		final double pulse = (Math.sin(ticks / 8.0) + 1.0) * 0.5;
		final double radius = 0.35 + pulse * 0.7;
		final float size = 0.8F + (float) pulse * 0.8F;

		final Particle.DustOptions black = new Particle.DustOptions(Color.BLACK, size);

		final Particle.DustOptions darkGray = new Particle.DustOptions(Color.fromRGB(45, 45, 45), size);

		// Rotating rings pulse around the lower section of the actual beam.
		for (int ring = 0; ring < 5; ring++) {
			final double y = origin.getY() + 1.0 + ring * 2.0;

			if (y >= world.getMaxHeight()) {
				break;
			}

			for (int point = 0; point < 10; point++) {
				final double angle = point * Math.PI * 2.0 / 10.0 + ticks * 0.045 + ring * 0.3;

				viewer.spawnParticle(Particle.REDSTONE, origin.getX() + Math.cos(angle) * radius, y,
						origin.getZ() + Math.sin(angle) * radius, 1, 0.02, 0.04, 0.02, 0.0,
						point % 2 == 0 ? black : darkGray);
			}
		}

		if (ticks % 8 == 0) {
			viewer.spawnParticle(Particle.SMOKE_LARGE, origin.getX(), origin.getY() + 0.4, origin.getZ(),
					6 + (int) (pulse * 8), 0.65, 0.2, 0.65, 0.015);
		}
	}

	@Override
	public void run() {
		if (finished) {
			return;
		}

		if (!world.isChunkLoaded(origin.getBlockX() >> 4, origin.getBlockZ() >> 4)) {
			cancel();
			return;
		}

		try {
			for (UUID playerId : viewerIds) {
				final Player player = Bukkit.getPlayer(playerId);
				final Player previous = shownTo.get(playerId);

				// A disconnected connection no longer owns a visible entity.
				if (previous != null && !previous.isOnline()) {
					shownTo.remove(playerId);
				}

				if (!canView(player)) {
					hide(playerId);
					continue;
				}

				if (!shownTo.containsKey(playerId)) {
					show(player);
				}

				if ((ticks & 1) == 0) {
					renderPulse(player);
				}
			}

			if (++ticks >= durationTicks) {
				cancel();
			}
		} catch (RuntimeException ex) {
			try {
				cancel();
			} catch (RuntimeException cleanupError) {
				ex.addSuppressed(cleanupError);
			}

			throw ex;
		}
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void cancel() {
		if (finished) {
			return;
		}

		finished = true;
		RuntimeException failure = null;

		for (UUID playerId : new ArrayList<>(shownTo.keySet())) {
			try {
				hide(playerId);
			} catch (RuntimeException ex) {
				if (failure == null) {
					failure = ex;
				} else {
					failure.addSuppressed(ex);
				}
			}
		}

		shownTo.clear();

		if (failure != null) {
			throw failure;
		}
	}
}