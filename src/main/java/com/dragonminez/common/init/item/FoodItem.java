package com.dragonminez.common.init.item;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FoodItem extends Item {
    private final int hunger;
    private final float saturation;
    private final boolean isSenzu;

    public FoodItem(int hunger, float saturation, boolean isMeat) {
        this(hunger, saturation, isMeat, false);
    }

    public FoodItem(int hunger, float saturation, boolean isMeat, boolean isSenzu) {
        super(new Properties().stacksTo(32).food(
                new FoodProperties.Builder()
                        .nutrition(hunger)
                        .saturationMod(saturation)
                        .meat()
                        .alwaysEat()
                        .build()
        ));
        this.hunger = hunger;
        this.saturation = saturation;
        this.isSenzu = isSenzu;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide && pLivingEntity instanceof ServerPlayer player) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                if (isSenzu && data.getCooldowns().hasCooldown(Cooldowns.SENZU_CD)) {
                    int remainingTicks = data.getCooldowns().getCooldown(Cooldowns.SENZU_CD);
                    int remainingSeconds = remainingTicks / 20;

                    player.displayClientMessage(
                            Component.translatable("item.dragonminez.senzu_bean.cooldown", remainingSeconds)
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                    return;
                }

                String itemId = "dragonminez:" + this.getDescriptionId().replace("item.dragonminez.", "");
                float[] regens = ConfigManager.getServerConfig().getGameplay().getFoodRegeneration(itemId);

                if (regens != null && regens.length >= 3) {
                    int maxHealth = data.getMaxHealth();
                    int maxEnergy = data.getMaxEnergy();
                    int maxStamina = data.getMaxStamina();

                    int healAmount = (int) (maxHealth * regens[0]);
                    int energyAmount = (int) (maxEnergy * regens[1]);
                    int staminaAmount = (int) (maxStamina * regens[2]);

                    player.heal(healAmount);
                    data.getResources().addEnergy(energyAmount);
                    data.getResources().addStamina(staminaAmount);

                    if (isSenzu) {
                        int cooldownTicks = ConfigManager.getServerConfig().getGameplay().getSenzuCooldownTicks();
                        data.getCooldowns().setCooldown(Cooldowns.SENZU_CD, cooldownTicks);
                    }

                    player.getFoodData().eat(hunger, saturation);

                    if (player.isCreative()) {
                        pStack.shrink(0);
                    } else {
                        pStack.shrink(1);
                    }
                }
            });
        }

        return pStack;
    }
}

