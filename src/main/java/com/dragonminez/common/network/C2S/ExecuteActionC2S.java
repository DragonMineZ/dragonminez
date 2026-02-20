package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainEffects;
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

	public enum ActionType {
		DESCEND,
		FORCE_DESCEND,
		CYCLE_FORM_GROUP,
		CYCLE_STACK_FORM_GROUP,
		INSTANT_TRANSFORM,
		TOGGLE_TAIL,
		TOGGLE_KI_WEAPON
	}

	private final ActionType action;
	private final boolean rightClick;

	public ExecuteActionC2S(ActionType action) {
		this(action, false);
	}

	public ExecuteActionC2S(ActionType action, boolean rightClick) {
		this.action = action;
		this.rightClick = rightClick;
	}

	public ExecuteActionC2S(FriendlyByteBuf buffer) {
		this.action = buffer.readEnum(ActionType.class);
		this.rightClick = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeEnum(action);
		buffer.writeBoolean(rightClick);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					boolean needsSync = false;
					switch (action) {
						case DESCEND -> {
							switch (data.getStatus().getSelectedAction()) {
								case STACK: {
									if (TransformationsHelper.canStackDescend(data)) {
										FormConfig.FormData previousForm = TransformationsHelper.getPreviousStackForm(data);
										if (previousForm != null) {
											data.getCharacter().setActiveStackForm(data.getCharacter().getActiveStackFormGroup(), previousForm.getName());
										} else {
											data.getCharacter().clearActiveStackForm();
											player.removeEffect(MainEffects.STACK_TRANSFORMED.get());
										}
									} else {
										data.getResources().setPowerRelease(0);
									}
									break;
								}
								case FORM: {
									if (TransformationsHelper.canDescend(data)) {
										FormConfig.FormData previousForm = TransformationsHelper.getPreviousForm(data);
										if (previousForm != null) {
											data.getCharacter().setActiveForm(data.getCharacter().getActiveFormGroup(), previousForm.getName());
										} else {
											if (data.getStatus().isAndroidUpgraded()) {
												data.getCharacter().setActiveForm("androidforms", "androidbase");
											} else {
												data.getCharacter().clearActiveForm();
											}
											player.removeEffect(MainEffects.TRANSFORMED.get());
										}
									} else {
										data.getResources().setPowerRelease(0);
									}
									break;
								}
								default: {
									data.getResources().setPowerRelease(0);
								}
							}
							needsSync = true;
						}
						case FORCE_DESCEND -> {
							if (rightClick) {
								data.getCharacter().clearActiveStackForm();
								if (data.getStatus().isAndroidUpgraded()) data.getCharacter().setActiveForm("androidforms", "androidbase");
								else data.getCharacter().clearActiveForm();
							} else {
								FormConfig.FormData previousStackForm = TransformationsHelper.getPreviousStackForm(data);
								if (previousStackForm != null) {
									data.getCharacter().setActiveStackForm(data.getCharacter().getActiveStackFormGroup(), previousStackForm.getName());
								} else {
									data.getCharacter().clearActiveStackForm();
								}

								FormConfig.FormData previousForm = TransformationsHelper.getPreviousForm(data);
								if (previousForm != null) {
									data.getCharacter().setActiveForm(data.getCharacter().getActiveFormGroup(), previousForm.getName());
								} else {
									if (data.getStatus().isAndroidUpgraded()) data.getCharacter().setActiveForm("androidforms", "androidbase");
									else data.getCharacter().clearActiveForm();
								}
							}

							if (data.getCharacter().getActiveForm().isEmpty() || (data.getStatus().isAndroidUpgraded() && "androidbase".equalsIgnoreCase(data.getCharacter().getActiveForm()))) {
								data.getResources().setPowerRelease(0);
							}
							needsSync = true;
						}
						case CYCLE_FORM_GROUP -> {
							data.getStatus().setSelectedAction(ActionMode.FORM);
							TransformationsHelper.cycleSelectedFormGroup(data, rightClick);
							needsSync = true;
						}
						case CYCLE_STACK_FORM_GROUP -> {
							data.getStatus().setSelectedAction(ActionMode.STACK);
							TransformationsHelper.cycleSelectedStackFormGroup(data, rightClick);
							needsSync = true;
						}
						case INSTANT_TRANSFORM -> {
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
						case TOGGLE_TAIL -> {
							data.getStatus().setTailVisible(!data.getStatus().isTailVisible());
							needsSync = true;
						}
						case TOGGLE_KI_WEAPON -> {
							if (data.getSkills().hasSkill("kimanipulation")) {
								if (rightClick) {
									data.getSkills().setSkillActive("kimanipulation", !data.getSkills().isSkillActive("kimanipulation"));
								} else {
									if (!data.getSkills().isSkillActive("kimanipulation")) data.getSkills().setSkillActive("kimanipulation", true);
									String currentWeapon = data.getStatus().getKiWeaponType();
									if (currentWeapon == null || currentWeapon.equals("clawlance")) {
										data.getStatus().setKiWeaponType("blade");
									} else if (currentWeapon.equals("blade")) {
										data.getStatus().setKiWeaponType("scythe");
									} else if (currentWeapon.equals("scythe")) {
										data.getStatus().setKiWeaponType("clawlance");
									}
								}
								needsSync = true;
							}
						}
					}

					player.refreshDimensions();
					if (needsSync) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				});
			}
		});
		context.setPacketHandled(true);
	}
}