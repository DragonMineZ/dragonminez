package com.dragonminez.common.init.entities;

/**
 * Implemented by entities that support swappable texture variants
 * (e.g. {@code saga_piccolo.png} / {@code saga_piccolo_1.png}).
 * Allows systems such as the quest spawner to request a specific
 * variant without knowing the concrete entity class.
 */
public interface ITextureVariant {
	int getTextureVariant();
	void setTextureVariant(int variant);
}
