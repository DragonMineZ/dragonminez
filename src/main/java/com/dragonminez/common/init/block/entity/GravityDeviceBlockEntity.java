package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.block.custom.GravityDeviceBlock;
import com.dragonminez.common.init.menu.menutypes.GravityDeviceMenu;
import com.dragonminez.server.energy.StarEnergyStorage;
import com.dragonminez.server.util.GravityDeviceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class GravityDeviceBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

	private final StarEnergyStorage energyStorage;

	private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

	protected final ContainerData data;

	private boolean active = false;
	private int targetGravity = 10;
	private boolean roomValid = false;
	private boolean running = false;
	private BlockPos roomMin = BlockPos.ZERO;
	private BlockPos roomMax = BlockPos.ZERO;
	private double energyAccumulator = 0.0;

	public GravityDeviceBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(MainBlockEntities.GRAVITY_DEVICE_BE.get(), pPos, pBlockState);
		this.energyStorage = new StarEnergyStorage(cfg().getDeviceEnergyCapacity(), 256) {
			@Override
			public void onEnergyChanged() { setChanged(); }
			@Override
			public boolean canExtract() { return false; }
		};
		this.data = new ContainerData() {
			@Override
			public int get(int pIndex) {
				return switch (pIndex) {
					case 0 -> active ? 1 : 0;
					case 1 -> targetGravity;
					case 2 -> energyStorage.getEnergyStored();
					case 3 -> energyStorage.getMaxEnergyStored();
					case 4 -> roomValid ? 1 : 0;
					case 5 -> running ? 1 : 0;
					default -> 0;
				};
			}
			@Override
			public void set(int pIndex, int pValue) {
				switch (pIndex) {
					case 0 -> active = pValue != 0;
					case 1 -> targetGravity = pValue;
					case 2 -> energyStorage.setEnergy(pValue);
					case 4 -> roomValid = pValue != 0;
					case 5 -> running = pValue != 0;
				}
			}
			@Override
			public int getCount() { return 6; }
		};
	}

	private static GeneralServerConfig.GravityConfig cfg() {
		return ConfigManager.getServerConfig().getGravity();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		lazyEnergyHandler.invalidate();
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && !level.isClientSide) {
			GravityDeviceManager.unregister(level, worldPosition);
		}
	}

	public void refreshRoom() {
		recomputeRoom();
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	public void applyMenuInput(boolean active, int gravity) {
		int max = cfg().getDeviceMaxGravity();
		this.targetGravity = Math.max(1, Math.min(gravity, max));
		this.active = active;
		recomputeRoom();
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
		if (pLevel.isClientSide) return;

		long time = pLevel.getGameTime();
		boolean nowRunning = false;

		if (active) {
			if (time % 40 == 0) recomputeRoom();

			if (roomValid) {
				double perSecond = cfg().getDeviceEnergyPerGravityPerSecond() * targetGravity;
				energyAccumulator += perSecond / 20.0;
				int toConsume = (int) energyAccumulator;
				if (toConsume > 0) {
					if (energyStorage.getEnergyStored() >= toConsume) {
						energyStorage.setEnergy(energyStorage.getEnergyStored() - toConsume);
						energyAccumulator -= toConsume;
						nowRunning = true;
					}
				} else {
					nowRunning = energyStorage.getEnergyStored() > 0;
				}
			}
		} else {
			energyAccumulator = 0.0;
		}

		if (nowRunning) {
			GravityDeviceManager.register(pLevel, pPos, roomAABB(), targetGravity);
		} else {
			GravityDeviceManager.unregister(pLevel, pPos);
		}

		if (nowRunning != running) {
			running = nowRunning;
			if (pState.hasProperty(GravityDeviceBlock.ACTIVE) && pState.getValue(GravityDeviceBlock.ACTIVE) != nowRunning) {
				pLevel.setBlock(pPos, pState.setValue(GravityDeviceBlock.ACTIVE, nowRunning), 3);
			}
			pLevel.sendBlockUpdated(pPos, getBlockState(), getBlockState(), 3);
		}

		setChanged();
	}

	private AABB roomAABB() {
		return new AABB(
				roomMin.getX(), roomMin.getY(), roomMin.getZ(),
				roomMax.getX() + 1.0, roomMax.getY() + 1.0, roomMax.getZ() + 1.0);
	}

	private void recomputeRoom() {
		if (level == null) { roomValid = false; return; }

		int minSize = cfg().getDeviceMinRoomSize();
		int maxSize = cfg().getDeviceMaxRoomSize();
		int cellCap = (2 * maxSize + 1) * (2 * maxSize + 1) * (2 * maxSize + 1);

		int ox = worldPosition.getX();
		int oy = worldPosition.getY();
		int oz = worldPosition.getZ();

		Set<Long> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		int minX = ox, minY = oy, minZ = oz, maxX = ox, maxY = oy, maxZ = oz;
		boolean invalid = false;

		for (Direction dir : Direction.values()) {
			BlockPos n = worldPosition.relative(dir);
			if (isPassable(n) && visited.add(n.asLong())) queue.add(n);
		}

		while (!queue.isEmpty()) {
			if (visited.size() > cellCap) { invalid = true; break; }
			BlockPos c = queue.poll();

			if (Math.abs(c.getX() - ox) > maxSize || Math.abs(c.getY() - oy) > maxSize || Math.abs(c.getZ() - oz) > maxSize) {
				invalid = true;
				break;
			}

			if (c.getX() < minX) minX = c.getX();
			if (c.getY() < minY) minY = c.getY();
			if (c.getZ() < minZ) minZ = c.getZ();
			if (c.getX() > maxX) maxX = c.getX();
			if (c.getY() > maxY) maxY = c.getY();
			if (c.getZ() > maxZ) maxZ = c.getZ();

			for (Direction dir : Direction.values()) {
				cursor.setWithOffset(c, dir);
				if (isPassable(cursor) && visited.add(cursor.asLong())) {
					queue.add(cursor.immutable());
				}
			}
		}

		if (invalid || visited.isEmpty()) { roomValid = false; return; }

		int dx = maxX - minX + 1;
		int dy = maxY - minY + 1;
		int dz = maxZ - minZ + 1;
		if (dx < minSize || dy < minSize || dz < minSize || dx > maxSize || dy > maxSize || dz > maxSize) {
			roomValid = false;
			return;
		}

		roomValid = true;
		roomMin = new BlockPos(minX, minY, minZ);
		roomMax = new BlockPos(maxX, maxY, maxZ);
	}

	private boolean isPassable(BlockPos pos) {
		if (pos.equals(worldPosition)) return false;
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof GravityDeviceBlock) return false;
		if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) return false;
		return state.getCollisionShape(level, pos).isEmpty();
	}

	public boolean isRoomValid() { return roomValid; }
	public boolean isActive() { return active; }
	public int getTargetGravity() { return targetGravity; }
	public BlockPos getRoomMin() { return roomMin; }
	public BlockPos getRoomMax() { return roomMax; }

	@Override
	protected void saveAdditional(CompoundTag pTag) {
		pTag.putBoolean("active", active);
		pTag.putInt("targetGravity", targetGravity);
		pTag.putBoolean("roomValid", roomValid);
		pTag.putLong("roomMin", roomMin.asLong());
		pTag.putLong("roomMax", roomMax.asLong());
		energyStorage.saveNBT(pTag);
		super.saveAdditional(pTag);
	}

	@Override
	public void load(CompoundTag pTag) {
		super.load(pTag);
		active = pTag.getBoolean("active");
		targetGravity = pTag.getInt("targetGravity");
		roomValid = pTag.getBoolean("roomValid");
		roomMin = BlockPos.of(pTag.getLong("roomMin"));
		roomMax = BlockPos.of(pTag.getLong("roomMax"));
		energyStorage.loadNBT(pTag);
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		tag.putBoolean("active", active);
		tag.putBoolean("roomValid", roomValid);
		tag.putInt("targetGravity", targetGravity);
		tag.putLong("roomMin", roomMin.asLong());
		tag.putLong("roomMax", roomMax.asLong());
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		active = tag.getBoolean("active");
		roomValid = tag.getBoolean("roomValid");
		targetGravity = tag.getInt("targetGravity");
		roomMin = BlockPos.of(tag.getLong("roomMin"));
		roomMax = BlockPos.of(tag.getLong("roomMax"));
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
		if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.dragonminez.gravity_device");
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
		BlockState state = this.getBlockState();
		boolean on = state.hasProperty(GravityDeviceBlock.ACTIVE) && state.getValue(GravityDeviceBlock.ACTIVE);
		if (on) {
			return tAnimationState.setAndContinue(RawAnimation.begin().then("work", Animation.LoopType.LOOP));
		}
		return tAnimationState.setAndContinue(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object blockEntity) {
		return RenderUtils.getCurrentTick();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new GravityDeviceMenu(pContainerId, pPlayerInventory, this, this.data);
	}
}
