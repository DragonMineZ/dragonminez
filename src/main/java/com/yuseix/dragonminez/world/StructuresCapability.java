package com.yuseix.dragonminez.world;

import com.mojang.logging.LogUtils;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.MainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

import java.util.List;
import java.util.Random;

public class StructuresCapability {
    private static final Logger LOGGER = LogUtils.getLogger();

    private boolean hasTorreKamisama = false;
    private boolean hasHabTiempo = false;
    private boolean hasGokuHouse = false;
    private boolean hasRoshiHouse = false;
    private boolean hasElderGuru = false;
    private boolean hasEnmaPalace = false;
    private boolean hasKaioPlanet = false;

    private BlockPos torreKamisamaPosition;
    private BlockPos portalHabTiempoPosition;
    private BlockPos torreKarinPosition;
    private BlockPos gokuHousePosition;
    private BlockPos habTiempoPos;
    private BlockPos db4Position;
    private BlockPos roshiHousePosition;
    private BlockPos elderGuruPosition;
    private BlockPos namekDB4Position;
    private BlockPos enmaPalacePosition;
    private BlockPos kaioPlanetPosition;

    public void setHasTorreKamisama(boolean hasTorreKamisama) {
        this.hasTorreKamisama = hasTorreKamisama;
    }

    public void setHasHabTiempo(boolean hasHabTiempo) {
        this.hasHabTiempo = hasHabTiempo;
    }

    public void setHasGokuHouse(boolean hasGokuHouse) {
        this.hasGokuHouse = hasGokuHouse;
    }

    public void setHabTiempoPos(BlockPos habTiempoPos) {
        this.habTiempoPos = habTiempoPos;
    }

    public void setHasRoshiHouse(boolean hasRoshiHouse) {
        this.hasRoshiHouse = hasRoshiHouse;
    }

    public void setHasElderGuru(boolean hasElderGuru) {
        this.hasElderGuru = hasElderGuru;
    }

    public void setHasEnmaPalace(boolean hasEnmaPalace) {
        this.hasEnmaPalace = hasEnmaPalace;
    }

    public void setHasKaioPlanet(boolean hasKaioPlanet) {
        this.hasKaioPlanet = hasKaioPlanet;
    }

    public boolean getHasTorreKami() {
        return this.hasTorreKamisama;
    }

    public boolean getHasGokuHouse() {
        return this.hasGokuHouse;
    }

    public boolean getHasRoshiHouse() {
        return this.hasRoshiHouse;
    }

    public boolean getHasElderGuru() {
        return this.hasElderGuru;
    }

    public boolean getHasKaioPlanet() {
        return this.hasKaioPlanet;
    }

    public void setTorreKamisamaPosition(BlockPos torreKamisamaPosition) {
        this.torreKamisamaPosition = torreKamisamaPosition;
    }

    private void setPortalHabTiempoPosition(BlockPos portalHabTiempoPosition) {
        this.portalHabTiempoPosition = portalHabTiempoPosition;
    }

    public void setTorreKarinPosition(BlockPos torreKarinPosition) {
        this.torreKarinPosition = torreKarinPosition;
    }

    public void setGokuHousePosition(BlockPos gokuHousePosition) {
        this.gokuHousePosition = gokuHousePosition;
    }

    public void setDB4Position(BlockPos db4Position) {
        this.db4Position = db4Position;
    }

    public void setRoshiHousePosition(BlockPos roshiHousePosition) {
        this.roshiHousePosition = roshiHousePosition;
    }

    public void setElderGuruPosition(BlockPos elderGuruPosition) {
        this.elderGuruPosition = elderGuruPosition;
    }

    public void setNamekDB4Position(BlockPos namekDB4Position) {
        this.namekDB4Position = namekDB4Position;
    }

    public void setEnmaPalacePosition(BlockPos enmaPalacePosition) {
        this.enmaPalacePosition = enmaPalacePosition;
    }

