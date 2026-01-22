package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExecuteActionC2S {
	private final String action;
	private static int kiWeaponCycle = 0;

	public ExecuteActionC2S(String action) {
		this.action = action;
	}

	public ExecuteActionC2S(FriendlyByteBuf buffer) {
		this.action = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(action);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					boolean needsSync = false;
					switch (action) {
						case "descend" -> {
							if (data.getSkills().isSkillActive("kaioken") && data.getStatus().getActiveKaiokenPhase() != 0) {
								if (data.getStatus().getActiveKaiokenPhase() <= 0 || data.getStatus().getActiveKaiokenPhase() - 1 <= 0) {
									data.getSkills().setSkillActive("kaioken", false);
								}
								data.getStatus().setActiveKaiokenPhase(data.getStatus().getActiveKaiokenPhase() - 1);
								needsSync = true;
							} else if (TransformationsHelper.canDescend(data)) {
								FormConfig.FormData previousForm = TransformationsHelper.getPreviousForm(data);
								if (previousForm != null) {
									data.getCharacter().setActiveForm(data.getCharacter().getActiveFormGroup(), previousForm.getName());
								} else {
									data.getCharacter().clearActiveForm();
								}
								needsSync = true;
							} else {
								data.getResources().setPowerRelease(0);
								needsSync = true;
							}
						}
						case "cycle_form_group" -> {
							data.getStatus().setSelectedAction(ActionMode.FORM);
							TransformationsHelper.cycleSelectedFormGroup(data);
							needsSync = true;
						}
						case "instant_transform" -> {
							FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
							if (nextForm != null) {
								String group = data.getCharacter().hasActiveForm() ? data.getCharacter().getActiveFormGroup() : data.getCharacter().getSelectedFormGroup();

								double mastery = data.getCharacter().getFormMasteries().getMastery(group, nextForm.getName());
								double maxMastery = nextForm.getMaxMastery();

								if (mastery >= (maxMastery * 0.25)) {
									int cost = (int) (data.getMaxEnergy() * nextForm.getEnergyDrain() * 3);

									if (data.getResources().getCurrentEnergy() >= cost) {
										data.getResources().removeEnergy(cost);
										data.getCharacter().setActiveForm(group, nextForm.getName());
										needsSync = true;
									} else {
										player.displayClientMessage(Component.translatable("message.dragonminez.form.no_ki_instant", cost), true);
									}
								}
							}
						}
						case "toggle_tail" -> {
							data.getStatus().setTailVisible(!data.getStatus().isTailVisible());
							needsSync = true;
						}
						case "toggle_ki_weapon" -> {
							switch (kiWeaponCycle) {
								case 0 -> {
									if (data.getSkills().hasSkill("kimanipulation")) {
										data.getSkills().setSkillActive("kimanipulation", true);
										data.getStatus().setKiWeaponType("blade");
										kiWeaponCycle = 1;
									}
								}
								case 1 -> {
									data.getStatus().setKiWeaponType("scythe");
									kiWeaponCycle = 2;
								}
								case 2 -> {
									data.getStatus().setKiWeaponType("clawlance");
									kiWeaponCycle = 3;
								}
								case 3 -> {
									data.getSkills().setSkillActive("kimanipulation", false);
									data.getStatus().setKiWeaponType("blade");
									kiWeaponCycle = 0;
								}
							}
							needsSync = true;
						}
					}

					if (needsSync) {
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
					}
				});
			}
		});
		context.setPacketHandled(true);
	}
}