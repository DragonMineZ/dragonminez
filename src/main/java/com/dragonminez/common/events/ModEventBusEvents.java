package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.animal.Dino1Entity;
import com.dragonminez.common.init.entities.animal.Dino2Entity;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import com.dragonminez.common.init.entities.animal.DinoKidEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.SagaNappaEntity;
import com.dragonminez.common.init.entities.sagas.SagaRaditzEntity;
import com.dragonminez.common.init.entities.sagas.SagaSaibamanEntity;
import com.dragonminez.common.init.entities.sagas.SagaVegetaEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MainEntities.MASTER_KARIN.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_GOKU.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_KAIOSAMA.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_ROSHI.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_URANAI.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_ENMA.get(), MastersEntity.createAttributes().build());

        event.put(MainEntities.SAGA_SAIBAMAN.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN2.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN3.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN4.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN5.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN6.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_RADITZ.get(), SagaRaditzEntity.createAttributes().build());
        event.put(MainEntities.SAGA_NAPPA.get(), SagaNappaEntity.createAttributes().build());
        event.put(MainEntities.SAGA_VEGETA.get(), SagaVegetaEntity.createAttributes().build());

        event.put(MainEntities.DINOSAUR1.get(), Dino1Entity.createAttributes().build());
        event.put(MainEntities.DINOSAUR2.get(), Dino2Entity.createAttributes().build());
        event.put(MainEntities.DINOSAUR3.get(), DinoFlyEntity.createAttributes().build());
        event.put(MainEntities.DINO_KID.get(), DinoKidEntity.createAttributes().build());

        event.put(MainEntities.BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(MainEntities.RED_RIBBON_ROBOT1.get(), RobotEntity.createAttributes().build());
        event.put(MainEntities.RED_RIBBON_ROBOT2.get(), RobotEntity.createAttributes().build());
        event.put(MainEntities.RED_RIBBON_ROBOT3.get(), RobotEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MainAttributes.DMZ_HEALTH.get());
    }
}
