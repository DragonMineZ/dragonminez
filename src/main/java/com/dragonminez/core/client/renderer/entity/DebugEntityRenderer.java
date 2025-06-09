package com.dragonminez.core.client.renderer.entity;

import com.dragonminez.core.common.player.capability.CapManagerRegistry;
import com.dragonminez.core.common.util.JavaUtil;
import com.dragonminez.core.common.util.TextUtil;
import com.dragonminez.mod.common.Reference;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class DebugEntityRenderer {

  private static final int REFRESH_TICKS = 40;
  private static final int CLEANUP_TICKS = 200;

  private static final Map<UUID, Entry> DEBUG_CACHE = new HashMap<>();

  @SubscribeEvent
  public static void onRenderLiving(RenderLivingEvent<Player, PlayerModel<Player>> event) {
    final LivingEntity entity = event.getEntity();
    if (!(entity instanceof Player player)) {
      return;
    }

    final Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.player == player) {
      return;
    }

    final int currentTick = player.tickCount;
    final UUID uuid = player.getUUID();

    DEBUG_CACHE.entrySet().removeIf(
        e -> currentTick - e.getValue().lastAccessTick > CLEANUP_TICKS);

    Entry entry = DEBUG_CACHE.get(uuid);
    boolean needsRefresh = entry == null || entry.data.isEmpty()
        || currentTick - entry.lastAccessTick >= REFRESH_TICKS;

    if (needsRefresh) {
      List<CachedDebug> updated = new ArrayList<>();
      CapManagerRegistry.INSTANCE.values(Dist.CLIENT).forEach(manager -> {
        manager.retrieveData(player, cap -> {
          String label = JavaUtil.toLegible(cap.holder().identifier().getPath());
          Tag tag = cap.serialize(new CompoundTag());
          String pretty = TextUtil.prettyPrintNBT(tag);
          updated.add(new CachedDebug(label, pretty));
        });
      });
      entry = new Entry(updated, currentTick);
      DEBUG_CACHE.put(uuid, entry);
    } else {
      entry.lastAccessTick = currentTick;
    }

    renderDebugText(event, player, entry.data);
  }

  private static void renderDebugText(RenderLivingEvent<Player, PlayerModel<Player>> event,
      Player player, List<CachedDebug> debugData) {
    PoseStack poseStack = event.getPoseStack();
    MultiBufferSource buffer = event.getMultiBufferSource();
    Font font = Minecraft.getInstance().font;

    poseStack.pushPose();
    poseStack.translate(0, player.getBbHeight() + 4.3, 0.7);
    poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
    poseStack.scale(-0.025f, -0.025f, 0.025f);

    float y = 0f;
    final float lineHeight = 10f;

    int maxWidth = 0;
    for (CachedDebug debug : debugData) {
      int labelWidth = font.width(debug.label);
      if (labelWidth > maxWidth) {
        maxWidth = labelWidth;
      }

      for (String line : debug.pretty.split("\n")) {
        if (!line.isBlank()) {
          int lineWidth = font.width(line) + 10;
          if (lineWidth > maxWidth) {
            maxWidth = lineWidth;
          }
        }
      }
    }

    for (CachedDebug debug : debugData) {
      font.drawInBatch(debug.label, -maxWidth / 2f, y, 0xFFFFFF, false,
          poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 15728880);
      y += lineHeight;

      for (String line : debug.pretty.split("\n")) {
        if (!line.isBlank()) {
          font.drawInBatch(line, -maxWidth / 2f + 10, y, 0xFFAA00, false,
              poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 15728880);
          y += lineHeight;
        }
      }

      y += lineHeight;
    }

    poseStack.popPose();
  }

  private record CachedDebug(String label, String pretty) {

  }

  private static class Entry {

    final List<CachedDebug> data;
    int lastAccessTick;

    Entry(List<CachedDebug> data, int tick) {
      this.data = data;
      this.lastAccessTick = tick;
    }
  }
}
