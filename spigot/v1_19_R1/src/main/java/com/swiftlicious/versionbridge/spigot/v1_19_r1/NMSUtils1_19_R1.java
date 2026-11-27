package com.swiftlicious.versionbridge.spigot.v1_19_r1;

import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.world.inventory.BrewingStandMenu;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftFishHook;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftMinecart;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftWither;
import org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftVector;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.versionbridge.nms.NMSHandler;
import com.swiftlicious.versionbridge.nms.border.BorderColor;
import com.swiftlicious.versionbridge.nms.bossbar.BossBarColor;
import com.swiftlicious.versionbridge.nms.bossbar.BossBarOverlay;
import com.swiftlicious.versionbridge.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.versionbridge.nms.entity.display.FakeItemDisplay;
import com.swiftlicious.versionbridge.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.versionbridge.nms.entity.firework.FakeFirework;
import com.swiftlicious.versionbridge.nms.entity.wither.FakeWither;
import com.swiftlicious.versionbridge.nms.exception.UnsupportedVersionException;
import com.swiftlicious.versionbridge.nms.fluid.FluidData;
import com.swiftlicious.versionbridge.nms.inventory.HandSlot;
import com.swiftlicious.versionbridge.nms.util.BossBarUtils;
import com.swiftlicious.versionbridge.nms.util.SelfIncreaseEntityID;

