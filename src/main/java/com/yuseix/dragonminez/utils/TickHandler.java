package com.yuseix.dragonminez.utils;

import com.yuseix.dragonminez.config.races.DMZHumanConfig;
import com.yuseix.dragonminez.config.races.DMZMajinConfig;
import com.yuseix.dragonminez.config.races.DMZNamekConfig;
import com.yuseix.dragonminez.config.races.DMZSaiyanConfig;
import com.yuseix.dragonminez.events.characters.StatsEvents;
import com.yuseix.dragonminez.stats.DMZStatsAttributes;
import com.yuseix.dragonminez.stats.skills.DMZSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TickHandler {
	private int energyRegenCounter = 0;
	private int staminaRegenCounter = 0;
	private int energyConsumeCounter = 0;
	private int chargeTimer = 0;
	private int babaTimer = 0;
	private int dmzformTimer = 0;
	private int flyTimer = 0;
	private int passiveMajinCounter = 0, passiveSaiyanCounter = 0;

	public void tickRegenConsume(DMZStatsAttributes playerStats, DMZDatos dmzDatos, ServerPlayer player) {
		DMZSkill meditation = playerStats.getDMZSkills().get("meditation");
		DMZSkill flySkill = playerStats.getDMZSkills().get("fly");
		var raza = playerStats.getIntValue("race");

		if (playerStats.getIntValue("babaalivetimer") != 0) {
			int remainingTicks = playerStats.getIntValue("babaalivetimer");
			int remainingMinutes = (remainingTicks / 1200); // 1200 ticks = 1 minuto
			int remainingSeconds = (remainingTicks / 20) % 60; // Convertimos a segundos y obtenemos los restantes

			// Formatear el texto para mostrar el tiempo restante
			String formattedTime = String.format("%dmin %ds", remainingMinutes, remainingSeconds);

			babaTimer++;
			if (babaTimer >= 20) {
				player.displayClientMessage(Component.translatable("dmz.lines.babaalive", formattedTime), true);
				babaTimer = 0;
			}
		}

		// Regeneración de stamina cada 1 segundo
		staminaRegenCounter++;
		if (staminaRegenCounter >= 20) {
			int maxStamina = dmzDatos.calcStamina(playerStats);
			int regenStamina = Math.max((int) Math.round(maxStamina / 24.0), 1);
			if (playerStats.getIntValue("curstam") < maxStamina) {
				if (meditation != null) {
					// Si tiene meditación, aumenta o reduce según el nivel de meditación (+5% por nivel)
					int medLevel = meditation.getLevel();
					regenStamina += (int) Math.ceil(regenStamina * 0.05 * medLevel);
				}
				playerStats.addIntValue("curstam", regenStamina);
				staminaRegenCounter = 0;
			}
		}

		// Regeneración de energía cada 1 segundo (con turbo activo o no)
		energyRegenCounter++;
		if (energyRegenCounter >= 20) {
			int maxEnergy = dmzDatos.calcEnergy(playerStats);

			if (playerStats.getBoolean("turbo") && !player.isCreative() && !player.isSpectator()) {
				// Si el turbo está activo, consumo de energía
				int consumeEnergy = (int) (dmzDatos.calcKiRegen(playerStats) * 1.5);
				if (consumeEnergy < 2) consumeEnergy = 2;
				if (meditation != null) {
					// Reduce 5% del consumo por nivel de meditación
					int medLevel = meditation.getLevel();
					consumeEnergy -= (int) Math.ceil(consumeEnergy * 0.05 * medLevel);
				}
				playerStats.removeIntValue("curenergy", consumeEnergy);
			} else if (!playerStats.getBoolean("turbo") && playerStats.getIntValue("curenergy") < maxEnergy) {
				if (flySkill != null && flySkill.isActive() && flySkill.getLevel() <= 7) {
					// No hacer nada
				} else {
					// Si el turbo no está activo, regeneración de energía
					int regenEnergy = dmzDatos.calcKiRegen(playerStats) / 2;
					if (regenEnergy < 1) regenEnergy = 1;

					if (meditation != null) {
						// Aumenta 10% de la regeneración por nivel de meditación
						int medLevel = meditation.getLevel();
						regenEnergy += (int) Math.ceil(regenEnergy * 0.1 * medLevel);
					}
					if (raza == 2) {
						float passiveNamek = (float) DMZNamekConfig.PASSIVE_REGEN.get() / 100;
						// Aumenta 40% (default) de la regeneración por ser Namek
						regenEnergy += (int) Math.ceil(regenEnergy * passiveNamek);
					}

					playerStats.addIntValue("curenergy", regenEnergy);
				}
			}
			energyRegenCounter = 0;
		}

		// Consumo de energía cada 1 segundo
		energyConsumeCounter++;
		if (energyConsumeCounter >= 20) {
			if (!player.isCreative() && !player.isSpectator()) {
				int consumeEnergy = dmzDatos.calcKiConsume(playerStats);
				if (playerStats.getIntValue("curenergy") < consumeEnergy) {
					playerStats.setStringValue("form", "base");
				} else {
					playerStats.removeIntValue("curenergy", (int) (consumeEnergy / 2.25));
					if (!playerStats.getStringValue("form").equals("base")) {
					}
				}
				energyConsumeCounter = 0;
			}
		}
	}

	public void manejarPasivaMajin(DMZStatsAttributes playerstats, ServerPlayer player) {
		// Pasiva del Majin: Regen de HP
		passiveMajinCounter++;
		if (passiveMajinCounter >= 20) {
			if (playerstats.getIntValue("race") == 5 && player.getHealth() < playerstats.getIntValue("maxhealth")) {
				double passiveMajin = DMZMajinConfig.PASSIVE_HEALTH_REGEN.get() / 100;
				int regenHP = (int) Math.ceil(playerstats.getIntValue("maxhealth") * passiveMajin);
				if (regenHP < 1) regenHP = 1;
				player.heal(regenHP);
			}
			passiveMajinCounter = 0;
		}
	}

	public void manejarPasivaSaiyan(DMZStatsAttributes playerstats, ServerPlayer player) {
		// Pasiva del Saiyan: Zenkai
		/* Si el jugador permanece debajo del 10% de vida, durante 6 segundos, recupera 25% (config) de vida
		 * y obtiene un aumento de stats del 10% (config) actuales. Esto tiene un cooldown de 45 minutos (config).
		 */
		float zenkaiHealth = (float) DMZSaiyanConfig.ZENKAI_HEALTH_REGEN.get() / 100;
		float zenkaiStatBoost = (float) DMZSaiyanConfig.ZENKAI_STAT_BOOST.get() / 100;
		int zenkaiCant = DMZSaiyanConfig.ZENKAI_CANT.get();
		int zenkaiCooldown = DMZSaiyanConfig.ZENKAI_COOLDOWN.get();

		if (player.getHealth() < (playerstats.getIntValue("maxhealth") * 0.10)) {
			passiveSaiyanCounter++;
			if (passiveSaiyanCounter >= 20 * 6 && playerstats.getIntValue("zenkaicount") < zenkaiCant) {
				player.heal((int) Math.ceil(playerstats.getIntValue("maxhealth") * zenkaiHealth));
				playerstats.setStat("STR", (int) (playerstats.getStat("STR") + (playerstats.getStat("STR") * zenkaiStatBoost)));
				playerstats.setStat("DEF", (int) (playerstats.getStat("DEF") + (playerstats.getStat("DEF") * zenkaiStatBoost)));
				playerstats.setStat("CON", (int) (playerstats.getStat("CON") + (playerstats.getStat("CON") * zenkaiStatBoost)));
				playerstats.setStat("PWR", (int) (playerstats.getStat("PWR") + (playerstats.getStat("PWR") * zenkaiStatBoost)));
				playerstats.setStat("ENE", (int) (playerstats.getStat("ENE") + (playerstats.getStat("ENE") * zenkaiStatBoost)));
				int cantZenkais = playerstats.getIntValue("zenkaicount") + 1;

				playerstats.setIntValue("zenkaicount", cantZenkais);
				playerstats.setIntValue("zenkaitimer", (zenkaiCooldown * 20 * 60));
				passiveSaiyanCounter = 0;
			}
		} else {
			passiveSaiyanCounter = 0;
		}
	}

	public void manejarCargaDeAura(DMZStatsAttributes playerstats, int maxenergia) {
		// Incrementa el temporizador en cada tick
		chargeTimer++;

		DMZDatos dmzdatos = new DMZDatos();
		DMZSkill meditation = playerstats.getDMZSkills().get("meditation");
		int meditationLevel = meditation != null ? meditation.getLevel() : 0;
		DMZSkill potUnlock = playerstats.getDMZSkills().get("potential_unlock");
		DMZSkill flySkill = playerstats.getDMZSkills().get("fly");
		int potUnlockLevel = potUnlock != null ? potUnlock.getLevel() : 0;
		int maxRelease = 50 + (potUnlockLevel * 5);
		var raza = playerstats.getIntValue("race");

		if (chargeTimer >= 20) {
			if (playerstats.getBoolean("aura") && playerstats.getBoolean("descend")) {
				if (playerstats.getIntValue("release") > 0) {
					playerstats.setIntValue("release", playerstats.getIntValue("release") - 1);
					if (playerstats.getIntValue("release") < 0) {
						playerstats.setIntValue("release", 0);
					}
				}
				chargeTimer = 16;
			} else if (playerstats.getBoolean("aura")) {
				if (playerstats.getIntValue("release") < maxRelease) {
					playerstats.setIntValue("release", playerstats.getIntValue("release") + 5);
					if (playerstats.getIntValue("release") > maxRelease) {
						playerstats.setIntValue("release", maxRelease);
					}
				}
				if (!playerstats.getBoolean("turbo")) {
					if (playerstats.getIntValue("curenergy") < maxenergia) {
						if ((flySkill != null && flySkill.isActive() && flySkill.getLevel() <= 7) || playerstats.getBoolean("transform")) {
							// No hacer nada
						} else {
							int kiRegen = dmzdatos.calcKiCharge(playerstats) / 2;
							if (kiRegen < 1) kiRegen = 3;
							if (meditation != null) {
								kiRegen += (int) Math.ceil(kiRegen * 0.10 * meditationLevel);
							}
							if (raza == 0) {
								float passiveHuman = (float) DMZHumanConfig.KICHARGE_REGEN_BOOST.get() / 100;
								// Aumenta 25% (default) de la regeneración por ser humano
								kiRegen += (int) Math.ceil(kiRegen * passiveHuman);
							}
							playerstats.addIntValue("curenergy", kiRegen);
						}
					}
				}
				if (playerstats.getBoolean("turbo") && playerstats.getIntValue("curenergy") <= 1) {
					playerstats.setIntValue("release", 0);
				}
				chargeTimer = 0;
			}
		}
	}

	public void manejarCargaForma(DMZStatsAttributes playerstats, ServerPlayer player) {
		if (!playerstats.hasFormSkill("super_form")) return;

		int formLevel = playerstats.getFormSkillLevel("super_form");


		if (playerstats.getBoolean("transform") && playerstats.getIntValue("formrelease") >= 100) {
			manejarForms(playerstats);
			playerstats.setIntValue("formrelease", 0);
		}

		if (playerstats.getBoolean("transform") && playerstats.getIntValue("formrelease") >= 0) {
			if (StatsEvents.getNextForm(playerstats) == null) return;
			dmzformTimer++;
			if (dmzformTimer >= 20) {
				if (StatsEvents.getNextForm(playerstats).equals("oozaru")) {
					// Si el jugador está mirando hacia arriba y (es de noche + luna llena), aumenta la carga
					if (player.getXRot() <= -45.0F && player.level().getMoonPhase() == 0 && player.level().getDayTime() % 24000 >= 13000) {
						playerstats.setIntValue("formrelease", playerstats.getIntValue("formrelease") + 10);
					}
				} else playerstats.setIntValue("formrelease", playerstats.getIntValue("formrelease") + 5 + (5 * formLevel));
				dmzformTimer = 0;
			}
		} else if (!playerstats.getBoolean("transform") && playerstats.getIntValue("formrelease") > 0) {
			playerstats.setIntValue("formrelease", playerstats.getIntValue("formrelease") - 1);
			if (dmzformTimer != 0) dmzformTimer = 0;
		}
	}

	public void manejarForms(DMZStatsAttributes playerstats) {
		if (playerstats.getIntValue("formrelease") < 100 || !playerstats.hasFormSkill("super_form")) return;

		// Obtener la siguiente transformación posible
		String nextForm = StatsEvents.getNextForm(playerstats);

		if (nextForm != null) {
			playerstats.setStringValue("form", nextForm); // Transformar al jugador
		}
	}


	public void manejarFlyConsume(DMZStatsAttributes playerStats, int maxEnergy, ServerPlayer player) {
		DMZSkill flySkill = playerStats.getDMZSkills().get("fly");
		int flyLevel = flySkill != null ? flySkill.getLevel() : 0;
		if (player.isCreative() || player.isSpectator()) return;

		// Solo consume Ki si la habilidad está a nivel 7 o menos. A partir de nivel 8, no consume Ki.
		if (flyLevel > 0 && flyLevel <= 7 && flySkill.isActive()) {
			flyTimer++;

			if (flyTimer >= 20) {
				int consumeEnergy = 0;
				if (flyLevel < 4) {
					consumeEnergy = (int) Math.ceil(maxEnergy * 0.04);
				} else {
					consumeEnergy = (int) Math.ceil(maxEnergy * 0.02);
				}

				if (playerStats.getIntValue("curenergy") < consumeEnergy) {
					if (!player.isCreative() && !player.isSpectator()) player.getAbilities().mayfly = false;

					player.getAbilities().flying = false;
					player.fallDistance = 0;
					player.onUpdateAbilities();
					flySkill.setActive(false);
				} else {
					playerStats.removeIntValue("curenergy", consumeEnergy);
					flyTimer = 0;
				}
			}
		}
	}
}