    public void setKaioPlanetPosition(BlockPos kaioPlanetPosition) {
        this.kaioPlanetPosition = kaioPlanetPosition;
    }

    public BlockPos getHabTiempoPos() {
        return habTiempoPos;
    }

    public BlockPos getTorreKamisamaPosition() {
        return torreKamisamaPosition;
    }

    public BlockPos getPortalHabTiempoPosition() {
        return portalHabTiempoPosition;
    }

    public BlockPos getTorreKarinPosition() {
        return torreKarinPosition;
    }

    public BlockPos getGokuHousePosition() {
        return gokuHousePosition;
    }

    public BlockPos getDB4Position() {
        return db4Position;
    }

    public BlockPos getRoshiHousePosition() {
        return roshiHousePosition;
    }

    public BlockPos getElderGuruPosition() {
        return elderGuruPosition;
    }

    public BlockPos getNamekDB4Position() {
        return namekDB4Position;
    }

    public BlockPos getEnmaPalacePosition() {
        return enmaPalacePosition;
    }

    public BlockPos getKaioPlanetPosition() {
        return kaioPlanetPosition;
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean("hasTorreKamisama", hasTorreKamisama);
        nbt.putBoolean("hasHabTiempo", hasHabTiempo);
        nbt.putBoolean("hasGokuHouse", hasGokuHouse);
        nbt.putBoolean("hasRoshiHouse", hasRoshiHouse);
        nbt.putBoolean("hasElderGuru", hasElderGuru);
        nbt.putBoolean("hasEnmaPalace", hasEnmaPalace);
        nbt.putBoolean("hasKaioPlanet", hasKaioPlanet);

        if (torreKamisamaPosition != null || torreKarinPosition != null || portalHabTiempoPosition != null) {
            nbt.put("torreKamisamaPosition", NbtUtils.writeBlockPos(torreKamisamaPosition));
            nbt.put("torreKarinPosition", NbtUtils.writeBlockPos(torreKarinPosition));
            nbt.put("portalHabPosition", NbtUtils.writeBlockPos(portalHabTiempoPosition));
        }
        if (habTiempoPos != null) {
            nbt.put("habTiempoPosition", NbtUtils.writeBlockPos(habTiempoPos));
        }
        if (gokuHousePosition != null) {
            nbt.put("gokuHousePosition", NbtUtils.writeBlockPos(gokuHousePosition));
        }
        if (db4Position != null) {
            nbt.put("db4Position", NbtUtils.writeBlockPos(db4Position));
        }
        if (roshiHousePosition != null) {
            nbt.put("roshiHousePosition", NbtUtils.writeBlockPos(roshiHousePosition));
        }
        if (elderGuruPosition != null) {
            nbt.put("elderGuruPosition", NbtUtils.writeBlockPos(elderGuruPosition));
        }
        if (namekDB4Position != null) {
            nbt.put("namekDB4Position", NbtUtils.writeBlockPos(namekDB4Position));
        }
        if (enmaPalacePosition != null) {
            nbt.put("enmaPalacePosition", NbtUtils.writeBlockPos(enmaPalacePosition));
        }
        if (kaioPlanetPosition != null) {
            nbt.put("kaioPlanetPosition", NbtUtils.writeBlockPos(kaioPlanetPosition));
        }
    }

