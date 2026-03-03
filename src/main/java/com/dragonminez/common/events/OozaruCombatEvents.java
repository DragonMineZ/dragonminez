package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OozaruCombatEvents {

    @SubscribeEvent
    public static void onOozaruAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;
        if (isOozaru(player)) {
            Vec3 center = event.getTarget().position().add(0, event.getTarget().getBbHeight() / 2.0, 0);
            performOozaruAoEAttack(player, (ServerLevel) level, center);

        }
    }

    @SubscribeEvent
    public static void onOozaruHitBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide) return;

        if (player.getAttackStrengthScale(0.5f) > 0.9f && isOozaru(player)) {
            Vec3 center = Vec3.atCenterOf(event.getPos());

            performOozaruAoEAttack(player, (ServerLevel) level, center);

        }
    }

    private static void performOozaruAoEAttack(Player player, ServerLevel level, Vec3 center) {
        double radius = 7.0;

        //level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);

        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.6f);

        float damage = 1;

        AABB searchArea = new AABB(
                center.x - radius, center.y - 3, center.z - radius,
                center.x + radius, center.y + 4, center.z + radius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != player && entity.isAlive() && entity.distanceToSqr(center) <= (radius * radius));

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), damage);

            double d0 = target.getX() - center.x;
            double d1 = target.getZ() - center.z;
            target.knockback(1.5D, -d0, -d1);
        }
    }

    private static boolean isOozaru(Player player) {
        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (statsCap == null) return false;

        var character = statsCap.getCharacter();
        String race = character.getRaceName().toLowerCase();
        String currentForm = character.getActiveForm();
        var activeForm = character.getActiveFormData();

        var raceConfig = ConfigManager.getRaceCharacter(race);
        String raceCustomModel = (raceConfig != null && raceConfig.getCustomModel() != null) ? raceConfig.getCustomModel().toLowerCase() : "";
        String formCustomModel = (character.hasActiveForm() && activeForm != null && activeForm.hasCustomModel())
                ? activeForm.getCustomModel().toLowerCase() : "";

        String logicKey = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
        if (logicKey.isEmpty()) {
            logicKey = race;
        }

        return logicKey.startsWith("oozaru") ||
                (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU)));
    }
}