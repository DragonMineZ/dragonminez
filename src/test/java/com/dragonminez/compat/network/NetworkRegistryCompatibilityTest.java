package com.dragonminez.compat.network;

import com.dragonminez.compat.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkRegistryCompatibilityTest {
    @Test
    void channelRetainsStrictCompatibilityPredicates() {
        SimpleChannel channel = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath("dragonminez", "compatibility_test"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions("1.0"::equals)
                .serverAcceptedVersions("1.0"::equals)
                .simpleChannel();

        assertEquals("1.0", channel.protocolVersion());
        assertTrue(channel.acceptsClientVersion("1.0"));
        assertTrue(channel.acceptsServerVersion("1.0"));
        assertFalse(channel.acceptsClientVersion("0.9"));
        assertFalse(channel.acceptsServerVersion("2.0"));
    }

    @Test
    void channelRejectsIncompleteOrSelfIncompatibleConfiguration() {
        var incomplete = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath("dragonminez", "incomplete"))
                .networkProtocolVersion(() -> "1.0");
        assertThrows(IllegalStateException.class, incomplete::simpleChannel);

        var incompatible = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath("dragonminez", "incompatible"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions("2.0"::equals)
                .serverAcceptedVersions("1.0"::equals);
        assertThrows(IllegalArgumentException.class, incompatible::simpleChannel);
    }
}
