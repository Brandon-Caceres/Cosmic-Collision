package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


public class Plataforma extends ObjetoJuego implements Colisionable {
    private float velPxPorSeg = 200f;
    
    private Texture texture;

    public Plataforma(int x, int y, int ancho, int alto, Texture texture) {
        super(x, y, ancho, alto);
        this.texture = texture;
    }

    public void setVelPxPorSeg(float v) {
        this.velPxPorSeg = v;
    }

    public void actualizar() {
        float dt = Gdx.graphics.getDeltaTime();
        int nx = x;

        // MOVER A LA IZQUIERDA
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            nx = (int)(x - velPxPorSeg * dt);
        }

        //MOVER A LA DERECHA
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            nx = (int)(x + velPxPorSeg * dt);
        }
        
        if (nx < 0) nx = 0;
        if (nx + ancho > Gdx.graphics.getWidth()) {
            nx = Gdx.graphics.getWidth() - ancho;
        }

        x = nx;
    }

    @Override
    public Rectangle getRect() {
        return new Rectangle(x, y, ancho, alto);
    }

    @Override
    public void alChocarConBola(BolaPing bola) {
        bola.setColor(Color.GREEN);
        int centro = x + ancho / 2;
        int dif = bola.x - centro;
        

        if (Math.abs(dif) > 0) {
            int signo = dif > 0 ? 1 : -1;
            bola.setXY(bola.x + signo * Math.min(4, Math.abs(dif) / 6), bola.y);
        }
    }

    
    public void dibujar(SpriteBatch batch) {
    	batch.draw(texture, x, y, ancho, alto);
    }
}