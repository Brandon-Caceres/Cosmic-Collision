package com.cosmic.collision;

public class HardStrategy implements DifficultyStrategy {

    @Override
    public long getPaletaDurationMs() { return 3000L; }

    @Override
    public float getProbDropModifier() { return 0.7f; }

    @Override
    public PowerUpDistribution getBaseDistribution() {
        return new PowerUpDistribution(
                0.15, 0.45, 0.15, 0.05, 0.12, 0.06, 0.02
        );
    }

    @Override
    public double getExtraLifeProbability(int nivel) {
        return 1.0;
    }

    @Override
    public LevelProgressionResult applyLevelProgression(int currentVelX, int currentVelY, int currentPaddleWidth) {
        int newVelX = currentVelX + (currentVelX > 0 ? 1 : -1);
        int newVelY = currentVelY + (currentVelY > 0 ? 1 : -1);
        int newWidth = Math.max(60, currentPaddleWidth - 12);
        return new LevelProgressionResult(newVelX, newVelY, newWidth, false);
    }

    @Override
    public PowerUpDistribution adjustDistributionForLevel(PowerUpDistribution base, int nivel) {
        double levelSkew = Math.min(0.25, Math.max(0.0, (nivel - 1) * 0.02));
        if (levelSkew <= 0) return base;
        double utilesSum = base.grow + base.life + base.speedUp;
        if (utilesSum <= 0) return base;

        double reducTotal = utilesSum * levelSkew;
        double growRed = (base.grow / utilesSum) * reducTotal;
        double lifeRed = (base.life / utilesSum) * reducTotal;
        double speedUpRed = (base.speedUp / utilesSum) * reducTotal;

        base.grow = Math.max(0.0, base.grow - growRed);
        base.life = Math.max(0.0, base.life - lifeRed);
        base.speedUp = Math.max(0.0, base.speedUp - speedUpRed);
        base.shrink += (growRed + lifeRed + speedUpRed);

        return base;
    }
}