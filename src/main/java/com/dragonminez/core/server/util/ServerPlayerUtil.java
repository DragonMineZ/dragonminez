package com.dragonminez.core.server.util;

import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side utility class for performing various player-related operations.
 * <p>
 * This class is intended to contain multiple utilities that affect player state or attributes.
 * All methods in this class are strictly server-side and should not be called on the client.
 */
public class ServerPlayerUtil {

  /**
   * UUID used to uniquely identify the max health attribute modifier applied by this utility.
   */
  private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString(
      "890b48cc-b28d-4e9b-bd8b-dc55e771fc04"
  );

  /**
   * Sets the player's maximum health to the specified amount.
   * If a modifier with the same UUID exists, it is removed before applying the new one.
   * This method only executes if called on the server side.
   *
   * @param player the player whose max health should be modified
   * @param amount the new maximum health value
   */
  public static void setMaxHealth(Player player, double amount) {
    if (player.level().isClientSide) {
      return;
    }

    final AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealthAttr == null) {
      return;
    }

    maxHealthAttr.removeModifier(HEALTH_MODIFIER_UUID);
    final double base = player.getAttributeBaseValue(Attributes.MAX_HEALTH);
    final AttributeModifier modifier = new AttributeModifier(
        HEALTH_MODIFIER_UUID,
        "dmz_max_health",
        amount - base,
        AttributeModifier.Operation.ADDITION
    );
    maxHealthAttr.addPermanentModifier(modifier);

    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
  }

  /**
   * Resets the player's maximum health to its original value by removing the custom modifier.
   * This method only executes if called on the server side.
   *
   * @param player the player whose max health modifier should be removed
   */
  public static void resetMaxHealth(Player player) {
    if (player.level().isClientSide) {
      return;
    }

    final AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealthAttr == null) {
      return;
    }

    maxHealthAttr.removeModifier(HEALTH_MODIFIER_UUID);
    if (player.getHealth() > player.getMaxHealth()) {
      player.setHealth(player.getMaxHealth());
    }
  }
}
