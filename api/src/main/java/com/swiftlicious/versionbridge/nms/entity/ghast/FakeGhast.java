package com.swiftlicious.versionbridge.nms.entity.ghast;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.swiftlicious.versionbridge.nms.entity.FakeEntity;

/**
 * A packet-only Ghast model with per-viewer movement and cleanup.
 * Use on the server thread. No real Bukkit entity is spawned; callers must
 * remove viewers who leave and invoke destroyAll when the animation ends.
 */
public interface FakeGhast extends FakeEntity {

	/**
	 * Updates the fake Ghast's position and rotation for the specified viewer.
	 * Spawns the visual entity first if it is not already tracked for that viewer.
	 *
	 * @param viewer   the player receiving the visual update
	 * @param location the destination, including yaw and pitch, in the same world
	 */
	void teleport(Player viewer, Location location);

	/**
	 * Removes the fake Ghast from all tracked viewers' clients and clears viewer
	 * tracking. Does not remove or modify any real server entity.
	 */
	void destroyAll();
}