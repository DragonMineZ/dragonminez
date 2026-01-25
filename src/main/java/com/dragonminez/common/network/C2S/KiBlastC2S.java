package com.dragonminez.common.network.C2S;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KiBlastC2S {

	public KiBlastC2S() {}

	public KiBlastC2S(FriendlyByteBuf buffer) {}

	public void encode(FriendlyByteBuf buffer) {}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getCooldowns().hasCooldown(Cooldowns.KI_BLAST_CD)) return;
				if (data.getStatus().isStunned()) return;

				int cost = (int) (data.getMaxEnergy() * 0.08);
				if (data.getResources().getCurrentEnergy() < cost) return;
				data.getResources().removeEnergy(cost);

				data.getCooldowns().setCooldown(Cooldowns.KI_BLAST_CD, 30);

				float damage = (float) (data.getKiDamage() * 0.25f);

				String hexColor = data.getCharacter().getAuraColor();
				int colorMain = ColorUtils.hexToInt(hexColor);
				int colorBorder = ColorUtils.darkenColor(colorMain, 0.9f);

				KiBlastEntity kiBlast = new KiBlastEntity(player.level(), player);
				kiBlast.setup(player, damage, 0.5F, 0.0f, colorMain, colorBorder);
				kiBlast.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.0F, 0.5F);
				player.level().addFreshEntity(kiBlast);

				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.ABSORB1.get(), SoundSource.PLAYERS, 0.05F, 0.5F);
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C("ki_blast_shot", 0, player.getId()), player);

				NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}