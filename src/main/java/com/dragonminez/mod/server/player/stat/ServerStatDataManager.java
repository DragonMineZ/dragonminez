package com.dragonminez.mod.server.player.stat;

import com.dragonminez.mod.common.player.cap.stat.StatData;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataType;
import com.dragonminez.mod.common.player.cap.stat.StatDataManager;
import com.dragonminez.mod.core.server.player.capability.IServerCapDataManager;
import net.minecraft.server.level.ServerPlayer;

public class ServerStatDataManager extends StatDataManager implements
    IServerCapDataManager<StatDataManager, StatData> {

  public static ServerStatDataManager INSTANCE = new ServerStatDataManager();

  private ServerStatDataManager() {
    super();
  }

  public void setStrength(ServerPlayer player, int strength, boolean log) {
    this.setStatInternal(this, player, StatDataType.STRENGTH, strength, data ->
        data.setStrength(strength), log);
  }

  public void setStrikePower(ServerPlayer player, int strikePower, boolean log) {
    this.setStatInternal(this, player, StatDataType.STRIKE_POWER, strikePower,
        data -> data.setStrikePower(strikePower), log);
  }

  public void setEnergy(ServerPlayer player, int energy, boolean log) {
    this.setStatInternal(this, player, StatDataType.ENERGY, energy, data ->
        data.setEnergy(energy), log);
  }

  public void setVitality(ServerPlayer player, int vitality, boolean log) {
    this.setStatInternal(this, player, StatDataType.VITALITY, vitality, data ->
        data.setVitality(vitality), log);
  }

  public void setResistance(ServerPlayer player, int resistance, boolean log) {
    this.setStatInternal(this, player, StatDataType.RESISTANCE, resistance,
        data -> data.setResistance(resistance), log);
  }

  public void setKiPower(ServerPlayer player, int kiPower, boolean log) {
    this.setStatInternal(this, player, StatDataType.KI_POWER, kiPower, data ->
        data.setKiPower(kiPower), log);
  }

  public void setAlignment(ServerPlayer player, int alignment, boolean log) {
    this.setStatInternal(this, player, StatDataType.ALIGNMENT, alignment,
        data -> data.setAlignment(alignment), log);
  }
}
