package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Pelota con la misma lógica original de rebotes y arranque.
 * Añadido: almacena velocidades base para poder aplicar y revertir multiplicadores.
 */
public class BolaPing extends ObjetoJuego {
    private int radio;
    private int velX;
    private int velY;
    private int baseVelX;
    private int baseVelY;
    private Color color = Color.WHITE;
    private boolean quieta;

    public BolaPing(int x, int y, int radio, int velX, int velY, boolean iniciaQuieta) {
        super(x, y, radio * 2, radio * 2);
        this.radio = radio;
        this.velX = velX;
        this.velY = velY;
        this.baseVelX = velX;
        this.baseVelY = velY;
        this.quieta = iniciaQuieta;
    }

    public boolean estaQuieta() { return quieta; }
    public void setEstaQuieta(boolean b) { quieta = b; }
    public void setXY(int nx, int ny) { this.x = nx; this.y = ny; }
    public int getY() { return y; }
    public int getX() { return x; }
    public int getRadio() { return radio; }
    public void setColor(Color c) { this.color = c; }

    @Override
    public void dibujar(ShapeRenderer sr) {
        sr.setColor(color);
        sr.circle(x, y, radio);
    }

    @Override
    public void actualizar() {
        if (quieta) return;
        x += velX;
        y += velY;
        if (x - radio < 0) {
            x = radio;
            velX = -velX;
        } else if (x + radio > Gdx.graphics.getWidth()) {
            x = Gdx.graphics.getWidth() - radio;
            velX = -velX;
        }
        if (y + radio > Gdx.graphics.getHeight()) {
            y = Gdx.graphics.getHeight() - radio;
            velY = -velY;
        }
    }

    private boolean colisionaCon(Rectangle r) {
        float masCercX = clamp(x, r.x, r.x + r.width);
        float masCercY = clamp(y, r.y, r.y + r.height);
        float dx = x - masCercX;
        float dy = y - masCercY;
        return (dx * dx + dy * dy) <= (radio * radio);
    }

    private float clamp(float v, float a, float b) {
        if (v < a) return a;
        if (v > b) return b;
        return v;
    }

    public void comprobarColision(Colisionable c) {
        Rectangle r = c.getRect();
        if (colisionaCon(r)) {
            velY = -velY;
            c.alChocarConBola(this);
        }
    }

    // ----------------- velocidad dinámica -----------------

    // Aplica multiplicador (m > 0). Se basa en baseVelX/baseVelY para evitar drift.
    public void aplicarMultiplicadorVelocidad(float m) {
        if (m <= 0f) return;
        int signX = velX >= 0 ? 1 : -1;
        int signY = velY >= 0 ? 1 : -1;
        int nvx = Math.max(1, Math.round(Math.abs(baseVelX * m)));
        int nvy = Math.max(1, Math.round(Math.abs(baseVelY * m)));
        velX = signX * nvx;
        velY = signY * nvy;
    }

    // Restaurar a baseVel
    public void restaurarVelBase() {
        // conservar signo actual si base es 0 improbable, así que asignar base
        int signX = baseVelX >= 0 ? 1 : -1;
        int signY = baseVelY >= 0 ? 1 : -1;
        velX = signX * Math.abs(baseVelX);
        velY = signY * Math.abs(baseVelY);
    }

    // Cuando creamos nuevas bolas por split, permitir ajustar su velocidad base
    public void setBaseVelocities(int baseX, int baseY) {
        this.baseVelX = baseX;
        this.baseVelY = baseY;
        // actualizar vel según signo actual (mantener dirección)
        int signX = velX >= 0 ? 1 : -1;
        int signY = velY >= 0 ? 1 : -1;
        this.velX = signX * Math.abs(baseX);
        this.velY = signY * Math.abs(baseY);
    }
}