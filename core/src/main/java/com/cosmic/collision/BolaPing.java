package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class BolaPing extends ObjetoJuego {
    private int radio;
    private int velX;
    private int velY;
    private Color color = Color.WHITE;
    private boolean quieta;

    public BolaPing(int x, int y, int radio, int velX, int velY, boolean iniciaQuieta) {
        super(x, y, radio * 2, radio * 2);
        this.radio = radio;
        this.velX = velX;
        this.velY = velY;
        this.quieta = iniciaQuieta;
    }

    public boolean estaQuieta() { return quieta; }
    public void setEstaQuieta(boolean b) { quieta = b; }
    public void setXY(int nx, int ny) { this.x = nx; this.y = ny; }
    public int getY() { return y; }
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
}