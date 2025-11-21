package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * Ítem que cae y aplica un efecto al ser capturado por la paleta.
 * No implementa Colisionable (la bola no debe interactuar con él).
 * Tiene un breve delay tras generarse antes de poder ser recogido por la paleta,
 * para evitar solapamientos instantáneos que parezcan "rebotar" la bola.
 */
public class PowerUp extends ObjetoJuego {

    private final PowerUpType tipo;
    private final float velYPxPorSeg; // velocidad de caída
    private final Color color;

    // control de availability
    private final long spawnTimeMs;
    private final long pickupDelayMs; // tiempo en ms antes de poder ser recogido

    public PowerUp(int x, int y, int ancho, int alto, PowerUpType tipo) {
        super(x, y, ancho, alto);
        this.tipo = tipo;
        this.velYPxPorSeg = 140f;
        this.color = colorPorTipo(tipo);

        this.spawnTimeMs = TimeUtils.millis();
        this.pickupDelayMs = 150; // 150 ms; ajustar si quieres más/menos
    }

    public PowerUpType getTipo() { return tipo; }

    public Rectangle getRect() { return new Rectangle(x, y, ancho, alto); }

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
            case EXPLOSIVE_BALL: return new Color(1f, 0.40f, 0.0f, 1f);   // naranja
            case PADDLE_GROW:    return new Color(0.2f, 0.8f, 0.2f, 1f);   // verde
            case PADDLE_SHRINK:  return new Color(0.8f, 0.2f, 0.2f, 1f);   // rojo
            case EXTRA_LIFE:     return new Color(0.2f, 0.6f, 1f, 1f);     // celeste
            case SPLIT_BALL:     return new Color(1f, 1f, 0.2f, 1f);       // amarillo claro
            case SPEED_UP:       return new Color(0.9f, 0.6f, 0.0f, 1f);   // dorado
            case SPEED_DOWN:     return new Color(0.3f, 0.5f, 0.9f, 1f);   // azul oscuro
            default:             return Color.WHITE;
        }
    }

    /**
     * Indica si ya ha pasado el delay y puede ser recogido por la paleta.
     */
    public boolean isActive() {
        return TimeUtils.millis() >= (spawnTimeMs + pickupDelayMs);
    }

    // Exponer getters simples (ya que ObjetoJuego tiene campos protegidos)
    public int getY() { return y; }
    public int getAlto() { return alto; }
}