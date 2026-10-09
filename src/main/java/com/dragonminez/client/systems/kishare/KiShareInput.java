package com.dragonminez.client.systems.kishare;

import com.dragonminez.Reference;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.network.C2S.KiShareC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.GodRitualHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KiShareInput {
	private static final int RETRY_TICKS = 10;
	private static final int STOP_RESEND_TICKS = 10;

	private static boolean requested;
	private static int retryCooldown;
	private static int stopCooldown;

	private KiShareInput() {}

	public static boolean isChordDown() {
		Minecraft mc = Minecraft.getInstance();
		return mc.screen == null && Screen.hasControlDown() && Screen.hasAltDown();
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			reset();
			return;
		}
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null || !data.getStatus().isHasCreatedCharacter()) return;

		if (data.getStatus().isKiShareLocked()) player.setSprinting(false);

		boolean sharing = data.getStatus().isSharingKi();
		boolean holding = isChordDown() && mc.options.keyUse.isDown();
		if (stopCooldown > 0) stopCooldown--;
		if (!holding) {
			if ((requested || sharing) && stopCooldown == 0) {
				NetworkHandler.sendToServer(KiShareC2S.stop());
				stopCooldown = STOP_RESEND_TICKS;
			}
			requested = false;
			retryCooldown = 0;
			return;
		}
		stopCooldown = 0;
		if (sharing || data.getStatus().isStunned()) return;
		if (retryCooldown > 0) {
			retryCooldown--;
			return;
		}
		GeneralServerConfig.KiTransferConfig cfg = GodRitualHelper.transferConfig();
		if (cfg == null || !cfg.getEnabled()) return;
		Player target = pickTarget(player, cfg.getStartRange());
		if (target == null) return;
		NetworkHandler.sendToServer(KiShareC2S.start(target.getId()));
		requested = true;
		retryCooldown = RETRY_TICKS;
	}

	public static Player pickTarget(LocalPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 end = eye.add(look.scale(range));
		BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double maxDistanceSq = block.getType() != HitResult.Type.MISS ? eye.distanceToSqr(block.getLocation()) : range * range;
		AABB area = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, area,
				entity -> entity instanceof Player other && other != player && other.isAlive() && !other.isSpectator(), maxDistanceSq);
		return hit != null && hit.getEntity() instanceof Player other ? other : null;
	}

	@SubscribeEvent
	public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		boolean locked = StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> data.getStatus().isKiShareLocked()).orElse(false);
		boolean blockUse = event.isUseItem() && (locked || isChordDown());
		boolean blockAttack = event.isAttack() && locked;
		if (!blockUse && !blockAttack) return;
		event.setSwingHand(false);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		reset();
	}

	private static void reset() {
		requested = false;
		retryCooldown = 0;
		stopCooldown = 0;
	}
}
