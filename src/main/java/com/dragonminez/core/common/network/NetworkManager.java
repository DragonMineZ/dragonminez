package com.dragonminez.core.common.network;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.util.LogUtil;
import com.dragonminez.core.common.network.event.RegisterNetHandlerEvent;
import com.dragonminez.core.common.network.event.RegisterNetHandlerEvent.Phase;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkManager {

  public static final NetworkManager INSTANCE = new NetworkManager();
  private static SimpleChannel channel;
  private static int packetId = 0;

  public void init() {
    NetworkManager.channel = net.minecraftforge.network.NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(Reference.MOD_ID, "channel"))
        .networkProtocolVersion(() -> Reference.VERSION)
        .clientAcceptedVersions(s -> true)
        .serverAcceptedVersions(s -> true)
        .simpleChannel();
    this.fireDispatcher(Phase.START);
    this.fireDispatcher(Phase.END);
  }

  private void fireDispatcher(Phase phase) {
    MinecraftForge.EVENT_BUS.start();
    MinecraftForge.EVENT_BUS.post(new RegisterNetHandlerEvent(channel, phase));
  }

  public void retrieveChannel(Consumer<SimpleChannel> consumer) {
    if (NetworkManager.channel == null) {
      LogUtil.error(
          "Network channel is not registered. Please register the channel before using it.");
      return;
    }
    consumer.accept(NetworkManager.channel);
  }

  public int assignId() {
    final int id = NetworkManager.packetId;
    NetworkManager.packetId++;
    return id;
  }
}
