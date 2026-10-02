package com.dragonminez.common.stats;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairLimits;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.hair.HairPresets;
import com.dragonminez.common.hair.HairSanitizer;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.common.stats.techniques.ReviveTechniqueData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.Techniques;
import com.dragonminez.common.util.FusionForms;
import com.dragonminez.common.util.FusionHair;
import com.dragonminez.common.util.FusionTraits;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Getter
public class FusedData {
	public static final String NBT_KEY = "Fused";

	public enum HairSource { NONE, LEADER, PARTNER, FUSED }

	private String partnerRace = "";
	private String partnerHeadBone = "";
	private boolean partnerSaiyanTail;
	private int partnerBodyType;
	private String partnerGender = Character.GENDER_MALE;
	private HairSource hairSource = HairSource.NONE;
	private String hairColor = "";
	private boolean hairBase;
	@Getter(AccessLevel.NONE)
	private final EnumMap<HairStyleSlot, CustomHair> hairStyles = new EnumMap<>(HairStyleSlot.class);
	@Getter(AccessLevel.NONE)
	private CompoundTag hairTag;
	@Getter(AccessLevel.NONE)
	private float[] rgbHairColor;
	@Getter(AccessLevel.NONE)
	private String planKey;
	@Getter(AccessLevel.NONE)
	private FusionTraits.Plan plan = FusionTraits.Plan.EMPTY;
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
		Character partnerCharacter = partner.getCharacter();
		fused.partnerHeadBone = partnerCharacter.getActiveHeadBone() != null ? partnerCharacter.getActiveHeadBone() : "";
		fused.partnerSaiyanTail = FusionTraits.hasSaiyanTail(partnerCharacter);
		fused.partnerBodyType = partnerCharacter.getBodyType();
		fused.partnerGender = partnerCharacter.getGender() != null ? partnerCharacter.getGender() : Character.GENDER_MALE;
		fused.buildHair(leader, partner);

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

	private void buildHair(StatsData leader, StatsData partner) {
		Character own = leader.getCharacter();
		Character other = partner.getCharacter();
		boolean ownHair = wearsHair(own);
		boolean otherHair = wearsHair(other);
		if (ownHair && otherHair) {
			hairSource = HairSource.FUSED;
			hairColor = own.getHairColor();
			hairBase = own.isRenderHairBase();
			long seed = seedOf(leader.getPlayer(), partner.getPlayer());
			HairLimits limits = HairLimits.current();
			Map<CustomHair, Map<CustomHair, CustomHair>> done = new IdentityHashMap<>();
			for (HairStyleSlot slot : HairStyleSlot.values()) {
				CustomHair a = own.getHairStyle(slot);
				CustomHair b = other.getHairStyle(slot);
				CustomHair result = done.computeIfAbsent(a, key -> new IdentityHashMap<>()).computeIfAbsent(b, key -> {
					CustomHair fusedStyle = FusionHair.fuse(a, b, other.getHairColor(), seed);
					HairSanitizer.sanitize(fusedStyle, slot, limits);
					return fusedStyle;
				});
				hairStyles.put(slot, result);
			}
		} else if (otherHair) {
			hairSource = HairSource.PARTNER;
			hairColor = other.getHairColor();
			hairBase = other.isRenderHairBase();
			Map<CustomHair, CustomHair> copies = new IdentityHashMap<>();
			for (HairStyleSlot slot : HairStyleSlot.values()) {
				hairStyles.put(slot, copies.computeIfAbsent(other.getHairStyle(slot), CustomHair::copy));
			}
		} else if (ownHair) {
			hairSource = HairSource.LEADER;
			hairColor = own.getHairColor();
			hairBase = own.isRenderHairBase();
		}
	}

	private static boolean wearsHair(Character character) {
		if (!HairManager.canUseHair(character)) return false;
		if (character.getHairId() == HairPresets.BALD_PRESET_ID) return false;
		return !character.getHairStyle(HairStyleSlot.BASE).isEmpty();
	}

	private static long seedOf(Player leader, Player partner) {
		long high = leader != null ? leader.getUUID().hashCode() : 0L;
		long low = partner != null ? partner.getUUID().hashCode() : 0L;
		return (high << 32) ^ (low & 0xFFFFFFFFL);
	}

	public boolean hasHair() {
		return hairSource != HairSource.NONE;
	}

	public CustomHair getHairStyle(HairStyleSlot slot) {
		return hairStyles.get(slot);
	}

	public float[] getRgbHairColor() {
		if (rgbHairColor == null) rgbHairColor = ColorUtils.hexToRgb(hairColor != null && !hairColor.isEmpty() ? hairColor : "#000000");
		return rgbHairColor;
	}

