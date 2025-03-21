package com.yuseix.dragonminez.init.items.custom;

import com.yuseix.dragonminez.config.DMZGeneralConfig;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ComidaDinoCookedItem extends Item {
    private static final int HUNGER = 8;
    private static final float SATURATION = 12.8F;

    public ComidaDinoCookedItem() {
        super(new Properties().stacksTo(32).food(
                new FoodProperties.Builder()
                        .nutrition(HUNGER)
                        .saturationMod(SATURATION)
                        .meat()
                        .alwaysEat()
                        .build()
        ));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack pStack) {
        return Component.translatable("item.dragonminez.comida_dino_cooked");
    }

    // Curación Vida/Ki
    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide && pLivingEntity instanceof ServerPlayer player) {
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
                int maxHp = stats.getIntValue("maxhealth");
                int curarVida = (int) (maxHp * DMZGeneralConfig.DINO_MEAT_COOKED_HEALTHREGEN.get());
                int maxKi = stats.getIntValue("maxenergy");
                int curarKi = (int) (maxKi * DMZGeneralConfig.DINO_MEAT_COOKED_KIREGEN.get());
                int maxStm = stats.getIntValue("maxstam");
                int curarStm = (int) (maxStm * DMZGeneralConfig.DINO_MEAT_COOKED_STAMREGEN.get());

                player.heal(curarVida);
                stats.addIntValue("curenergy", curarKi);
                stats.addIntValue("curstam", curarStm);
            });
            player.getFoodData().eat(HUNGER, SATURATION);
        }
        if (pLivingEntity instanceof ServerPlayer player && player.isCreative()) {
            pStack.shrink(0);
        } else pStack.shrink(1);
        return pStack;
    }
}
