package com.dragonminez.common.combat.logic.weapon;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.combat.util.CompressionHelper;
import com.dragonminez.common.combat.weapon.AttributesContainer;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import com.dragonminez.common.combat.weapon.WeaponAttributesHelper;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeaponRegistry {
    public static Map<ResourceLocation, WeaponAttributes> registrations = new HashMap<>();
    public static Map<ResourceLocation, AttributesContainer> containers = new HashMap<>();

    public static void register(ResourceLocation itemId, WeaponAttributes attributes) {
        registrations.put(itemId, attributes);
    }

    public static WeaponAttributes getAttributes(ItemStack itemStack) {
        if (itemStack == null) return null;
        if (itemStack.isEmpty()) return getAttributes(ResourceLocation.parse("minecraft:air"));

        var inStackAttributes = WeaponAttributesHelper.getContainerFromNBT(itemStack);
        if (inStackAttributes != null) {
            if (inStackAttributes.attributes() != null) {
                if (inStackAttributes.parent() == null) {
                    return inStackAttributes.attributes();
                } else {
                    var parentAttributes = getAttributes(ResourceLocation.parse(inStackAttributes.parent()));
                    if (parentAttributes != null) {
                        return WeaponAttributesHelper.override(parentAttributes, inStackAttributes.attributes());
                    }
                }
            } else {
                if (inStackAttributes.parent() != null) {
                    return getAttributes(ResourceLocation.parse(inStackAttributes.parent()));
                }
            }
        }

        var itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if (itemId != null) {
            return getAttributes(itemId);
        }
        return null;
    }

    public static WeaponAttributes getAttributes(ResourceLocation itemId) {
        if (itemId == null) {
            return null;
        }
        return registrations.get(itemId);
    }

    public static void resolveAndRegisterAttributes(ResourceLocation itemId, AttributesContainer container) {
        if (container.attributes() != null) {
            if (container.parent() == null) {
                register(itemId, container.attributes());
            } else {
                var parentAttributes = getAttributes(ResourceLocation.parse(container.parent()));
                if (parentAttributes != null) {
                    register(itemId, WeaponAttributesHelper.override(parentAttributes, container.attributes()));
                }
            }
        } else {
            if (container.parent() != null) {
                var parentAttributes = getAttributes(ResourceLocation.parse(container.parent()));
                if (parentAttributes != null) {
                    register(itemId, parentAttributes);
                }
            }
        }
    }

    public static void loadAttributes(ResourceManager resourceManager) {
        containers.clear();
        registrations.clear();

        var resources = resourceManager.listResources("weapon_attributes", id -> id.getPath().endsWith(".json"));
        for (var entry : resources.entrySet()) {
            var id = entry.getKey();
            var resource = entry.getValue();
            try {
                var reader = new JsonReader(new InputStreamReader(resource.open()));
                var container = WeaponAttributesHelper.decode(reader);
                if (container != null) {
                    var itemId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring("weapon_attributes/".length(), id.getPath().length() - ".json".length()));
                    containers.put(itemId, container);
                }
            } catch (Exception e) {
                LogUtil.error(Env.COMMON, "Failed to load weapon attributes from: " + id, e);
            }
        }

        for (var entry : containers.entrySet()) {
            resolveAndRegisterAttributes(entry.getKey(), entry.getValue());
        }

        WeaponAttributesFallback.initialize();
        encodeRegistry();
    }

    static FriendlyByteBuf encodedRegistrations = new FriendlyByteBuf(Unpooled.buffer());

    public static void encodeRegistry() {
        var gson = new Gson();
        String json = gson.toJson(registrations);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        if (ConfigManager.getCombatConfig().getWeaponRegistryCompression()) {
            buffer.writeBoolean(true);
            var compressedJson = CompressionHelper.gzipCompress(json);
            if (compressedJson == null) {
                LogUtil.error(Env.COMMON, "Weapon Attribute registry compression failed!");
                return;
            }
            List<String> chunks = splitToChunks(compressedJson);
            buffer.writeInt(chunks.size());
            for (String chunk : chunks) {
                buffer.writeUtf(chunk, chunk.length() * 2 + 10);
            }
            LogUtil.info(Env.COMMON, "Encoded and compressed Weapon Attribute registry into " + chunks.size() + " chunks");
        } else {
            buffer.writeBoolean(false);
            List<String> chunks = splitToChunks(json);
            buffer.writeInt(chunks.size());
            for (String chunk : chunks) {
                buffer.writeUtf(chunk, chunk.length() * 2 + 10);
            }
            LogUtil.info(Env.COMMON, "Encoded Weapon Attribute registry into " + chunks.size() + " chunks");
        }
        encodedRegistrations = buffer;
    }

    public static void decodeRegistry(String json) {
        if (ConfigManager.getCombatConfig().getWeaponRegistryLogging()) {
            LogUtil.info(Env.COMMON, "Weapon Attribute registry received: " + json);
        }

        var gson = new Gson();
        Type mapType = new TypeToken<Map<String, WeaponAttributes>>() {}.getType();
        Map<String, WeaponAttributes> readRegistrations = gson.fromJson(json, mapType);
        Map<ResourceLocation, WeaponAttributes> newRegistrations = new HashMap<>();

        readRegistrations.forEach((key, value) -> {
            newRegistrations.put(ResourceLocation.parse(key), value);
        });
        registrations = newRegistrations;
    }

    public static FriendlyByteBuf getEncodedRegistry() {
        return encodedRegistrations;
    }

    private static List<String> splitToChunks(String string) {
        int chunkSize = 20000;
        List<String> chunks = new ArrayList<>();
        int length = string.length();
        for (int i = 0; i < length; i += chunkSize) {
            chunks.add(string.substring(i, Math.min(length, i + chunkSize)));
        }
        return chunks;
    }
}