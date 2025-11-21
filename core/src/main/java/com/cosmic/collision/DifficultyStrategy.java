package com.cosmic.collision;

/**
 * Interfaz Strategy para políticas por dificultad.
 */
public interface DifficultyStrategy {
    /**
     * Duración en ms de efectos temporales (p. ej. tamaño de paleta, velocidad de bola).
     */
    long getPaletaDurationMs();

    /**
     * Multiplicador que se aplica a la probabilidad base de drop (por ejemplo 1.25 en Fácil).
     */
    float getProbDropModifier();

    /**
     * Distribución base de probabilidades por tipo de power-up.
     */
    PowerUpDistribution getBaseDistribution();
}