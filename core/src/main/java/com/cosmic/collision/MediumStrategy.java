package com.cosmic.collision;

/**
 * Estrategia para modalidad Media.
 */
public class MediumStrategy implements DifficultyStrategy {

    @Override
    public long getPaletaDurationMs() { return 5000L; }

    @Override
    public float getProbDropModifier() { return 1.0f; }

    @Override
    public PowerUpDistribution getBaseDistribution() {
        return new PowerUpDistribution(
                0.30, // grow
                0.20, // shrink
                0.15, // explosive
                0.15, // life
                0.12, // split
                0.05, // speedUp
                0.03  // speedDown
        );
    }
}