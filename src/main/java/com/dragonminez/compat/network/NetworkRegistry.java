package com.dragonminez.compat.network;

import net.minecraft.resources.ResourceLocation;
import com.dragonminez.compat.network.simple.SimpleChannel;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.Objects;

/**
 * Compatibility shim for Forge NetworkRegistry.ChannelBuilder.
 */
public final class NetworkRegistry {
	private NetworkRegistry() {}

	public static ChannelBuilder newSimpleChannel(ResourceLocation name, Supplier<String> networkProtocolVersion,
												  Predicate<String> clientAcceptedVersions, Predicate<String> serverAcceptedVersions) {
		return ChannelBuilder.named(name)
				.networkProtocolVersion(networkProtocolVersion)
				.clientAcceptedVersions(clientAcceptedVersions)
				.serverAcceptedVersions(serverAcceptedVersions);
	}

	public static final class ChannelBuilder {
		private final ResourceLocation name;
		private Supplier<String> networkProtocolVersion;
		private Predicate<String> clientAcceptedVersions;
		private Predicate<String> serverAcceptedVersions;

		private ChannelBuilder(ResourceLocation name) {
			this.name = name;
		}

		public static ChannelBuilder named(ResourceLocation name) {
			return new ChannelBuilder(name);
		}

		public ChannelBuilder networkProtocolVersion(Supplier<String> version) {
			this.networkProtocolVersion = Objects.requireNonNull(version, "networkProtocolVersion");
			return this;
		}

		public ChannelBuilder clientAcceptedVersions(Predicate<String> accepted) {
			this.clientAcceptedVersions = Objects.requireNonNull(accepted, "clientAcceptedVersions");
			return this;
		}

		public ChannelBuilder serverAcceptedVersions(Predicate<String> accepted) {
			this.serverAcceptedVersions = Objects.requireNonNull(accepted, "serverAcceptedVersions");
			return this;
		}

		public SimpleChannel simpleChannel() {
			if (networkProtocolVersion == null || clientAcceptedVersions == null || serverAcceptedVersions == null) {
				throw new IllegalStateException("Network channel '" + name + "' is missing protocol compatibility configuration");
			}
			return new SimpleChannel(name, networkProtocolVersion, clientAcceptedVersions, serverAcceptedVersions);
		}
	}
}
