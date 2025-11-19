package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Ítem que cae y aplica un efecto al ser capturado por la paleta.
 */
public class PowerUp extends ObjetoJuego {

    private final PowerUpType tipo;
    private final float velYPxPorSeg; // velocidad de caída
    private final Color color;

    public PowerUp(int x, int y, int ancho, int alto, PowerUpType tipo) {
        super(x, y, ancho, alto);
        this.tipo = tipo;
        this.velYPxPorSeg = 140f;
        this.color = colorPorTipo(tipo);
    }

    public PowerUpType getTipo() { return tipo; }

    public Rectangle getRect() {
        return new Rectangle(x, y, ancho, alto);
    }

    @Override
    public void actualizar() {
        float dt = Gdx.graphics.getDeltaTime();
        y -= velYPxPorSeg * dt;
    }

    @Override
    public void dibujar(ShapeRenderer sr) {
        sr.setColor(color);
        sr.rect(x, y, ancho, alto);
    }

    private Color colorPorTipo(PowerUpType t) {
        switch (t) {
            case EXPLOSIVE_BALL: return new Color(1f, 0.40f, 0.0f, 1f);
            case PADDLE_GROW:    return new Color(0.2f, 0.8f, 0.2f, 1f);
            case PADDLE_SHRINK:  return new Color(0.8f, 0.2f, 0.2f, 1f);
            case EXTRA_LIFE:     return new Color(0.2f, 0.6f, 1f, 1f);
            default:             return Color.WHITE;
        }
    }
}