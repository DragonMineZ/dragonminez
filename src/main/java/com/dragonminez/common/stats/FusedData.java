package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.common.stats.techniques.ReviveTechniqueData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.Techniques;
import com.dragonminez.common.util.FusionForms;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Getter
public class FusedData {
	public static final String NBT_KEY = "Fused";

	private String partnerRace = "";
	private boolean crossRace;
	private boolean leaderMutant;
	private boolean partnerMutant;
	private boolean partnerAndroid;
	private final Skills skills = new Skills();
	private final Skills partnerFormSkills = new Skills();
	private final Techniques techniques = new Techniques();
	private final FormMasteries formMasteries = new FormMasteries();
	private final FormMasteries stackFormMasteries = new FormMasteries();
	private final FormMasteries formMasteriesAtStart = new FormMasteries();
	private final FormMasteries stackFormMasteriesAtStart = new FormMasteries();
	private final Map<String, Integer> techniqueExperienceAtStart = new HashMap<>();

	public static FusedData build(StatsData leader, StatsData partner) {
		FusedData fused = new FusedData();
		String leaderRace = leader.getCharacter().getRaceName().toLowerCase(Locale.ROOT);
		fused.partnerRace = partner.getCharacter().getRaceName().toLowerCase(Locale.ROOT);
		fused.crossRace = !leaderRace.equals(fused.partnerRace);
		fused.leaderMutant = leader.getEffects().hasEffect(FusionForms.MUTANT_EFFECT);
		fused.partnerMutant = partner.getEffects().hasEffect(FusionForms.MUTANT_EFFECT);
		fused.partnerAndroid = partner.getStatus().isAndroidUpgraded();

		Set<String> formSkillNames = formSkillNames(leaderRace, fused.partnerRace);
		fused.skills.copyFrom(leader.getBaseSkills());
		for (Skill skill : partner.getBaseSkills().getAllSkills().values()) {
			boolean formSkill = formSkillNames.contains(skill.getName().toLowerCase(Locale.ROOT));
			if (fused.crossRace && formSkill) fused.partnerFormSkills.mergeMax(skill);
			else fused.skills.mergeMax(skill);
		}

		fused.techniques.copyFrom(leader.getBaseTechniques());
		Techniques partnerTechniques = partner.getBaseTechniques();
		for (TechniqueData technique : partnerTechniques.getUnlockedTechniques().values()) {
			if (ReviveTechniqueData.isRevive(technique.getId())) continue;
			if (partnerTechniques.isFormLoadoutGranted(technique.getId())) continue;
			TechniqueData own = fused.techniques.getUnlockedTechniques().get(technique.getId());
			if (own == null) fused.techniques.unlockTechnique(Techniques.copyOf(technique));
			else if (technique.getExperience() > own.getExperience()) own.setExperience(technique.getExperience());
		}
		for (TechniqueData technique : fused.techniques.getUnlockedTechniques().values()) {
			fused.techniqueExperienceAtStart.put(technique.getId(), technique.getExperience());
		}

		fused.formMasteries.copyFrom(leader.getCharacter().getBaseFormMasteries());
		for (Map.Entry<String, Double> entry : partner.getCharacter().getBaseFormMasteries().entries().entrySet()) {
			String key = fused.crossRace ? FusionForms.qualify(fused.partnerRace, entry.getKey()) : entry.getKey();
			fused.formMasteries.putMax(key, entry.getValue());
		}
		fused.stackFormMasteries.copyFrom(leader.getCharacter().getBaseStackFormMasteries());
		for (Map.Entry<String, Double> entry : partner.getCharacter().getBaseStackFormMasteries().entries().entrySet()) {
			fused.stackFormMasteries.putMax(entry.getKey(), entry.getValue());
		}
		fused.formMasteriesAtStart.copyFrom(fused.formMasteries);
		fused.stackFormMasteriesAtStart.copyFrom(fused.stackFormMasteries);
		return fused;
	}

	private static Set<String> formSkillNames(String leaderRace, String partnerRace) {
		Set<String> names = new HashSet<>();
		for (String name : ConfigManager.getSkillsConfig().getFormSkills()) names.add(name.toLowerCase(Locale.ROOT));
		for (String race : new String[]{leaderRace, partnerRace}) {
			RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
			if (config == null) continue;
			for (String name : config.getFormSkills()) names.add(name.toLowerCase(Locale.ROOT));
		}
		return names;
	}

