package com.dragonminez.common.stats.character;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.dragonminez.common.stats.extras.UsedForms;
import com.dragonminez.common.init.MainSounds;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Setter
@Getter
public class Character {
	private String race;
	private String gender;
	private String characterClass;

	private String selectedMaster = "";
	private String selectedFormGroup = "";
	private String activeFormGroup = "";
	private String selectedForm = "";
	private String activeForm = "";
	private final FormMasteries formMasteries = new FormMasteries();
	private UsedForms formsUsedBefore = new UsedForms();
	private int activeFormItemDurationTicks = 0;

	private String selectedStackFormGroup = "";
	private String activeStackFormGroup = "";
	private String selectedStackForm = "";
	private String activeStackForm = "";
	private final FormMasteries stackFormMasteries = new FormMasteries();
	private UsedForms stackFormsUsedBefore = new UsedForms();
	private int activeStackFormItemDurationTicks = 0;

	private boolean hasSaiyanTail = true;
	private boolean renderHairBase = true;

	private final Map<String, MasterLocation> interactedMasters = new HashMap<>();

	private final Set<String> knownMinigames = new HashSet<>();

	public void clearInteractedMasters() {
		interactedMasters.clear();
	}

	public boolean isMinigameKnown(String minigameId) {
		return minigameId != null && knownMinigames.contains(minigameId.toLowerCase());
	}

	public void addKnownMinigame(String minigameId) {
		if (minigameId != null) knownMinigames.add(minigameId.toLowerCase());
	}

	public void removeKnownMinigame(String minigameId) {
		if (minigameId != null) knownMinigames.remove(minigameId.toLowerCase());
	}

	public static final String GENDER_MALE = "male";
	public static final String GENDER_FEMALE = "female";

	public static final String CLASS_WARRIOR = "warrior";

	private int hairId;
	private CustomHair hairBase = new CustomHair();
	private CustomHair hairSSJ = new CustomHair();
	private CustomHair hairSSJ2 = new CustomHair();
	private CustomHair hairSSJ3 = new CustomHair();
	private String activeHeadBone = "";
	private int bodyType;
	private int eyesType;
	private int noseType;
	private int mouthType;
	private int tattooType;
	private float boobScale = 1.0f;
	private String bodyColor;
	private String bodyColor2;
	private String bodyColor3;
	private String hairColor;
	private String eye1Color;
	private String eye2Color;
	private String auraColor;

	private transient float[] rgbBodyColor;
	private transient float[] rgbBodyColor2;
	private transient float[] rgbBodyColor3;
	private transient float[] rgbHairColor;
	private transient float[] rgbEye1Color;
	private transient float[] rgbEye2Color;
	private transient float[] rgbAuraColor;

	private transient boolean oozaruCached = false;

	private Boolean armored;

	private static String safeString(String value) {
		return value != null ? value : "";
	}

	public Character() {
		this.race = "human";
		this.gender = GENDER_MALE;
		this.characterClass = CLASS_WARRIOR;
		this.armored = false;

		RaceCharacterConfig config = ConfigManager.getRaceCharacter("human");
		if (config != null) {
			this.hairId = config.getDefaultHairType();
			this.activeHeadBone = (config.getHeadBones().length > 0) ? config.getHeadBones()[0] : "";
			this.bodyType = config.getDefaultBodyType();
			this.eyesType = config.getDefaultEyesType();
			this.noseType = config.getDefaultNoseType();
			this.mouthType = config.getDefaultMouthType();
			this.tattooType = config.getDefaultTattooType();
			this.bodyColor = config.getDefaultBodyColor() != null ? config.getDefaultBodyColor() : "#F5D5A6";
			this.bodyColor2 = config.getDefaultBodyColor2() != null ? config.getDefaultBodyColor2() : "#F5D5A6";
			this.bodyColor3 = config.getDefaultBodyColor3() != null ? config.getDefaultBodyColor3() : "#F5D5A6";
			this.hairColor = config.getDefaultHairColor() != null ? config.getDefaultHairColor() : "#000000";
			this.eye1Color = config.getDefaultEye1Color() != null ? config.getDefaultEye1Color() : "#000000";
			this.eye2Color = config.getDefaultEye2Color() != null ? config.getDefaultEye2Color() : "#000000";
			this.auraColor = config.getDefaultAuraColor() != null ? config.getDefaultAuraColor() : "#FFFFFF";
		} else {
			this.hairId = 0;
			this.activeHeadBone = "";
			this.bodyType = 0;
			this.eyesType = 0;
			this.noseType = 0;
			this.mouthType = 0;
			this.tattooType = 0;
			this.bodyColor = "#F5D5A6";
			this.bodyColor2 = "#F5D5A6";
			this.bodyColor3 = "#F5D5A6";
			this.hairColor = "#000000";
			this.eye1Color = "#000000";
			this.eye2Color = "#000000";
			this.auraColor = "#FFFFFF";
		}
	}

