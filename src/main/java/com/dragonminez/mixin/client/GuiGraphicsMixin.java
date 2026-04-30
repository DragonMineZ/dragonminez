package com.dragonminez.mixin.client;

import com.dragonminez.client.gui.tooltip.BedrockCenteringPositionModule;
import com.dragonminez.client.gui.tooltip.PrioritizeTooltipTopPositionModule;
import com.dragonminez.client.gui.tooltip.ScrollTracker;
import com.dragonminez.client.gui.tooltip.TooltipPositionModule;
import com.dragonminez.client.util.TooltipUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Shadow public abstract PoseStack pose();
    @Shadow public abstract int guiWidth();
    @Shadow public abstract int guiHeight();

    @ModifyVariable(method = "renderTooltipInternal", at = @At("HEAD"), argsOnly = true)
    private List<ClientTooltipComponent> dragonminez$wrapTooltip(List<ClientTooltipComponent> components, Font font) {
        List<ClientTooltipComponent> wrapped = TooltipUtil.wrapComponents(components, font, this.guiWidth());
        ScrollTracker.updateTooltip(wrapped);
        return wrapped;
    }

    @Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"))
    private Vector2ic dragonminez$moveTooltip(ClientTooltipPositioner positioner, int screenWidth, int screenHeight, int x, int y, int width, int height) {
        Vector2ic currentPosition = positioner.positionTooltip(screenWidth, screenHeight, x, y, width, height);

        pose().pushPose();

        for (TooltipPositionModule module : List.of(
                new PrioritizeTooltipTopPositionModule(),
                new BedrockCenteringPositionModule()
        )) {
            Optional<Vector2ic> position = module.repositionTooltip(currentPosition.x(), currentPosition.y(), width, height, x, y, this.guiWidth(), this.guiHeight());
            if (position.isPresent()) currentPosition = position.get();
        }

        ScrollTracker.applyScroll((GuiGraphics) (Object) this, currentPosition.x(), currentPosition.y(), width, height, screenWidth, screenHeight);
        return currentPosition;
    }

    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void dragonminez$closeCustomMatrices(Font textRenderer, List<ClientTooltipComponent> tooltip, int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        pose().popPose();
    }
}
