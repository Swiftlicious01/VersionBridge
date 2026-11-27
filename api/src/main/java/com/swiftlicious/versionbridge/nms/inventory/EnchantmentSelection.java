package com.swiftlicious.versionbridge.nms.inventory;

import java.util.Map;
import org.bukkit.enchantments.Enchantment;

/**
 * A complete seeded vanilla selection and the guaranteed clue displayed for it.
 */
public record EnchantmentSelection(Map<Enchantment, Integer> enchantments, Enchantment hint, int hintLevel, int seed) {
	public EnchantmentSelection {
		enchantments = Map.copyOf(enchantments);
	}
}