	public void addInteractedMaster(String id, String name, String dimension, BlockPos pos) {
		interactedMasters.put(id, new MasterLocation(id, name, dimension, pos));
	}

	public void setBodyColor(String hex) {
		this.bodyColor = hex;
		this.rgbBodyColor = ColorUtils.hexToRgb(hex);
	}
	public void setBodyColor2(String hex) {
		this.bodyColor2 = hex;
		this.rgbBodyColor2 = ColorUtils.hexToRgb(hex);
	}
	public void setBodyColor3(String hex) {
		this.bodyColor3 = hex;
		this.rgbBodyColor3 = ColorUtils.hexToRgb(hex);
	}
	public void setHairColor(String hex) {
		this.hairColor = hex;
		this.rgbHairColor = ColorUtils.hexToRgb(hex);
	}
	public void setEye1Color(String hex) {
		this.eye1Color = hex;
		this.rgbEye1Color = ColorUtils.hexToRgb(hex);
	}
	public void setEye2Color(String hex) {
		this.eye2Color = hex;
		this.rgbEye2Color = ColorUtils.hexToRgb(hex);
	}
	public void setAuraColor(String hex) {
		this.auraColor = hex;
		this.rgbAuraColor = ColorUtils.hexToRgb(hex);
	}

	public float[] getRgbBodyColor() {
		if (rgbBodyColor == null) rgbBodyColor = ColorUtils.hexToRgb(bodyColor != null ? bodyColor : "#FFFFFF");
		return rgbBodyColor;
	}
	public float[] getRgbBodyColor2() {
		if (rgbBodyColor2 == null) rgbBodyColor2 = ColorUtils.hexToRgb(bodyColor2 != null ? bodyColor2 : "#FFFFFF");
		return rgbBodyColor2;
	}
	public float[] getRgbBodyColor3() {
		if (rgbBodyColor3 == null) rgbBodyColor3 = ColorUtils.hexToRgb(bodyColor3 != null ? bodyColor3 : "#FFFFFF");
		return rgbBodyColor3;
	}
	public float[] getRgbHairColor() {
		if (rgbHairColor == null) rgbHairColor = ColorUtils.hexToRgb(hairColor != null ? hairColor : "#FFFFFF");
		return rgbHairColor;
	}
	public float[] getRgbEye1Color() {
		if (rgbEye1Color == null) rgbEye1Color = ColorUtils.hexToRgb(eye1Color != null ? eye1Color : "#FFFFFF");
		return rgbEye1Color;
	}
	public float[] getRgbEye2Color() {
		if (rgbEye2Color == null) rgbEye2Color = ColorUtils.hexToRgb(eye2Color != null ? eye2Color : "#FFFFFF");
		return rgbEye2Color;
	}
	public float[] getRgbAuraColor() {
		if (rgbAuraColor == null) rgbAuraColor = ColorUtils.hexToRgb(auraColor != null ? auraColor : "#FFFFFF");
		return rgbAuraColor;
	}

	public void updateOozaruCache() {
		String raceName = this.getRaceName().toLowerCase();
		String currentForm = this.getActiveForm();
		var activeFormConfig = this.getActiveFormData();

		var raceConfig = ConfigManager.getRaceCharacter(raceName);
		String raceCustomModel = (raceConfig != null && raceConfig.getCustomModel() != null) ? raceConfig.getCustomModel().toLowerCase() : "";
		String formCustomModel = (this.hasActiveForm() && activeFormConfig != null && activeFormConfig.hasCustomModel())
				? activeFormConfig.getCustomModel().toLowerCase() : "";

		String logicKey = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
		if (logicKey.isEmpty()) {
			logicKey = raceName;
		}

		this.oozaruCached = logicKey.startsWith("oozaru") ||
				(raceName.equals("saiyan") && (Objects.equals(currentForm, com.dragonminez.common.util.lists.SaiyanForms.OOZARU) || Objects.equals(currentForm, com.dragonminez.common.util.lists.SaiyanForms.GOLDEN_OOZARU)));
	}

