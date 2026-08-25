package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import com.dragonminez.common.init.menu.menutypes.GravityDeviceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GravityDeviceUpdateC2S {

	private final BlockPos pos;
	private final boolean active;
	private final int gravity;

	public GravityDeviceUpdateC2S(BlockPos pos, boolean active, int gravity) {
		this.pos = pos;
		this.active = active;
		this.gravity = gravity;
	}

	public GravityDeviceUpdateC2S(FriendlyByteBuf buf) {
		this.pos = buf.readBlockPos();
		this.active = buf.readBoolean();
		this.gravity = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeBoolean(active);
		buf.writeInt(gravity);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			if (!(player.containerMenu instanceof GravityDeviceMenu menu) || !menu.getBlockPos().equals(pos)) return;
			if (!player.level().isLoaded(pos)) return;
			if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;

			BlockEntity be = player.level().getBlockEntity(pos);
			if (be instanceof GravityDeviceBlockEntity device) {
				device.applyMenuInput(active, gravity);
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
