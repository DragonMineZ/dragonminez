package com.dragonminez.compat.network;

import com.dragonminez.compat.network.simple.SimpleChannel;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPacketRegistrationTest {
    @Test
    void bidirectionalLegacyPacketsCoexistWithExistingDirectionalIds() {
        SimpleChannel channel = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath("dragonminez", "legacy_packet_test"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions("1.0"::equals)
                .serverAcceptedVersions("1.0"::equals)
                .simpleChannel();
        channel.registerMessage(7, TestPacket.class,
                (packet, buffer) -> buffer.writeInt(packet.number()),
                buffer -> new TestPacket(buffer.readInt()), (packet, context) -> {});
        channel.messageBuilder(ExistingPacket.class, 8, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeInt(packet.number()))
                .decoder(buffer -> new ExistingPacket(buffer.readInt()))
                .consumerMainThread((packet, context) -> {}).add();

        RecordingRegistrar registrar = new RecordingRegistrar();
        SimpleChannel.onRegisterPayloads(new RegisterPayloadHandlersEvent() {
            @Override
            public PayloadRegistrar registrar(String version) {
                return registrar;
            }
        });

        ResourceLocation serverbound = id("7_testpacket_c2s");
        ResourceLocation clientbound = id("7_testpacket_s2c");
        assertTrue(registrar.serverbound.containsKey(serverbound));
        assertTrue(registrar.clientbound.containsKey(clientbound));
        assertTrue(registrar.clientbound.containsKey(id("8_existingpacket")));
        assertFalse(registrar.clientbound.containsKey(id("8_existingpacket_s2c")));
        assertEquals(123, registrar.serverbound.get(serverbound).applyAsInt(123));
        assertEquals(-456, registrar.clientbound.get(clientbound).applyAsInt(-456));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez", "legacy_packet_test/" + path);
    }

    private record TestPacket(int number) {}
    private record ExistingPacket(int number) {}

    private static final class RecordingRegistrar extends PayloadRegistrar {
        private final Map<ResourceLocation, IntUnaryOperator> serverbound = new HashMap<>();
        private final Map<ResourceLocation, IntUnaryOperator> clientbound = new HashMap<>();

        private RecordingRegistrar() {
            super("1.0");
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playToServer(
                CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                IPayloadHandler<T> handler) {
            serverbound.put(type.id(), value -> roundTrip(type, codec, value));
            return this;
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playToClient(
                CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                IPayloadHandler<T> handler) {
            clientbound.put(type.id(), value -> roundTrip(type, codec, value));
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static <T extends CustomPacketPayload> int roundTrip(
                CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, int value) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            try {
                T payload = (T) new SimpleChannel.WrappedPayload((CustomPacketPayload.Type) type, new TestPacket(value));
                codec.encode(buffer, payload);
                SimpleChannel.WrappedPayload<?> decoded = assertInstanceOf(SimpleChannel.WrappedPayload.class, codec.decode(buffer));
                assertEquals(0, buffer.readableBytes());
                return assertInstanceOf(TestPacket.class, decoded.message()).number();
            } finally {
                buffer.release();
            }
        }
    }
}
