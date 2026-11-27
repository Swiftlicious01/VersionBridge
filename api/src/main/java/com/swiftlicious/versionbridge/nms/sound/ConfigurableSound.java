package com.swiftlicious.versionbridge.nms.sound;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Routes version-specific sound effects through the core's configured Adventure
 * playback.
 */
public final class ConfigurableSound {

	private static volatile Consumer<Request> playback = request -> {
	};

	private ConfigurableSound() {
	}

	/**
	 * Installs playback after sound configuration loads, without linking NMS
	 * modules to core classes.
	 */
	public static void setPlayback(Consumer<Request> handler) {
		playback = Objects.requireNonNull(handler);
	}

	/**
	 * Sends a sound only to the animation viewer, preserving its world position and
	 * original settings.
	 */
	public static void play(Player player, Location location, String usage, String key, float volume, float pitch) {
		playback.accept(new Request(player, location.clone(), usage, key, volume, pitch));
	}

	public record Request(Player player, Location location, String usage, String key, float volume, float pitch) {
	}
}