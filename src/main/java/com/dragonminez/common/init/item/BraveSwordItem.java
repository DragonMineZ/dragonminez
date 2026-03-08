package com.dragonminez.common.init.item;

import com.dragonminez.common.init.item.weapons.render.BraveSwordRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class BraveSwordItem extends WeaponItem implements GeoItem {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public BraveSwordItem() {
		super(100, -2.4f, 2600, 20, "brave_sword");
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {

			private BraveSwordRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.renderer == null)
					this.renderer = new BraveSwordRenderer();

				return this.renderer;

			}
		});
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
