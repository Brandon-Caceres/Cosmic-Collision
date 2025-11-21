package com.cosmic.collision;

/**
 * Estrategia para modalidad Fácil.
 */
public class EasyStrategy implements DifficultyStrategy {

    @Override
    public long getPaletaDurationMs() { return 7000L; }

    @Override
    public float getProbDropModifier() { return 1.25f; }

    @Override
    public PowerUpDistribution getBaseDistribution() {
        return new PowerUpDistribution(
                0.40, // grow
                0.10, // shrink
                0.15, // explosive
                0.20, // life
                0.10, // split
                0.03, // speedUp
                0.02  // speedDown
        );
    }
}