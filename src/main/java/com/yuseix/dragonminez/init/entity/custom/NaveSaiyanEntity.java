package com.yuseix.dragonminez.init.entity.custom;

import com.yuseix.dragonminez.init.MainItems;
import com.yuseix.dragonminez.init.MainSounds;
import com.yuseix.dragonminez.network.C2S.PlanetSelectionC2S;
import com.yuseix.dragonminez.network.C2S.SpacePodC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.utils.Keys;
import com.yuseix.dragonminez.worldgen.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import static com.yuseix.dragonminez.client.hud.spaceship.SaiyanSpacePodOverlay.isKaioAvailable;

public class NaveSaiyanEntity extends Mob implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_OPEN = SynchedEntityData.defineId(NaveSaiyanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation ANIM_ABIERTO = RawAnimation.begin().then("animation.navesaiyan.open", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation ANIM_CERRADO = RawAnimation.begin().then("animation.navesaiyan.close", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private int teleportHoldTime = 0;  // Contador delay
    private static final int teleportTime = 5; // Segundos
    private boolean isTeleporting = false;
    private int teleportCountdown = teleportTime;
    private int planetaObjetivo = 0;  // 0: Overworld, 1: Namek, 2: Kaio

    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public NaveSaiyanEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        //this.setNoAi(true);

        this.entityData.define(IS_OPEN, false);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5F).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) { // Solo ejecuta en el servidor
            if (!isOpen()) { // Abre la nave al interactuar (Ahora esta cerrada)
                if (!player.isPassenger()) {
                    setOpenNave(true);
                };
            } else { // Si la nave está abierta, el jugador se puede montar y cierra la nave
                setOpenNave(false);
                if (!player.isPassenger()) {
                    player.startRiding(this);
                    ModMessages.sendToServer(new PlanetSelectionC2S(0));
                }


            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void travel(Vec3 travelVector) {
        LivingEntity controllingPassenger = this.getControllingPassenger();
        float upward = 0.0f; // Movimiento vertical

        if (controllingPassenger instanceof Player player) {
            this.setNoGravity(true);  // Desactiva la gravedad para volar

            // Movimiento hacia adelante y lateral (según la cámara del jugador)
            float forward = player.zza;  // Movimiento hacia adelante y atrás (W/S)
            float strafe = player.xxa;   // Movimiento lateral (izquierda/derecha - A/D)

            if (Minecraft.getInstance().options.keyJump.isDown()) {
                upward = 0.3f;
            } else if (Keys.DESCEND_KEY.isDown()) {
                upward = -0.3f;
            } else if (!this.onGround()) { //Descenso lento
                upward = -0.00f;
            }

            // Actualiza la selección de planeta y envía al servidor cuando cambian las flechas de dirección
            if (Keys.SELECT_DOWN.consumeClick()) {
                switch (planetaObjetivo) {
                    case 0 -> {
                        planetaObjetivo = 1;
                    }
                    case 1 -> {
                        planetaObjetivo = isKaioAvailable() ? 2 : 0;
                    }
                    case 2 -> {
                        planetaObjetivo = 0;
                    }
                }
                ModMessages.sendToServer(new PlanetSelectionC2S(planetaObjetivo));
                //System.out.println("Planeta objetivo: ABAJO " + planetaObjetivo);
            }

            if (Keys.SELECT_UP.consumeClick()) {
                switch (planetaObjetivo) {
                    case 0 -> {
                        planetaObjetivo = isKaioAvailable() ? 2 : 1;
                    }
                    case 1 -> {
                        planetaObjetivo = 0;
                    }
                    case 2 -> {
                        planetaObjetivo = 1;
                    }
                }
                ModMessages.sendToServer(new PlanetSelectionC2S(planetaObjetivo));
                //System.out.println("Planeta objetivo: ARRIBA " + planetaObjetivo);
            }

            // Al pulsar la tecla, hace "toggle" del teletransporte.
            if (Keys.FUNCTION.consumeClick()) {
                if (isTeleporting) {
                    // Si el teletransporte está activo, se cancela y reinicia el contador
                    isTeleporting = false;
                    teleportCountdown = teleportTime;
                    player.displayClientMessage(Component.translatable("ui.dmz.spacepod.teleport.cancel"), true);
                } else {
                    // Si está desactivado, lo activa :D
                    isTeleporting = true;
                }
            }

            if (isTeleporting) {
                if (teleportCountdown > 0) {
                    // Mostrar cuenta regresiva cada segundo
                    if (player.level().getGameTime() % 20 == 0) { // Cada segundo (20 ticks)
                        player.displayClientMessage(Component.translatable("ui.dmz.spacepod.teleport", teleportCountdown), true);
                        teleportCountdown--;
                    }
                } else {
                    // Teletransportar al jugador cuando el contador llegue a 0
                    switch (planetaObjetivo) {
                        case 0 -> {
                            ModMessages.sendToServer(new SpacePodC2S(Level.OVERWORLD));
                            player.sendSystemMessage(Component.translatable("ui.dmz.spacepod.overworld.arrive"));
                        }
                        case 1 -> {
                            ModMessages.sendToServer(new SpacePodC2S(ModDimensions.NAMEK_DIM_LEVEL_KEY));
                            player.sendSystemMessage(Component.translatable("ui.dmz.spacepod.namek.arrive"));
                        }
                        case 2 -> {
                            if (isKaioAvailable()) {
                                ModMessages.sendToServer(new SpacePodC2S(Level.OVERWORLD));
                                player.sendSystemMessage(Component.literal("Has llegado al planeta de Kaio"));
                            } else {
                                player.sendSystemMessage(Component.literal("Kaio no disponible"));
                            }
                        }
                    }
                    // Reiniciar el estado del teletransporte
                    isTeleporting = false;
                    teleportCountdown = teleportTime;
                }
            } else {
                teleportCountdown = teleportTime; // Reiniciar el contador si no está activo
            }


            Vec3 cameraDirection = player.getLookAngle().normalize();

            cameraDirection = new Vec3(cameraDirection.x, 0, cameraDirection.z).normalize();

            Vec3 forwardMovement = cameraDirection.scale(forward * 0.5);  // Movimiento hacia adelante y atrás

            Vec3 rightDirection = new Vec3(cameraDirection.z, 0, -cameraDirection.x).normalize(); // Dirección derecha
            Vec3 strafeMovement = rightDirection.scale(strafe * 0.5);  // Movimiento lateral

            Vec3 movement = forwardMovement.add(strafeMovement).add(0, upward, 0);

            if (cameraDirection.length() > 0.1f) {  // Solo gira si hay movimiento
                double angle = Math.atan2(cameraDirection.z, cameraDirection.x);  // Calcula el ángulo de rotación
                this.setYRot((float) Math.toDegrees(angle) - 90);  // Ajusta la rotación de la nave (invirtiendo el ángulo)
            }

            // Aplica el movimiento a la nave
            this.setDeltaMovement(movement);
            this.move(MoverType.SELF, movement);  // Aplica el movimiento
            this.hasImpulse = true;

        } else {
            this.setNoGravity(false);
            super.travel(travelVector);
        }
    }
    @Override
    public LivingEntity getControllingPassenger() {
        // Busca entre los pasajeros de la nave al primero que sea una instancia de Player
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof Player) {
                return (LivingEntity) passenger;  // Devuelve el jugador que controla la nave
            }
        }
        return null;  // Devuelve null si no hay ningún jugador controlando la nave
    }

    @Override
    protected void positionRider(Entity pPassenger, MoveFunction pCallback) {
        // Verifica que el pasajero sea un jugador
        if (pPassenger instanceof Player) {
            double offsetX = 0.0;  // Ajuste lateral (déjalo en 0 para evitar desplazamiento lateral indeseado)
            double offsetY = 0.001; // Ajuste vertical menor para que esté ligeramente sobre la nave
            double offsetZ = 0.0;  // Ajuste hacia adelante (opcional)

            // Calcula la posición del asiento con relación a la rotación de la nave
            Vec3 seatPosition = new Vec3(offsetX, offsetY, offsetZ).yRot((float) Math.toRadians(-this.getYRot()));

            // Aplica la posición ajustada al jugador
            pCallback.accept(pPassenger, this.getX() + seatPosition.x, this.getY() + seatPosition.y, this.getZ() + seatPosition.z);
        } else {
            super.positionRider(pPassenger, pCallback);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public boolean isOpen() {
        return this.entityData.get(IS_OPEN);
    }

    public void setOpenNave(boolean open) {
        this.entityData.set(IS_OPEN, open);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.getEntity() instanceof Player) {
            if (!this.level().isClientSide) {
                //Aca poner el item que va a dropear
                this.spawnAtLocation(MainItems.NAVE_SAIYAN_ITEM.get());
                this.remove(RemovalReason.KILLED);
            }
            return true;
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
        if ("drown".equals(pSource.getMsgId()) || "fall".equals(pSource.getMsgId()) || "falling_anvil".equals(pSource.getMsgId()) || "falling_block".equals(pSource.getMsgId())
                || "falling_stalactite".equals(pSource.getMsgId()) || "in_fire".equals(pSource.getMsgId()) || "in_wall".equals(pSource.getMsgId()) || "lava".equals(pSource.getMsgId())
                || "lightning_bolt".equals(pSource.getMsgId()) || "magic".equals(pSource.getMsgId()) || "sonic_boom".equals(pSource.getMsgId()) || "thrown".equals(pSource.getMsgId())) {
            return true;
        }
        return super.isInvulnerableTo(pSource);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        AnimationController<?> controller = tAnimationState.getController();

        if (isOpen()) {
            controller.setAnimation(ANIM_ABIERTO);
        } else {
            controller.setAnimation(ANIM_CERRADO);
        }

        return PlayState.CONTINUE;
    }
}