    public void loadNBTData(CompoundTag nbt) {
        hasTorreKamisama = nbt.getBoolean("hasTorreKamisama");
        hasHabTiempo = nbt.getBoolean("hasHabTiempo");
        hasGokuHouse = nbt.getBoolean("hasGokuHouse");
        hasRoshiHouse = nbt.getBoolean("hasRoshiHouse");
        hasElderGuru = nbt.getBoolean("hasElderGuru");
        hasEnmaPalace = nbt.getBoolean("hasEnmaPalace");
        hasKaioPlanet = nbt.getBoolean("hasKaioPlanet");

        if (nbt.contains("torreKamisamaPosition") || nbt.contains("torreKarinPosition") || nbt.contains("portalHabPosition")) {
            torreKamisamaPosition = NbtUtils.readBlockPos(nbt.getCompound("torreKamisamaPosition"));
            torreKarinPosition = NbtUtils.readBlockPos(nbt.getCompound("torreKarinPosition"));
            portalHabTiempoPosition = NbtUtils.readBlockPos(nbt.getCompound("portalHabPosition"));
        }
        if (nbt.contains("habTiempoPosition")) {
            habTiempoPos = NbtUtils.readBlockPos(nbt.getCompound("habTiempoPosition"));
        }
        if (nbt.contains("gokuHousePosition")) {
            gokuHousePosition = NbtUtils.readBlockPos(nbt.getCompound("gokuHousePosition"));
        }
        if (nbt.contains("db4Position")) {
            db4Position = NbtUtils.readBlockPos(nbt.getCompound("db4Position"));
        }
        if (nbt.contains("roshiHousePosition")) {
            roshiHousePosition = NbtUtils.readBlockPos(nbt.getCompound("roshiHousePosition"));
        }
        if (nbt.contains("elderGuruPosition")) {
            elderGuruPosition = NbtUtils.readBlockPos(nbt.getCompound("elderGuruPosition"));
        }
        if (nbt.contains("namekDB4Position")) {
            namekDB4Position = NbtUtils.readBlockPos(nbt.getCompound("namekDB4Position"));
        }
        if (nbt.contains("enmaPalacePosition")) {
            enmaPalacePosition = NbtUtils.readBlockPos(nbt.getCompound("enmaPalacePosition"));
        }
        if (nbt.contains("kaioPlanetPosition")) {
            kaioPlanetPosition = NbtUtils.readBlockPos(nbt.getCompound("kaioPlanetPosition"));
        }
    }