	public CustomHair emptyHair() {
		return HairManager.getPresetHair(5, this.hairColor);
	}

	public CustomHair getHairBase() {
		if (this.hairId > 0) return HairManager.getPresetHair(this.hairId, this.hairColor);
		return hairBase;
	}

	public CustomHair getHairSSJ() {
		if (this.hairId > 0) return HairManager.getPresetHairSSJ(this.hairId, this.hairColor);
		if (hairSSJ == null || hairSSJ.isEmpty()) return hairBase;
		return hairSSJ;
	}

	public CustomHair getHairSSJ2() {
		if (this.hairId > 0) return HairManager.getPresetHairSSJ2(this.hairId, this.hairColor);
		if (hairSSJ2 == null || hairSSJ2.isEmpty()) return (hairSSJ != null && !hairSSJ.isEmpty()) ? hairSSJ : hairBase;
		return hairSSJ2;
	}

	public CustomHair getHairSSJ3() {
		if (this.hairId > 0) return HairManager.getPresetHairSSJ3(this.hairId, this.hairColor);
		if (hairSSJ3 == null || hairSSJ3.isEmpty()) return (hairSSJ != null && !hairSSJ.isEmpty()) ? hairSSJ : hairBase;
		return hairSSJ3;
	}

	public void setRace(String race) {
		if (race != null) this.race = race.toLowerCase();
		else this.race = "human";
		if (!canHaveGender() && !gender.equals(GENDER_MALE)) this.gender = GENDER_MALE;
		updateOozaruCache();
	}

	public void setSelectedFormGroup(String selectedFormGroup) {
		this.selectedFormGroup = safeString(selectedFormGroup);
	}

	public void setSelectedForm(String selectedForm) {
		this.selectedForm = safeString(selectedForm);
	}

	public void setSelectedStackFormGroup(String selectedStackFormGroup) {
		this.selectedStackFormGroup = safeString(selectedStackFormGroup);
	}

	public void setSelectedStackForm(String selectedStackForm) {
		this.selectedStackForm = safeString(selectedStackForm);
	}

	public String getRaceName() {
		return race != null && !race.isEmpty() ? race : "human";
	}

	public boolean canHaveGender() {
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(getRaceName());
		return raceConfig != null ? raceConfig.getHasGender() : true;
	}

