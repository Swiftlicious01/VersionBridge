package com.swiftlicious.versionbridge.nms.invasion;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PiglinAIUtils {

	/**
	 * Removes passive goals from the given entity.
	 */
	abstract void stripAllPassiveGoals(@NotNull LivingEntity bukkitEntity);

	/**
	 * Adds a "look at nearest player" goal to the entity.
	 */
	abstract void addFollowPlayerGoal(@NotNull LivingEntity bukkitEntity);

	/**
	 * Restores default goal selector for Striders.
	 */
	abstract void restoreStriderGoals(@NotNull org.bukkit.entity.Strider bukkitStrider);

	/**
	 * Restores default goal selector for Magma Cubes.
	 */
	abstract void restoreMagmaCubeGoals(@NotNull org.bukkit.entity.MagmaCube magma);

	/**
	 * Sets the target to be attacked for the defined invasion mob.
	 */
	default void setInvasionTarget(@NotNull org.bukkit.entity.Mob mob,
			@Nullable org.bukkit.entity.LivingEntity target) {
		mob.setTarget(target);
	}

	/**
	 * Performs one short, terrain-validated retreat movement.
	 *
	 * Called on the server thread. The caller validates support and clearance. The
	 * implementation preserves passengers and applies block collisions.
	 */
	abstract void moveRetreatUnit(@NotNull org.bukkit.entity.Mob mob, @NotNull org.bukkit.util.Vector movement);

	/**
	 * Overrides the rendered temperature of an occupied invasion Strider for one
	 * viewer.
	 *
	 * The warm view includes the client-only no-AI bit, preventing the client's
	 * local temperature tick from immediately making it shiver again. Server AI and
	 * all unrelated entity flags remain unchanged. Pass false to restore the
	 * entity's real flags when the rider detaches or the encounter ends. Must be
	 * called on the server thread and refreshed for newly tracking viewers.
	 */
	abstract void sendStriderWarmState(@NotNull org.bukkit.entity.Player viewer,
			@NotNull org.bukkit.entity.Strider strider, boolean warm);
}