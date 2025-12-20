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

            if (entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
            }
            if (entity.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
                entity.setHealth((float) health);
            }

            LogUtil.info(Env.SERVER, "Saibaman Spawneado. HardMode: " + hardMode + " | Vida: " + entity.getHealth() + "/" + entity.getMaxHealth());
        }
    }
    private void spawnRaditz(ServerPlayer player, boolean hardMode) {
        var entity = MainEntities.SAGA_RADITZ.get().create(player.level());

        if (entity != null) {
            entity.setPos(player.getX() + 2, player.getY(), player.getZ());
            //PONER STATS IMPORTANTEEEE (por cierto los saibamans no tienen ni pincho xxdd solo explotan)
            entity.setFlySpeed(0.45); //recomendable
            entity.setKiBlastDamage(20.0f); //Si pones 20, son 10 corazones.
            entity.setKiBlastSpeed(0.6f); //Te recomiendo no poner más de 1.0f es demasiado rapido xdxxd
            //Esto de aca es por si la entidad es ozaru, si no lo es no pongas esto :v
            entity.setRoarDamage(100.0); //Aca es igual, si es 100 son 50 corazones y asi. este es el rugido del ozaru
            entity.setRoarRange(15.0); //este es el rango del rugido al impactar

            //Ahora spawnea la entidad
            player.level().addFreshEntity(entity);

            //Como la entidad ya esta en el mundo se comprueba si esta en hard y se cambia vida y damage
            double health = hardMode ? 100.0 : 20.0;
            double damage = hardMode ? 10.0 : 3.0;

            if (entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
            }
            if (entity.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
                entity.setHealth((float) health);
            }

        }
    }

}