	public FusionTraits.Plan traitPlan(Character leader) {
		boolean leaderTail = FusionTraits.hasSaiyanTail(leader);
		boolean partnerModel = usesPartnerModel(leader);
		boolean partnerHeadBones = !partnerModel || keepsPartnerHeadBones(leader);
		String key = leader.getRaceName() + '|' + leader.getActiveHeadBone() + '|' + leaderTail + '|' + partnerModel + '|' + partnerHeadBones;
		if (!key.equals(planKey)) {
			plan = FusionTraits.plan(
					FusionTraits.partsOf(leader.getRaceName(), leader.getActiveHeadBone(), leaderTail, true),
					FusionTraits.partsOf(partnerRace, partnerHeadBone, partnerSaiyanTail, false),
					partnerModel, partnerHeadBones);
			planKey = key;
		}
		return plan;
	}

	public boolean usesPartnerModel(Character leader) {
		if (!crossRace) return false;
		String family = FusionTraits.modelFamily(leader.getRenderLogicKey());
		return !family.equals(FusionTraits.raceFamily(leader.getRaceName())) && family.equals(FusionTraits.raceFamily(partnerRace));
	}

	private static boolean keepsPartnerHeadBones(Character leader) {
		if (!leader.hasActiveForm()) return true;
		FormConfig.FormData raw = ConfigManager.getForm(leader.getRaceName(), leader.getActiveFormGroup(), leader.getActiveForm());
		return raw == null || raw.isKeepBaseFormHeadBones();
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
		tag.putString("PartnerHeadBone", partnerHeadBone);
		tag.putBoolean("PartnerSaiyanTail", partnerSaiyanTail);
		tag.putInt("PartnerBodyType", partnerBodyType);
		tag.putString("PartnerGender", partnerGender);
		tag.put("Hair", saveHair());
		return tag;
	}

	private CompoundTag saveHair() {
		if (hairTag == null) {
			CompoundTag tag = new CompoundTag();
			tag.putByte("Source", (byte) hairSource.ordinal());
			tag.putString("Color", hairColor != null ? hairColor : "");
			tag.putBoolean("Base", hairBase);
			Map<CustomHair, HairStyleSlot> written = new IdentityHashMap<>();
			for (HairStyleSlot slot : HairStyleSlot.values()) {
				CustomHair style = hairStyles.get(slot);
				if (style == null) continue;
				HairStyleSlot first = written.putIfAbsent(style, slot);
				if (first != null) tag.putString(slot.getNbtKey(), first.getNbtKey());
				else tag.put(slot.getNbtKey(), style.save());
			}
			hairTag = tag;
		}
		return hairTag.copy();
	}

	private void loadHair(CompoundTag tag, FusedData previous) {
		if (previous != null && previous.hairTag != null && previous.hairTag.equals(tag)) {
			hairSource = previous.hairSource;
			hairColor = previous.hairColor;
			hairBase = previous.hairBase;
			hairStyles.putAll(previous.hairStyles);
			hairTag = previous.hairTag;
			return;
		}
		int source = tag.getByte("Source");
		hairSource = source >= 0 && source < HairSource.values().length ? HairSource.values()[source] : HairSource.NONE;
		hairColor = tag.getString("Color");
		hairBase = tag.getBoolean("Base");
		for (HairStyleSlot slot : HairStyleSlot.values()) {
			String key = slot.getNbtKey();
			if (tag.contains(key, Tag.TAG_COMPOUND)) {
				hairStyles.put(slot, CustomHair.fromTag(tag.getCompound(key)));
			} else if (tag.contains(key, Tag.TAG_STRING)) {
				HairStyleSlot earlier = slotByNbtKey(tag.getString(key));
				if (earlier != null && hairStyles.containsKey(earlier)) hairStyles.put(slot, hairStyles.get(earlier));
			}
		}
		hairTag = tag.copy();
	}

	private static HairStyleSlot slotByNbtKey(String key) {
		for (HairStyleSlot slot : HairStyleSlot.values()) {
			if (slot.getNbtKey().equals(key)) return slot;
		}
		return null;
	}

	public static FusedData fromTag(CompoundTag tag) {
		return fromTag(tag, null);
	}

	public static FusedData fromTag(CompoundTag tag, FusedData previous) {
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
		fused.partnerHeadBone = tag.getString("PartnerHeadBone");
		fused.partnerSaiyanTail = tag.getBoolean("PartnerSaiyanTail");
		fused.partnerBodyType = tag.getInt("PartnerBodyType");
		fused.partnerGender = tag.contains("PartnerGender") ? tag.getString("PartnerGender") : Character.GENDER_MALE;
		fused.loadHair(tag.getCompound("Hair"), previous);
		return fused;
	}

	public FusedData copy() {
		return fromTag(save());
	}
}
