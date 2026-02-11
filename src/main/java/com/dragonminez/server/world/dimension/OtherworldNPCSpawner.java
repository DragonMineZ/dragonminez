package com.dragonminez.server.world.dimension;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.masters.MasterEnmaEntity;
import com.dragonminez.common.init.entities.masters.MasterKaiosamaEntity;
import com.dragonminez.common.init.entities.masters.MasterUranaiEntity;
import com.dragonminez.server.world.data.OtherworldNPCSavedData;
import net.minecraft.server.level.ServerLevel;

public class OtherworldNPCSpawner {

    public static void spawnNPCs(ServerLevel otherworld) {
        if (otherworld == null || otherworld.dimension() != OtherworldDimension.OTHERWORLD_KEY) {
            return;
        }

        OtherworldNPCSavedData data = OtherworldNPCSavedData.get(otherworld);
        if (data.hasNPCsSpawned()) {
            LogUtil.info(Env.COMMON, "Otherworld NPCs already spawned, skipping.");
            return;
        }

        MasterKaiosamaEntity kaiosama = new MasterKaiosamaEntity(MainEntities.MASTER_KAIOSAMA.get(), otherworld);
        kaiosama.moveTo(54.5, 190, 1082.5, 0.0F, 0.0F);
        kaiosama.setPersistenceRequired();
        otherworld.addFreshEntity(kaiosama);
        LogUtil.info(Env.COMMON, "Spawned Master Kaiosama at 54, 190, 1082");

        MasterEnmaEntity enma = new MasterEnmaEntity(MainEntities.MASTER_ENMA.get(), otherworld);
        enma.moveTo(0.5, 41, 66.5, 0.0F, 0.0F);
        enma.setPersistenceRequired();
        otherworld.addFreshEntity(enma);
        LogUtil.info(Env.COMMON, "Spawned Master Enma at 0, 41, 69");

        MasterUranaiEntity uranai = new MasterUranaiEntity(MainEntities.MASTER_URANAI.get(), otherworld);
        uranai.moveTo(14.5, 41, 76.5, 0.0F, 0.0F);
        uranai.setPersistenceRequired();
        otherworld.addFreshEntity(uranai);
        LogUtil.info(Env.COMMON, "Spawned Master Uranai at 6, 41, 53");

        data.setNPCsSpawned();
        LogUtil.info(Env.COMMON, "All Otherworld NPCs have been spawned successfully.");
    }
}


