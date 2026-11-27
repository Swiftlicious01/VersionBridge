package com.swiftlicious.versionbridge.nms.entity.display;

import org.bukkit.inventory.ItemStack;

import com.swiftlicious.versionbridge.nms.entity.FakeEntity;

public interface FakeItemDisplay extends FakeEntity {

	void item(ItemStack itemStack);
}