package com.cosmic.collision;

public class LevelProgressionResult {
    public final int newVelX;
    public final int newVelY;
    public final int newPaddleWidth;
    public final boolean resetEffects;

    public LevelProgressionResult(int newVelX, int newVelY, int newPaddleWidth, boolean resetEffects) {
        this.newVelX = newVelX;
        this.newVelY = newVelY;
        this.newPaddleWidth = newPaddleWidth;
        this.resetEffects = resetEffects;
    }
}