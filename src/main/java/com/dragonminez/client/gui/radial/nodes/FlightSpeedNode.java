package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.network.C2S.SetFlightSpeedLimitC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class FlightSpeedNode extends AbstractRadialNode {

    public static int maxFlightSpeed() {
        return 100;
    }

    private int currentFlightSpeed(StatsData stats) {
        return stats.getResources().getFlightSpeedLimit();
    }

    @Override
    public Component label(StatsData stats) {
        return Component.translatable("gui.action.dragonminez.flightspeed");
    }

    @Override
    public ResourceLocation icon(StatsData stats) {
        return null;
    }

    @Override
    public String faceText(StatsData stats) {
        int limit = currentFlightSpeed(stats);
        int value = limit > 0 ? limit : maxFlightSpeed();
        return value + "%";
    }

    @Override
    public boolean visible(StatsData stats) {
        if (stats.getCharacter() == null) return false;
        String race = stats.getCharacter().getRaceName();
        return race != null && !race.isEmpty() && stats.getSkills().hasSkill("fly");
    }

    @Override
    public boolean active(StatsData stats) {
        return currentFlightSpeed(stats) > 0 && currentFlightSpeed(stats) < 100;
    }

    @Override
    public int labelColor(StatsData stats) {
        return active(stats) ? GREEN : 0xFFFFFF;
    }

    @Override
    public boolean expandable(StatsData stats) {
        return false;
    }

    @Override
    public void onSelect(StatsData stats) {
        NetworkHandler.sendToServer(new SetFlightSpeedLimitC2S(100));
        playToggle(false);
    }

    public List<RadialNode> buildOptions() {
        List<RadialNode> out = new ArrayList<>();
        for (int value = maxFlightSpeed(); value >= 25; value -= 5) {
            out.add(new FlightSpeedLimitOptionNode(value));
        }
        return out;
    }
}
