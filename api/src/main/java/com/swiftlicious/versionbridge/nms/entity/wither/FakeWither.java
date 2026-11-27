package com.swiftlicious.versionbridge.nms.entity.wither;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.swiftlicious.versionbridge.nms.entity.FakeEntity;

public interface FakeWither extends FakeEntity {

	/**
	 * Updates the fake Wither's position and rotation for the specified viewer.
	 * Spawns the visual entity first if it is not already tracked for that viewer.
	 *
	 * @param viewer   the player receiving the visual update
	 * @param location the destination, including yaw and pitch, in the same world
	 */
	void teleport(Player viewer, Location location);

	/**
	 * Removes the fake Wither from all tracked viewers' clients and clears viewer
	 * tracking. Does not remove or modify any real server entity.
	 */
	void destroyAll();
}