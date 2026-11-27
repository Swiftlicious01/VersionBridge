package com.swiftlicious.versionbridge.nms.util;

import java.util.Objects;

import org.bukkit.NamespacedKey;

/** Supplies the persistent key used by the consuming plugin's fortress data. */
public final class FakeFortressKeys {

	private static volatile NamespacedKey ownerKey = Objects.requireNonNull(
			NamespacedKey.fromString("versionbridge:fake_fortress_v1"));

	private FakeFortressKeys() {
	}

	public static void setOwnerKey(NamespacedKey key) {
		ownerKey = Objects.requireNonNull(key, "key");
	}

	public static NamespacedKey ownerKey() {
		return ownerKey;
	}
}
