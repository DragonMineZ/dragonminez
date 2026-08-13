package com.dragonminez.compat.network.simple;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.dragonminez.compat.network.NetworkDirection;
import com.dragonminez.compat.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Compatibility shim that presents the Forge SimpleChannel API and registers NeoForge payloads under the hood.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class SimpleChannel {
	private static final List<SimpleChannel> CHANNELS = new ArrayList<>();

	private final ResourceLocation name;
	private final String protocolVersion;
	private final Predicate<String> clientAcceptedVersions;
	private final Predicate<String> serverAcceptedVersions;
	private final List<PendingRegistration<?>> pending = new ArrayList<>();
	private final Map<Class<?>, PayloadBinding<?>> bindings = new ConcurrentHashMap<>();
	private boolean registered;

	public SimpleChannel(ResourceLocation name, Supplier<String> protocolVersion,
			Predicate<String> clientAcceptedVersions, Predicate<String> serverAcceptedVersions) {
		this.name = name;
		this.protocolVersion = protocolVersion.get();
		this.clientAcceptedVersions = clientAcceptedVersions;
		this.serverAcceptedVersions = serverAcceptedVersions;
		if (this.protocolVersion == null || this.protocolVersion.isBlank()) {
			throw new IllegalArgumentException("Network protocol version for '" + name + "' must not be blank");
		}
		if (!acceptsClientVersion(this.protocolVersion) || !acceptsServerVersion(this.protocolVersion)) {
			throw new IllegalArgumentException("Network protocol predicates for '" + name + "' reject its local version '" + this.protocolVersion + "'");
		}
		CHANNELS.add(this);
	}

	public String protocolVersion() {
		return protocolVersion;
	}

	public boolean acceptsClientVersion(String version) {
		return clientAcceptedVersions.test(version);
	}

	public boolean acceptsServerVersion(String version) {
		return serverAcceptedVersions.test(version);
	}

	public <MSG> MessageBuilder<MSG> messageBuilder(Class<MSG> type, int id, NetworkDirection direction) {
		return new MessageBuilder<>(type, id, direction);
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToServer(MSG message) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		PacketDistributor.sendToServer(binding.wrap(message));
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		PacketDistributor.sendToPlayer(player, binding.wrap(message));
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToAllPlayers(MSG message) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		PacketDistributor.sendToAllPlayers(binding.wrap(message));
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToTrackingEntityAndSelf(MSG message, Entity entity) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, binding.wrap(message));
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToTrackingEntity(MSG message, Entity entity) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		PacketDistributor.sendToPlayersTrackingEntity(entity, binding.wrap(message));
	}

	@SubscribeEvent
	public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
		for (SimpleChannel channel : List.copyOf(CHANNELS)) {
			channel.flush(event.registrar(channel.protocolVersion));
		}
	}

	private void flush(PayloadRegistrar registrar) {
		if (registered) return;
		registered = true;
		for (PendingRegistration<?> reg : pending) {
			reg.register(registrar, name, bindings);
		}
	}

	public final class MessageBuilder<MSG> {
		private final Class<MSG> type;
		private final int id;
		private final NetworkDirection direction;
		private BiConsumer<MSG, FriendlyByteBuf> encoder;
		private Function<FriendlyByteBuf, MSG> decoder;
		private BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer;

		MessageBuilder(Class<MSG> type, int id, NetworkDirection direction) {
			this.type = type;
			this.id = id;
			this.direction = direction;
		}

		public MessageBuilder<MSG> encoder(BiConsumer<MSG, FriendlyByteBuf> encoder) {
			this.encoder = encoder;
			return this;
		}

		public MessageBuilder<MSG> decoder(Function<FriendlyByteBuf, MSG> decoder) {
			this.decoder = decoder;
			return this;
		}

		public MessageBuilder<MSG> consumerMainThread(BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
			this.consumer = consumer;
			return this;
		}

		public void add() {
			pending.add(new PendingRegistration<>(type, id, direction, encoder, decoder, consumer));
		}
	}

	private record PendingRegistration<MSG>(
			Class<MSG> type,
			int id,
			NetworkDirection direction,
			BiConsumer<MSG, FriendlyByteBuf> encoder,
			Function<FriendlyByteBuf, MSG> decoder,
			BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer
	) {
		void register(PayloadRegistrar registrar, ResourceLocation channelName, Map<Class<?>, PayloadBinding<?>> bindings) {
			ResourceLocation payloadId = ResourceLocation.fromNamespaceAndPath(
					channelName.getNamespace(),
					channelName.getPath() + "/" + id + "_" + type.getSimpleName().toLowerCase()
			);
			CustomPacketPayload.Type<WrappedPayload<MSG>> payloadType = new CustomPacketPayload.Type<>(payloadId);
			StreamCodec<RegistryFriendlyByteBuf, WrappedPayload<MSG>> codec = StreamCodec.of(
					(buf, wrapped) -> encoder.accept(wrapped.message(), buf),
					buf -> new WrappedPayload<>(payloadType, decoder.apply(buf))
			);

			IPayloadHandler<WrappedPayload<MSG>> handler = (payload, ctx) ->
					consumer.accept(payload.message(), NetworkEvent.wrap(ctx));

			if (direction == NetworkDirection.PLAY_TO_SERVER) {
				registrar.playToServer(payloadType, codec, handler);
			} else {
				registrar.playToClient(payloadType, codec, handler);
			}
			bindings.put(type, new PayloadBinding<>(payloadType));
		}
	}

	private record PayloadBinding<MSG>(CustomPacketPayload.Type<WrappedPayload<MSG>> payloadType) {
		WrappedPayload<MSG> wrap(MSG message) {
			return new WrappedPayload<>(payloadType, message);
		}
	}

	public record WrappedPayload<MSG>(CustomPacketPayload.Type<WrappedPayload<MSG>> payloadType, MSG message) implements CustomPacketPayload {
		@Override
		public Type<? extends CustomPacketPayload> type() {
			return payloadType;
		}
	}
}
