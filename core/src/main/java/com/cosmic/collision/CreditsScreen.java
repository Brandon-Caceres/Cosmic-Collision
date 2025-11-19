package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Créditos con scroll.
 * - Dibuja línea por línea hacia ABAJO (y -= interlineado) para que el título quede arriba y el contenido debajo.
 * - Arranca cerca del borde inferior para que el texto aparezca de inmediato.
 */
public class CreditsScreen {

    public interface Listener { void onSalirMenu(); }

    private final BitmapFont fuente;
    private final GlyphLayout layout = new GlyphLayout();
    private final Listener listener;

    private List<String> lineas = new ArrayList<>();
    private float desplazamientoY;

    // Velocidad del scroll (px/seg). Ajusta a gusto.
    private float velocidadScroll = 35f;

    // Cuando la última línea sobrepasa esta Y, se reinicia el scroll
    private float umbralReinicioY;
    private final float margenExtra = 50f;

    // Escalas usadas (se restauran tras render)
    private static final float ESCALA_TEXTO = 2.0f;
    private static final float ESCALA_PISTA = 1.5f;

    public CreditsScreen(BitmapFont fuente, Listener listener) {
        this.fuente = fuente;
        this.listener = listener;
        construirTexto();
    }

    private void construirTexto() {
        lineas.clear();
        lineas.add("LOS DEL MOLINOGEA EN ADA PRESENTAN:");
        lineas.add("");
        lineas.add("BLOCKBREAKER - NUESTRA VERSIÓN");
        lineas.add("");
        lineas.add("------------------------------------------");
        lineas.add("");
        lineas.add("EQUIPO DE PROGRAMACIÓN:");
        lineas.add("  BRANDON CÁCERES");
        lineas.add("  JOSUÉ HUAIQUIL");
        lineas.add("  IGNACIO MUÑOZ");
        lineas.add("");
        lineas.add("DISEÑADOR:");
        lineas.add("  JOSUÉ HUAIQUIL");
        lineas.add("");
        lineas.add("ESCRITOR:");
        lineas.add("  IGNACIO MUÑOZ");
        lineas.add("");
        lineas.add("LENGUAJE USADO:");
        lineas.add("  JAVA");
        lineas.add("");
        lineas.add("HERRAMIENTAS USADAS:");
        lineas.add("  ECLIPSE");
        lineas.add("  LIBGDX");
        lineas.add("  GITHUB");
        lineas.add("");
        lineas.add("------------------------------------------");
        lineas.add("");
        lineas.add("¡GRACIAS POR JUGAR!");
    }

    /**
     * Llamar al entrar a Créditos para reiniciar el scroll con el tamaño actual.
     */
    public void reiniciar(float ancho, float alto) {
        float oldX = fuente.getData().scaleX, oldY = fuente.getData().scaleY;

        fuente.getData().setScale(ESCALA_TEXTO);

        // Interlineado consistente con la fuente
        float interlineado = fuente.getLineHeight() * 1.15f;

        // Altura total del bloque en "líneas" (sin sumar capHeight extra, ya que el orden es descendente)
        float altoBloque = interlineado * (lineas.size() - 1);

        // La PRIMERA línea (título) empezará apenas debajo del borde inferior, así entra rápido
        desplazamientoY = -30f;

        // Reinicia cuando la ÚLTIMA línea sobrepasa la parte superior
        // Condición: yUltima = desplazamientoY - altoBloque > alto + margen
        // => desplazamientoY > alto + margen + altoBloque
        umbralReinicioY = alto + margenExtra + altoBloque;

        fuente.getData().setScale(oldX, oldY);
    }

    public void render(SpriteBatch batch, float ancho, float alto) {
        float oldX = fuente.getData().scaleX, oldY = fuente.getData().scaleY;

        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);

        fuente.getData().setScale(ESCALA_TEXTO);
        float interlineado = fuente.getLineHeight() * 1.15f;

        // Dibuja título arriba y contenido debajo (y disminuye en cada línea)
        float y = desplazamientoY;
        for (String s : lineas) {
            layout.setText(fuente, s);
            float x = (ancho - layout.width) / 2f;
            fuente.draw(batch, s, x, y);
            y -= interlineado;
        }

        // Pista fija abajo
        fuente.getData().setScale(ESCALA_PISTA);
        String pista = "Presiona ENTER o ESC para volver al menú...";
        layout.setText(fuente, pista);
        fuente.draw(batch, pista, (ancho - layout.width) / 2f, 80);

        batch.end();

        fuente.getData().setScale(oldX, oldY);
    }

    public void actualizar(float delta) {
        desplazamientoY += velocidadScroll * delta;

        // Cuando la última línea ya pasó el borde superior, reinicia
        if (desplazamientoY > umbralReinicioY) {
            reiniciar(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    public void handleInput() {
        if (just(Input.Keys.ESCAPE) || just(Input.Keys.ENTER)) {
            listener.onSalirMenu();
        }
    }

    private boolean just(int key) { return Gdx.input.isKeyJustPressed(key); }

    // Permite ajustar la velocidad sin recompilar, si lo deseas.
    public void setVelocidadScroll(float pxPorSegundo) {
        this.velocidadScroll = Math.max(5f, pxPorSegundo);
    }
}