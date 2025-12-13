package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.animal.Dino1Entity;
import com.dragonminez.common.init.entities.animal.Dino2Entity;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import com.dragonminez.common.init.entities.animal.DinoKidEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MainEntities.MASTER_KARIN.get(), MastersEntity.createAttributes().build());

        event.put(MainEntities.DINOSAUR1.get(), Dino1Entity.createAttributes().build());
        event.put(MainEntities.DINOSAUR2.get(), Dino2Entity.createAttributes().build());
        event.put(MainEntities.DINOSAUR3.get(), DinoFlyEntity.createAttributes().build());
        event.put(MainEntities.DINO_KID.get(), DinoKidEntity.createAttributes().build());

    }
}
