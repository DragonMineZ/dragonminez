package com.dragonminez.mod.mixin.common;

import com.dragonminez.mod.core.common.player.capability.CapManagerRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinInjectCapDataOnEntity
 * <p>
 * This mixin injects into {@link Entity#saveWithoutId(CompoundTag)} and
 * {@link Entity#load(CompoundTag)} to directly handle the serialization and deserialization of
 * custom player capability data.
 *
 * <p><strong>Why this exists:</strong>
 * The default Forge capability persistence system is rigid, verbose, and too entangled with Forge's
 * internal lifecycle hooks (AttachCapabilitiesEvent, RegisterCapabilitiesEvent, etc). When
 * designing a clean and centralized capability management layer ({@link CapManagerRegistry}), it
 * becomes unreasonably complex and error-prone to rely on Forge’s event-based saving and loading if
 * you're aiming for clarity, scalability, and less boilerplate.
 *
 * <p>Instead of cluttering the codebase with scattered capability save/load handlers or relying on
 * Forge's clunky NBT tagging via capability providers, this mixin enables precise control over
 * where and how player data is stored — directly within the core `Entity` save/load flow.
 *
 * <p><strong>Scope:</strong>
 * This is intentionally limited to `Player` entities to avoid unintended side effects on other
 * entities. All capability managers registered in {@link CapManagerRegistry} are automatically
 * serialized and deserialized with the player's main NBT, keeping data logically grouped and
 * reducing redundancy.
 *
 * <p><strong>Note:</strong>
 * This is a workaround by necessity — not due to ignorance of Forge’s systems, but due to a
 * conscious choice to preserve code maintainability and flexibility in a large, modular
 * capability-based mod architecture.
 */
@Mixin(Entity.class)
public class MixinInjectCapDataOnEntity {

  @Inject(method = "saveWithoutId(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"))
  public void injectData(CompoundTag pCompound, CallbackInfoReturnable<CompoundTag> cir) {
    final Entity thisEntity = (Entity) (Object) this;
    if (!(thisEntity instanceof Player player)) {
      return;
    }
    CapManagerRegistry.INSTANCE.values()
        .forEach((manager) ->
            manager.retrieveData(player)
                .serialize(pCompound));
  }

  @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;readAdditional"
          + "SaveData(Lnet/minecraft/nbt/CompoundTag;)V"))
  public void readData(CompoundTag pCompound, CallbackInfo ci) {
    final Entity thisEntity = (Entity) (Object) this;
    if (!(thisEntity instanceof Player player)) {
      return;
    }
    CapManagerRegistry.INSTANCE.values()
        .forEach((manager) ->
            manager.retrieveData(player)
                .deserialize(pCompound, true));
  }
}