    public void generateKamisamaStructure(ServerLevel level) {
        if (!hasTorreKamisama) {
            Random random = new Random();
            BlockPos spawnPos = level.getSharedSpawnPos();

            int intentos = 0; // Contador para testear xd
            BlockPos posicionValida = new BlockPos(0, 0, 0);

            while (posicionValida.equals(new BlockPos(0, 0, 0))) {
                // Generar posición aleatoria dentro de un rango de 1200 bloques a cada lado desde el spawn
                int x = spawnPos.getX() + random.nextInt(2400) - 1200;
                int z = spawnPos.getZ() + random.nextInt(2400) - 1200;

                level.getChunk(x >> 4, z >> 4); // Carga el chunk

                // Obtener altura del terreno en esa posición
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                // Validar que la altura sea menor a 90 para q no se corte la estructura
                if (y <= 90) {
                    BlockPos posiblePos = new BlockPos(x, y, z);
                    BlockState belowBlockState = level.getBlockState(posiblePos.below());
                    BlockState belowBelowBlockState = level.getBlockState(posiblePos.below().below());

                    // Validar que la posición sea sólida
                    if (!belowBlockState.isAir() && !(belowBlockState.is(Blocks.WATER))
                            && !belowBelowBlockState.isAir() && !(belowBelowBlockState.is(Blocks.WATER))) {
                        posicionValida = posiblePos;
                        //System.out.println("Posición válida encontrada: " + posicionValida);
                        //System.out.println("/teleport Dev " + x + " " + y + " " + z);
                    }
                } else {
                    //System.out.println("No se encontró posición válida, reintentando...");
                }

                intentos++;
            }

            if (!posicionValida.equals(new BlockPos(0, 0, 0))) {
                // Obtener bloques anteriores
                BlockState blockState = level.getBlockState(posicionValida.below()).getBlock().defaultBlockState();
                BlockState redstoneBlockState = level.getBlockState(posicionValida.below().offset(1, 0, 0)).getBlock().defaultBlockState();
                BlockState belowBlockState = level.getBlockState(posicionValida.below().below()).getBlock().defaultBlockState();
                BlockState belowRedstoneBlockState = level.getBlockState(posicionValida.below().below().offset(1, 0, 0)).getBlock().defaultBlockState();

                // Generar StructureBlocks
                BlockState structureBlock = Blocks.STRUCTURE_BLOCK.defaultBlockState();
                BlockState redstoneBlockAdyacente = Blocks.REDSTONE_BLOCK.defaultBlockState();

                // Colocar primer StructureBlock
                level.setBlock(posicionValida.below(), structureBlock, 3);
                // Obtener el BlockEntity
                BlockEntity blockEntity = level.getBlockEntity(posicionValida.below());
                if (blockEntity instanceof StructureBlockEntity) {
                    StructureBlockEntity structureBlockEntity = (StructureBlockEntity) blockEntity;

                    // Crear y colocar el NBT
                    CompoundTag nbtData = new CompoundTag();
                    nbtData.putString("mirror", "NONE");
                    nbtData.putString("rotation", "NORTH");
                    nbtData.putInt("posX", -1);
                    nbtData.putInt("posY", 1);
                    nbtData.putInt("posZ", -1);
                    nbtData.putString("mode", "LOAD");
                    nbtData.putString("name", "dragonminez:kamisama/kami_bottom");


                    structureBlockEntity.load(nbtData);
                    structureBlockEntity.setChanged();
                    //System.out.println("StructureBlock 1 cargado en " + posicionValida.below() + " con NBT: " + nbtData);
                    //System.out.println("Comando: /setblock " + posicionValida.below().getX() + " " + posicionValida.below().getY() + " " + posicionValida.below().getZ() + " minecraft:structure_block" + nbtData);

                    // Colocar la Redstone que active el StructureBlock
                    level.setBlock(posicionValida.below().offset(1, 0, 0), redstoneBlockAdyacente, 3);
                }

                BlockPos secPos = posicionValida.offset(-3, 0, -3);

                level.setBlock(secPos.below().below(), structureBlock, 3);
                BlockEntity blockEntityBelow = level.getBlockEntity(secPos.below().below());
                if (blockEntityBelow instanceof StructureBlockEntity) {
                    StructureBlockEntity structureBlockEntity = (StructureBlockEntity) blockEntityBelow;

                    CompoundTag nbtData = new CompoundTag();
                    nbtData.putString("mirror", "NONE");
                    nbtData.putString("rotation", "NORTH");
                    nbtData.putInt("posX", -51);
                    nbtData.putInt("posY", 76);
                    nbtData.putInt("posZ", -51);
                    nbtData.putString("mode", "LOAD");
                    nbtData.putString("name", "dragonminez:kamisama/kami_top");

                    structureBlockEntity.load(nbtData);
                    structureBlockEntity.setChanged();
                    //System.out.println("StructureBlock 2 cargado en " + secPos.below().below() + " con NBT: " + nbtData);
                    //System.out.println("Comando: /setblock " + secPos.below().below().getX() + " " + secPos.below().below().getY() + " " + secPos.below().below().getZ() + " minecraft:structure_block" + nbtData);

                    level.setBlock(secPos.below().below().offset(1, 0, 0), redstoneBlockAdyacente, 3);
                }

                level.setBlock(posicionValida.below(), blockState, 3);
                level.setBlock(posicionValida.below().offset(1, 0, 0), redstoneBlockState, 3);
                level.setBlock(secPos.below().below(), belowBlockState, 3);
                level.setBlock(secPos.below().below().offset(1, 0, 0), belowRedstoneBlockState, 3);
            }

            BlockPos torreKami = new BlockPos(posicionValida.getX(), posicionValida.getY() + 152, posicionValida.getZ() + 50);
            BlockPos portalHab = new BlockPos(posicionValida.getX(), posicionValida.getY() + 153, posicionValida.getZ() - 27);
            BlockPos torreKarin = new BlockPos(posicionValida.getX() - 6, posicionValida.getY() + 56, posicionValida.getZ() - 4);
            setHasTorreKamisama(true);
            setTorreKamisamaPosition(torreKami);
            setTorreKarinPosition(torreKarin);
            setPortalHabTiempoPosition(portalHab);
			LOGGER.info("[DMZ-Generation] Kami's Tower generated in {}", torreKami);
            LOGGER.info("[DMZ-Generation] Kami's Lookout generated in {}", torreKarin);
        }
    }

