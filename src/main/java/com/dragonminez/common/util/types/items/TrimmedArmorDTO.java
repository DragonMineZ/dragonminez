package com.dragonminez.common.util.types.items;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import java.util.Map;

public class TrimmedArmorDTO extends EnchantedItemDTO {
	public static final String ITEM_TYPE = "trimmed_armor";
	protected ResourceLocation material;
	protected ResourceLocation pattern;

	public TrimmedArmorDTO(ResourceLocation itemId, ResourceLocation material, ResourceLocation pattern) {
		super(ITEM_TYPE, itemId, 1);
		this.material = material;
		this.pattern = pattern;
	}

	public TrimmedArmorDTO(ResourceLocation itemId, Map<ResourceLocation, Integer> enchantments, ResourceLocation material, ResourceLocation pattern) {
		super(ITEM_TYPE, itemId, 1, enchantments);
		this.material = material;
		this.pattern = pattern;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.material == null) throw new JsonSyntaxException(errorPrefix() + " \'material\' must not be null.");
		if (this.pattern == null) throw new JsonSyntaxException(errorPrefix() + " \'pattern\' must not be null.");
	}

	@Override
	public ItemStack getItemStack(RegistryAccess registryAccess) {
		ItemStack stack = super.getItemStack(registryAccess);
		if (stack.isEmpty() || registryAccess == null) return stack;
		if (!(stack.getItem() instanceof ArmorItem)) {
			LogUtil.warn(Env.COMMON, "Item \'{}\' is not armour, skipping its trim.", this.itemId);
			return stack;
		}
		if (this.material == null || this.pattern == null) return stack;
		Holder<TrimMaterial> materialHolder = registryAccess.lookupOrThrow(Registries.TRIM_MATERIAL).get(ResourceKey.create(Registries.TRIM_MATERIAL, this.material)).orElse(null);
		Holder<TrimPattern> patternHolder = registryAccess.lookupOrThrow(Registries.TRIM_PATTERN).get(ResourceKey.create(Registries.TRIM_PATTERN, this.pattern)).orElse(null);
		if (materialHolder == null || patternHolder == null) {
			LogUtil.warn(Env.COMMON, "Unknown trim material or pattern on item \'{}\', skipping.", this.itemId);
			return stack;
		}
		stack.set(DataComponents.TRIM, new ArmorTrim(materialHolder, patternHolder));
		return stack;
	}

	public ResourceLocation getMaterial() {
		return this.material;
	}

	public ResourceLocation getPattern() {
		return this.pattern;
	}

	public void setMaterial(final ResourceLocation material) {
		this.material = material;
	}

	public void setPattern(final ResourceLocation pattern) {
		this.pattern = pattern;
	}

	public TrimmedArmorDTO() {
	}
}
