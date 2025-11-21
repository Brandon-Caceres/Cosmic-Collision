package com.cosmic.collision;

/**
 * Tipos de power-ups disponibles en el juego.
 */
public enum PowerUpType {
    EXPLOSIVE_BALL,   // bola explosiva por tiempo
    PADDLE_GROW,      // agrandar paleta (temporal en la nueva implementación)
    PADDLE_SHRINK,    // encoger paleta (temporal)
    EXTRA_LIFE,       // vida extra (muy rara)
    SPLIT_BALL,       // divide la/s bola/s en 3
    SPEED_UP,         // aumenta velocidad de la/s bola/s temporalmente
    SPEED_DOWN        // reduce velocidad de la/s bola/s temporalmente
}