    public void generateGokuHouseStructure(ServerLevel level) {
        if (!hasGokuHouse) {
            Random random = new Random();
            BlockPos spawnPos = level.getSharedSpawnPos();

            BlockPos posicionValida = new BlockPos(0, 0, 0);

            while(posicionValida.equals(new BlockPos(0, 0, 0))) {
                int x = spawnPos.getX() + random.nextInt(3000) - 1500;
                int z = spawnPos.getZ() + random.nextInt(3000) - 1500;

                level.getChunk(x >> 4, z >> 4); // Carga el chunk

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos posiblePos = new BlockPos(x, y, z);
                posiblePos = new BlockPos(posiblePos.getX(), posiblePos.getY() + 60, posiblePos.getZ());

                if (y <= 200) posicionValida = posiblePos;
            }

            if (!posicionValida.equals(new BlockPos(0, 0, 0))) {
                BlockPos posicionFinal = new BlockPos(posicionValida.getX(), 280, posicionValida.getZ());
                BlockState blockState = level.getBlockState(posicionFinal).getBlock().defaultBlockState();
                BlockState redstoneBlockState = level.getBlockState(posicionFinal.offset(1, 0, 0)).getBlock().defaultBlockState();

                BlockState structureBlock = Blocks.STRUCTURE_BLOCK.defaultBlockState(); BlockState redstoneBlock = Blocks.REDSTONE_BLOCK.defaultBlockState();

                level.setBlock(posicionFinal, structureBlock, 3);
                BlockEntity blockEntity1 = level.getBlockEntity(posicionFinal);
                if (blockEntity1 instanceof StructureBlockEntity) {
                    StructureBlockEntity structureBlockEntity = (StructureBlockEntity) blockEntity1;

                    // Crear y colocar el NBT - GokuHouse (1/3)
                    CompoundTag nbtData = new CompoundTag();
                    nbtData.putString("mirror", "NONE");
                    nbtData.putString("rotation", "NORTH");
                    nbtData.putInt("posX", -95);
                    nbtData.putInt("posY", -52);
                    nbtData.putInt("posZ", -50);
                    nbtData.putString("mode", "LOAD");
                    nbtData.putString("name", "dragonminez:gokuhouse");

                    structureBlockEntity.load(nbtData);
                    structureBlockEntity.setChanged();
                    //System.out.println("Comando: /setblock " + posicionValida.below().below().below().getX() + " " + posicionValida.below().below().below().getY() + " " + posicionValida.below().below().below().getZ() + " minecraft:structure_block" + nbtData);

                    level.setBlock(posicionFinal.offset(1, 0, 0), redstoneBlock, 3);
                    level.setBlock(posicionFinal, Blocks.AIR.defaultBlockState(), 3);
                }
                // Colocar bloques anteriores
                level.setBlock(posicionFinal, blockState, 3);
                level.setBlock(posicionFinal.offset(1, 0, 0), redstoneBlockState, 3);
                level.setBlock(posicionFinal.offset(47, 4, 2), Blocks.AIR.defaultBlockState(), 3);
            }
            BlockPos posicionFinal = new BlockPos(posicionValida.getX(), 200, posicionValida.getZ());
            BlockPos db4pos = new BlockPos(posicionValida.getX() - 18, 287, posicionValida.getZ() + 30);

            BlockPos spawnPosition = new BlockPos(posicionFinal.getX() + 32, posicionFinal.getY() + 85, posicionFinal.getZ() - 13);
            // Marcar como generada y guardar la posición
            setDB4Position(db4pos);
            setHasGokuHouse(true);
            setGokuHousePosition(spawnPosition);
            LOGGER.info("[DMZ-Generation] Goku House generated in {}", spawnPosition);
        }
    }

