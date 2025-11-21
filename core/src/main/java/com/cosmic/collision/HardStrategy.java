package com.cosmic.collision;

/**
 * Estrategia para modalidad Difícil.
 */
public class HardStrategy implements DifficultyStrategy {

    @Override
    public long getPaletaDurationMs() { return 3000L; }

    @Override
    public float getProbDropModifier() { return 0.7f; }

    @Override
    public PowerUpDistribution getBaseDistribution() {
        return new PowerUpDistribution(
                0.15, // grow
                0.45, // shrink
                0.15, // explosive
                0.05, // life
                0.12, // split
                0.06, // speedUp
                0.02  // speedDown
        );
    }
}