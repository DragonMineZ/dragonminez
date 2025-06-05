package com.dragonminez.mod.mixin.common.geckolib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import software.bernie.example.GeckoLibMod;

@Mixin(value = GeckoLibMod.class, remap = false)
public class MixinPreventGeckolibExamplesRegistration {

    @Redirect(method = "shouldRegisterExamples()Z", at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;getBoolean(Ljava/lang/String;)Z"), remap = false)
    private static boolean cancelExampleRegistry(String name) {
        return true;
    }
}