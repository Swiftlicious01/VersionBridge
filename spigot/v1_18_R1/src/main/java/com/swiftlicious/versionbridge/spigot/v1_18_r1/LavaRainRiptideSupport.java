package com.swiftlicious.versionbridge.spigot.v1_18_r1;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import com.swiftlicious.versionbridge.nms.inventory.HandSlot;

/** Native trident use and spin operations for this server version. */
final class LavaRainRiptideSupport {
	private LavaRainRiptideSupport() {
	}

	static boolean start(Player player, HandSlot slot) {
		final var handle = ((CraftPlayer) player).getHandle();
		final InteractionHand hand = slot == HandSlot.OFF ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		final var stack = handle.getItemInHand(hand);
		if (handle.isUsingItem() || handle.isInWaterOrRain() || !stack.is(Items.TRIDENT)
				|| stack.getDamageValue() >= stack.getMaxDamage() - 1)
			return false;
		handle.startUsingItem(hand);
		if (handle.isUsingItem())
			syncChargingPose(player);
		return handle.isUsingItem();
	}

	static int ticks(Player player, HandSlot slot) {
		final var handle = ((CraftPlayer) player).getHandle();
		if (handle.isInWaterOrRain())
			return -2;
		final InteractionHand hand = slot == HandSlot.OFF ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		if (!handle.isUsingItem() || handle.getUsedItemHand() != hand)
			return -1;
		final int ticks = handle.getTicksUsingItem();
		// Refresh after the denied vanilla-use response and its inventory correction.
		if (ticks <= 2)
			syncChargingPose(player);
		return ticks;
	}

	/** The dry client did not predict item use; send the real charging state to its owner. */
	private static void syncChargingPose(Player player) {
		final var handle = ((CraftPlayer) player).getHandle();
		// The legacy packet constructor clears dirty tracking; serialize a snapshot instead.
		final var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
		try {
			buf.writeVarInt(handle.getId());
			net.minecraft.network.syncher.SynchedEntityData.pack(handle.getEntityData().getAll(), buf);
			handle.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(buf));
		} finally {
			buf.release();
		}
	}

	static void cancel(Player player, HandSlot slot) {
		final var handle = ((CraftPlayer) player).getHandle();
		final InteractionHand hand = slot == HandSlot.OFF ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		if (handle.isUsingItem() && handle.getUsedItemHand() == hand && handle.getUseItem().is(Items.TRIDENT))
			handle.stopUsingItem();
	}

	@SuppressWarnings("deprecation")
	static boolean release(Player player, HandSlot slot, int level) {
		final var handle = ((CraftPlayer) player).getHandle();
		final InteractionHand hand = slot == HandSlot.OFF ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		final var stack = handle.getItemInHand(hand);
		if (level <= 0 || handle.isUsingItem() || handle.isInWaterOrRain() || handle.isPassenger()
				|| !stack.is(Items.TRIDENT) || stack.getDamageValue() >= stack.getMaxDamage() - 1)
			return false;

		final org.bukkit.inventory.ItemStack item = (slot == HandSlot.OFF
				? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand()).clone();
		// Vanilla Riptide strength: levels I/II/III launch at 1.5/2.25/3.0.
		final Vector motion = player.getLocation().getDirection().multiply(3.0 * (1.0 + level) / 4.0);
		final Vector velocity = player.getVelocity().add(motion);
		final PlayerRiptideEvent event = new PlayerRiptideEvent(player, item);
		Bukkit.getPluginManager().callEvent(event);
		if ((Object) event instanceof org.bukkit.event.Cancellable cancelled && cancelled.isCancelled())
			return false;
		if (!player.isOnline() || player.isDead() || handle.getItemInHand(hand) != stack)
			return false;

		stack.hurtAndBreak(1, handle, entity -> entity.broadcastBreakEvent(hand));
		handle.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(Items.TRIDENT));

		// The client did not perform its dry-weather launch prediction. Synchronize
		// vanilla's collision-aware ground lift as well as the resulting velocity.
		if (player.isOnGround()) {
			handle.move(MoverType.SELF, new Vec3(0.0, 1.1999999284744263, 0.0));
			handle.connection.teleport(handle.getX(), handle.getY(), handle.getZ(), player.getLocation().getYaw(),
					player.getLocation().getPitch());
		}
		handle.startAutoSpinAttack(20);
		player.setVelocity(velocity);
		// The shared weather handler plays the release sound after a successful launch.
		return true;
	}
}
