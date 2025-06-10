package com.dragonminez.core.client.hud;

import com.dragonminez.core.common.player.capability.CapManagerRegistry;
import com.dragonminez.core.common.util.DebugUtil;
import com.dragonminez.core.common.util.JavaUtil;
import com.dragonminez.core.common.util.TextUtil;
import com.dragonminez.mod.common.Reference;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Debug HUD for displaying serialized capability NBT data of the player in the in-game GUI.
 * <p>
 * This class listens to Forge's RenderGuiOverlayEvent and renders player capability data when the
 * game is running in a non-production environment. It caches the pretty-printed NBT strings once
 * per player tick to optimize performance.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DebugHud {

  /**
   * Cache storing pretty-printed NBT data keyed by capability identifier
   */
  private static final Map<String, String> nbtCache = new HashMap<>();

  /**
   * Last game tick when the NBT cache was updated
   */
  private static int lastUpdateTick = -1;

  /**
   * Event handler for the GUI rendering event. Skips rendering in production environment and calls
   * the method to render debug info.
   */
  @SubscribeEvent
  public static void onRenderGameOverlay(RenderGuiOverlayEvent.Pre event) {
    if (!DebugUtil.isDebug()) {
      return;
    }
    renderDebugInformation(event.getGuiGraphics());
  }

  /**
   * Renders the debug information on the screen. Updates the cache every tick to avoid unnecessary
   * recomputation.
   */
  private static void renderDebugInformation(GuiGraphics graphics) {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer player = mc.player;
    if (player == null || mc.options.hideGui) return;

    int currentTick = player.tickCount;
    if (currentTick != lastUpdateTick) {
      lastUpdateTick = currentTick;
      nbtCache.clear();

      CapManagerRegistry.INSTANCE.values(Dist.CLIENT)
          .forEach(manager ->
              manager.retrieveData(player, cap -> {
                Tag tag = cap.serialize(new CompoundTag(), false);
                String prettyData = TextUtil.prettyPrintNBT(tag);
                nbtCache.put(JavaUtil.toLegible(cap.holder().identifier().getPath()), prettyData);
              }));
    }

    Font font = mc.font;
    int unscaledLineHeight = 10;
    int totalLines = 0;

    for (String pretty : nbtCache.values()) {
      totalLines++; // label
      for (String line : pretty.split("\n")) {
        if (!line.isEmpty()) totalLines++;
      }
      totalLines++; // spacing
    }

    int unscaledHeight = totalLines * unscaledLineHeight;
    int screenHeight = mc.getWindow().getGuiScaledHeight() - 10;
    float scale = unscaledHeight > screenHeight ? (float) screenHeight / unscaledHeight : 1.0f;

    PoseStack pose = graphics.pose();
    pose.pushPose();
    pose.scale(scale, scale, scale);

    int x = 10;
    int y = 10;

    for (Map.Entry<String, String> entry : nbtCache.entrySet()) {
      graphics.drawString(font, entry.getKey(), x, y, 0xFFFFFF, true);
      y += unscaledLineHeight;

      for (String line : entry.getValue().split("\n")) {
        if (!line.isEmpty()) {
          graphics.drawString(font, line, x + 10, y, 0xFFAA00, true);
          y += unscaledLineHeight;
        }
      }

      y += unscaledLineHeight;
    }

    pose.popPose();
  }


  private static float getScale(Minecraft mc) {
    int totalLines = 0;
    for (String pretty : nbtCache.values()) {
      totalLines++;
      for (String line : pretty.split("\n")) {
        if (!line.isEmpty()) {
          totalLines++;
        }
      }
      totalLines++;
    }

    final int lineHeight = 10;
    final int availableHeight = mc.getWindow().getGuiScaledHeight() - 20;

    float scale = 0.8f;
    float requiredHeight = totalLines * lineHeight * scale;
    if (requiredHeight > availableHeight) {
      scale = (float) availableHeight / (totalLines * lineHeight);
    }
    return scale;
  }
}
