package com.yuseix.dragonminez.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.blocks.custom.dballs.*;
import com.yuseix.dragonminez.init.items.custom.DragonBallRadarItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Events {
    private static final ResourceLocation fondo = new ResourceLocation(DragonMineZ.MOD_ID,
            "textures/gui/radar.png");
    private static final ResourceLocation boton = new ResourceLocation(DragonMineZ.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");


    private static List<BlockPos> closestDballPositions = new ArrayList<>();
    private static long lastUpdateTime = 0;
    private static final int UPDATE_INTERVAL_TICKS = 20 * 5; // (20 Ticks * Cant Segundos) = Segundos en Minecraft, default 5.

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        GuiGraphics gui = event.getGuiGraphics();
        int radarSize = 140; // Tamaño de la textura del radar 121x146 px

        // Comprobar si el jugador está en el Overworld
        if (!player.level().dimension().equals(Level.OVERWORLD)) {
            return; // No hacer nada si no estamos en el Overworld
        }

        if (player.getMainHandItem().getItem() instanceof DragonBallRadarItem) {
            // Obtener el ItemStack en la mano principal
            ItemStack radarItem = player.getMainHandItem();

            // Obtener el rango del radar desde el NBT
            int radarRange = radarItem.getOrCreateTag().getInt(DragonBallRadarItem.NBT_RANGE);
            if (radarRange == 0) {
                // Establecer un rango por defecto si no hay valor en el NBT
                radarRange = 75; // Valor por defecto
            }

            // Dibujar el radar en la esquina de la pantalla
            int centerX = mc.getWindow().getGuiScaledWidth() - radarSize - 10;
            int centerY = mc.getWindow().getGuiScaledHeight() - radarSize - 10;
            gui.blit(fondo, centerX, centerY, 0, 0, 121, 146); // Dibujar el fondo del radar

            // Actualizar solo cada 2 segundos (40 ticks)
            long currentTime = player.level().getGameTime();
            if (currentTime - lastUpdateTime > UPDATE_INTERVAL_TICKS) {
                closestDballPositions = findAllDballBlocks(player, radarRange); // Detectar todas las esferas
                lastUpdateTime = currentTime; // Actualizar el tiempo de la última búsqueda
            }

            // Dibujar los puntos amarillos para cada posición detectada
            for (BlockPos pos : closestDballPositions) {
                // Calcular la distancia y dirección hacia cada bloque
                double distance = Math.sqrt(player.blockPosition().distSqr(pos));

                // Calcular el ángulo entre el jugador y el bloque
                double angleToBlock = Math.atan2(pos.getZ() - player.getZ(), pos.getX() - player.getX());

                // Obtener la rotación del jugador (yaw) y ajustarla al ángulo del bloque
                double playerYaw = Math.toRadians(player.getYRot()); // Convertir el yaw del jugador a radianes
                double adjustedAngle = angleToBlock - playerYaw; // Restar el yaw nuevamente

                // Convertir la distancia a un valor entre 0 y 50 píxeles
                double radarRadius = Math.min(distance / radarRange * 50, 50);

                // Calcular la posición del punto amarillo en el radar, invirtiendo los ejes X e Y
                int radarX = (int) (centerX + 61 - radarRadius * Math.cos(adjustedAngle)); // Invertimos X
                int radarY = (int) (centerY + 87 - radarRadius * Math.sin(adjustedAngle)); // Invertimos Y

                // Dibujar el punto amarillo
                gui.blit(fondo, radarX - 2, radarY - 2, 121, 0, 6, 6); // Tamaño del punto: 6x6 px
            }
        }
    }

    private static List<BlockPos> findAllDballBlocks(Player player, int range) {
        Level world = player.level();
        BlockPos playerPos = player.blockPosition();
        List<BlockPos> dballPositions = new ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                BlockPos pos = playerPos.offset(x, 0, z); // Ignoramos Y para hacer la búsqueda en plano horizontal

                // Comprobar si es un bloque Dball de 1 a 7
                Block block = world.getBlockState(pos).getBlock();
                if (block instanceof Dball1Block || block instanceof Dball2Block || block instanceof Dball3Block
                        || block instanceof Dball4Block || block instanceof Dball5Block
                        || block instanceof Dball6Block || block instanceof Dball7Block) {
                    dballPositions.add(pos); // Agregar la posición del bloque detectado
                }
            }
        }

        return dballPositions; // Devolver todas las posiciones encontradas
    }

}