	public Float[] getModelScaling() {
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(getRaceName());
		if (raceConfig != null) {
			return raceConfig.getDefaultModelScaling();
		}
		return new Float[]{0.9375f, 0.9375f, 0.9375f};
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Race", safeString(race));
		tag.putString("Gender", safeString(gender));
		tag.putString("Class", safeString(characterClass));
		tag.putInt("HairId", hairId);
		tag.put("HairBase", hairBase.save());
		tag.put("HairSSJ", hairSSJ.save());
		tag.put("HairSSJ2", hairSSJ2.save());
		tag.put("HairSSJ3", hairSSJ3.save());
		tag.putString("ActiveHeadBone", activeHeadBone != null ? activeHeadBone : "");
		tag.putInt("BodyType", bodyType);
		tag.putInt("EyesType", eyesType);
		tag.putInt("NoseType", noseType);
		tag.putInt("MouthType", mouthType);
		tag.putInt("TattooType", tattooType);
		tag.putFloat("BoobScale", boobScale);
		saveAppearance(tag);
		tag.putString("SelectedMaster",  safeString(selectedMaster));
		tag.putString("SelectedFormGroup", safeString(selectedFormGroup));
		tag.putString("CurrentFormGroup", safeString(activeFormGroup));
		tag.putString("SelectedForm", safeString(selectedForm));
		tag.putString("CurrentForm", safeString(activeForm));
		tag.putInt("ActiveFormItemDurationTicks", activeFormItemDurationTicks);
		tag.put("FormMasteries", formMasteries.save());
		tag.putString("SelectedStackFormGroup", safeString(selectedStackFormGroup));
		tag.putString("CurrentStackFormGroup", safeString(activeStackFormGroup));
		tag.putString("SelectedStackForm", safeString(selectedStackForm));
		tag.putString("CurrentStackForm", safeString(activeStackForm));
		tag.putInt("ActiveStackFormItemDurationTicks", activeStackFormItemDurationTicks);
		tag.put("StackFormMasteries", stackFormMasteries.save());
		tag.put("FormsUsedBefore", (formsUsedBefore != null ? formsUsedBefore : new UsedForms()).save());
		tag.put("StackFormsUsedBefore", (stackFormsUsedBefore != null ? stackFormsUsedBefore : new UsedForms()).save());
		tag.putBoolean("HasSaiyanTail", hasSaiyanTail);
		tag.putBoolean("RenderHairBase", renderHairBase);
		tag.putBoolean("isArmored", armored);

		ListTag mastersList = new ListTag();
		for (MasterLocation master : interactedMasters.values()) {
			CompoundTag masterTag = new CompoundTag();
			masterTag.putString("Id", master.getMasterId());
			masterTag.putString("Name", master.getDisplayName());
			masterTag.putString("Dimension", master.getDimension());
			masterTag.putLong("Pos", master.getPosition().asLong());
			mastersList.add(masterTag);
		}
		tag.put("InteractedMasters", mastersList);

		ListTag minigamesList = new ListTag();
		for (String minigame : knownMinigames) minigamesList.add(net.minecraft.nbt.StringTag.valueOf(minigame));
		tag.put("KnownMinigames", minigamesList);

		return tag;
	}

	public void load(CompoundTag tag) {
		if (tag.contains("Race", 8)) {
			this.race = tag.getString("Race");
		} else if (tag.contains("Race", 3)) {
			int oldRaceId = tag.getInt("Race");
			List<String> races = ConfigManager.getLoadedRaces();
			this.race = oldRaceId >= 0 && oldRaceId < races.size() ? races.get(oldRaceId) : "human";
		} else this.race = "human";

		this.gender = tag.getString("Gender");
		this.characterClass = tag.getString("Class");
		this.hairId = tag.getInt("HairId");
		if (tag.contains("HairBase")) this.hairBase.load(tag.getCompound("HairBase"));
		if (tag.contains("HairSSJ")) this.hairSSJ.load(tag.getCompound("HairSSJ"));
		if (tag.contains("HairSSJ2")) this.hairSSJ2.load(tag.getCompound("HairSSJ2"));
		if (tag.contains("HairSSJ3")) this.hairSSJ3.load(tag.getCompound("HairSSJ3"));
		this.activeHeadBone = tag.getString("ActiveHeadBone");
		this.bodyType = tag.getInt("BodyType");
		this.eyesType = tag.getInt("EyesType");
		this.noseType = tag.getInt("NoseType");
		this.mouthType = tag.getInt("MouthType");
		this.tattooType = tag.getInt("TattooType");
		this.boobScale = tag.contains("BoobScale") ? tag.getFloat("BoobScale") : 1.0f;
		setBodyColor(tag.getString("BodyColor"));
		setBodyColor2(tag.getString("BodyColor2"));
		setBodyColor3(tag.getString("BodyColor3"));
		setHairColor(tag.getString("HairColor"));
		setEye1Color(tag.getString("Eye1Color"));
		setEye2Color(tag.getString("Eye2Color"));
		setAuraColor(tag.getString("AuraColor"));
		if (tag.contains("SelectedMaster")) this.selectedMaster = tag.getString("SelectedMaster");
		this.selectedFormGroup = tag.getString("SelectedFormGroup");
		this.activeFormGroup = tag.getString("CurrentFormGroup");
		this.selectedForm = tag.getString("SelectedForm");
		this.activeForm = tag.getString("CurrentForm");
		this.activeFormItemDurationTicks = tag.getInt("ActiveFormItemDurationTicks");
		if (tag.contains("FormMasteries")) formMasteries.load(tag.getCompound("FormMasteries"));
		if (tag.contains("FormsUsedBefore")) formsUsedBefore.load(tag.getCompound("FormsUsedBefore"));
		this.selectedStackFormGroup = tag.getString("SelectedStackFormGroup");
		this.activeStackFormGroup = tag.getString("CurrentStackFormGroup");
		this.selectedStackForm = tag.getString("SelectedStackForm");
		this.activeStackForm = tag.getString("CurrentStackForm");
		this.activeStackFormItemDurationTicks = tag.getInt("ActiveStackFormItemDurationTicks");
		if (tag.contains("StackFormMasteries")) stackFormMasteries.load(tag.getCompound("StackFormMasteries"));
		if (tag.contains("StackFormsUsedBefore")) stackFormsUsedBefore.load(tag.getCompound("StackFormsUsedBefore"));
		this.hasSaiyanTail = tag.getBoolean("HasSaiyanTail");
		this.renderHairBase = tag.getBoolean("RenderHairBase");
		this.armored = tag.getBoolean("isArmored");

		this.interactedMasters.clear();
		if (tag.contains("InteractedMasters")) {
			ListTag mastersList = tag.getList("InteractedMasters", 10);
			for (int i = 0; i < mastersList.size(); i++) {
				CompoundTag masterTag = mastersList.getCompound(i);
				String id = masterTag.getString("Id");
				String name = masterTag.getString("Name");
				String dim = masterTag.getString("Dimension");
				BlockPos pos = BlockPos.of(masterTag.getLong("Pos"));
				this.interactedMasters.put(id, new MasterLocation(id, name, dim, pos));
			}
		}

		this.knownMinigames.clear();
		if (tag.contains("KnownMinigames")) {
			ListTag minigamesList = tag.getList("KnownMinigames", 8);
			for (int i = 0; i < minigamesList.size(); i++) this.knownMinigames.add(minigamesList.getString(i));
		}

		updateOozaruCache();
	}

