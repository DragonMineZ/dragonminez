package com.yuseix.dragonminez.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.blocks.custom.dballs.*;
import com.yuseix.dragonminez.init.items.custom.DragonBallRadarItem;
import com.yuseix.dragonminez.init.items.custom.NamekDragonBallRadarItem;
import com.yuseix.dragonminez.worldgen.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,  value = Dist.CLIENT)
public class RadarEvents {
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
        if (mc.isPaused()) return;

        Player player = mc.player;
        GuiGraphics gui = event.getGuiGraphics();
        int radarSize = 140; // Tamaño de la textura del radar 121x146 px

        // Comprobar si el jugador está en el Overworld
        if (player.level().dimension().equals(Level.OVERWORLD)) {
            // Si estamos en el Overworld, se comprueba si el jugador tiene el radar en la mano
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

                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.setShaderTexture(0, fondo);
                gui.blit(fondo, centerX, centerY, 0, 0, 121, 146); // Dibujar el fondo del radar

                // Actualizar solo cada 2 segundos (40 ticks)
                long currentTime = player.level().getGameTime();
                if (currentTime - lastUpdateTime > UPDATE_INTERVAL_TICKS) {
                    closestDballPositions = findAllDballBlocks(player, radarRange); // Detectar todas las esferas
                    lastUpdateTime = currentTime; // Actualizar el tiempo de la última búsqueda
                }

                // Dibujar los puntos amarillos para cada posición detectada
                for (BlockPos pos : closestDballPositions) {

                    // Calculate the 2D distance (ignoring Y)
                    double dx = pos.getX() - player.getX();
                    double dz = pos.getZ() - player.getZ();
                    double distance = Math.sqrt(dx * dx + dz * dz); // 2D distance

                    // Calculate the angle to the block
                    double angleToBlock = Math.atan2(dz, dx);

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
        } else if (player.level().dimension().equals(ModDimensions.NAMEK_DIM_LEVEL_KEY)) {
            // Si estamos en el Overworld, se comprueba si el jugador tiene el radar en la mano
            if (player.getMainHandItem().getItem() instanceof NamekDragonBallRadarItem) {
                // Obtener el ItemStack en la mano principal
                ItemStack radarItem = player.getMainHandItem();

                // Obtener el rango del radar desde el NBT
                int radarRange = radarItem.getOrCreateTag().getInt(NamekDragonBallRadarItem.NBT_RANGE);
                if (radarRange == 0) {
                    // Establecer un rango por defecto si no hay valor en el NBT
                    radarRange = 75; // Valor por defecto
                }

                // Dibujar el radar en la esquina de la pantalla
                int centerX = mc.getWindow().getGuiScaledWidth() - radarSize - 10;
                int centerY = mc.getWindow().getGuiScaledHeight() - radarSize - 10;

                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.setShaderTexture(0, fondo);
                gui.blit(fondo, centerX, centerY, 0, 0, 121, 146); // Dibujar el fondo del radar

                // Actualizar solo cada 2 segundos (40 ticks)
                long currentTime = player.level().getGameTime();
                if (currentTime - lastUpdateTime > UPDATE_INTERVAL_TICKS) {
                    closestDballPositions = findAllNamekDballBlocks(player, radarRange); // Detectar todas las esferas
                    lastUpdateTime = currentTime; // Actualizar el tiempo de la última búsqueda
                }

                // Dibujar los puntos amarillos para cada posición detectada
                for (BlockPos pos : closestDballPositions) {
                    // Calculate the 2D distance (ignoring Y)
                    double dx = pos.getX() - player.getX();
                    double dz = pos.getZ() - player.getZ();
                    double distance = Math.sqrt(dx * dx + dz * dz); // 2D distance

                    // Calculate the angle to the block
                    double angleToBlock = Math.atan2(dz, dx);

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
    }

    private static List<BlockPos> findAllDballBlocks(Player player, int range) {
        Level world = player.level();
        BlockPos playerPos = player.blockPosition();
        List<BlockPos> dballPositions = new ArrayList<>();
        int verticalRange = 363; // Rango en el eje Y (±363 bloques)

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -verticalRange; y <= verticalRange; y++) { // Búsqueda en el rango vertical
                    BlockPos pos = playerPos.offset(x, y, z); // Incluir Y en la búsqueda

                    // Comprobar si es un bloque Dball de 1 a 7
                    Block block = world.getBlockState(pos).getBlock();
                    if (block instanceof Dball1Block || block instanceof Dball2Block || block instanceof Dball3Block
                            || block instanceof Dball4Block || block instanceof Dball5Block
                            || block instanceof Dball6Block || block instanceof Dball7Block) {
                        dballPositions.add(pos); // Agregar la posición del bloque detectado
                    }
                }
            }
        }

        return dballPositions; // Devolver todas las posiciones encontradas
    }

    private static List<BlockPos> findAllNamekDballBlocks(Player player, int range) {
        Level world = player.level();
        BlockPos playerPos = player.blockPosition();
        List<BlockPos> dballPositions = new ArrayList<>();
        int verticalRange = 363; // Rango en el eje Y (±363 bloques)

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -verticalRange; y <= verticalRange; y++) { // Búsqueda en el rango vertical
                    BlockPos pos = playerPos.offset(x, y, z); // Incluir Y en la búsqueda

                    // Comprobar si es un bloque Dball de 1 a 7
                    Block block = world.getBlockState(pos).getBlock();
                    if (block instanceof Dball1NamekBlock || block instanceof Dball2NamekBlock || block instanceof Dball3NamekBlock
                            || block instanceof Dball4NamekBlock || block instanceof Dball5NamekBlock
                            || block instanceof Dball6NamekBlock || block instanceof Dball7NamekBlock) {
                        dballPositions.add(pos); // Agregar la posición del bloque detectado
                    }
                }
            }
        }

        return dballPositions; // Devolver todas las posiciones encontradas
    }
}