	public void reconcileInto(StatsData leader, StatsData partner) {
		Techniques baseTechniques = leader.getBaseTechniques();
		int fusedReviveSlot = techniques.getReviveSlot();
		if (fusedReviveSlot >= 0 && baseTechniques.getReviveSlot() < 0) baseTechniques.installRevive(fusedReviveSlot);
		else if (fusedReviveSlot < 0 && baseTechniques.hasRevive()) baseTechniques.uninstallRevive();

		for (Map.Entry<String, Integer> entry : techniqueExperienceAtStart.entrySet()) {
			TechniqueData now = techniques.getUnlockedTechniques().get(entry.getKey());
			if (now == null) continue;
			int delta = now.getExperience() - entry.getValue();
			if (delta <= 0) continue;
			TechniqueData target = leader.getBaseTechniques().getUnlockedTechniques().get(entry.getKey());
			if (target == null && partner != null) target = partner.getBaseTechniques().getUnlockedTechniques().get(entry.getKey());
			if (target != null) target.addExperience(delta);
		}

		for (Map.Entry<String, Double> entry : formMasteries.entries().entrySet()) {
			double delta = entry.getValue() - formMasteriesAtStart.getRaw(entry.getKey());
			if (delta <= 0.0) continue;
			if (FusionForms.isQualified(entry.getKey())) {
				if (partner != null) partner.getCharacter().getBaseFormMasteries().addRaw(FusionForms.baseGroup(entry.getKey()), delta);
			} else {
				leader.getCharacter().getBaseFormMasteries().addRaw(entry.getKey(), delta);
			}
		}

		for (Map.Entry<String, Double> entry : stackFormMasteries.entries().entrySet()) {
			double delta = entry.getValue() - stackFormMasteriesAtStart.getRaw(entry.getKey());
			if (delta > 0.0) leader.getCharacter().getBaseStackFormMasteries().addRaw(entry.getKey(), delta);
		}
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("PartnerRace", partnerRace);
		tag.putBoolean("CrossRace", crossRace);
		tag.putBoolean("LeaderMutant", leaderMutant);
		tag.putBoolean("PartnerMutant", partnerMutant);
		tag.putBoolean("PartnerAndroid", partnerAndroid);
		tag.put("Skills", skills.save());
		tag.put("PartnerFormSkills", partnerFormSkills.save());
		tag.put("Techniques", techniques.save());
		tag.put("FormMasteries", formMasteries.save());
		tag.put("StackFormMasteries", stackFormMasteries.save());
		tag.put("FormMasteriesAtStart", formMasteriesAtStart.save());
		tag.put("StackFormMasteriesAtStart", stackFormMasteriesAtStart.save());
		CompoundTag experienceTag = new CompoundTag();
		for (Map.Entry<String, Integer> entry : techniqueExperienceAtStart.entrySet()) experienceTag.putInt(entry.getKey(), entry.getValue());
		tag.put("TechniqueExperienceAtStart", experienceTag);
		return tag;
	}

	public static FusedData fromTag(CompoundTag tag) {
		FusedData fused = new FusedData();
		fused.partnerRace = tag.getString("PartnerRace");
		fused.crossRace = tag.getBoolean("CrossRace");
		fused.leaderMutant = tag.getBoolean("LeaderMutant");
		fused.partnerMutant = tag.getBoolean("PartnerMutant");
		fused.partnerAndroid = tag.getBoolean("PartnerAndroid");
		fused.skills.load(tag.getCompound("Skills"));
		fused.partnerFormSkills.load(tag.getCompound("PartnerFormSkills"));
		fused.techniques.load(tag.getCompound("Techniques"));
		fused.formMasteries.load(tag.getCompound("FormMasteries"));
		fused.stackFormMasteries.load(tag.getCompound("StackFormMasteries"));
		fused.formMasteriesAtStart.load(tag.getCompound("FormMasteriesAtStart"));
		fused.stackFormMasteriesAtStart.load(tag.getCompound("StackFormMasteriesAtStart"));
		CompoundTag experienceTag = tag.getCompound("TechniqueExperienceAtStart");
		for (String key : experienceTag.getAllKeys()) fused.techniqueExperienceAtStart.put(key, experienceTag.getInt(key));
		return fused;
	}

	public FusedData copy() {
		return fromTag(save());
	}
}
