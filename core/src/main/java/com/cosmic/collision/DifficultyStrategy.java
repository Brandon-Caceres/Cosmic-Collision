package com.cosmic.collision;

public interface DifficultyStrategy {
    long getPaletaDurationMs();
    
    float getProbDropModifier();
    
    PowerUpDistribution getBaseDistribution();

    double getExtraLifeProbability(int nivel);

    LevelProgressionResult applyLevelProgression(int currentVelX, int currentVelY, int currentPaddleWidth);

    PowerUpDistribution adjustDistributionForLevel(PowerUpDistribution base, int nivel);
}