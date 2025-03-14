package com.yuseix.dragonminez.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.character.renders.DmzRenderer;
import com.yuseix.dragonminez.client.hud.spaceship.SaiyanSpacePodOverlay;
import com.yuseix.dragonminez.events.characters.StatsEvents;
import com.yuseix.dragonminez.init.MainParticles;
import com.yuseix.dragonminez.init.MainSounds;
import com.yuseix.dragonminez.init.entity.custom.NaveSaiyanEntity;
import com.yuseix.dragonminez.init.particles.particleoptions.KiStarParticleOptions;
import com.yuseix.dragonminez.init.particles.particleoptions.AjissaLeavesParticleOptions;
import com.yuseix.dragonminez.init.particles.particleoptions.SacredLeavesParticleOptions;
import com.yuseix.dragonminez.network.C2S.FlyToggleC2S;
import com.yuseix.dragonminez.network.C2S.PermaEffC2S;
import com.yuseix.dragonminez.network.C2S.SpacePodC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.stats.DMZStatsAttributes;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.stats.skills.DMZSkill;
import com.yuseix.dragonminez.utils.DMZRenders;
import com.yuseix.dragonminez.utils.Keys;
import com.yuseix.dragonminez.worldgen.biome.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
	private static int soundTimer = 200;
	private static final String MOD_VERSION = System.getProperty("mod_version", "unknown");

	private static final Random RANDOM = new Random();
	private static final String title = "DragonMine Z v" + "1.2.3 - StoryMode and Skills!";
	private static boolean isDescending = false;

	private static final int teleportTime = 5; // Segundos
	private static boolean isTeleporting = false;
	private static int teleportCountdown = teleportTime;
	private static int planetaObjetivo = 0;  // 0: Overworld, 1: Namek, 2: Kaio

	private static final Set<String> jugadoresConAura = new HashSet<>(Set.of(
			// Staffs y Patreons
			"Dev",
			"ezShokkoh",
			"ImYuseix",
			"Toji71_",
			"InmortalPx",
			"LecuTheAnimator",
			"Baby_Poop12311",
			"SpaceCarp",
			"prolazorbema10",
			"iLalox",
			"Robberto10",
			"Athrizel"
	));
	@SubscribeEvent
	public static void onRenderTick(TickEvent.RenderTickEvent event) {
		if (event.phase == TickEvent.RenderTickEvent.Phase.END) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.getWindow() != null) {
				// Cambia el título de la ventana
				minecraft.getWindow().setTitle(title);
			}
		}
	}

	@SubscribeEvent
	public static void onRenderLevelLast(RenderLevelStageEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_PARTICLES)) return;

		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		// Obtener posición de la cámara
		double camX = camera.getPosition().x;
		double camY = camera.getPosition().y;
		double camZ = camera.getPosition().z;

		for (Player player : minecraft.level.players()) {
			if (player != null) {


				double interpX = Mth.lerp(event.getPartialTick(), player.xOld, player.getX());
				double interpY = Mth.lerp(event.getPartialTick(), player.yOld, player.getY());
				double interpZ = Mth.lerp(event.getPartialTick(), player.zOld, player.getZ());

				var poseStack = event.getPoseStack();
				poseStack.pushPose();

				boolean isLocalPlayer = player == minecraft.player;

				var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
				if (renderer instanceof DmzRenderer dmzRenderer) {
					DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
						var raza = cap.getIntValue("race");
						var transf = cap.getStringValue("form");


						switch (raza){
							case 1:
								switch (transf){
									case "ssgrade2":
										poseStack.translate((interpX - camX) + 0.05, (interpY - camY) + 0.05, interpZ - camZ);
										break;
									case "ssgrade3":
										poseStack.translate((interpX - camX) + 0.2, (interpY - camY) + 0.15, interpZ - camZ);
										break;
									case "oozaru", "goldenoozaru":
										poseStack.translate((interpX - camX) - 0.08, (interpY - camY) + 0.2, interpZ - camZ);
										poseStack.scale(4.0f,4.0f,4.0f);
										break;
									default:
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										break;
								}
								break;
							case 2:
								switch (transf){
									case "giant","orange_giant":
										poseStack.translate((interpX - camX) - 0.08, (interpY - camY) + 0.2, interpZ - camZ);
										poseStack.scale(4.0f,4.0f,4.0f);
										break;
									default:
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										poseStack.scale(1.0f, 1.0f, 1.0f);
										break;
								}
								break;
							case 3:
								switch (transf){
									case "semi_perfect":
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										poseStack.scale(1.1f, 1.1f, 1.1f);
										break;
									default:
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										poseStack.scale(1.0f, 1.0f, 1.0f);
									break;
								}
								break;
							case 4:
								switch (transf){
									case "second_form":
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										poseStack.scale(1.5f, 1.5f, 1.5f);
										break;
									case "third_form":
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										poseStack.scale(1.3f, 1.3f, 1.3f);
										break;
									default:
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										poseStack.scale(1.0f, 1.0f, 1.0f);
										break;
								}
								break;
							case 5:
								switch (transf){
									case "evil":
										poseStack.translate((interpX - camX) + 0.05f, interpY - camY, interpZ - camZ);
										poseStack.scale(1.0f, 1.0f, 1.0f);
										break;
									case "kid":
										poseStack.translate((interpX - camX) + 0.05f, (interpY - camY) + 0.05f, interpZ - camZ);
										poseStack.scale(0.8f, 0.8f, 0.8f);
										break;
									default:
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										poseStack.scale(1.0f, 1.0f, 1.0f);
										break;
								}
								break;
							default:
								switch (transf){
									case "buffed":
										poseStack.translate((interpX - camX) - 0.08, (interpY - camY) + 0.15, interpZ - camZ);
										poseStack.scale(1.0f,1.0f,1.0f);
										break;

									default:
										poseStack.translate(interpX - camX, interpY - camY, interpZ - camZ);
										poseStack.scale(1.0f,1.0f,1.0f);
										break;
								}
								break;
						}
						dmzRenderer.renderOnWorld((AbstractClientPlayer) player, 0, event.getPartialTick(), poseStack, minecraft.renderBuffers().bufferSource(), 15728880);

					});

				}
				poseStack.popPose();


				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
					if (cap.getBoolean("aura") || cap.getBoolean("turbo")) {
						event.getPoseStack().pushPose();
						float transparency = isLocalPlayer && minecraft.options.getCameraType().isFirstPerson() ? 0.075f : 0.325f;

						if (!player.isSpectator()) {
							RenderSystem.disableDepthTest();
							DMZRenders.renderAuraBase(
									(AbstractClientPlayer) player,
									event.getPoseStack(),
									minecraft.renderBuffers().bufferSource(),
									15728880,
									event.getPartialTick(),
									transparency,
									cap.getIntValue("auracolor")
							);
							event.getPoseStack().popPose();
							RenderSystem.enableDepthTest();
						}
					}
					});
			}
		}


	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;

		if (level == null || mc.player == null || mc.isPaused() || mc.player.isSpectator()) return;


		for (Player player : level.players()) {
			if (jugadoresConAura.contains(player.getGameProfile().getName())) {
				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(capability -> {
					if (capability.getBoolean("aura") || capability.getBoolean("turbo")) {
						int color = 16763441;

						for (int i = 0; i < 5; i++) {
							level.addParticle(new KiStarParticleOptions(color),
									player.getX() + (Math.random() - 0.5) * 3.5,
									player.getY() + Math.random() * 3,
									player.getZ() + (Math.random() - 0.5) * 3.5,
									0, 0.1, 0);
						}
					}

				});
			}
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
				if (cap.getBoolean("aura") || cap.getBoolean("turbo")) {
					String particleValue = Minecraft.getInstance().options.particles().get().toString();
					if (player.onGround()) {
						int particleCount = 3;
						switch (particleValue) {
							case "DECREASED" -> particleCount = 2;
							case "MINIMAL" -> particleCount = 1;
						}
						for (int i = 0; i < particleCount; i++) {
							double y = player.getY() + 0.2;

							double xSpeed = (Math.random() - 0.5) * 0.02;
							double zSpeed = (Math.random() - 0.5) * 0.02;

							level.addParticle(MainParticles.DUST_PARTICLE.get(), player.getX(), y, player.getZ(), xSpeed, 0, zSpeed);
						}
					}
				}
				if (cap.getBoolean("transform") || (!cap.getStringValue("form").equals("base") && (cap.getBoolean("aura") || cap.getBoolean("turbo")))) {
					if (player.onGround()) {
						for (int i = 0; i < 1; i++) {
							double xSpeed = (Math.random() - 0.5) * 0.02;
							double ySpeed = Math.random() * 0.01;
							double zSpeed = (Math.random() - 0.5) * 0.02;

							level.addParticle(MainParticles.ROCK_PARTICLE.get(), player.getX(), player.getY() + 0.6, player.getZ(), xSpeed, ySpeed, zSpeed);
						}
					}
				}
			});
		}


		BlockPos playerPos = mc.player.blockPosition();
		ResourceKey<Biome> currentBiomeKey = level.registryAccess()
				.registryOrThrow(Registries.BIOME)
				.getResourceKey(level.getBiome(playerPos).value())
				.orElse(null);

		if (currentBiomeKey == null) return;

		if (playerPos.getY() > 140 || playerPos.getY() < 62) return;

		double x = playerPos.getX() + RANDOM.nextDouble() * 16 - 8;
		double y = playerPos.getY() + RANDOM.nextDouble() * 6;
		double z = playerPos.getZ() + RANDOM.nextDouble() * 16 - 8;
		double xSpeed = (RANDOM.nextDouble() - 0.5) * 0.02;
		double ySpeed = RANDOM.nextDouble() * 0.01;
		double zSpeed = (RANDOM.nextDouble() - 0.5) * 0.02;
		
		if (currentBiomeKey.equals(ModBiomes.AJISSA_PLAINS)) {
			level.addParticle(new AjissaLeavesParticleOptions(55265), x, y, z, xSpeed, ySpeed, zSpeed);
		} else if (currentBiomeKey.equals(ModBiomes.SACRED_LAND)) {
			level.addParticle(new SacredLeavesParticleOptions(10663616), x, y, z, xSpeed, ySpeed, zSpeed);
		}
	}

	@SubscribeEvent
	public static void onKeyPress(InputEvent.Key event) {
		if (Keys.FLY_KEY.consumeClick()) {
			LocalPlayer player = Minecraft.getInstance().player;

			if (player != null) {
				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
					DMZSkill jumpSkill = cap.getDMZSkills().get("jump");
					DMZSkill flySkill = cap.getDMZSkills().get("fly");
					if (flySkill == null) return;

					int flyLevel = flySkill.getLevel();
					int consumeEnergy = 0;
					if (flyLevel < 4) {
						consumeEnergy = (int) Math.ceil(cap.getIntValue("maxenergy") * 0.04);
					} else {
						consumeEnergy = (int) Math.ceil(cap.getIntValue("maxenergy") * 0.02);
					}

					if (flySkill.getLevel() > 0 && cap.getIntValue("curenergy") > consumeEnergy) {

						if (player.onGround()) {
							// Aplicar el salto inicial
							player.jumpFromGround();

							// Si el jugador tiene habilidad de salto, potenciamos el salto
							if (jumpSkill != null && jumpSkill.isActive()) {
								int jumpLevel = jumpSkill.getLevel();
								if (jumpLevel > 0) {
									float jumpBoost = 0.1f * jumpLevel;
									player.setDeltaMovement(player.getDeltaMovement().add(0, jumpBoost, 0));
								}
							}
							// Si no tiene habilidad de salto, salta normalmente
							else {
								player.setDeltaMovement(player.getDeltaMovement().x, 0.42D, player.getDeltaMovement().z);
							}
							isDescending = true;
						} else {
							ModMessages.sendToServer(new FlyToggleC2S());
						}
					}

					if(flySkill.isActive()){
						ModMessages.sendToServer(new PermaEffC2S("remove", "fly", 1));
					} else {
						ModMessages.sendToServer(new PermaEffC2S("add", "fly", 1));

					}


				});
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) return;
		if (!(event.player instanceof LocalPlayer player)) return;

		AtomicBoolean isKaioAvailable = new AtomicBoolean(false);
		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
			if (isDescending && player.getDeltaMovement().y < 0 && !player.isSpectator() && !player.isCreative()) { // Si está cayendo después del salto
				isDescending = false;
				ModMessages.sendToServer(new FlyToggleC2S());
			}

			DMZSkill flySkill = cap.getDMZSkills().get("fly");

			if (flySkill != null && flySkill.isActive()) {
				int flyLevel = flySkill.getLevel();

				// La vel de vuelo aumenta un 20% por nivel
				float baseSpeed = 0.05F;
				float flySpeed = baseSpeed * (1.0F + (0.20F * flyLevel));

				if (cap.getBoolean("turbo")) {
					if (player.getAbilities().flying) {
						if (player.isSprinting()) {
							player.getAbilities().setFlyingSpeed(flySpeed + 0.06f);
							player.onUpdateAbilities();
						} else {
							player.getAbilities().setFlyingSpeed(flySpeed + 0.03f);
							player.onUpdateAbilities();
						}
					}
				} else {
					if (player.getAbilities().flying) {
						if (player.isSprinting()) {
							player.getAbilities().setFlyingSpeed(flySpeed + 0.03f);
							player.onUpdateAbilities();
						} else {
							player.getAbilities().setFlyingSpeed(flySpeed);
							player.onUpdateAbilities();
						}
					}
				}

				Vec3 motion = player.getDeltaMovement();
				double yVelocity = motion.y;

				// Si mantiene espacio, ascender
				if (player.input.jumping) {
					yVelocity = 0.1;
				}
				// Si mantiene shift, descender
				else if (player.input.shiftKeyDown) {
					yVelocity = -0.1;
				}
				// Si no presiona nada, descenso lento
				else {
					if (yVelocity != -0.02) {
						yVelocity = -0.02;
					}
				}

				player.setDeltaMovement(motion.x, yVelocity, motion.z);
				player.onUpdateAbilities();
			}

			if (cap.getBoolean("turbo")) {
				if (player.isSprinting()) {
					player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.75);
				} else {
					player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.5);
				}
			} else {
				if (player.isSprinting()) {
					player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.11);
				} else {
					player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1);
				}

				if (Minecraft.getInstance().options.fovEffectScale().get() != StatsEvents.getPreviousFov()) Minecraft.getInstance().options.fovEffectScale().set(StatsEvents.getPreviousFov());
			}

			if (cap.getBoolean("kaioplanet")) {
				isKaioAvailable.set(true);
			}

			if (cap.getStringValue("form").equals("oozaru")) {
				soundTimer--;
				if (soundTimer == 0) {
					reproducirSonidoIdle(MainSounds.OOZARU_GROWL_PLAYER.get());
				} else if (soundTimer < 0) {
					Random random = new Random();
					soundTimer = random.nextInt(200) + 400;
				}
			}
		});

		if (player.isPassenger() && player.getVehicle() instanceof NaveSaiyanEntity) {

			if (Keys.FUNCTION.consumeClick()) {
				if (isTeleporting) {
					isTeleporting = false;
					teleportCountdown = teleportTime;
					player.displayClientMessage(Component.translatable("ui.dmz.spacepod.teleport.cancel"), true);
				} else {
					Minecraft.getInstance().setScreen(new SaiyanSpacePodOverlay());
				}
			}

			if (isTeleporting) {
				if (teleportCountdown >= 0) {
					// Mostrar cuenta regresiva cada segundo
					if (player.level().getGameTime() % 20 == 0) { // Cada segundo (20 ticks)
						if (teleportCountdown == 1) {
							player.playSound(MainSounds.UI_NAVE_TAKEOFF.get(), 0.5F, 1.0F);
						} else if (teleportCountdown != 0 && teleportCountdown <= teleportTime) {
							player.playSound(MainSounds.UI_NAVE_COOLDOWN.get(), 0.5F, 1.0F);
						}
						player.displayClientMessage(Component.translatable("ui.dmz.spacepod.teleport", teleportCountdown), true);
						teleportCountdown--;
					}
				} else {
					// Teletransportar al jugador cuando el contador llegue a 0
					switch (planetaObjetivo) {
						case 0 -> {
							ModMessages.sendToServer(new SpacePodC2S("overworld"));
							player.sendSystemMessage(Component.translatable("ui.dmz.spacepod.overworld.arrive"));
						}
						case 1 -> {
							ModMessages.sendToServer(new SpacePodC2S("namek"));
							player.sendSystemMessage(Component.translatable("ui.dmz.spacepod.namek.arrive"));
						}
						case 3 -> {
							ModMessages.sendToServer(new SpacePodC2S("otherworld"));
							player.sendSystemMessage(Component.translatable("ui.dmz.spacepod.kaio.arrive"));
						}
					}

					// Reiniciar el estado del teletransporte
					isTeleporting = false;
					teleportCountdown = teleportTime;

					player.playSound(MainSounds.NAVE_LANDING_OPEN.get(), 0.5F, 1.0F);
				}
			} else {
				teleportCountdown = teleportTime; // Reiniciar el contador si no está activo
			}
		}
	}

	public static void setTeleporting(boolean teleporting, int planeta) {
		isTeleporting = teleporting;
		planetaObjetivo = planeta;
		teleportCountdown = teleportTime;
	}

	public static void reproducirSonidoIdle(SoundEvent soundEvent) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
						soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F, false);
			}
		});
	}
}
