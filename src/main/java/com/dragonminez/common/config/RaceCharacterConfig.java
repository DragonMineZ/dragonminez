package com.dragonminez.common.config;

import com.dragonminez.common.diagnostics.RemovedConfigKeys;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.*;

@RemovedConfigKeys({"raceName"})
public class RaceCharacterConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	private String configVersion;
	private Boolean hasGender = true;
	private Boolean useVanillaSkin = false;
	private String customModel = "";
	private Boolean isLayered = false;
	private String[] headBones = new String[0];
	private String racialSkill = "human";
	private Boolean hasSaiyanTail = false;
	private String auraType = "kakarot";
	private String auraType3D = FormConfig.AURA_3D_USER_PREFERENCE;
	private FormConfig.Aura3DStyle aura3DStyle = new FormConfig.Aura3DStyle();
	private Float[] defaultModelScaling = {0.9375F, 0.9375F, 0.9375F};
	private Integer defaultBodyType = 0;
	private Integer[] slimBodyTypes = new Integer[0];
	private Integer defaultHairType = 0;
	private Integer defaultEyesType = 0;
	private Integer defaultNoseType = 0;
	private Integer defaultMouthType = 0;
	private Integer defaultTattooType = 0;
	private String defaultBodyColor = null;
	private String defaultBodyColor2 = null;
	private String defaultBodyColor3 = null;
	private String defaultHairColor = null;
	private String defaultEye1Color = null;
	private String defaultEye2Color = null;
	private String defaultAuraColor = null;
	private Map<String, FormSkillCost> formSkillsCosts = new HashMap<>();

	public FormConfig.Aura3DStyle getAura3DStyle() {
		return aura3DStyle != null ? aura3DStyle : FormConfig.Aura3DStyle.DEFAULT;
	}

	public boolean isSlimBodyType(int bodyType) {
		if (slimBodyTypes == null) return false;
		for (Integer slim : slimBodyTypes) if (slim != null && slim == bodyType) return true;
		return false;
	}

	private FormSkillCost getFormSkillEntry(String form) {
		if (form == null) return null;
		FormSkillCost entry = formSkillsCosts.get(form);
		return entry != null ? entry : formSkillsCosts.get(form.toLowerCase());
	}

	public Integer[] getFormSkillTpCosts(String form) {
		FormSkillCost entry = getFormSkillEntry(form);
		List<Integer> list = (entry != null && entry.getPrices() != null) ? entry.getPrices() : new ArrayList<>();
		return list.toArray(new Integer[0]);
	}

	public boolean isFormSkillBuyFromMaster(String form) {
		FormSkillCost entry = getFormSkillEntry(form);
		return entry != null && entry.isBuyFromMaster();
	}

	public boolean hasFormSkill(String form) {
		return getFormSkillEntry(form) != null;
	}

	public Collection<String> getFormSkills() {
		return formSkillsCosts.keySet();
	}

	public void setFormSkillTpCosts(String form, Integer[] costs) {
		formSkillsCosts.put(form, new FormSkillCost(new ArrayList<>(Arrays.asList(costs))));
	}

	public boolean normalizeFormSkillKeys(Collection<String> canonicalFormSkills) {
		if (formSkillsCosts == null || formSkillsCosts.isEmpty() || canonicalFormSkills == null || canonicalFormSkills.isEmpty()) return false;
		Set<String> canonical = new HashSet<>();
		for (String name : canonicalFormSkills) if (name != null) canonical.add(name.toLowerCase());
		Map<String, FormSkillCost> normalized = new LinkedHashMap<>();
		boolean changed = false;
		for (Map.Entry<String, FormSkillCost> entry : formSkillsCosts.entrySet()) {
			String key = entry.getKey();
			if (key == null) continue;
			String lower = key.toLowerCase();
			if (canonical.contains(lower)) {
				if (!normalized.containsKey(lower)) normalized.put(lower, entry.getValue());
				if (!key.equals(lower)) changed = true;
			}
		}
		for (Map.Entry<String, FormSkillCost> entry : formSkillsCosts.entrySet()) {
			String key = entry.getKey();
			if (key == null) continue;
			String lower = key.toLowerCase();
			if (canonical.contains(lower)) continue;
			String target = canonical.contains(lower + "s") ? lower + "s" : lower;
			if (!target.equals(key)) changed = true;
			FormSkillCost existing = normalized.get(target);
			if (existing == null || existing.getPrices() == null || existing.getPrices().isEmpty()) normalized.put(target, entry.getValue());
		}
		if (changed) {
			formSkillsCosts.clear();
			formSkillsCosts.putAll(normalized);
		}
		return changed;
	}

	public Boolean hasCustomModel() {
		return this.customModel != null && !this.customModel.isEmpty();
	}


	public static class FormSkillCost {
		private boolean buyFromMaster = false;
		private List<Integer> prices = new ArrayList<>();

		public FormSkillCost(List<Integer> prices) {
			this.prices = prices != null ? prices : new ArrayList<>();
		}

		public FormSkillCost(boolean buyFromMaster, List<Integer> prices) {
			this.buyFromMaster = buyFromMaster;
			this.prices = prices != null ? prices : new ArrayList<>();
		}


		public static class Adapter implements JsonDeserializer<FormSkillCost>, JsonSerializer<FormSkillCost> {
			@Override
			public FormSkillCost deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
				if (json == null || json.isJsonNull()) return new FormSkillCost();
				if (json.isJsonArray()) return new FormSkillCost(false, parsePrices(json.getAsJsonArray()));
				if (json.isJsonObject()) {
					JsonObject obj = json.getAsJsonObject();
					boolean buyFromMaster = obj.has("buyFromMaster") && !obj.get("buyFromMaster").isJsonNull() && obj.get("buyFromMaster").getAsBoolean();
					List<Integer> prices = (obj.has("prices") && obj.get("prices").isJsonArray()) ? parsePrices(obj.getAsJsonArray("prices")) : new ArrayList<>();
					return new FormSkillCost(buyFromMaster, prices);
				}
				return new FormSkillCost();
			}

			@Override
			public JsonElement serialize(FormSkillCost src, Type typeOfSrc, JsonSerializationContext ctx) {
				JsonObject obj = new JsonObject();
				obj.addProperty("buyFromMaster", src != null && src.isBuyFromMaster());
				JsonArray prices = new JsonArray();
				if (src != null && src.getPrices() != null) for (Integer p : src.getPrices()) prices.add(p);
				obj.add("prices", prices);
				return obj;
			}

			private static List<Integer> parsePrices(JsonArray arr) {
				List<Integer> prices = new ArrayList<>();
				for (JsonElement el : arr) {
					if (el == null || el.isJsonNull()) continue;
					try {
						prices.add(el.getAsInt());
					} catch (NumberFormatException | UnsupportedOperationException ignored) {
					}
				}
				return prices;
			}
		}

		public boolean isBuyFromMaster() {
			return this.buyFromMaster;
		}

		public List<Integer> getPrices() {
			return this.prices;
		}

		public void setBuyFromMaster(final boolean buyFromMaster) {
			this.buyFromMaster = buyFromMaster;
		}

		public void setPrices(final List<Integer> prices) {
			this.prices = prices;
		}

		public FormSkillCost() {
		}
	}

	public void setConfigVersion(final String configVersion) {
		this.configVersion = configVersion;
	}

	public void setHasGender(final Boolean hasGender) {
		this.hasGender = hasGender;
	}

	public void setUseVanillaSkin(final Boolean useVanillaSkin) {
		this.useVanillaSkin = useVanillaSkin;
	}

	public void setCustomModel(final String customModel) {
		this.customModel = customModel;
	}

	public void setIsLayered(final Boolean isLayered) {
		this.isLayered = isLayered;
	}

	public void setHeadBones(final String[] headBones) {
		this.headBones = headBones;
	}

	public void setRacialSkill(final String racialSkill) {
		this.racialSkill = racialSkill;
	}

	public void setHasSaiyanTail(final Boolean hasSaiyanTail) {
		this.hasSaiyanTail = hasSaiyanTail;
	}

	public void setAuraType(final String auraType) {
		this.auraType = auraType;
	}

	public void setAuraType3D(final String auraType3D) {
		this.auraType3D = auraType3D;
	}

	public void setAura3DStyle(final FormConfig.Aura3DStyle aura3DStyle) {
		this.aura3DStyle = aura3DStyle;
	}

	public void setDefaultModelScaling(final Float[] defaultModelScaling) {
		this.defaultModelScaling = defaultModelScaling;
	}

	public void setDefaultBodyType(final Integer defaultBodyType) {
		this.defaultBodyType = defaultBodyType;
	}

	public void setSlimBodyTypes(final Integer[] slimBodyTypes) {
		this.slimBodyTypes = slimBodyTypes;
	}

	public void setDefaultHairType(final Integer defaultHairType) {
		this.defaultHairType = defaultHairType;
	}

	public void setDefaultEyesType(final Integer defaultEyesType) {
		this.defaultEyesType = defaultEyesType;
	}

	public void setDefaultNoseType(final Integer defaultNoseType) {
		this.defaultNoseType = defaultNoseType;
	}

	public void setDefaultMouthType(final Integer defaultMouthType) {
		this.defaultMouthType = defaultMouthType;
	}

	public void setDefaultTattooType(final Integer defaultTattooType) {
		this.defaultTattooType = defaultTattooType;
	}

	public void setDefaultBodyColor(final String defaultBodyColor) {
		this.defaultBodyColor = defaultBodyColor;
	}

	public void setDefaultBodyColor2(final String defaultBodyColor2) {
		this.defaultBodyColor2 = defaultBodyColor2;
	}

	public void setDefaultBodyColor3(final String defaultBodyColor3) {
		this.defaultBodyColor3 = defaultBodyColor3;
	}

	public void setDefaultHairColor(final String defaultHairColor) {
		this.defaultHairColor = defaultHairColor;
	}

	public void setDefaultEye1Color(final String defaultEye1Color) {
		this.defaultEye1Color = defaultEye1Color;
	}

	public void setDefaultEye2Color(final String defaultEye2Color) {
		this.defaultEye2Color = defaultEye2Color;
	}

	public void setDefaultAuraColor(final String defaultAuraColor) {
		this.defaultAuraColor = defaultAuraColor;
	}

	public void setFormSkillsCosts(final Map<String, FormSkillCost> formSkillsCosts) {
		this.formSkillsCosts = formSkillsCosts;
	}

	public String getConfigVersion() {
		return this.configVersion;
	}

	public Boolean getHasGender() {
		return this.hasGender;
	}

	public Boolean getUseVanillaSkin() {
		return this.useVanillaSkin;
	}

	public String getCustomModel() {
		return this.customModel;
	}

	public Boolean getIsLayered() {
		return this.isLayered;
	}

	public String[] getHeadBones() {
		return this.headBones;
	}

	public String getRacialSkill() {
		return this.racialSkill;
	}

	public Boolean getHasSaiyanTail() {
		return this.hasSaiyanTail;
	}

	public String getAuraType() {
		return this.auraType;
	}

	public String getAuraType3D() {
		return this.auraType3D;
	}

	public Float[] getDefaultModelScaling() {
		return this.defaultModelScaling;
	}

	public Integer getDefaultBodyType() {
		return this.defaultBodyType;
	}

	public Integer[] getSlimBodyTypes() {
		return this.slimBodyTypes;
	}

	public Integer getDefaultHairType() {
		return this.defaultHairType;
	}

	public Integer getDefaultEyesType() {
		return this.defaultEyesType;
	}

	public Integer getDefaultNoseType() {
		return this.defaultNoseType;
	}

	public Integer getDefaultMouthType() {
		return this.defaultMouthType;
	}

	public Integer getDefaultTattooType() {
		return this.defaultTattooType;
	}

	public String getDefaultBodyColor() {
		return this.defaultBodyColor;
	}

	public String getDefaultBodyColor2() {
		return this.defaultBodyColor2;
	}

	public String getDefaultBodyColor3() {
		return this.defaultBodyColor3;
	}

	public String getDefaultHairColor() {
		return this.defaultHairColor;
	}

	public String getDefaultEye1Color() {
		return this.defaultEye1Color;
	}

	public String getDefaultEye2Color() {
		return this.defaultEye2Color;
	}

	public String getDefaultAuraColor() {
		return this.defaultAuraColor;
	}

	public Map<String, FormSkillCost> getFormSkillsCosts() {
		return this.formSkillsCosts;
	}

	public RaceCharacterConfig() {
	}
}