	public boolean hasActiveForm() {
		return !activeFormGroup.isEmpty() && !activeForm.isEmpty();
	}

	public void setActiveForm(String groupName, String formName) {
		this.activeFormGroup = groupName != null ? groupName : "";
		this.activeForm = formName != null ? formName : "";
		this.activeFormItemDurationTicks = 0;
		updateOozaruCache();
	}

	public void clearActiveForm() {
		this.activeFormGroup = "";
		this.activeForm = "";
		this.activeFormItemDurationTicks = 0;
		updateOozaruCache();

	}

	public void clearActiveForm(LivingEntity entity) {
		String oldGroup = activeFormGroup;
		String oldForm = activeForm;
		boolean hadForm = hasActiveForm();
		if (entity != null && hadForm) {
			entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), MainSounds.TRANSFORM_OFF.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            entity.refreshDimensions();
        }
		clearActiveForm();
		if (hadForm && entity instanceof ServerPlayer serverPlayer && !serverPlayer.level().isClientSide) {
			MinecraftForge.EVENT_BUS.post(new DMZEvent.FormChangeEvent(serverPlayer, oldGroup, oldForm, "", ""));
		}
	}

	public FormConfig.FormData getActiveFormData() {
		if (!hasActiveForm()) return null;
		return ConfigManager.getForm(getRaceName(), activeFormGroup, activeForm);
	}

	public boolean hasActiveStackForm() {
		return !activeStackFormGroup.isEmpty() && !activeStackForm.isEmpty();
	}

	public void setActiveStackForm(String groupName, String formName) {
		this.activeStackFormGroup = groupName != null ? groupName : "";
		this.activeStackForm = formName != null ? formName : "";
		this.activeStackFormItemDurationTicks = 0;
	}

	public void clearActiveStackForm() {
		this.activeStackFormGroup = "";
		this.activeStackForm = "";
		this.activeStackFormItemDurationTicks = 0;
	}

	public void clearActiveStackForm(LivingEntity entity) {
		String oldGroup = activeStackFormGroup;
		String oldForm = activeStackForm;
		boolean hadForm = hasActiveStackForm();
		if (entity != null && hadForm) {
			entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), MainSounds.TRANSFORM_OFF.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            entity.refreshDimensions();
        }
		clearActiveStackForm();
		if (hadForm && entity instanceof ServerPlayer serverPlayer && !serverPlayer.level().isClientSide) {
			MinecraftForge.EVENT_BUS.post(new DMZEvent.StackFormChangeEvent(serverPlayer, oldGroup, oldForm, "", ""));
        }
	}

	public FormConfig.FormData getActiveStackFormData() {
		if (!hasActiveStackForm()) return null;
		return ConfigManager.getStackForm(activeStackFormGroup, activeStackForm);
	}

	public boolean areExtraHeadBonesEnabled() {
		FormConfig.FormData activeStackFormData = getActiveStackFormData();
		if (activeStackFormData != null && !activeStackFormData.isKeepBaseFormHeadBones()) return false;
		FormConfig.FormData activeFormData = getActiveFormData();
		return activeFormData == null || activeFormData.isKeepBaseFormHeadBones();
	}

	public String getRenderableHeadBone() {
		String bone = safeString(activeHeadBone);
		if (bone.isEmpty() || bone.equals("hair")) {
			return bone;
		}
		return areExtraHeadBonesEnabled() ? bone : "";
	}

	public void saveAppearance(CompoundTag tag) {
		tag.putString("BodyColor", safeString(bodyColor));
		tag.putString("BodyColor2", safeString(bodyColor2));
		tag.putString("BodyColor3", safeString(bodyColor3));
		tag.putString("HairColor", safeString(hairColor));
		tag.putString("Eye1Color", safeString(eye1Color));
		tag.putString("Eye2Color", safeString(eye2Color));
		tag.putString("AuraColor", safeString(auraColor));
	}

	public void loadAppearance(CompoundTag tag) {
		if (tag.contains("BodyColor")) this.bodyColor = tag.getString("BodyColor");
		if (tag.contains("BodyColor2")) this.bodyColor2 = tag.getString("BodyColor2");
		if (tag.contains("BodyColor3")) this.bodyColor3 = tag.getString("BodyColor3");
		if (tag.contains("HairColor")) this.hairColor = tag.getString("HairColor");
		if (tag.contains("Eye1Color")) this.eye1Color = tag.getString("Eye1Color");
		if (tag.contains("Eye2Color")) this.eye2Color = tag.getString("Eye2Color");
		if (tag.contains("AuraColor")) this.auraColor = tag.getString("AuraColor");
	}

	public void copyFrom(Character other) {
		this.race = other.race;
		this.gender = other.gender;
		this.characterClass = other.characterClass;
		this.hairId = other.hairId;
		this.hairBase = other.hairBase.copy();
		this.hairSSJ = other.hairSSJ.copy();
		this.hairSSJ2 = other.hairSSJ2.copy();
		this.hairSSJ3 = other.hairSSJ3.copy();
		this.activeHeadBone = other.activeHeadBone;
		this.bodyType = other.bodyType;
		this.eyesType = other.eyesType;
		this.noseType = other.noseType;
		this.mouthType = other.mouthType;
		this.tattooType = other.tattooType;
		this.boobScale = other.boobScale;
		setBodyColor(other.bodyColor);
		setBodyColor2(other.bodyColor2);
		setBodyColor3(other.bodyColor3);
		setHairColor(other.hairColor);
		setEye1Color(other.eye1Color);
		setEye2Color(other.eye2Color);
		setAuraColor(other.auraColor);
		this.selectedMaster = safeString(other.selectedMaster);
		this.selectedFormGroup = safeString(other.selectedFormGroup);
		this.activeFormGroup = safeString(other.activeFormGroup);
		this.selectedForm = safeString(other.selectedForm);
		this.activeForm = safeString(other.activeForm);
		this.activeFormItemDurationTicks = other.activeFormItemDurationTicks;
		this.formMasteries.copyFrom(other.formMasteries);
		this.selectedStackFormGroup = safeString(other.selectedStackFormGroup);
		this.activeStackFormGroup = safeString(other.activeStackFormGroup);
		this.selectedStackForm = safeString(other.selectedStackForm);
		this.activeStackForm = safeString(other.activeStackForm);
		this.activeStackFormItemDurationTicks = other.activeStackFormItemDurationTicks;
		this.stackFormMasteries.copyFrom(other.stackFormMasteries);
		this.hasSaiyanTail = other.hasSaiyanTail;
		this.renderHairBase = other.renderHairBase;
		this.armored = other.armored;
		this.interactedMasters.clear();
		this.interactedMasters.putAll(other.interactedMasters);
		this.knownMinigames.clear();
		this.knownMinigames.addAll(other.knownMinigames);
	}
}