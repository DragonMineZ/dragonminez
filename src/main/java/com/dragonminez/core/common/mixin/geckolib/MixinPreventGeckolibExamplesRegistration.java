package com.dragonminez.core.common.mixin.geckolib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import software.bernie.example.GeckoLibMod;

/**
 * MixinPreventGeckolibExamplesRegistration
 * <p>
 * GeckoLib, by default, includes example content (e.g., test entities and animations) which are conditionally
 * registered based on a system property (`-Dgeckolib.register_examples=true`). This registration occurs in
 * {@link GeckoLibMod#shouldRegisterExamples()}, which checks that property.
 *
 * <p><strong>Why this exists:</strong>
 * In development environments, GeckoLib’s example content may be loaded unintentionally if the system property
 * is misconfigured or absent. Worse yet, in a production environment, the mere *existence* of this example code
 * (especially if referenced or loaded) causes compatibility issues: production clients cannot connect to dev
 * servers due to mismatches in registered content (e.g., missing entities, animations).
 *
 * <p>This Mixin forcibly overrides the example registration logic to always return `false`, ensuring that
 * GeckoLib’s internal test/demo content is never registered — regardless of environment or system properties.
 * This prevents unintended content from affecting networking, world state, or compatibility.
 *
 * <p><strong>Note:</strong>
 * - This Mixin uses {@code remap = false} because GeckoLib is an external library without Mojang mappings.
 * - This fix is safe and stable as it does not interfere with actual functionality — only internal testing hooks.
 */
@Mixin(value = GeckoLibMod.class, remap = false)
public class MixinPreventGeckolibExamplesRegistration {

    @Redirect(method = "shouldRegisterExamples()Z", at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;getBoolean(Ljava/lang/String;)Z"), remap = false)
    private static boolean cancelExampleRegistry(String name) {
        return true;
    }
}