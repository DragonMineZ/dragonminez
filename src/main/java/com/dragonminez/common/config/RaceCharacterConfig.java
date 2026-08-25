package com.dragonminez.common.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.*;

@Setter
@Getter
@NoArgsConstructor
public class RaceCharacterConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	private String configVersion;

	private String raceName;
	private Boolean hasGender = true;
	private Boolean useVanillaSkin = false;
	private String customModel = "";
	private Boolean isLayered = false;
	private String[] headBones = new String[0];
	private String racialSkill = "human";
	private Boolean hasSaiyanTail = false;
	private String auraType = "kakarot";
	private Float[] defaultModelScaling = {0.9375f, 0.9375f, 0.9375f};
	private Integer defaultBodyType = 0;
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

	@Getter
	@Setter
	@NoArgsConstructor
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
					boolean buyFromMaster = obj.has("buyFromMaster")
							&& !obj.get("buyFromMaster").isJsonNull()
							&& obj.get("buyFromMaster").getAsBoolean();
					List<Integer> prices = (obj.has("prices") && obj.get("prices").isJsonArray())
							? parsePrices(obj.getAsJsonArray("prices"))
							: new ArrayList<>();
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
					try { prices.add(el.getAsInt()); } catch (NumberFormatException | UnsupportedOperationException ignored) {}
				}
				return prices;
			}
		}
	}
}
