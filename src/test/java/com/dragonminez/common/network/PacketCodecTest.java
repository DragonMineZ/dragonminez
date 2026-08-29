package com.dragonminez.common.network;

import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import com.dragonminez.common.network.S2C.BeamClashStateS2C;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketCodecTest {
    @Test
    void combatAttackRequestRoundTrips() {
        CombatAttackRequestC2S original = new CombatAttackRequestC2S(3, true, 7, new int[]{11, 23, 47});
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            original.encode(buffer);
            CombatAttackRequestC2S decoded = new CombatAttackRequestC2S(buffer);

            assertEquals(3, decoded.getComboCount());
            assertTrue(decoded.isSneaking());
            assertEquals(7, decoded.getSelectedSlot());
            assertArrayEquals(new int[]{11, 23, 47}, decoded.getEntityIds());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void beamClashStateRoundTrips() {
        BeamClashStateS2C original = new BeamClashStateS2C(true, 0.25F, 0.4F, 0.6F, 0.75F, 0x12ABEF, 81);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BeamClashStateS2C.encode(original, buffer);
            BeamClashStateS2C decoded = BeamClashStateS2C.decode(buffer);

            assertTrue(decoded.isActive());
            assertEquals(0.25F, decoded.getMeterPhase());
            assertEquals(0.4F, decoded.getSweetLow());
            assertEquals(0.6F, decoded.getSweetHigh());
            assertEquals(0.75F, decoded.getAdvantage());
            assertEquals(0x12ABEF, decoded.getBeamColor());
            assertEquals(81, decoded.getOpponentEntityId());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
