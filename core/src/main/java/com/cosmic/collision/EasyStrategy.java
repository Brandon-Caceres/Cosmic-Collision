package com.cosmic.collision;

public class EasyStrategy implements DifficultyStrategy {

    @Override
    public long getPaletaDurationMs() { return 7000L; }

    @Override
    public float getProbDropModifier() { return 1.25f; }

    @Override
    public PowerUpDistribution getBaseDistribution() {
        return new PowerUpDistribution(
                0.40, 0.10, 0.15, 0.20, 0.10, 0.03, 0.02
        );
    }

    @Override
    public double getExtraLifeProbability(int nivel) {
        return 0.25;
    }

    @Override
    public LevelProgressionResult applyLevelProgression(int currentVelX, int currentVelY, int currentPaddleWidth) {
        // FÁCIL: NO cambia nada y pide resetEffects para limpiar restos de power-ups.
        return new LevelProgressionResult(currentVelX, currentVelY, currentPaddleWidth, true);
    }

    @Override
    public PowerUpDistribution adjustDistributionForLevel(PowerUpDistribution base, int nivel) {
        // Sesgo suave opcional (puedes comentar esto si quieres exactamente estático).
        double levelSkew = Math.min(0.20, Math.max(0.0, (nivel - 1) * 0.02));
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