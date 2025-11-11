package com.dragonminez.client.animation;

/**
 * Interfaz para acceder a las capacidades de animación del jugador
 * Implementada por el mixin PlayerGeoAnimatableMixin
 */
public interface IPlayerAnimatable {
    /**
     * Establece el estado de vuelo creativo para sincronización de animaciones
     * @param flying true si el jugador está volando, false en caso contrario
     */
    void dragonminez$setCreativeFlying(boolean flying);
}

