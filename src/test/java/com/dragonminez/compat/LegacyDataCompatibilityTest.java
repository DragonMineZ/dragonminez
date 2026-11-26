package com.dragonminez.compat;

import com.dragonminez.common.util.AttributeMods;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDataCompatibilityTest {
    @Test
    void blockPositionsReadBothLegacyCoordinatesAndPackedLongs() {
        BlockPos expected = new BlockPos(-1234, -64, 5678);
        for (boolean uppercase : new boolean[]{true, false}) {
            CompoundTag tag = new CompoundTag();
            tag.putInt(uppercase ? "X" : "x", expected.getX());
            tag.putInt(uppercase ? "Y" : "y", expected.getY());
            tag.putInt(uppercase ? "Z" : "z", expected.getZ());
            assertEquals(expected, LegacyNbtData.readBlockPos(tag));
        }
        assertEquals(expected, LegacyNbtData.readBlockPos(LongTag.valueOf(expected.asLong())));
        assertThrows(IllegalArgumentException.class, () -> LegacyNbtData.readBlockPos(null));
    }

    @Test
    void attributeIdsMatchTheExistingPort() {
        UUID legacyId = UUID.fromString("12345678-1234-5678-9012-123456789012");
        assertEquals(AttributeMods.id(legacyId), LegacyAttributeModifierId.from(legacyId));
    }

    @Test
    void legacyItemTagsSurviveCopiesPersistenceAndNetworkEncoding() {
        ItemStack original = new ItemStack(Items.PAPER, 3);
        LegacyItemStackData.getOrCreateTag(original).putString("owner", "test-player");
        assertEquals("test-player", LegacyItemStackData.getTag(original.copy()).getString("owner"));

        ItemStack loaded = LegacyItemStackData.parse(LegacyItemStackData.save(original));
        assertEquals(3, loaded.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(original, loaded));

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        try {
            LegacyItemStackNetworkCodec.write(buffer, original);
            LegacyItemStackNetworkCodec.write(buffer, null);
            ItemStack decoded = LegacyItemStackNetworkCodec.read(buffer);
            assertEquals(3, decoded.getCount());
            assertTrue(ItemStack.isSameItemSameComponents(original, decoded));
            assertTrue(LegacyItemStackNetworkCodec.read(buffer).isEmpty());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
