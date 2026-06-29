package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.shader.ClientGravityState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GravityZoneSyncS2C {

	private final float machineGravity;
	private final float environmentalGravity;
	private final float netGravity;
	private final float statMult;
	private final float tpGravityMult;
	private final int idealWeight;
	private final int totalWeight;
	private final float loadRatio;
	private final float weightTpMult;
	private final int zone;

	public GravityZoneSyncS2C(float machineGravity, float environmentalGravity, float netGravity,
							  float statMult, float tpGravityMult, int idealWeight, int totalWeight,
							  float loadRatio, float weightTpMult, int zone) {
		this.machineGravity = machineGravity;
		this.environmentalGravity = environmentalGravity;
		this.netGravity = netGravity;
		this.statMult = statMult;
		this.tpGravityMult = tpGravityMult;
		this.idealWeight = idealWeight;
		this.totalWeight = totalWeight;
		this.loadRatio = loadRatio;
		this.weightTpMult = weightTpMult;
		this.zone = zone;
	}

	public GravityZoneSyncS2C(FriendlyByteBuf buf) {
		this.machineGravity = buf.readFloat();
		this.environmentalGravity = buf.readFloat();
		this.netGravity = buf.readFloat();
		this.statMult = buf.readFloat();
		this.tpGravityMult = buf.readFloat();
		this.idealWeight = buf.readVarInt();
		this.totalWeight = buf.readVarInt();
		this.loadRatio = buf.readFloat();
		this.weightTpMult = buf.readFloat();
		this.zone = buf.readVarInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeFloat(machineGravity);
		buf.writeFloat(environmentalGravity);
		buf.writeFloat(netGravity);
		buf.writeFloat(statMult);
		buf.writeFloat(tpGravityMult);
		buf.writeVarInt(idealWeight);
		buf.writeVarInt(totalWeight);
		buf.writeFloat(loadRatio);
		buf.writeFloat(weightTpMult);
		buf.writeVarInt(zone);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> ClientGravityState.update(
				machineGravity, environmentalGravity, netGravity, statMult, tpGravityMult,
				idealWeight, totalWeight, loadRatio, weightTpMult, zone));
		ctx.get().setPacketHandled(true);
	}
}
