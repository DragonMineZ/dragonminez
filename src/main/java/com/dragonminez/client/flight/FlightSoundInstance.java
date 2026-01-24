package com.dragonminez.client.flight;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class FlightSoundInstance extends AbstractTickableSoundInstance {
	private final LocalPlayer player;
	private int time;

	public FlightSoundInstance(LocalPlayer player) {
		super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, net.minecraft.util.RandomSource.create());
		this.player = player;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.1F;
	}

	@Override
	public void tick() {
		++this.time;

		if (this.player.isRemoved() || !isFlying(this.player)) {
			this.stop();
			return;
		}

		this.x = (double)((float)this.player.getX());
		this.y = (double)((float)this.player.getY());
		this.z = (double)((float)this.player.getZ());

		float speedSqr = (float)this.player.getDeltaMovement().lengthSqr();
		float speed = 0;

		if (speedSqr >= 1.0E-7D) {
			speed = (float) Math.sqrt(speedSqr);
		}

		float maxSpeedRef = 2.0F;
		float speedPct = speed / maxSpeedRef;

		float targetVolume = 0.0F;
		float targetPitch = 1.0F;

		if (speedPct < 0.33F) {
			targetVolume = 0.0F;
		} else if (speedPct < 0.50F) {
			float range = (speedPct - 0.33F) / (0.50F - 0.33F);
			targetVolume = Mth.lerp(range, 0.1F, 0.4F);
			targetPitch = 1.0F;
		} else if (speedPct < 0.75F) {
			float range = (speedPct - 0.50F) / (0.75F - 0.50F);
			targetVolume = Mth.lerp(range, 0.4F, 0.8F);
			targetPitch = Mth.lerp(range, 1.0F, 1.2F);
		} else {
			targetVolume = 1.0F;
			targetPitch = 1.2F + (speedPct - 0.75F) * 0.5F;
		}

		this.volume = Mth.lerp(0.1F, this.volume, targetVolume);
		this.pitch = Mth.lerp(0.1F, this.pitch, targetPitch);

		if (this.volume > 1.0F) this.volume = 1.0F;
	}

	private boolean isFlying(LocalPlayer player) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).resolve();
		return stats.isPresent() && stats.get().getSkills().isSkillActive("fly") && !player.onGround() && !player.isInWater();
	}
}