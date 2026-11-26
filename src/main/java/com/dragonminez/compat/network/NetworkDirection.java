// SPDX-License-Identifier: GPL-3.0-only
package com.dragonminez.compat.network;

/**
 * Compatibility shim for Forge NetworkDirection.
 */
public enum NetworkDirection {
	PLAY_TO_SERVER,
	PLAY_TO_CLIENT;

	public net.neoforged.fml.LogicalSide getReceptionSide() {
		return this == PLAY_TO_SERVER ? net.neoforged.fml.LogicalSide.SERVER
				: net.neoforged.fml.LogicalSide.CLIENT;
	}
}
