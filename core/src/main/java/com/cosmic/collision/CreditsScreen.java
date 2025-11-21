package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Créditos con scroll, ahora como AbstractScreen.
 */
public class CreditsScreen extends AbstractScreen {

    public interface Listener { void onSalirMenu(); }

    private final BitmapFont fuente;
    private final GlyphLayout layout = new GlyphLayout();
    private final Listener listener;

    private List<String> lineas = new ArrayList<>();
    private float desplazamientoY;
    private float velocidadScroll = 35f;
    private float umbralReinicioY;
    private final float margenExtra = 50f;

    private static final float ESCALA_TEXTO = 2.0f;
    private static final float ESCALA_PISTA = 1.5f;

    public CreditsScreen(SpriteBatch batch, BitmapFont fuente, Listener listener) {
        super(batch);
        this.fuente = fuente;
        this.listener = listener;
        construirTexto();
        reiniciar(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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

    public void reiniciar(float ancho, float alto) {
        float oldX = fuente.getData().scaleX, oldY = fuente.getData().scaleY;

        fuente.getData().setScale(ESCALA_TEXTO);
        float interlineado = fuente.getLineHeight() * 1.15f;
        float altoBloque = interlineado * (lineas.size() - 1);
        desplazamientoY = -30f;
        umbralReinicioY = alto + margenExtra + altoBloque;

        fuente.getData().setScale(oldX, oldY);
    }

    @Override
    protected void onUpdate(float delta) {
        desplazamientoY += velocidadScroll * delta;
        if (desplazamientoY > umbralReinicioY) {
            reiniciar(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        if (just(Input.Keys.ESCAPE) || just(Input.Keys.ENTER)) {
            listener.onSalirMenu();
        }
    }

    @Override
    protected void onDraw(SpriteBatch batch, float delta) {
        float oldX = fuente.getData().scaleX, oldY = fuente.getData().scaleY;

        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);

        fuente.getData().setScale(ESCALA_TEXTO);
        float interlineado = fuente.getLineHeight() * 1.15f;

        float y = desplazamientoY;
        for (String s : lineas) {
            layout.setText(fuente, s);
            float x = (Gdx.graphics.getWidth() - layout.width) / 2f;
            fuente.draw(batch, s, x, y);
            y -= interlineado;
        }

        fuente.getData().setScale(ESCALA_PISTA);
        String pista = "Presiona ENTER o ESC para volver al menú...";
        layout.setText(fuente, pista);
        fuente.draw(batch, pista, (Gdx.graphics.getWidth() - layout.width) / 2f, 80);

        batch.end();

        fuente.getData().setScale(oldX, oldY);
    }

    private boolean just(int key) { return Gdx.input.isKeyJustPressed(key); }
}