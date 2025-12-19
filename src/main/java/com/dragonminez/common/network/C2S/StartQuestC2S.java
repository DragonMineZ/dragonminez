package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.quest.*;
import com.dragonminez.common.stats.StatsCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

public class StartQuestC2S {
    private final String sagaId;
    private final int questId;
    private final boolean isHardMode;

    public StartQuestC2S(String sagaId, int questId, boolean isHardMode) {
        this.sagaId = sagaId;
        this.questId = questId;
        this.isHardMode = isHardMode;
    }

    public StartQuestC2S(FriendlyByteBuf buffer) {
        this.sagaId = buffer.readUtf();
        this.questId = buffer.readInt();
        this.isHardMode = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(sagaId);
        buffer.writeInt(questId);
        buffer.writeBoolean(isHardMode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(StatsCapability.INSTANCE).ifPresent(stats -> {
                    QuestData questData = stats.getQuestData();

                    if (!questData.isSagaUnlocked(sagaId)) {
                        return;
                    }
                    spawnSaibaman(player, isHardMode);
                });
            }
        });
        return true;
    }

    private void spawnSaibaman(ServerPlayer player, boolean hardMode) {
        // 1. Creamos la entidad
        var entity = MainEntities.SAGA_SAIBAMAN.get().create(player.level());

        if (entity != null) {
            // 2. Posicionamos
            entity.setPos(player.getX() + 2, player.getY(), player.getZ());

            // 3. ¡IMPORTANTE! Primero añadimos la entidad al mundo con sus stats base
            // Esto asegura que la entidad exista y esté "trackeada" por el servidor y el cliente.
            player.level().addFreshEntity(entity);

            // 4. AHORA aplicamos los cambios.
            // Al estar ya en el mundo, estos cambios envían paquetes de actualización instantáneos al cliente.

            double health = hardMode ? 100.0 : 20.0;
            double damage = hardMode ? 10.0 : 3.0;

            // Modificar Daño
            if (entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
            }

            // Modificar Vida
            if (entity.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                // Primero subimos el techo máximo
                entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
                // Luego rellenamos la vida hasta ese nuevo techo
                entity.setHealth((float) health);
            }

            // Debug: Confirma en consola que el servidor tiene el valor correcto
            LogUtil.info(Env.SERVER, "Saibaman Spawneado. HardMode: " + hardMode + " | Vida: " + entity.getHealth() + "/" + entity.getMaxHealth());
        }
    }
}