    public void generateRoshiHouseStructure(ServerLevel level) {
        if (!hasRoshiHouse) {
            Random random = new Random();
            BlockPos spawnPos = level.getSharedSpawnPos();

            BlockPos posicionValida = new BlockPos(0, 0, 0);

            while(posicionValida.equals(new BlockPos(0, 0, 0))) {
                int x = spawnPos.getX() + random.nextInt(3000) - 1500;
                int z = spawnPos.getZ() + random.nextInt(3000) - 1500;

                level.getChunk(x >> 4, z >> 4); // Carga el chunk

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos posiblePos = new BlockPos(x, y, z);
                posiblePos = new BlockPos(posiblePos.getX(), posiblePos.getY() + 60, posiblePos.getZ());

                if (y <= 200) posicionValida = posiblePos;
            }

            if (!posicionValida.equals(new BlockPos(0, 0, 0))) {
                BlockPos posicionFinal = new BlockPos(posicionValida.getX(), 280, posicionValida.getZ());
                BlockState blockState = level.getBlockState(posicionFinal.offset(0, 0, 0)).getBlock().defaultBlockState();
                BlockState redstoneBlockState = level.getBlockState(posicionFinal.offset(1, 0, 0)).getBlock().defaultBlockState();

                BlockState structureBlock = Blocks.STRUCTURE_BLOCK.defaultBlockState(); BlockState redstoneBlock = Blocks.REDSTONE_BLOCK.defaultBlockState();

                level.setBlock(posicionFinal.offset(0, 0, 0), structureBlock, 3);
                BlockEntity blockEntity = level.getBlockEntity(posicionFinal.offset(0, 0, 0));
                if (blockEntity instanceof StructureBlockEntity) {
                    StructureBlockEntity structureBlockEntity = (StructureBlockEntity) blockEntity;

                    CompoundTag nbtData = new CompoundTag();
                    nbtData.putString("mirror", "NONE");
                    nbtData.putString("rotation", "NORTH");
                    nbtData.putInt("posX", -95);
                    nbtData.putInt("posY", -52);
                    nbtData.putInt("posZ", -50);
                    nbtData.putString("mode", "LOAD");
                    nbtData.putString("name", "dragonminez:roshihouse");

                    structureBlockEntity.load(nbtData);
                    structureBlockEntity.setChanged();
                    //System.out.println("Comando: /setblock " + posicionValida.offset(0, -4, 0).getX() + " " + posicionValida.offset(0, -4, 0).getY() + " " + posicionValida.offset(0, -4, 0).getZ() + " minecraft:structure_block" + nbtData);

                    level.setBlock(posicionFinal.offset(1, 0, 0), redstoneBlock, 3);
                }

                level.setBlock(posicionFinal.offset(0, 0, 0), blockState, 3);
                level.setBlock(posicionFinal.offset(1, 0, 0), redstoneBlockState, 3);
                level.setBlock(posicionFinal.offset(47, 4, 2), Blocks.AIR.defaultBlockState(), 3);

                BlockPos spawnPosition = new BlockPos(posicionFinal.getX() + 19, posicionFinal.getY() + 10, posicionFinal.getZ());
                setHasRoshiHouse(true);
                setRoshiHousePosition(spawnPosition);
                LOGGER.info("[DMZ-Generation] Roshi House generated in {}", spawnPosition);
            }
        }
    }

