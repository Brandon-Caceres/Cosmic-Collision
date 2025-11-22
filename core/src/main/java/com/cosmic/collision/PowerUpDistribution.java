package com.cosmic.collision;

/**
 * Probabilidades base por tipo de power-up. Se normalizan antes de sortear.
 */
public class PowerUpDistribution {
    public double grow;
    public double shrink;
    public double explosive;
    public double life;
    public double split;
    public double speedUp;
    public double speedDown;

    public PowerUpDistribution(double grow,
                               double shrink,
                               double explosive,
                               double life,
                               double split,
                               double speedUp,
                               double speedDown) {
        this.grow = grow;
        this.shrink = shrink;
        this.explosive = explosive;
        this.life = life;
        this.split = split;
        this.speedUp = speedUp;
        this.speedDown = speedDown;
    }
}