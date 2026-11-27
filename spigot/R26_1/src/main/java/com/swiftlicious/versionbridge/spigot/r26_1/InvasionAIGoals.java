package com.swiftlicious.versionbridge.spigot.r26_1;

import java.util.List;
import java.util.Set;

import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.LivingEntity;

import com.swiftlicious.versionbridge.nms.invasion.PiglinAIUtils;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

public class InvasionAIGoals implements PiglinAIUtils {

	@Override
	public void stripAllPassiveGoals(LivingEntity bukkitEntity) {
		if (!(bukkitEntity instanceof org.bukkit.entity.Mob mob))
			return;

		Mob nmsEntity = ((CraftMob) mob).getHandle();
		saveMountGoals(nmsEntity);

		removePassiveGoals(nmsEntity.goalSelector);
		removePassiveGoals(nmsEntity.targetSelector);

		if (nmsEntity instanceof net.minecraft.world.entity.monster.piglin.Piglin piglin) {
			piglin.setCanPickUpLoot(false);
			piglin.setAggressive(true);
			piglin.setDancing(false);

			Goal invasionGoal = createInvasionPiglinGoal(piglin);

			// Prevent duplicate instances if this method is called again.
			piglin.goalSelector.removeAllGoals(goal -> goal.getClass().equals(invasionGoal.getClass()));

			piglin.goalSelector.addGoal(0, invasionGoal);

			// Apply immediately instead of waiting for the first goal tick.
			invasionGoal.tick();
		}
	}

	@Override
	public void moveRetreatUnit(org.bukkit.entity.Mob mob, org.bukkit.util.Vector movement) {
		if (!mob.isValid() || mob.isDead())
			return;

		final double x = movement.getX();
		final double y = movement.getY();
		final double z = movement.getZ();

		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || Math.hypot(x, z) > 0.25
				|| Math.abs(y) > 1.05) {
			throw new IllegalArgumentException("Invalid retreat movement: " + movement);
		}

		final net.minecraft.world.entity.Mob handle = ((CraftMob) mob).getHandle();

		handle.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

