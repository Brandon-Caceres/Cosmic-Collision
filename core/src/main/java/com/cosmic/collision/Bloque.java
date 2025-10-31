package com.cosmic.collision;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class Bloque extends ObjetoJuego implements Colisionable {
    private boolean destruido;
    private boolean irrompible;
    private int hp;
    private final Color base;

    public Bloque(int x, int y, int ancho, int alto) {
        this(x, y, ancho, alto, 1, false, new Color(0.7f, 0.2f, 0.9f, 1f));
    }

    public Bloque(int x, int y, int ancho, int alto, int hp, boolean irrompible, Color base) {
        super(x, y, ancho, alto);
        this.irrompible = irrompible;
        this.hp = Math.max(1, hp);
        this.destruido = false;
        this.base = base;
    }

    public boolean estaDestruido() { return destruido; }

    @Override public Rectangle getRect() { return new Rectangle(x, y, ancho, alto); }

    @Override public void alChocarConBola(BolaPing bola) { recibirImpacto(); }

    public void recibirImpacto() {
        if (destruido || irrompible) return;
        hp--;
        if (hp <= 0) destruido = true;
    }

    @Override
    public void dibujar(ShapeRenderer sr) {
        if (destruido) return;
        if (irrompible) {
            sr.setColor(Color.DARK_GRAY);
        } else if (hp >= 2) {
            float f = Math.max(0.45f, 1f - 0.18f * (hp - 1));
            sr.setColor(base.r * f, base.g * f, base.b * f, 1f);
        } else {
            sr.setColor(base);
        }
        sr.rect(x, y, ancho, alto);
    }
}