    public void generateElderGuru(ServerLevel level) {
        if (!hasElderGuru) {
            Random random = new Random();
            BlockPos spawnPos = level.getSharedSpawnPos();

            BlockPos posicionValida = new BlockPos(0, 0, 0);

            while(posicionValida.equals(new BlockPos(0, 0, 0))) {
                int x = spawnPos.getX() + random.nextInt(3000) - 1500;
                int z = spawnPos.getZ() + random.nextInt(3000) - 1500;

                level.getChunk(x >> 4, z >> 4); // Carga el chunk

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos posiblePos = new BlockPos(x, y, z);
                posiblePos = new BlockPos(posiblePos.getX(), posiblePos.getY() + 60, posiblePos.getZ());

                if (y <= 200) posicionValida = posiblePos;
            }

            if (!posicionValida.equals(new BlockPos(0, 0, 0))) {
                BlockPos posicionFinal = new BlockPos(posicionValida.getX(), 260, posicionValida.getZ());
                BlockState blockState = level.getBlockState(posicionFinal).getBlock().defaultBlockState();
                BlockState redstoneBlockState = level.getBlockState(posicionFinal.offset(1, 0, 0)).getBlock().defaultBlockState();

                BlockState structureBlock = Blocks.STRUCTURE_BLOCK.defaultBlockState(); BlockState redstoneBlock = Blocks.REDSTONE_BLOCK.defaultBlockState();

                level.setBlock(posicionFinal, structureBlock, 3);
                BlockEntity blockEntity = level.getBlockEntity(posicionFinal);
                if (blockEntity instanceof StructureBlockEntity) {
                    StructureBlockEntity structureBlockEntity = (StructureBlockEntity) blockEntity;

                    CompoundTag nbtData = new CompoundTag();
                    nbtData.putString("mirror", "NONE");
                    nbtData.putString("rotation", "NORTH");
                    nbtData.putInt("posX", -95);
                    nbtData.putInt("posY", -52);
                    nbtData.putInt("posZ", -50);
                    nbtData.putString("mode", "LOAD");
                    nbtData.putString("name", "dragonminez:elder_guru");

                    structureBlockEntity.load(nbtData);
                    structureBlockEntity.setChanged();
                    //System.out.println("Comando: /setblock " + posicionValida.below().below().below().below().below().below().getX() + " " + posicionValida.below().below().below().below().below().below().getY() + " " + posicionValida.below().below().below().below().below().below().getZ() + " minecraft:structure_block" + nbtData);

                    level.setBlock(posicionFinal.offset(1, 0, 0), redstoneBlock, 3);
                }

                level.setBlock(posicionFinal, blockState, 3);
                level.setBlock(posicionFinal.offset(1, 0, 0), redstoneBlockState, 3);
                level.setBlock(posicionFinal.offset(47, 4, 2), Blocks.AIR.defaultBlockState(), 3);

                BlockPos spawnPosition = new BlockPos(posicionFinal.getX() + 2, posicionFinal.getY() + 5, posicionFinal.getZ() + 41);
                BlockPos namekDB4 = new BlockPos(posicionFinal.getX() + 8, posicionFinal.getY() + 13, posicionFinal.getZ() - 2);
                setHasElderGuru(true);
                setNamekDB4Position(namekDB4);
                setElderGuruPosition(spawnPosition);
                LOGGER.info("[DMZ-Generation] Elder Guru's House generated in {}", spawnPosition);
            }
        }
    }

    public void generateHabTiempoStructure(ServerLevel level) {
        if (!hasHabTiempo) {
            BlockPos position = new BlockPos(-9, 31, -70);
            StructureTemplate template = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "habtiempo"));

            if (template != null) {
                StructurePlaceSettings settings = new StructurePlaceSettings();
                template.placeInWorld(level, position, position, settings, level.getRandom(), 2);
            }

