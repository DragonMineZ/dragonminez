package com.dragonminez.common.util.types.items;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.gson.PotionEffectListTypeAdapter;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PotionDTO extends GenericItemDTO {
	public static final String ITEM_TYPE = "potion";
	private static final ResourceLocation POTION_ITEM_ID = ResourceLocation.withDefaultNamespace("potion");
	protected ResourceLocation potion;
	@JsonAdapter(PotionEffectListTypeAdapter.class)
	protected List<PotionEffectDTO> mobEffects = new ArrayList<>();

	public PotionDTO(ResourceLocation potion, List<PotionEffectDTO> mobEffects) {
		this(ITEM_TYPE, POTION_ITEM_ID, 1, potion, mobEffects);
	}

	protected PotionDTO(String itemType, ResourceLocation itemId, int count, ResourceLocation potion, List<PotionEffectDTO> mobEffects) {
		super(itemType, itemId, count);
		this.potion = potion;
		this.mobEffects = mobEffects;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.mobEffects == null) return;
		for (PotionEffectDTO effect : this.mobEffects) {
			if (effect == null) {
				throw new JsonSyntaxException(errorPrefix() + " \'mobEffects\' contains a null entry.");
			}
			effect.validate(errorPrefix());
		}
	}

	@Override
	public ItemStack getItemStack() {
		ItemStack stack = super.getItemStack();
		if (!stack.isEmpty()) applyPotionData(stack);
		return stack;
	}

	@Override
	public ItemStack getItemStack(net.minecraft.core.RegistryAccess registryAccess) {
		return getItemStack();
	}

	private void applyPotionData(ItemStack stack) {
		Holder<Potion> potionHolder = Potions.WATER;
		if (this.potion != null) {
			Holder<Potion> registered = BuiltInRegistries.POTION.getHolder(this.potion).orElse(null);
			if (registered != null) {
				potionHolder = registered;
			} else {
				LogUtil.warn(Env.COMMON, "Unknown potion \'{}\' on item \'{}\', using water.", this.potion, this.itemId);
			}
		}
		List<MobEffectInstance> customEffects = new ArrayList<>();
		if (this.mobEffects != null) {
			for (PotionEffectDTO effect : this.mobEffects) {
				if (effect == null || effect.getEffect() == null) continue;
				Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.getHolder(effect.getEffect()).orElse(null);
				if (holder == null) {
					LogUtil.warn(Env.COMMON, "Unknown mob effect \'{}\' on item \'{}\', skipping.", effect.getEffect(), this.itemId);
					continue;
				}
				customEffects.add(new MobEffectInstance(holder, effect.getDuration() == null ? 1 : effect.getDuration(), effect.getAmplifier() == null ? 0 : effect.getAmplifier(), Boolean.TRUE.equals(effect.getAmbient()), !Boolean.FALSE.equals(effect.getVisible()), !Boolean.FALSE.equals(effect.getShowIcon())));
			}
		}
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(potionHolder), Optional.empty(), customEffects));
	}

	public ResourceLocation getPotion() {
		return this.potion;
	}

	public List<PotionEffectDTO> getMobEffects() {
		return this.mobEffects;
	}

	public void setPotion(final ResourceLocation potion) {
		this.potion = potion;
	}

	public void setMobEffects(final List<PotionEffectDTO> mobEffects) {
		this.mobEffects = mobEffects;
	}

	public PotionDTO() {
	}
}
