package com.dragonminez.server.events.players;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.dimension.HTCDimension;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TPGainEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTPGain(DMZEvent.TPGainEvent event) {
        if (event.getPlayer() == null || event.getTpGain() <= 0) {
            return;
        }

		int baseTP = event.getTpGain();
        int modifiedTP = event.getTpGain();

        if (event.getPlayer().level().dimension().equals(HTCDimension.HTC_KEY)) {
            double htcMultiplier = ConfigManager.getServerConfig().getGameplay().getHTCTpMultiplier();
            modifiedTP = (int) (baseTP + baseTP * (htcMultiplier - 1.0));
        }

		// FrostDemon passive
		StatsProvider.get(StatsCapability.INSTANCE, event.getPlayer()).ifPresent(data -> {
			//if (data.getCharacter().getRace().equals("frostdemon")) {
			//	double frostDemonMultiplier = ConfigManager.getServerConfig().getPassives().getFrostDemonTpGainMultiplier();
			//	modifiedTP = (int) (modifiedTP + baseTP * (frostDemonMultiplier - 1.0));
			//}
		});


        event.setTpGain(modifiedTP);
    }
}

