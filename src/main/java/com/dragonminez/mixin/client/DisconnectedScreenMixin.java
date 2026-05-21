package com.dragonminez.mixin.client;

import com.dragonminez.client.beta.BetaAccessVerification;
import com.dragonminez.client.gui.BetaAccessVerificationScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
	@Shadow
	@Final
	private Component reason;

	protected DisconnectedScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void dragonminez$addBetaAccessVerificationButton(CallbackInfo info) {
		if (this.minecraft == null || this.reason == null) {
			return;
		}
		if (!BetaAccessVerification.isBetaAccessDisconnect(this.reason.getString())) {
			return;
		}

		String username = this.minecraft.getUser().getName();
		if (!BetaAccessVerification.isValidMinecraftUsername(username)) {
			return;
		}

		int y = Math.min(this.height - 26, this.height / 2 + 74);
		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.dragonminez.beta_access.verify_here"),
						button -> this.minecraft.setScreen(new BetaAccessVerificationScreen(this, username))
				)
				.bounds(this.width / 2 - 100, y, 200, 20)
				.build());
	}
}
