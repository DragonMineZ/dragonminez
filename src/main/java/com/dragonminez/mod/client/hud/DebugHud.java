package com.dragonminez.mod.client.hud;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.player.capability.CapManagerRegistry;
import com.dragonminez.core.common.util.JavaUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

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
    if (FMLEnvironment.production) {
      return;
    }
    renderDebugInformation(event.getGuiGraphics());
  }

  /**
   * Renders the debug information on the screen. Updates the cache every tick to avoid unnecessary
   * recomputation.
   */
  private static void renderDebugInformation(GuiGraphics graphics) {
    final Minecraft mc = Minecraft.getInstance();
    final LocalPlayer player = mc.player;
    if (player == null || mc.options.hideGui) {
      return;
    }

    final int currentTick = player.tickCount;
    if (currentTick != lastUpdateTick) {
      lastUpdateTick = currentTick;
      nbtCache.clear();

      CapManagerRegistry.INSTANCE.values(Dist.CLIENT)
          .forEach((manager) ->
              manager.retrieveData(player, cap -> {
                final Tag tag = cap.serialize(new CompoundTag());
                final String prettyData = prettyPrintNBT(tag);
                nbtCache.put(JavaUtil.toLegible(cap.holder().identifier().getPath()), prettyData);
              }));
    }

    final Font font = mc.font;
    final PoseStack pose = graphics.pose();

    pose.pushPose();
    pose.scale(0.8f, 0.8f, 0.8f);

    final int labelX = 10;
    final AtomicInteger y = new AtomicInteger(50);

    nbtCache.forEach((label, pretty) -> {
      graphics.drawString(font, label, labelX, y.get(), 0xffffff, true);
      y.addAndGet(10);
      for (String line : pretty.split("\n")) {
        if (!line.isEmpty()) {
          graphics.drawString(font, line, labelX + 10, y.get(), 0xffaa00, true);
          y.addAndGet(10);
        }
      }
      y.addAndGet(10);
    });

    pose.popPose();
  }

  /**
   * Recursively pretty-prints the NBT tag content into a human-readable string. Uses indentation to
   * represent nested compound and list tags.
   *
   * @param tag the NBT tag to serialize as string
   * @return a pretty-printed multiline string representation of the tag
   */
  private static String prettyPrintNBT(Tag tag) {
    StringBuilder sb = new StringBuilder();
    tag.accept(new StringTagVisitor() {
      private int indent = 0;

      private void appendIndent() {
        sb.append("  ".repeat(indent));
      }

      @Override
      public void visitString(@NotNull StringTag stringTag) {
        appendIndent();
        sb.append(stringTag).append("\n");
      }

      @Override
      public void visitList(@NotNull ListTag listTag) {
        appendIndent();
        sb.append("List[").append(listTag.size()).append("]:\n");
        indent++;
        for (Tag element : listTag) {
          element.accept(this);
        }
        indent--;
      }

      @Override
      public void visitCompound(@NotNull CompoundTag compoundTag) {
        appendIndent();
        indent++;
        sb.append("\n");
        for (String key : compoundTag.getAllKeys()) {
          Tag value = compoundTag.get(key);
          appendIndent();
          sb.append(key).append(": ");
          if (value instanceof CompoundTag || value instanceof ListTag) {
            sb.append("\n");
            value.accept(this);
          } else {
            sb.append(value).append("\n");
          }
        }
        indent--;
        appendIndent();
        sb.append("\n");
      }

      @Override
      public void visitEnd(@NotNull EndTag endTag) {
        appendIndent();
        sb.append("END\n");
      }
    });
    return sb.toString();
  }
}
