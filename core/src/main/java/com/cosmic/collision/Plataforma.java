package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class Plataforma extends ObjetoJuego implements Colisionable {
    private float velPxPorSeg = 600f;

    public Plataforma(int x, int y, int ancho, int alto) {
        super(x, y, ancho, alto);
    }

    public void setVelPxPorSeg(float v) { this.velPxPorSeg = v; }

    @Override
    public void actualizar() {
        float dt = Gdx.graphics.getDeltaTime();
        int nx = x;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  nx = (int)(x - velPxPorSeg * dt);
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) nx = (int)(x + velPxPorSeg * dt);
        if (nx < 0) nx = 0;
        if (nx + ancho > Gdx.graphics.getWidth()) nx = Gdx.graphics.getWidth() - ancho;
        x = nx;
    }

    @Override public Rectangle getRect() { return new Rectangle(x, y, ancho, alto); }

    @Override
    public void alChocarConBola(BolaPing bola) {
        bola.setColor(Color.GREEN);
        int centro = x + ancho / 2;
        int dif = bola.x - centro; // mismo paquete => acceso protegido
        if (Math.abs(dif) > 0) {
            int signo = dif > 0 ? 1 : -1;
            bola.setXY(bola.x + signo * Math.min(4, Math.abs(dif) / 6), bola.y);
        }
    }

    @Override
    public void dibujar(ShapeRenderer sr) {
        sr.setColor(Color.BLUE);
        sr.rect(x, y, ancho, alto);
    }
}