import io.netty.buffer.Unpooled;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class NMSUtils1_19_R1 implements NMSHandler {

	// Fields and initialization

	private final Enum<?> addBossBarOperation;
	private final Enum<?> updateBossBarNameOperation;

	private final Enum<?> updateBossBarProgressOperation;
	private final Enum<?> updateBossBarStyleOperation;

	private final EntityDataAccessor<Boolean> dataBiting;

	private static final NamespacedKey FAKE_FORTRESS_KEY = java.util.Objects
			.requireNonNull(com.swiftlicious.versionbridge.nms.util.FakeFortressKeys.ownerKey());
	private static final int FAKE_FORTRESS_PIECE_DEPTH = 1_904_271;

	private final Field fishingHookOwnerUUIDField;
	private final Method sendPacketImmediateMethod;

	@SuppressWarnings("unchecked")
	public NMSUtils1_19_R1() {
		try {
			Class<?> cls = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutBoss$d");
			Field fieldAdd = cls.getDeclaredField("a");
			fieldAdd.setAccessible(true);
			addBossBarOperation = (Enum<?>) fieldAdd.get(null);
			Field fieldUpdateProgress = cls.getDeclaredField("c");
			fieldUpdateProgress.setAccessible(true);
			updateBossBarProgressOperation = (Enum<?>) fieldUpdateProgress.get(null);
			Field fieldUpdateName = cls.getDeclaredField("d");
			fieldUpdateName.setAccessible(true);
			updateBossBarNameOperation = (Enum<?>) fieldUpdateName.get(null);
			Field fieldUpdateStyle = cls.getDeclaredField("e");
			fieldUpdateStyle.setAccessible(true);
			updateBossBarStyleOperation = (Enum<?>) fieldUpdateStyle.get(null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get add boss bar operation", e);
		}
		try {
			Field dataBitingField = FishingHook.class.getDeclaredField("ap");
			dataBitingField.setAccessible(true);
			dataBiting = (EntityDataAccessor<Boolean>) dataBitingField.get(null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to get hook biting state", e);
		}
		try {
			fishingHookOwnerUUIDField = com.swiftlicious.versionbridge.nms.util.ReflectionUtils.getInstanceFieldInHierarchy(FishingHook.class, UUID.class, "ownerUUID");
			fishingHookOwnerUUIDField.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException("Could not access FishingHook.ownerUUID", e);
		}
		try {
			sendPacketImmediateMethod = Connection.class.getDeclaredMethod("sendPacket", Packet.class,
					PacketSendListener.class, Boolean.class);
			sendPacketImmediateMethod.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to get send packet method", e);
		}
		FluidDataInstance.register(LavaFluid.Source.class, FallingFluidDataInstance::new);
		FluidDataInstance.register(WaterFluid.Source.class, FallingFluidDataInstance::new);
		FluidDataInstance.register(LavaFluid.Flowing.class, FlowingFluidDataInstance::new);
		FluidDataInstance.register(WaterFluid.Flowing.class, FlowingFluidDataInstance::new);
	}

	// Messages and notifications

	@Override
	public void sendPlayerListHeaderFooter(Player player, String header, String footer) {
		Component top = CraftChatMessage.fromStringOrNull(header, true);
		Component bottom = CraftChatMessage.fromStringOrNull(footer, true);
		((CraftPlayer) player).getHandle().connection.send(new ClientboundTabListPacket(
				top == null ? Component.literal("") : top, bottom == null ? Component.literal("") : bottom));
	}

	@Override
	public Object getMinecraftComponent(String json) {
		return CraftChatMessage.fromJSON(json);
	}

	@Override
	public void sendMessage(Player player, String messageJson) {
		ClientboundSystemChatPacket systemChatPacket = new ClientboundSystemChatPacket(
				CraftChatMessage.fromJSON(messageJson), false);
		((CraftPlayer) player).getHandle().connection.send(systemChatPacket);
	}

	@Override
	public void sendActionBar(Player player, String json) {
		CraftPlayer craftPlayer = (CraftPlayer) player;
		ServerPlayer serverPlayer = craftPlayer.getHandle();
		ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(
				Objects.requireNonNull(Component.Serializer.fromJson(json)));
		serverPlayer.connection.send(packet);
	}

	@Override
	public void sendTitle(Player player, @Nullable String titleJson, @Nullable String subTitleJson, int fadeInTicks,
			int stayTicks, int fadeOutTicks) {
		CraftPlayer craftPlayer = (CraftPlayer) player;
		ServerPlayer serverPlayer = craftPlayer.getHandle();
		ArrayList<Packet<ClientGamePacketListener>> packetListeners = new ArrayList<>();
		packetListeners.add(new ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks));
		if (titleJson != null) {
			packetListeners.add(new ClientboundSetTitleTextPacket(
					Objects.requireNonNull(Component.Serializer.fromJson(titleJson))));
		} else {
			packetListeners.add(new ClientboundSetTitleTextPacket(Objects.requireNonNull(Component.empty())));
		}
		if (subTitleJson != null) {
			packetListeners.add(new ClientboundSetSubtitleTextPacket(
					Objects.requireNonNull(Component.Serializer.fromJson(subTitleJson))));
		}
		packetListeners.forEach(packet -> sendPacketImmediately(serverPlayer, packet));
	}

	@Override
	public void sendToast(Player player, ItemStack icon, String titleJson, String advancementType) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(icon);
		DisplayInfo displayInfo = new DisplayInfo(nmsStack,
				Objects.requireNonNull(Component.Serializer.fromJson(titleJson)), Component.literal(""), null,
				FrameType.valueOf(advancementType), true, false, true);
		AdvancementRewards advancementRewards = AdvancementRewards.EMPTY;
		ResourceLocation id = new ResourceLocation("versionbridge", "toast");
		Criterion criterion = new Criterion(new ImpossibleTrigger.TriggerInstance());
		HashMap<String, Criterion> criteria = new HashMap<>(Map.of("impossible", criterion));
		String[][] requirements = { { "impossible" } };
		Advancement advancement = new Advancement(id, null, displayInfo, advancementRewards, criteria, requirements);
		Map<ResourceLocation, AdvancementProgress> advancementsToGrant = new HashMap<>();
		AdvancementProgress advancementProgress = new AdvancementProgress();
		advancementProgress.update(criteria, requirements);
		Objects.requireNonNull(advancementProgress.getCriterion("impossible")).grant();
		advancementsToGrant.put(id, advancementProgress);
		ClientboundUpdateAdvancementsPacket packet1 = new ClientboundUpdateAdvancementsPacket(false,
				new ArrayList<>(List.of(advancement)), new HashSet<>(), advancementsToGrant);
		serverPlayer.connection.send(packet1);
		ClientboundUpdateAdvancementsPacket packet2 = new ClientboundUpdateAdvancementsPacket(false, new ArrayList<>(),
				new HashSet<>(List.of(id)), new HashMap<>());
		serverPlayer.connection.send(packet2);
	}

	private void sendPacketImmediately(ServerPlayer serverPlayer, Packet<ClientGamePacketListener> packet) {
		try {
			sendPacketImmediateMethod.invoke(serverPlayer.connection.connection, packet, null, true);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to send packet", e);
		}
	}

	// Boss bars

	@Override
	public void createBossBar(Player player, UUID uuid, Object component, BossBarColor color, BossBarOverlay overlay,
			float progress, boolean createWorldFog, boolean playBossMusic, boolean darkenScreen) {
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			buf.writeUUID(uuid);
			buf.writeEnum(addBossBarOperation);
			buf.writeComponent((Component) component);
			buf.writeFloat(progress);
			buf.writeEnum(BossEvent.BossBarColor.valueOf(color.name()));
			buf.writeEnum(BossEvent.BossBarOverlay.valueOf(overlay.name()));
			buf.writeByte(BossBarUtils.encodeProperties(darkenScreen, playBossMusic, createWorldFog));

			final ClientboundBossEventPacket packet = new ClientboundBossEventPacket(buf);
			((CraftPlayer) player).getHandle().connection.send(packet);
		} finally {
			buf.release();
		}
	}

	@Override
	public void updateBossBarName(Player player, UUID uuid, Object component) {
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			buf.writeUUID(uuid);
			buf.writeEnum(updateBossBarNameOperation);
			buf.writeComponent((Component) component);

			final ClientboundBossEventPacket packet = new ClientboundBossEventPacket(buf);
			((CraftPlayer) player).getHandle().connection.send(packet);
		} finally {
			buf.release();
		}
	}

	@Override
	public void updateBossBarProgress(Player player, UUID uuid, float progress) {
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			buf.writeUUID(uuid);
			buf.writeEnum(updateBossBarProgressOperation);
			buf.writeFloat(progress);

			final ClientboundBossEventPacket packet = new ClientboundBossEventPacket(buf);
			((CraftPlayer) player).getHandle().connection.send(packet);
		} finally {
			buf.release();
		}
	}

	@Override
	public void updateBossBarStyle(Player player, UUID uuid, BossBarColor color, BossBarOverlay overlay) {
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			buf.writeUUID(uuid);
			buf.writeEnum(updateBossBarStyleOperation);
			buf.writeEnum(BossEvent.BossBarColor.valueOf(color.name()));
			buf.writeEnum(BossEvent.BossBarOverlay.valueOf(overlay.name()));

			final ClientboundBossEventPacket packet = new ClientboundBossEventPacket(buf);
			((CraftPlayer) player).getHandle().connection.send(packet);
		} finally {
			buf.release();
		}
	}

	@Override
	public void removeBossBar(Player player, UUID uuid) {
		((CraftPlayer) player).getHandle().connection.send(ClientboundBossEventPacket.createRemovePacket(uuid));
	}

	// Inventories and container displays

	@Override
	public void openCustomInventory(Player player, Inventory inventory, String jsonTitle) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		MenuType<?> menuType = CraftContainer.getNotchInventoryType(inventory);
		AbstractContainerMenu menu = new CraftContainer(inventory, serverPlayer, serverPlayer.nextContainerCounter());
		menu = CraftEventFactory.callInventoryOpenEvent(serverPlayer, menu);
		if (menu != null) {
			Component titleComponent = CraftChatMessage.fromJSON(jsonTitle);
			menu.checkReachable = false;
			serverPlayer.connection.send(new ClientboundOpenScreenPacket(menu.containerId, menuType, titleComponent));
			serverPlayer.containerMenu = menu;
			serverPlayer.initMenu(menu);
		}
	}

	@Override
	public void updateInventoryTitle(Player player, String jsonTitle) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		AbstractContainerMenu menu = serverPlayer.containerMenu;
		serverPlayer.connection.send(new ClientboundOpenScreenPacket(menu.containerId, menu.getType(),
				CraftChatMessage.fromJSON(jsonTitle)));
		serverPlayer.initMenu(menu);
	}

	@Override
	public void sendBrewingDisplay(Player player, int ticksLeft, int fuel) {
		final var handle = ((CraftPlayer) player).getHandle();
		if (!(handle.containerMenu instanceof BrewingStandMenu))
			return;

		final int menuId = handle.containerMenu.containerId;
		handle.connection.send(new ClientboundContainerSetDataPacket(menuId, 0, Math.max(0, ticksLeft)));
		handle.connection.send(new ClientboundContainerSetDataPacket(menuId, 1, Math.max(0, fuel)));
	}

	@Override
	public void playChestAnimation(Player player, Location location, boolean open) {
		ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
		BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		BlockState blockState = serverLevel.getBlockState(pos);
		Block block = blockState.getBlock();

		int actionId = open ? 1 : 0;

		// Get the NMS player connection
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

		// Handle (double/single) chests and trapped chests
		if (block instanceof ChestBlock || block instanceof TrappedChestBlock) {
			// Send animation for the current chest block
			serverPlayer.connection.send(new ClientboundBlockEventPacket(pos, block, 1, actionId));

			// Play appropriate sound
			com.swiftlicious.versionbridge.nms.sound.ConfigurableSound.play(player, location, open ? "containers.chest.open" : "containers.chest.close",
 open ? "minecraft:block.chest.open" : "minecraft:block.chest.close", 1.0f, 1.0f);

			// Check and handle the second half of a double chest
			ChestType type = blockState.getValue(ChestBlock.TYPE);
			if (type != ChestType.SINGLE) {
				Direction facing = blockState.getValue(ChestBlock.FACING);
				Direction offset = getConnectedChestOffset(type, facing);

				BlockPos otherHalfPos = new BlockPos(pos.getX() + offset.getStepX(), pos.getY() + offset.getStepY(),
						pos.getZ() + offset.getStepZ());

				BlockState otherHalfState = serverLevel.getBlockState(otherHalfPos);
				if (otherHalfState.getBlock() == block) {
					serverPlayer.connection.send(new ClientboundBlockEventPacket(otherHalfPos, block, 1, actionId));
				}
			}
			return;
		}

		// Ender Chest
		if (block instanceof EnderChestBlock) {
			serverPlayer.connection.send(new ClientboundBlockEventPacket(pos, block, 1, actionId));
			com.swiftlicious.versionbridge.nms.sound.ConfigurableSound.play(player, location, open ? "containers.ender-chest.open" : "containers.ender-chest.close",
 open ? "minecraft:block.ender_chest.open" : "minecraft:block.ender_chest.close", 1.0f, 1.0f);
			return;
		}

		// Barrel
		if (block instanceof BarrelBlock) {
			serverPlayer.connection.send(new ClientboundBlockEventPacket(pos, block, 1, actionId));
			com.swiftlicious.versionbridge.nms.sound.ConfigurableSound.play(player, location, open ? "containers.barrel.open" : "containers.barrel.close",
 open ? "minecraft:block.barrel.open" : "minecraft:block.barrel.close", 1.0f, 1.0f);
			return;
		}

		// Shulker Boxes (all colors)
		if (block instanceof ShulkerBoxBlock) {
			serverPlayer.connection.send(new ClientboundBlockEventPacket(pos, block, 1, actionId));
			com.swiftlicious.versionbridge.nms.sound.ConfigurableSound.play(player, location, open ? "containers.shulker-box.open" : "containers.shulker-box.close",
 open ? "minecraft:block.shulker_box.open" : "minecraft:block.shulker_box.close", 1.0f, 1.0f);
			return;
		}
	}

	/**
	 * Gets the relative direction to the other half of a double chest based on its
	 * type and facing.
	 *
	 * LEFT and RIGHT are determined from the player's point of view looking at the
	 * front of the chest.
	 *
	 * @param chestType the type of the chest (LEFT, RIGHT, SINGLE)
	 * @param facing    the front-facing direction of the chest
	 * @return the direction toward the connected chest block
	 */
	private Direction getConnectedChestOffset(ChestType chestType, Direction facing) {
		return chestType == ChestType.LEFT ? facing.getClockWise() // LEFT chest connects clockwise
				: facing.getCounterClockWise(); // RIGHT chest connects counter-clockwise
	}

	// World borders

	@Override
	public void sendWorldBorder(Player player, BoundingBox box, BorderColor borderColor) {
		org.bukkit.World world = player.getWorld();
		ServerLevel serverLevel = ((CraftWorld) world).getHandle();
		Location center = box.getCenter().toLocation(world);

		double minX = box.getMinX();
		double minZ = box.getMinZ();

		double maxX = box.getMaxX();
		double maxZ = box.getMaxZ();

		double width = maxX - minX; // Dimension along the X-axis
		double length = maxZ - minZ; // Dimension along the Z-axis

		double size = Math.max(width, length) / 2.0D;

		WorldBorder worldBorder = new WorldBorder();
		worldBorder.world = serverLevel;
		worldBorder.setWarningBlocks(0);
		worldBorder.setCenter(center.getX(), center.getZ());

		switch (borderColor) {
		case BLUE -> {
			worldBorder.setSize(size * 2);
		}
		case GREEN -> {
			worldBorder.setSize(size * 2);
			worldBorder.lerpSizeBetween(worldBorder.getSize() - 0.001D, worldBorder.getSize(), Long.MAX_VALUE);
		}
		case RED -> {
			worldBorder.setSize(size * 2);
			worldBorder.lerpSizeBetween(worldBorder.getSize(), worldBorder.getSize() - 0.001D, Long.MAX_VALUE);
		}
		}

		ClientboundInitializeBorderPacket initializeBorderPacket = new ClientboundInitializeBorderPacket(worldBorder);
		((CraftPlayer) player).getHandle().connection.send(initializeBorderPacket);
	}

	@Override
	public void updateWorldBorder(Player player, Location center, double startSize, double endSize, long durationMs,
			BorderColor borderColor) {
		ServerLevel serverLevel = ((CraftWorld) center.getWorld()).getHandle();

		WorldBorder worldBorder = new WorldBorder();
		worldBorder.world = serverLevel;
		worldBorder.setWarningBlocks(0);
		worldBorder.setCenter(center.getX(), center.getZ());

		// Expansion is naturally green; BorderHandler restores the red border after completion.
		// Do not replace this finite transition with an immediate infinite transition.
		worldBorder.lerpSizeBetween(startSize, endSize, durationMs);

		ClientboundInitializeBorderPacket packet = new ClientboundInitializeBorderPacket(worldBorder);
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	@Override
	public void clearWorldBorder(Player player) {
		ServerLevel serverLevel = ((CraftWorld) player.getWorld()).getHandle();
		WorldBorder worldBorder = new WorldBorder();
		worldBorder.world = serverLevel;
		worldBorder.setCenter(0, 0); // default world spawn
		worldBorder.setSize(30_000_000); // vanilla max border size

		ClientboundInitializeBorderPacket packet = new ClientboundInitializeBorderPacket(worldBorder);
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	// World data and block physics

	@Override
	public FluidData getFluidData(Location location) {
		World world = location.getWorld();
		FluidState state = ((CraftWorld) world).getHandle()
				.getFluidState(new BlockPos(location.getX(), location.getY(), location.getZ()));
		return FluidDataInstance.createData(state);
	}

	@Override
	public String getBiomeResourceLocation(Location location) {
		ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();

		// Get the biome Holder at the location
		Holder<Biome> biomeHolder = world.getNoiseBiome(location.getBlockX() >> 2, location.getBlockY() >> 2,
				location.getBlockZ() >> 2);

		// Get biome key from the Holder
		return biomeHolder.unwrapKey().map(resourceKey -> resourceKey.location().toString()) // "minecraft:plains"
				.orElse("void"); // fallback if unregistered
	}

	@Override
	public boolean scheduleGravityCheck(org.bukkit.block.Block block) {
		final World world = block.getWorld();
		if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
			return false;
		}

		final ServerLevel level = ((CraftWorld) world).getHandle();
		final BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
		final BlockState state = level.getBlockState(pos);

		if (!(state.getBlock() instanceof net.minecraft.world.level.block.FallingBlock)) {
			return false;
		}

		// Let the normal block tick decide whether support permits falling.
		level.scheduleTick(pos, state.getBlock(), 2);
		return true;
	}

	// Fortress regions

	/**
	 * Compatibility entry point for callers which do not supply diagnostics.
	 */
	@Override
	public void injectFakeFortress(World world, BoundingBox bounds) {
		injectFakeFortress(world, bounds, message -> {
		});
	}

	/**
	 * Registers a bounded fortress region without placing structure blocks.
	 *
	 * Each affected, fully generated chunk receives one bounded fortress piece and
	 * a reference to its own structure start.
	 *
	 * Ownership information is stored on the chunk so removal remains possible
	 * after the chunk unloads or the server restarts.
	 *
	 * Existing valid fortress starts are preserved: conflicting injections fail
	 * before any chunks are modified.
	 *
	 * Diagnostics are supplied by core through its existing debug method.
	 */
	@Override
	public void injectFakeFortress(World world, BoundingBox bounds, Consumer<String> debug) {
		java.util.Objects.requireNonNull(debug, "debug");
		requireFortressThread(world);

		final long started = System.nanoTime();
		final ServerLevel level = ((CraftWorld) world).getHandle();
		final Structure fortress = getFortressStructure(level);
		final int[] region = getFortressRegion(world, bounds);
		final String owner = getFortressOwner(region);

		final int minChunkX = region[0] >> 4;
		final int maxChunkX = region[3] >> 4;
		final int minChunkZ = region[2] >> 4;
		final int maxChunkZ = region[5] >> 4;

		final List<LevelChunk> chunks = new ArrayList<>();

		debug.accept("[Fortress] Injection started: world=" + world.getName() + ", bounds=" + bounds + ", ownerMarker="
				+ owner);

		// Load full chunks and validate every destination before modifying any.
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
				final org.bukkit.Chunk bukkitChunk = world.getChunkAt(chunkX, chunkZ);

				final String storedOwner = bukkitChunk.getPersistentDataContainer().get(FAKE_FORTRESS_KEY,
						PersistentDataType.STRING);

				final StructureStart existing = chunk.getStartForStructure(fortress);

				if (storedOwner != null) {
					if (!storedOwner.startsWith(owner + "|") || !isOwnedFortressStart(existing)) {
						debug.accept("[Fortress] Injection conflict: world=" + world.getName() + ", chunk=" + chunkX
								+ "," + chunkZ + ", expectedOwner=" + owner + ", storedOwner=" + storedOwner
								+ ", ownedStart=" + isOwnedFortressStart(existing));

						throw new IllegalStateException("Conflicting or inconsistent fake fortress data in chunk "
								+ chunkX + "," + chunkZ + " of " + world.getName());
					}
				} else if (existing != null && existing.isValid()) {
					debug.accept("[Fortress] Injection refused: existing unowned fortress" + ", world="
							+ world.getName() + ", chunk=" + chunkX + "," + chunkZ);

					throw new IllegalStateException("Refusing to overwrite an existing fortress in chunk " + chunkX
							+ "," + chunkZ + " of " + world.getName());
				}

				chunks.add(chunk);
			}
		}

		debug.accept("[Fortress] Injection preflight passed: world=" + world.getName() + ", chunks=" + chunks.size());

		int created = 0;
		int reused = 0;
		int referencesAdded = 0;
		int verified = 0;

		for (LevelChunk chunk : chunks) {
			final ChunkPos chunkPos = chunk.getPos();
			final org.bukkit.Chunk bukkitChunk = world.getChunkAt(chunkPos.x, chunkPos.z);
			final var container = bukkitChunk.getPersistentDataContainer();

			final String storedOwner = container.get(FAKE_FORTRESS_KEY, PersistentDataType.STRING);
			final long reference = ChunkPos.asLong(chunkPos.x, chunkPos.z);

			if (storedOwner == null) {
				final StructureStart previous = chunk.getStartForStructure(fortress);

				// Remember whether removal must restore an invalid start and/or
				// preserve a reference which existed before this injection.
				int previousState = previous != null ? 1 : 0;

				if (chunk.getReferencesForStructure(fortress).contains(reference)) {
					previousState |= 2;
				}

				final net.minecraft.world.level.levelgen.structure.BoundingBox pieceBounds = new net.minecraft.world.level.levelgen.structure.BoundingBox(
						Math.max(region[0], chunkPos.x << 4), region[1], Math.max(region[2], chunkPos.z << 4),
						Math.min(region[3], (chunkPos.x << 4) + 15), region[4],
						Math.min(region[5], (chunkPos.z << 4) + 15));

				// Use a vanilla serializable piece so its bounds survive chunk saves.
				// These chunks are already fully generated; postProcess is not called.
				final NetherFortressPieces.BridgeCrossing piece = new NetherFortressPieces.BridgeCrossing(
						FAKE_FORTRESS_PIECE_DEPTH, pieceBounds, Direction.NORTH);

				final StructureStart start = new StructureStart(fortress, chunkPos, 0,
						new PiecesContainer(List.of(piece)));

				if (!start.isValid()) {
					debug.accept(
							"[Fortress] Invalid generated start: world=" + world.getName() + ", chunk=" + chunkPos);

					throw new IllegalStateException("Created an invalid fake fortress start at " + chunkPos);
				}

				chunk.setStartForStructure(fortress, start);
				container.set(FAKE_FORTRESS_KEY, PersistentDataType.STRING, owner + "|" + previousState);

				created++;
			} else {
				reused++;
			}

			// Repair a missing reference on repeated injection as well.
			if (!chunk.getReferencesForStructure(fortress).contains(reference)) {
				referencesAdded++;
			}

			chunk.addReferenceForStructure(fortress, reference);
			chunk.setUnsaved(true);

			// Verify one structure-piece lookup position per affected chunk.
			final BlockPos probe = new BlockPos(Math.max(region[0], chunkPos.x << 4), region[1],
					Math.max(region[2], chunkPos.z << 4));

			final StructureStart resolved = level.structureManager().getStructureWithPieceAt(probe, fortress);

			if (!isOwnedFortressStart(resolved)) {
				debug.accept("[Fortress] Injection verification failed: world=" + world.getName() + ", chunk="
						+ chunkPos + ", probe=" + probe);

				throw new IllegalStateException("Fake fortress lookup failed at " + probe + " in " + world.getName());
			}

			verified++;
		}

		debug.accept("[Fortress] Injection verified: world=" + world.getName() + ", chunks=" + chunks.size()
				+ ", created=" + created + ", reused=" + reused + ", referencesAdded=" + referencesAdded
				+ ", verifiedLookups=" + verified + ", durationMs=" + (System.nanoTime() - started) / 1_000_000L);
	}

	/**
	 * Compatibility entry point for callers which do not supply diagnostics.
	 */
	@Override
	public void removeFakeFortress(World world, BoundingBox bounds) {
		removeFakeFortress(world, bounds, message -> {
		});
	}

	/**
	 * Removes only fortress starts owned by this injected region.
	 *
	 * Natural fortress starts and unrelated structure references are preserved.
	 * Ownership is read from chunk data, so removal also works after a restart.
	 *
	 * Reports removed chunks separately from chunks skipped because their ownership
	 * marker was absent or belonged to another region.
	 */
	@Override
	public void removeFakeFortress(World world, BoundingBox bounds, Consumer<String> debug) {
		java.util.Objects.requireNonNull(debug, "debug");
		requireFortressThread(world);

		final long started = System.nanoTime();
		final ServerLevel level = ((CraftWorld) world).getHandle();
		final Structure fortress = getFortressStructure(level);
		final int[] region = getFortressRegion(world, bounds);
		final String owner = getFortressOwner(region);

		int scanned = 0;
		int removed = 0;
		int missingMarkers = 0;
		int differentOwners = 0;
		int verified = 0;

		debug.accept("[Fortress] Removal started: world=" + world.getName() + ", bounds=" + bounds + ", ownerMarker="
				+ owner);

		for (int chunkX = region[0] >> 4; chunkX <= region[3] >> 4; chunkX++) {
			for (int chunkZ = region[2] >> 4; chunkZ <= region[5] >> 4; chunkZ++) {
				scanned++;

				final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
				final org.bukkit.Chunk bukkitChunk = world.getChunkAt(chunkX, chunkZ);
				final var container = bukkitChunk.getPersistentDataContainer();

				final String storedOwner = container.get(FAKE_FORTRESS_KEY, PersistentDataType.STRING);

				// Never remove a fortress solely because it lies inside these bounds.
				if (storedOwner == null) {
					missingMarkers++;
					continue;
				}

				if (!storedOwner.startsWith(owner + "|")) {
					differentOwners++;

					// Include a few examples without logging every mismatched chunk.
					if (differentOwners <= 3) {
						debug.accept("[Fortress] Removal skipped different owner: world=" + world.getName() + ", chunk="
								+ chunkX + "," + chunkZ + ", expectedOwner=" + owner + ", storedOwner=" + storedOwner);
					}

					continue;
				}

				final StructureStart current = chunk.getStartForStructure(fortress);

				if (!isOwnedFortressStart(current)) {
					debug.accept("[Fortress] Removal refused: owned marker has changed structure" + ", world="
							+ world.getName() + ", chunk=" + chunkX + "," + chunkZ + ", storedOwner=" + storedOwner);

					throw new IllegalStateException("Refusing to remove changed fortress data in chunk " + chunkX + ","
							+ chunkZ + " of " + world.getName());
				}

				final int previousState = Integer.parseInt(storedOwner.substring(storedOwner.lastIndexOf('|') + 1));

				final Map<Structure, StructureStart> starts = new HashMap<>(chunk.getAllStarts());

				if ((previousState & 1) != 0) {
					starts.put(fortress, StructureStart.INVALID_START);
				} else {
					starts.remove(fortress);
				}

				chunk.setAllStarts(starts);

				if ((previousState & 2) == 0) {
					final long reference = ChunkPos.asLong(chunkX, chunkZ);

					final Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> references = new HashMap<>(
							chunk.getAllReferences());

					final it.unimi.dsi.fastutil.longs.LongSet fortressReferences = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(
							chunk.getReferencesForStructure(fortress));

					fortressReferences.remove(reference);

					if (fortressReferences.isEmpty()) {
						references.remove(fortress);
					} else {
						references.put(fortress, fortressReferences);
					}

					chunk.setAllReferences(references);
				}

				container.remove(FAKE_FORTRESS_KEY);
				chunk.setUnsaved(true);
				removed++;

				// Verify that this chunk's injected start and ownership marker are gone.
				if (isOwnedFortressStart(chunk.getStartForStructure(fortress))
						|| container.has(FAKE_FORTRESS_KEY, PersistentDataType.STRING)) {
					throw new IllegalStateException("Fake fortress removal verification failed in chunk " + chunkX + ","
							+ chunkZ + " of " + world.getName());
				}

				verified++;
			}
		}

		debug.accept("[Fortress] Removal completed: world=" + world.getName() + ", scanned=" + scanned + ", removed="
				+ removed + ", verifiedRemovedStarts=" + verified + ", missingMarkers=" + missingMarkers
				+ ", differentOwners=" + differentOwners + ", durationMs="
				+ (System.nanoTime() - started) / 1_000_000L);
	}

	/** Structure and chunk mutations must happen on the server thread. */
	private void requireFortressThread(World world) {
		if (!Bukkit.isPrimaryThread()) {
			throw new IllegalStateException("Fake fortress changes must run on the server thread.");
		}

		if (world.getEnvironment() != World.Environment.NETHER) {
			throw new IllegalStateException("Fake fortress regions require a Nether world.");
		}
	}

	/** Resolves the server's actual registered fortress structure. */
	private Structure getFortressStructure(ServerLevel level) {
		final var registry = level.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);

		final var key = ResourceKey.create(Registry.STRUCTURE_REGISTRY, new ResourceLocation("minecraft", "fortress"));

		final Structure fortress = registry.get(key);

		if (fortress == null) {
			throw new IllegalStateException("Could not find fortress structure in registry!");
		}

		return fortress;
	}

	/**
	 * Converts Bukkit's exclusive maximum bounds to inclusive block coordinates.
	 * Flooring the minimum also handles negative fractional coordinates correctly.
	 */
	private int[] getFortressRegion(World world, BoundingBox bounds) {
		if (!Double.isFinite(bounds.getMinX()) || !Double.isFinite(bounds.getMinY())
				|| !Double.isFinite(bounds.getMinZ()) || !Double.isFinite(bounds.getMaxX())
				|| !Double.isFinite(bounds.getMaxY()) || !Double.isFinite(bounds.getMaxZ())) {
			throw new IllegalArgumentException("Fortress bounds must be finite.");
		}

		final int minX = (int) Math.floor(bounds.getMinX());
		final int minY = Math.max(world.getMinHeight(), (int) Math.floor(bounds.getMinY()));
		final int minZ = (int) Math.floor(bounds.getMinZ());

		final int maxX = (int) Math.ceil(bounds.getMaxX()) - 1;
		final int maxY = Math.min(world.getMaxHeight() - 1, (int) Math.ceil(bounds.getMaxY()) - 1);
		final int maxZ = (int) Math.ceil(bounds.getMaxZ()) - 1;

		if (minX > maxX || minY > maxY || minZ > maxZ) {
			throw new IllegalArgumentException("Fortress bounds must contain at least one block.");
		}

		return new int[] { minX, minY, minZ, maxX, maxY, maxZ };
	}

	/** Stable region identity, including its vertical bounds. */
	private String getFortressOwner(int[] region) {
		return region[0] + "," + region[1] + "," + region[2] + "," + region[3] + "," + region[4] + "," + region[5];
	}

	/**
	 * Recognizes our serialized marker without treating natural fortresses as ours.
	 */
	private boolean isOwnedFortressStart(StructureStart start) {
		return start != null && start.isValid() && start.getPieces().size() == 1
				&& start.getPieces().get(0) instanceof NetherFortressPieces.BridgeCrossing
				&& start.getPieces().get(0).getGenDepth() == FAKE_FORTRESS_PIECE_DEPTH;
	}

	// Fishing

	@Override
	public List<ItemStack> getFishingLoot(Player player, FishHook hook, ItemStack rod) {
		Location location = hook.getLocation();
		ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
		RandomSource source = RandomSource.create();
		source.setSeed(hook.getUniqueId().getLeastSignificantBits() ^ level.getGameTime());
		LootContext.Builder loottableinfo_builder = (new LootContext.Builder(level))
				.withParameter(LootContextParams.ORIGIN, new Vec3(location.getX(), location.getY(), location.getZ()))
				.withParameter(LootContextParams.TOOL, CraftItemStack.asNMSCopy(rod))
				.withParameter(LootContextParams.THIS_ENTITY, ((CraftFishHook) hook).getHandle()).withRandom(source)
				.withLuck((float) (rod.getEnchantmentLevel(Enchantment.LUCK)
						+ Optional.ofNullable(player.getAttribute(Attribute.GENERIC_LUCK))
								.map(AttributeInstance::getValue).orElse(0d)));
		LootTable loottable = level.getServer().getLootTables().get(BuiltInLootTables.FISHING);
		List<net.minecraft.world.item.ItemStack> list = loottable
				.getRandomItems(loottableinfo_builder.create(LootContextParamSets.FISHING));
		return list.stream().filter(itemStack -> itemStack != null && !itemStack.isEmpty())
				.map(CraftItemStack::asBukkitCopy).toList();
	}

	@Override
	public boolean isFishingHookBit(FishHook hook) {
		FishingHook fishingHook = ((CraftFishHook) hook).getHandle();
		return fishingHook.getEntityData().get(dataBiting);
	}

	@Override
	public UUID getFishingHookOwner(FishHook hook) {
		FishingHook fishingHook = ((CraftFishHook) hook).getHandle();
		try {
			return (UUID) fishingHookOwnerUUIDField.get(fishingHook);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to read ownerUUID from FishingHook", e);
		}
	}

	// Items and player actions

	@Override
	public void swingHand(Player player, HandSlot slot) {
		// Use the native swing packet/state, including the acting player. Legacy
		// animation IDs are not portable: 26.3 animate ID 0 means wake up.
		((CraftPlayer) player).getHandle().swing(
				slot == HandSlot.OFF ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, true);
	}

	@Override
	public void useItem(Player player, HandSlot handSlot, @Nullable ItemStack itemStack) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.gameMode.useItem(serverPlayer, ((CraftWorld) player.getWorld()).getHandle(),
				Optional.ofNullable(itemStack).map(CraftItemStack::asNMSCopy)
						.orElse(serverPlayer.getItemBySlot(EquipmentSlot.valueOf(handSlot.name() + "HAND"))),
				InteractionHand.valueOf(handSlot.name() + "_HAND"));
	}

	// Lava Riptide

	@Override
	public boolean startLavaRainRiptide(Player player, HandSlot hand) {
		return LavaRainRiptideSupport.start(player, hand);
	}

	@Override
	public int getLavaRainRiptideTicks(Player player, HandSlot hand) {
		return LavaRainRiptideSupport.ticks(player, hand);
	}

	@Override
	public boolean releaseLavaRainRiptide(Player player, HandSlot hand, int level) {
		return LavaRainRiptideSupport.release(player, hand, level);
	}

	@Override
	public void cancelLavaRainRiptide(Player player, HandSlot hand) {
		LavaRainRiptideSupport.cancel(player, hand);
	}

	// Entity behavior

	@Override
	@SuppressWarnings("deprecation") // Retain the cross-version shoulder entity API.
	public boolean releaseShoulderPets(Player player) {
		var handle = ((CraftPlayer) player).getHandle();
		boolean released = false;
		var left = handle.getShoulderEntityLeft();
		if (!left.isEmpty() && player.getShoulderEntityLeft() instanceof org.bukkit.entity.Parrot)
			released |= com.swiftlicious.versionbridge.nms.shoulder.ShoulderPetRelease.release(handle, left,
					() -> handle.setShoulderEntityLeft(new net.minecraft.nbt.CompoundTag()));
		var right = handle.getShoulderEntityRight();
		if (!right.isEmpty() && player.getShoulderEntityRight() instanceof org.bukkit.entity.Parrot)
			released |= com.swiftlicious.versionbridge.nms.shoulder.ShoulderPetRelease.release(handle, right,
					() -> handle.setShoulderEntityRight(new net.minecraft.nbt.CompoundTag()));
		return released;
	}

	@Override
	public void restrictWitherAI(Wither bukkitWither) {
		WitherBoss wither = ((CraftWither) bukkitWither).getHandle();

		// Preserve vanilla movement, attack, and spawn-phase behavior.
		GoalSelector targetSelector = wither.targetSelector;
		targetSelector.removeAllGoals();

		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(wither, ServerPlayer.class, true));
	}

	@Override
	public void prepareControlledFlight(org.bukkit.entity.Mob mob) {
		final net.minecraft.world.entity.Mob handle = ((org.bukkit.craftbukkit.v1_19_R1.entity.CraftMob) mob).getHandle();
		// Keep native movement physics enabled without competing wander/attack goals.
		for (var wrapped : java.util.List.copyOf(handle.goalSelector.getAvailableGoals()))
			handle.goalSelector.removeGoal(wrapped.getGoal());
		for (var wrapped : java.util.List.copyOf(handle.targetSelector.getAvailableGoals()))
			handle.targetSelector.removeGoal(wrapped.getGoal());
		mob.setAI(true);
		mob.setGravity(false);
		mob.setCollidable(false);
	}

	@Override
	public boolean isTNTMinecartIgnited(ExplosiveMinecart bukkitExplosiveMinecart) {
		// Cast to CraftMinecart
		CraftMinecart craftMinecart = (CraftMinecart) bukkitExplosiveMinecart;
		// Get NMS handle
		AbstractMinecart nmsCart = craftMinecart.getHandle();
		// Check if it's a MinecartTNT
		return (nmsCart instanceof MinecartTNT tntCart) ? tntCart.isPrimed() : false;
	}

	@Override
	public void igniteTNTMinecart(ExplosiveMinecart bukkitExplosiveMinecart) {
		// Cast to CraftMinecart
		CraftMinecart craftMinecart = (CraftMinecart) bukkitExplosiveMinecart;

		// Get NMS handle
		AbstractMinecart nmsCart = craftMinecart.getHandle();

		// Check if it's a MinecartTNT
		if (nmsCart instanceof MinecartTNT tntCart) {
			tntCart.primeFuse();
		}
	}

	// Client entities and camera movement

	@Override
	public int sendClientSideSkyFlash(@org.jetbrains.annotations.NotNull Player player) {
		int id = com.swiftlicious.versionbridge.nms.util.SelfIncreaseEntityID.getAndIncrease();
		((CraftPlayer) player).getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(id, java.util.UUID.randomUUID(), player.getLocation().getX(),
				-1000000.0, player.getLocation().getZ(), 0.0f, 0.0f, net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, 0,
				net.minecraft.world.phys.Vec3.ZERO, 0));
		return id;
	}

	@Override
	public void synchronizeSpectatorCamera(Player player) {
		final ServerPlayer handle = ((CraftPlayer) player).getHandle();
		if (handle.getCamera() == handle)
			return;
		handle.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCameraPacket(handle.getCamera()));
		handle.connection.resetPosition();
	}

	@Override
	public void sendClientSideTeleportEntity(Player player, Location location, Vector motion, boolean onGround,
			int... entityIDs) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		float ROTATION_FACTOR = 256.0F / 360.0F;
		float yaw = location.getYaw() * ROTATION_FACTOR;
		float pitch = location.getPitch() * ROTATION_FACTOR;
		for (int entityID : entityIDs) {
			FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
			buf.writeVarInt(entityID);
			buf.writeDouble(location.getX());
			buf.writeDouble(location.getY());
			buf.writeDouble(location.getZ());
			buf.writeByte((byte) yaw);
			buf.writeByte((byte) pitch);
			buf.writeBoolean(onGround);
			ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(buf);
			serverPlayer.connection.send(packet);
		}
	}

	@Override
	public void sendClientSideEntityMotion(Player player, Vector vector, int... entityIDs) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		Vec3 vec3 = CraftVector.toNMS(vector);
		for (int entityID : entityIDs) {
			ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(entityID, vec3);
			serverPlayer.connection.send(packet);
		}
	}

	@Override
	public void removeClientSideEntity(Player player, int... entityIDs) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityIDs);
		serverPlayer.connection.send(packet);
	}

	// Client-only entity creation

	@Override
	public int dropFakeItem(Player player, ItemStack itemStack, Location location) {
		UUID uuid = UUID.randomUUID();
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		int entityID = SelfIncreaseEntityID.getAndIncrease();
		ItemEntity item = new ItemEntity(EntityType.ITEM, serverPlayer.getLevel());
		ClientboundAddEntityPacket entityPacket = new ClientboundAddEntityPacket(entityID, uuid, location.getX(),
				location.getY(), location.getZ(), 0, 0, EntityType.ITEM, 0, Vec3.ZERO, 0);
		SynchedEntityData entityData = new SynchedEntityData(item);
		entityData.define(new EntityDataAccessor<>(8, EntityDataSerializers.ITEM_STACK),
				CraftItemStack.asNMSCopy(itemStack));
		entityData.define(new EntityDataAccessor<>(5, EntityDataSerializers.BOOLEAN), true);
		ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityID, entityData, true);
		serverPlayer.connection.send(entityPacket);
		serverPlayer.connection.send(dataPacket);
		return entityID;
	}

	@Override
	public FakeArmorStand createFakeArmorStand(Location location) {
		return new ArmorStandInstance(location);
	}

	@Override
	public FakeItemDisplay createFakeItemDisplay(Location location) {
		throw new UnsupportedVersionException("Version 1.19.1 does not support this action");
	}

	@Override
	public FakeTextDisplay createFakeTextDisplay(Location location) {
		throw new UnsupportedVersionException("Version 1.19.1 does not support this action");
	}

	@Override
	public FakeFirework createFakeFirework(Location location, Color color) {
		return new FireworkInstance(location, color);
	}

	@Override
	public FakeWither createFakeWither(Location location) {
		return new WitherInstance(location);
	}

	@Override
	public com.swiftlicious.versionbridge.nms.entity.ghast.FakeGhast createFakeGhast(Location location) {
		return new GhastInstance(location);
	}
}
