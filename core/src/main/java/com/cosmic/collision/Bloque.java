package com.cosmic.collision;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Bloque (asteroide).
 */
public class Bloque extends ObjetoJuego implements Colisionable {
    private boolean destruido;
    private boolean irrompible;
    private int hp;

    private Texture texturaNormal;
    private Texture texturaResistente2;
    private Texture texturaResistente3;
    private Texture texturaIrrompible;

    public Bloque(int x, int y, int ancho, int alto, Texture tx1, Texture tx2, Texture tx3, Texture txU) {
        this(x, y, ancho, alto, 1, false, tx1, tx2, tx3, txU);
    }

    public Bloque(int x, int y, int ancho, int alto, int hp, boolean irrompible,
                  Texture tx1, Texture tx2, Texture tx3, Texture txU) {
        super(x, y, ancho, alto);
        this.irrompible = irrompible;
        this.hp = Math.max(1, hp);
        this.destruido = false;
        this.texturaNormal = tx1;
        this.texturaResistente2 = tx2;
        this.texturaResistente3 = tx3;
        this.texturaIrrompible = txU;
    }

    @Override
    public void actualizar() {
        // Sin lógica dinámica
    }

    @Override
    public Rectangle getRect() {
        return new Rectangle(x, y, ancho, alto);
    }

    @Override
    public void alChocarConBola(BolaPing bola) {
        recibirImpacto();
    }

    public void recibirImpacto() {
        if (destruido || irrompible) return;
        hp--;
        if (hp <= 0) destruido = true;
    }

    public boolean estaDestruido() { return destruido; }
    public boolean esIrrompible() { return irrompible; }
    public int getHp() { return hp; }

    public void destruir() {
        if (!irrompible) {
            destruido = true;
            hp = 0;
        }
    }

    @Override
    public void dibujar(SpriteBatch batch) {
        Texture texturaActual;
        if (irrompible) {
            texturaActual = texturaIrrompible;
        } else {
            if (hp == 3)      texturaActual = texturaResistente3;
            else if (hp == 2) texturaActual = texturaResistente2;
            else              texturaActual = texturaNormal;
        }
        if (texturaActual != null) {
            batch.draw(texturaActual, x, y, ancho, alto);
        }
    }
}