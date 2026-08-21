package com.dragonminez.common.util.types.items;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.gson.PotionEffectListTypeAdapter;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * A potion: a base potion id, custom effects, or both.
 *
 * <p>Subclassed by the splash, lingering and tipped-arrow variants. The potion NBT
 * ({@code Potion} and {@code custom_potion_effects}) is written for any item, not only a
 * {@code PotionItem}: {@code Items.TIPPED_ARROW} is a {@code TippedArrowItem extends
 * ArrowItem} and reads the same tags.
 */
@Getter
@Setter
@NoArgsConstructor
public class PotionDTO extends GenericItemDTO {

	public static final String ITEM_TYPE = "potion";
	private static final ResourceLocation POTION_ITEM_ID = new ResourceLocation("minecraft", "potion");

	protected ResourceLocation potion;

	@JsonAdapter(PotionEffectListTypeAdapter.class)
	protected List<PotionEffectDTO> mobEffects = new ArrayList<>();

	public PotionDTO(ResourceLocation potion, List<PotionEffectDTO> mobEffects) {
		this(ITEM_TYPE, POTION_ITEM_ID, 1, potion, mobEffects);
	}

	protected PotionDTO(String itemType, ResourceLocation itemId, int count, ResourceLocation potion,
			List<PotionEffectDTO> mobEffects) {
		super(itemType, itemId, count);
		this.potion = potion;
		this.mobEffects = mobEffects;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.mobEffects == null) {
			return;
		}
		for (PotionEffectDTO effect : this.mobEffects) {
			if (effect == null) {
				throw new JsonSyntaxException(errorPrefix() + " 'mobEffects' contains a null entry.");
			}
			effect.validate(errorPrefix());
		}
	}

	@Override
	public ItemStack getItemStack() {
		ItemStack itemStack = super.getItemStack();
		if (itemStack.isEmpty()) {
			return itemStack;
		}
		applyPotionData(itemStack);
		return itemStack;
	}

	protected void applyPotionData(ItemStack itemStack) {
		if (this.potion != null) {
			Potion resolved = ForgeRegistries.POTIONS.getValue(this.potion);
			if (resolved == null) {
				LogUtil.warn(Env.COMMON, "Unknown potion '{}' on item '{}', skipping.", this.potion, this.itemId);
			} else {
				PotionUtils.setPotion(itemStack, resolved);
			}
		}

		List<MobEffectInstance> custom = resolveMobEffects();
		if (!custom.isEmpty()) {
			PotionUtils.setCustomEffects(itemStack, custom);
		}
	}

	/** Resolves what it can; unknown effects are logged and skipped, not fatal. */
	protected final List<MobEffectInstance> resolveMobEffects() {
		List<MobEffectInstance> resolved = new ArrayList<>();
		if (this.mobEffects == null) {
			return resolved;
		}
		for (PotionEffectDTO entry : this.mobEffects) {
			if (entry == null || entry.getEffect() == null) {
				continue;
			}
			MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(entry.getEffect());
			if (effect == null) {
				LogUtil.warn(Env.COMMON, "Unknown mob effect '{}' on item '{}', skipping.",
						entry.getEffect(), this.itemId);
				continue;
			}
			resolved.add(new MobEffectInstance(
					effect,
					entry.getDuration() == null ? 1 : entry.getDuration(),
					entry.getAmplifier() == null ? 0 : entry.getAmplifier(),
					Boolean.TRUE.equals(entry.getAmbient()),
					!Boolean.FALSE.equals(entry.getVisible()),
					!Boolean.FALSE.equals(entry.getShowIcon())
			));
		}
		return resolved;
	}
}
