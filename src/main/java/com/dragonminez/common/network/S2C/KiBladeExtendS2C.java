package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.KiWeaponRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class KiBladeExtendS2C {

	private final int entityId;
	private final boolean summon;
	private final float length;
	private final int ticks;

	public KiBladeExtendS2C(int entityId, boolean summon, float length, int ticks) {
		this.entityId = entityId;
		this.summon = summon;
		this.length = length;
		this.ticks = ticks;
	}

	public static KiBladeExtendS2C summon(int entityId, int ticks) {
		return new KiBladeExtendS2C(entityId, true, 0.0F, ticks);
	}

	public static KiBladeExtendS2C extend(int entityId, float length, int ticks) {
		return new KiBladeExtendS2C(entityId, false, length, ticks);
	}

	public KiBladeExtendS2C(FriendlyByteBuf buf) {
		this.entityId = buf.readInt();
		this.summon = buf.readBoolean();
		this.length = buf.readFloat();
		this.ticks = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeBoolean(summon);
		buf.writeFloat(length);
		buf.writeInt(ticks);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (summon) KiWeaponRenderer.summonBlade(entityId, ticks);
			else KiWeaponRenderer.extendBlade(entityId, length, ticks);
		}));
		ctx.get().setPacketHandled(true);
	}
}