		// Separate ascent from horizontal travel so a short step can clear a ledge.
		if (y > 0) {
			handle.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, y, 0));
		}

		handle.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(x, Math.min(0, y), z));

		handle.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

		// Keep head and body facing consistent with the caller's smoothed direction.
		handle.setYHeadRot(handle.getYRot());
		handle.setYBodyRot(handle.getYRot());

		// Keep the rider attached and positioned after moving its mount.
		for (net.minecraft.world.entity.Entity passenger : handle.getPassengers()) {
			handle.positionRider(passenger);
		}
	}

	private Goal createInvasionPiglinGoal(net.minecraft.world.entity.monster.piglin.Piglin piglin) {

		return new Goal() {

			@Override
			public boolean canUse() {
				return piglin.isAlive();
			}

			@Override
			public boolean canContinueToUse() {
				return piglin.isAlive();
			}

			@Override
			public boolean requiresUpdateEveryTick() {
				return true;
			}

			@Override
			public void start() {
				// Apply restrictions immediately when this goal starts.
				tick();
			}

			@Override
			public void tick() {
				if (!piglin.isAlive())
					return;

				final var brain = piglin.getBrain();

				piglin.setCanPickUpLoot(false);

				// Prevent seeking admired items and accepting new admiration.
				brain.setMemory(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM, true);
				brain.setMemory(MemoryModuleType.ADMIRING_DISABLED, true);

				// Keep the memory suppression from original goal.
				brain.eraseMemory(MemoryModuleType.IS_PANICKING);
				brain.eraseMemory(MemoryModuleType.IS_TEMPTED);
				brain.eraseMemory(MemoryModuleType.DANCING);
				brain.eraseMemory(MemoryModuleType.ADMIRING_ITEM);
				brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
				brain.eraseMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);

				piglin.setDancing(false);

				// startAggressionTask selects players or custom Hell Golems.
				// setInvasionTarget maintains their combat memories.
				// Do not overwrite or erase those memories here.
			}
		};
	}

	private void removePassiveGoals(GoalSelector selector) {
		final Set<Class<? extends Goal>> passiveGoalTypes = Set.of(RandomStrollGoal.class, RandomLookAroundGoal.class,
				LookAtPlayerGoal.class, PanicGoal.class, TemptGoal.class, FollowParentGoal.class);

		final List<Goal> goalsToRemove = selector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
				.filter(goal -> passiveGoalTypes.contains(goal.getClass())).toList();

		for (Goal goal : goalsToRemove) {
			selector.removeGoal(goal);
		}
	}

	@Override
	public void addFollowPlayerGoal(LivingEntity bukkitEntity) {
		if (!(bukkitEntity instanceof org.bukkit.entity.Mob mob))
			return;

		Mob nmsEntity = ((CraftMob) mob).getHandle();

		saveMountGoals(nmsEntity);

		boolean alreadyPresent = nmsEntity.goalSelector.getAvailableGoals().stream()
				.anyMatch(wrapped -> LookAtPlayerGoal.class.equals(wrapped.getGoal().getClass()));

		if (!alreadyPresent) {
			nmsEntity.goalSelector.addGoal(2, new LookAtPlayerGoal(nmsEntity, Player.class, 12.0F));
		}
	}

	@Override
	public void restoreStriderGoals(org.bukkit.entity.Strider bukkitStrider) {
		restoreMountGoals(bukkitStrider);
	}

	@Override
	public void restoreMagmaCubeGoals(org.bukkit.entity.MagmaCube bukkitMagma) {
		restoreMountGoals(bukkitMagma);
	}

	@Override
	public void setInvasionTarget(org.bukkit.entity.Mob mob, org.bukkit.entity.LivingEntity target) {
		if (target != null && (!target.isValid() || target.isDead()
				|| !mob.getWorld().getUID().equals(target.getWorld().getUID()))) {
			target = null;
		}

		Mob nmsMob = ((CraftMob) mob).getHandle();

		net.minecraft.world.entity.LivingEntity nmsTarget = target == null ? null
				: ((org.bukkit.craftbukkit.entity.CraftLivingEntity) target).getHandle();

		if (nmsTarget != null && !nmsMob.canAttack(nmsTarget)) {
			target = null;
			nmsTarget = null;
		}

		mob.setTarget(target);

		boolean piglin = nmsMob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin;
		boolean hoglin = nmsMob instanceof net.minecraft.world.entity.monster.hoglin.Hoglin;

		if (!piglin && !hoglin)
			return;

		var brain = nmsMob.getBrain();

		if (nmsTarget == null) {
			brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
			brain.eraseMemory(MemoryModuleType.WALK_TARGET);

			if (piglin)
				brain.eraseMemory(MemoryModuleType.ANGRY_AT);

			return;
		}

		// Explicit anger makes gold armor irrelevant to this target.
		brain.setMemory(MemoryModuleType.ATTACK_TARGET, nmsTarget);

		if (piglin) {
			brain.setMemory(MemoryModuleType.ANGRY_AT, target.getUniqueId());
		}
	}

	private void restoreMountGoals(org.bukkit.entity.Mob bukkitMob) {
		if (!bukkitMob.getPassengers().isEmpty())
			return;

		Mob mob = ((CraftMob) bukkitMob).getHandle();

		MountGoalSnapshot snapshot = mob.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
				.filter(MountGoalSnapshot.class::isInstance).map(MountGoalSnapshot.class::cast).findFirst()
				.orElse(null);

		if (snapshot == null)
			return;

		// Remove individually so running goals are stopped as they are removed.
		for (WrappedGoal wrapped : List.copyOf(mob.goalSelector.getAvailableGoals())) {
			mob.goalSelector.removeGoal(wrapped.getGoal());
		}

		for (WrappedGoal wrapped : List.copyOf(mob.targetSelector.getAvailableGoals())) {
			mob.targetSelector.removeGoal(wrapped.getGoal());
		}

		for (WrappedGoal wrapped : snapshot.movementGoals) {
			mob.goalSelector.addGoal(wrapped.getPriority(), wrapped.getGoal());
		}

		for (WrappedGoal wrapped : snapshot.targetGoals) {
			mob.targetSelector.addGoal(wrapped.getPriority(), wrapped.getGoal());
		}
	}

	private void saveMountGoals(Mob mob) {
		if (!(mob instanceof net.minecraft.world.entity.monster.MagmaCube)
				&& !(mob instanceof net.minecraft.world.entity.monster.Strider))
			return;

		boolean alreadySaved = mob.goalSelector.getAvailableGoals().stream()
				.anyMatch(wrapped -> wrapped.getGoal() instanceof MountGoalSnapshot);

		if (alreadySaved)
			return;

		MountGoalSnapshot snapshot = new MountGoalSnapshot(mob);
		mob.goalSelector.addGoal(Integer.MAX_VALUE, snapshot);
	}

	private static final class MountGoalSnapshot extends Goal {

		private final List<WrappedGoal> movementGoals;
		private final List<WrappedGoal> targetGoals;

		private MountGoalSnapshot(Mob mob) {
			this.movementGoals = List.copyOf(mob.goalSelector.getAvailableGoals());
			this.targetGoals = List.copyOf(mob.targetSelector.getAvailableGoals());
		}

		@Override
		public boolean canUse() {
			// Storage only; this goal must never execute.
			return false;
		}
	}

	private net.minecraft.network.syncher.EntityDataAccessor<Byte> striderMobFlags;
	private net.minecraft.network.syncher.EntityDataAccessor<Boolean> striderColdFlag;

	@Override
	public void sendStriderWarmState(org.bukkit.entity.Player viewer, org.bukkit.entity.Strider strider, boolean warm) {
		final Mob handle = ((CraftMob) strider).getHandle();
		if (striderMobFlags == null) {
			striderMobFlags = findTypedMetadata(Mob.class, Byte.class);
			striderColdFlag = findTypedMetadata(handle.getClass(), Boolean.class);
		}
		byte flags = handle.getEntityData().get(striderMobFlags);
		if (warm)
			flags = (byte) (flags | 1); // Only the viewer sees no-AI; server navigation is unaffected.
		boolean cold = !warm && handle.getEntityData().get(striderColdFlag);
		final var packet = new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(handle.getId(), java.util.List.of(
				net.minecraft.network.syncher.SynchedEntityData.DataValue.create(striderMobFlags, flags),
				net.minecraft.network.syncher.SynchedEntityData.DataValue.create(striderColdFlag, cold)));
		((org.bukkit.craftbukkit.entity.CraftPlayer) viewer).getHandle().connection.send(packet);
	}

	/** Resolve by declared value type, so reobfuscated field names do not matter. */
	@SuppressWarnings("unchecked")
	private static <T> net.minecraft.network.syncher.EntityDataAccessor<T> findTypedMetadata(Class<?> owner, Class<T> valueType) {
		for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
			net.minecraft.network.syncher.EntityDataAccessor<T> selected = null;
			for (java.lang.reflect.Field field : type.getDeclaredFields()) {
				if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
						|| field.getType() != net.minecraft.network.syncher.EntityDataAccessor.class
						|| !(field.getGenericType() instanceof java.lang.reflect.ParameterizedType generic)
						|| generic.getActualTypeArguments()[0] != valueType)
					continue;
				try {
					field.setAccessible(true);
					final var candidate = (net.minecraft.network.syncher.EntityDataAccessor<T>) field.get(null);
					// Older Striders also have a saddle Boolean. Temperature is the first
					// Boolean metadata slot; select by protocol ID, not reflection order.
					if (selected == null || candidate.id() < selected.id())
						selected = candidate;
				} catch (ReflectiveOperationException error) {
					throw new IllegalStateException("Cannot resolve Strider metadata", error);
				}
			}
			if (selected != null)
				return selected;
		}
		throw new IllegalStateException("Missing " + valueType.getSimpleName() + " metadata on " + owner.getName());
	}
}