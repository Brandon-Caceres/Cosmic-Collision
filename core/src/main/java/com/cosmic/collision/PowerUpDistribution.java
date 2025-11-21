package com.cosmic.collision;

/**
 * Estructura simple para almacenar probabilidades base de cada tipo de power-up.
 */
public class PowerUpDistribution {
    public double grow;     // PADDLE_GROW
    public double shrink;   // PADDLE_SHRINK
    public double explosive; // EXPLOSIVE_BALL
    public double life;     // EXTRA_LIFE
    public double split;    // SPLIT_BALL
    public double speedUp;  // SPEED_UP
    public double speedDown; // SPEED_DOWN

    public PowerUpDistribution(double grow, double shrink, double explosive, double life, double split, double speedUp, double speedDown) {
        this.grow = grow;
        this.shrink = shrink;
        this.explosive = explosive;
        this.life = life;
        this.split = split;
        this.speedUp = speedUp;
        this.speedDown = speedDown;
    }
}