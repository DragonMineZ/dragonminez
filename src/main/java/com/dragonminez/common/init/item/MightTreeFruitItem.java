package com.dragonminez.common.init.item;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MightTreeFruitItem extends Item {
    private static final int HUNGER = 2;
    private static final float SATURATION = 20;
    private static final int EFFECT_DURATION_TICKS = 20 * 60;

    public MightTreeFruitItem() {
        super(new Properties().stacksTo(1).food(
                new FoodProperties.Builder()
                        .nutrition(HUNGER)
                        .saturationMod(SATURATION)
                        .alwaysEat()
                        .build()
        ));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack pStack) {
        return Component.translatable("item.dragonminez.might_tree_fruit");
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("item.dragonminez.might_tree_fruit.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide && pLivingEntity instanceof ServerPlayer player) {
            String itemId = "dragonminez:might_tree_fruit";
            float[] regens = ConfigManager.getServerConfig().getGameplay().getFoodRegeneration(itemId);

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (regens != null && regens.length >= 3) {
                    int maxHealth = data.getMaxHealth();
                    int maxEnergy = data.getMaxEnergy();

                    int healAmount = (int) (maxHealth * regens[0]);
                    int energyAmount = (int) (maxEnergy * regens[1]);

                    player.heal(healAmount);
                    data.getResources().addEnergy(energyAmount);
                }

                // TODO: Hacer efectos xd
                // data.getEffects().addEffect("mightfruit", EFFECT_DURATION_TICKS);
            });

            player.getFoodData().eat(HUNGER, SATURATION);
            player.displayClientMessage(Component.translatable("item.dragonminez.might_tree_fruit.use"), true);

            if (player.isCreative()) {
                pStack.shrink(0);
            } else {
                pStack.shrink(1);
            }
        }

        return pStack;
    }
}

