package com.swiftlicious.versionbridge.spigot.r26_1;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;

import com.swiftlicious.versionbridge.nms.entity.display.FakeTextDisplay;
import com.swiftlicious.versionbridge.nms.util.ColorUtils;
import com.swiftlicious.versionbridge.nms.util.SelfIncreaseEntityID;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class TextDisplayInstance implements FakeTextDisplay {

	private final Location location;
	private String name;
	private final int entityID = SelfIncreaseEntityID.getAndIncrease();
	private final UUID uuid = UUID.randomUUID();
	private int rgba;

	public TextDisplayInstance(Location location) {
		this.location = location;
	}

	@Override
	public void rgba(int r, int g, int b, int a) {
		this.rgba = ColorUtils.rgbaToDecimal(r, g, b, a);
	}

	@Override
	public void name(String json) {
		this.name = json;
	}

	@Override
	public void destroy(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entityID);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void spawn(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		ClientboundAddEntityPacket entityPacket = new ClientboundAddEntityPacket(entityID, uuid, location.getX(),
				location.getY(), location.getZ(), location.getPitch(), location.getYaw(), EntityType.TEXT_DISPLAY, 0,
				Vec3.ZERO, 0);
		ArrayList<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(
				List.of(entityPacket, getMetaPacket()));
		ClientboundBundlePacket packet = new ClientboundBundlePacket(packets);
		serverPlayer.connection.send(packet);
	}

	@Override
	public void updateMetaData(Player player) {
		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
		serverPlayer.connection.send(getMetaPacket());
	}

	private ClientboundSetEntityDataPacket getMetaPacket() {
		ArrayList<SynchedEntityData.DataValue<?>> values = new ArrayList<>();
		values.add(
				SynchedEntityData.DataValue.create(new EntityDataAccessor<>(15, EntityDataSerializers.BYTE), (byte) 3));
		values.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(23, EntityDataSerializers.COMPONENT),
				CraftChatMessage.fromJSON(name)));
		values.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(25, EntityDataSerializers.INT), rgba));
		values.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(26, EntityDataSerializers.BYTE),
				(byte) -1));
		values.add(
				SynchedEntityData.DataValue.create(new EntityDataAccessor<>(27, EntityDataSerializers.BYTE), (byte) 0));
		return new ClientboundSetEntityDataPacket(entityID, values);
	}

	@Override
	public int entityID() {
		return entityID;
	}
}