            // Marcar como generada y guardar la posición
            setHasHabTiempo(true);
            setHabTiempoPos(position);
            LOGGER.info("[DMZ-Generation] Hyperbolic Time Chamber generated in {}", position);
        }
    }

    public void generatePalacioEnma(ServerLevel level) {
        if (!hasEnmaPalace) {
            StructurePlaceSettings settings = new StructurePlaceSettings();
            BlockPos position = new BlockPos(0, 40, 0);

            StructureTemplate template = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "otherworld/enma/palacioenma1"));

            if (template != null) {
                template.placeInWorld(level, position, position, settings, level.getRandom(), 2);
            }

            BlockPos secPos = position.offset(-129, 0, 0);

            StructureTemplate secTemp = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "otherworld/enma/palacioenma2"));

            if (secTemp != null) {
                secTemp.placeInWorld(level, secPos, secPos, settings, level.getRandom(), 2);
            }

            BlockPos terPos = position.offset(-129, 0, -161);

            StructureTemplate terTemp = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "otherworld/enma/palacioenma3"));

            if (terTemp != null) {
                terTemp.placeInWorld(level, terPos, terPos, settings, level.getRandom(), 2);
            }

            BlockPos quarPos = position.offset(0, 0, -161);

            StructureTemplate quarTemp = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "otherworld/enma/palacioenma4"));

            if (quarTemp != null) {
                quarTemp.placeInWorld(level, quarPos, quarPos, settings, level.getRandom(), 2);
            }

            BlockPos spawnPosition = new BlockPos(0, 41, -101);

            // Marcar como generada y guardar la posición
            setHasEnmaPalace(true);
            setEnmaPalacePosition(spawnPosition);
            LOGGER.info("[DMZ-Generation] Enma's Palace generated in {}", spawnPosition);

            BlockPos snakewayPos = new BlockPos(-67, 40, 146);

            StructureTemplate snakewayInit = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "otherworld/snakeway/snakeway_init"));

            if (snakewayInit != null) {
                snakewayInit.placeInWorld(level, snakewayPos, snakewayPos, settings, level.getRandom(), 2);
            }
            level.setBlock(new BlockPos(0, 42, 149), Blocks.AIR.defaultBlockState(), 3);

            BlockPos snakewayMid1 = snakewayPos.offset(-62, 4, 160);
            StructureTemplate snakewayMid = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "otherworld/snakeway/snakeway_mid"));
            if (snakewayMid != null) {
                snakewayMid.placeInWorld(level, snakewayMid1, snakewayMid1, settings, level.getRandom(), 2);
            }
            BlockPos snakewayMid2 = snakewayMid1.offset(-63, 1, 133);
            if (snakewayMid != null) {
                snakewayMid.placeInWorld(level, snakewayMid2, snakewayMid2, settings, level.getRandom(), 2);
            }
            BlockPos snakewayMid3 = snakewayMid2.offset(-63, 1, 133);
            if (snakewayMid != null) {
                snakewayMid.placeInWorld(level, snakewayMid3, snakewayMid3, settings, level.getRandom(), 2);
            }
            BlockPos snakewayMid4 = snakewayMid3.offset(-63, 1, 133);
            if (snakewayMid != null) {
                snakewayMid.placeInWorld(level, snakewayMid4, snakewayMid4, settings, level.getRandom(), 2);
            }

            BlockPos endSnakewayPos = new BlockPos(-330, 47, 842);

            StructureTemplate snakewayEnd = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "otherworld/snakeway/snakeway_end"));

            if (snakewayEnd != null) {
                snakewayEnd.placeInWorld(level, endSnakewayPos, endSnakewayPos, settings, level.getRandom(), 2);
            }

            BlockPos kaioStructure = endSnakewayPos.offset(-60, 10, 90);

            StructureTemplate kaioPlanet = level.getStructureManager().getOrCreate(new ResourceLocation(DragonMineZ.MOD_ID, "otherworld/kaioplanet"));

            if (kaioPlanet != null) {
                kaioPlanet.placeInWorld(level, kaioStructure, kaioStructure, settings, level.getRandom(), 2);
            }

            BlockPos kaioSpawnPos = new BlockPos(kaioStructure.getX() + 74, kaioStructure.getY() + 127, kaioStructure.getZ() + 29);
            setHasKaioPlanet(true);
            setKaioPlanetPosition(kaioSpawnPos);
            LOGGER.info("[DMZ-Generation] Kaio's Planet generated in {}", kaioSpawnPos);
        }
    }
}