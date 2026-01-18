package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.commands.EffectsCommand;
import com.dragonminez.common.commands.PartyCommand;
import com.dragonminez.common.commands.*;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommonEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StatsCommand.register(event.getDispatcher());
        PointsCommand.register(event.getDispatcher());
        SkillsCommand.register(event.getDispatcher());
        EffectsCommand.register(event.getDispatcher());
        PartyCommand.register(event.getDispatcher());
        BonusCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getSlot() != EquipmentSlot.CHEST) {
            return;
        }

        ItemStack newStack = event.getTo(); // El ítem que se acaba de poner

        // Obtenemos la capability
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {

            boolean shouldBeArmored = false;

            // 3. Verificamos si tiene algo puesto y si es una armadura
            if (!newStack.isEmpty() && newStack.getItem() instanceof ArmorItem) {

                boolean isVanilla = ForgeRegistries.ITEMS.getKey(newStack.getItem()).getNamespace().equals("minecraft");
                boolean isDbzArmor = newStack.getItem() instanceof DbzArmorItem;

                if (!isVanilla && !isDbzArmor) {
                    shouldBeArmored = true;
                }
            }

            if (stats.getCharacter().getArmored() != shouldBeArmored) {
                stats.getCharacter().setArmored(shouldBeArmored);
            }
        });
    }
}

