package com.dragonminez.common.init.sounds;

import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class VehicleEngineSound extends AbstractTickableSoundInstance {

	private static final float SMOOTHING = 0.12F;
	private static final float SILENT = 0.01F;

	private final VehicleEntity vehicle;
	private final VehicleProperties.Engine engine;

	public VehicleEngineSound(VehicleEntity vehicle, VehicleProperties.Engine engine) {
		super(engine.sound().get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
		this.vehicle = vehicle;
		this.engine = engine;
		this.looping = true;
		this.delay = 0;
		this.volume = SILENT;
		this.pitch = engine.minPitch();
		this.x = vehicle.getX();
		this.y = vehicle.getY();
		this.z = vehicle.getZ();
	}

	public VehicleEntity getVehicle() {
		return vehicle;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() {
		if (this.vehicle.isRemoved()) {
			this.stop();
			return;
		}
		boolean running = this.vehicle.getControllingPassenger() != null;
		float load = this.vehicle.getSpeedFactor();
		float targetVolume = running ? Mth.lerp(load, engine.idleVolume(), engine.maxVolume()) : 0.0F;
		float targetPitch = Mth.lerp(load, engine.minPitch(), engine.maxPitch());
		this.volume = Mth.lerp(SMOOTHING, this.volume, targetVolume);
		this.pitch = Mth.lerp(SMOOTHING, this.pitch, targetPitch);
		this.x = this.vehicle.getX();
		this.y = this.vehicle.getY();
		this.z = this.vehicle.getZ();
		if (!running && this.volume < SILENT) this.stop();
	}
}
