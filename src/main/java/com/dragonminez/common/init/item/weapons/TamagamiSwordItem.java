package com.dragonminez.common.init.item.weapons;

import com.dragonminez.common.init.item.weapons.render.TamagamiSwordRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class TamagamiSwordItem extends WeaponItem implements GeoItem {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public TamagamiSwordItem() {
		super(230, -2.4f, 2400, 18, "tamagami_sword");
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {

			private TamagamiSwordRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.renderer == null)
					this.renderer = new TamagamiSwordRenderer();

				return this.renderer;

			}
		});
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
