package com.cosmic.collision;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * HUD: No cambia la escala de la fuente. Dibuja siempre con su propia fuente.
 */
public class HUD {
    private final BitmapFont fuente;
    private final GlyphLayout layout = new GlyphLayout();

    public HUD(BitmapFont fuente) {
        this.fuente = fuente;
    }

    public void dibujar(SpriteBatch batch,
                        float ancho, float alto,
                        int puntaje, int vidas, int nivel, Dificultad dif,
                        long mostrarBonificacionVidaHastaMs, long ahora) {

        batch.setColor(1f,1f,1f,1f);

        fuente.draw(batch, "Puntos: " + puntaje, 10, 25);
        fuente.draw(batch, "Vidas : " + vidas, ancho - 240, 25);
        fuente.draw(batch, "Nivel : " + nivel, ancho/2f - 60, 25);
        fuente.draw(batch, "Dif   : " + dif, ancho/2f + 120, 25);

        if (ahora < mostrarBonificacionVidaHastaMs) {
            String msg = "¡VIDA EXTRA CONSEGUIDA!";
            layout.setText(fuente, msg);
            fuente.draw(batch, msg, (ancho - layout.width) / 2f, alto / 2f);
        }
    }
}