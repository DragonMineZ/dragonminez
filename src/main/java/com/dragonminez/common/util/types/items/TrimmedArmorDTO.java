package com.dragonminez.common.util.types.items;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * An armour piece with a smithing trim, and optionally enchantments.
 */
@Getter
@Setter
@NoArgsConstructor
public class TrimmedArmorDTO extends EnchantedItemDTO {

	public static final String ITEM_TYPE = "trimmed_armor";

	protected ResourceLocation material;
	protected ResourceLocation pattern;

	public TrimmedArmorDTO(ResourceLocation itemId, ResourceLocation material, ResourceLocation pattern) {
		super(ITEM_TYPE, itemId, 1);
		this.material = material;
		this.pattern = pattern;
	}

	public TrimmedArmorDTO(ResourceLocation itemId, Map<ResourceLocation, Integer> enchantments,
			ResourceLocation material, ResourceLocation pattern) {
		super(ITEM_TYPE, itemId, 1, enchantments);
		this.material = material;
		this.pattern = pattern;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.material == null) {
			throw new JsonSyntaxException(errorPrefix() + " 'material' must not be null.");
		}
		if (this.pattern == null) {
			throw new JsonSyntaxException(errorPrefix() + " 'pattern' must not be null.");
		}
	}

	@Override
	public ItemStack getItemStack() {
		ItemStack itemStack = super.getItemStack();
		if (itemStack.isEmpty()) {
			return itemStack;
		}
		applyTrim(itemStack);
		return itemStack;
	}

	protected void applyTrim(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ArmorItem)) {
			LogUtil.warn(Env.COMMON, "Item '{}' is not armour, skipping its trim.", this.itemId);
			return;
		}
		if (this.material == null || this.pattern == null) {
			return;
		}

		CompoundTag trimTag = itemStack.getOrCreateTagElement("Trim");
		trimTag.putString("material", this.material.toString());
		trimTag.putString("pattern", this.pattern.toString());
	}
}
