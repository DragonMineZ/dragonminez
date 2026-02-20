package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KiBlastC2S {
	private boolean isShooting;
	private final int colorMain;
	private final int colorBorder;

	public KiBlastC2S(boolean isShooting, int colorMain, int colorBorder) {
		this.isShooting = isShooting;
		this.colorMain = colorMain;
		this.colorBorder = colorBorder;
	}

	public KiBlastC2S(FriendlyByteBuf buffer) {
		this.isShooting = buffer.readBoolean();
		this.colorMain = buffer.readInt();
		this.colorBorder = buffer.readInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(isShooting);
		buffer.writeInt(colorMain);
		buffer.writeInt(colorBorder);
	}

	public static void handle(KiBlastC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			if (msg.isShooting) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					if (data.getCooldowns().hasCooldown(Cooldowns.KI_BLAST_CD)) return;
					if (data.getStatus().isStunned()) return;
					if (!data.getSkills().hasSkill("kicontrol")) return;

					int cost = (int) (data.getMaxEnergy() * 0.08);
					if (data.getResources().getCurrentEnergy() < cost) return;
					data.getResources().removeEnergy(cost);

					data.getCooldowns().setCooldown(Cooldowns.KI_BLAST_CD, 32);
					player.addEffect(
							new MobEffectInstance(
									MainEffects.KI_BLAST_CD.get(),
									32,
									0,
									false,
									false,
									true
							)
					);

					float damage = (float) (data.getKiDamage() * Math.max(0.5f, (0.05f * data.getSkills().getSkillLevel("kicontrol"))));

					KiBlastEntity kiBlast = new KiBlastEntity(player.level(), player);
					kiBlast.setup(player, damage, 0.5F, 1.5f, msg.colorMain, msg.colorBorder);
                    kiBlast.setPos(player.getX(), player.getEyeY() - 0.4, player.getZ());
                    kiBlast.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, kiBlast.getKiSpeed(), 0.5F);
					player.level().addFreshEntity(kiBlast);

					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), "ki_blast_shot", 0, player.getId()), player);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				});
			} else {
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), "ki_blast_shot", 1, player.getId()), player);
			}
		});

		ctx.get().setPacketHandled(true);
	}
}