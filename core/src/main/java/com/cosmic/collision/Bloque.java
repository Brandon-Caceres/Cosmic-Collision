package com.cosmic.collision;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Bloque extends ObjetoJuego implements Colisionable {
    private boolean destruido;
    private boolean irrompible;
    private int hp;

    private final Texture texturaNormal;
    private final Texture texturaResistente2;
    private final Texture texturaResistente3;
    private final Texture texturaIrrompible;

    public Bloque(int x, int y, int ancho, int alto, Texture tx1, Texture tx2, Texture tx3, Texture txU) {
        this(x, y, ancho, alto, 1, false, tx1, tx2, tx3, txU);
    }

    public Bloque(int x, int y, int ancho, int alto, int hp, boolean irrompible, Texture tx1, Texture tx2, Texture tx3, Texture txU) {
        super(x, y, ancho, alto);
        this.irrompible = irrompible;
        this.hp = Math.max(1, hp);
        this.destruido = false;
        this.texturaNormal = tx1;
        this.texturaResistente2 = tx2;
        this.texturaResistente3 = tx3;
        this.texturaIrrompible = txU;
    }

    @Override public Rectangle getRect() { return new Rectangle(x, y, ancho, alto); }
    @Override public void alChocarConBola(BolaPing bola) { recibirImpacto(); }

    public void recibirImpacto() {
        if (destruido || irrompible) return;
        hp--;
        if (hp <= 0) destruido = true;
    }

    public boolean estaDestruido() { return destruido; }

    public void dibujar(SpriteBatch batch) {
        if (destruido) return;
        Texture t;
        if (irrompible) t = texturaIrrompible;
        else if (hp >= 3) t = texturaResistente3;
        else if (hp == 2) t = texturaResistente2;
        else t = texturaNormal;
        batch.draw(t, x, y, ancho, alto);
    }
}