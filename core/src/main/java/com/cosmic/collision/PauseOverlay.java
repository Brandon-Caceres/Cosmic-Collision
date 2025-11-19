package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Overlay de pausa con save/restore de escala para evitar efectos colaterales.
 */
public class PauseOverlay {

    public interface Listener {
        void onReanudar();
        void onReiniciarNivel();
        void onMenuPrincipal();
        void onSalir();
    }

    private final String[] opciones = { "Reanudar", "Reiniciar Nivel", "Menú Principal", "Salir" };
    private int seleccion = 0;
    private final BitmapFont fuente;
    private final GlyphLayout layout = new GlyphLayout();
    private final Listener listener;

    public PauseOverlay(BitmapFont fuente, Listener listener) {
        this.fuente = fuente;
        this.listener = listener;
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, float ancho, float alto) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, 0.55f);
        shapes.rect(0, 0, ancho, alto);
        shapes.end();

        float oldX = fuente.getData().scaleX, oldY = fuente.getData().scaleY;
        fuente.getData().setScale(2.0f);

        batch.begin();
        batch.setColor(1f,1f,1f,1f);

        layout.setText(fuente, "PAUSA");
        fuente.draw(batch, "PAUSA", (ancho - layout.width) / 2f, alto - 120);

        float y = alto - 200;
        float interlineado = 44f;
        for (int i = 0; i < opciones.length; i++) {
            String prefijo = (i == seleccion) ? "> " : "  ";
            String texto = prefijo + opciones[i];
            layout.setText(fuente, texto);
            fuente.draw(batch, texto, (ancho - layout.width) / 2f, y);
            y -= interlineado;
        }

        String pista = "ESC: Reanudar | ENTER: Aceptar | ARRIBA/ABAJO: Navegar";
        layout.setText(fuente, pista);
        fuente.draw(batch, pista, (ancho - layout.width) / 2f, 120);
        batch.end();

        fuente.getData().setScale(oldX, oldY);
    }

    public void handleInput() {
        if (just(Input.Keys.ESCAPE)) { listener.onReanudar(); return; }
        if (just(Input.Keys.UP))   seleccion = (seleccion - 1 + opciones.length) % opciones.length;
        if (just(Input.Keys.DOWN)) seleccion = (seleccion + 1) % opciones.length;
        if (just(Input.Keys.ENTER) || just(Input.Keys.SPACE)) {
            switch (seleccion) {
                case 0: listener.onReanudar();        break;
                case 1: listener.onReiniciarNivel();  break;
                case 2: listener.onMenuPrincipal();   break;
                case 3: listener.onSalir();           break;
            }
        }
    }

    private boolean just(int key) { return Gdx.input.isKeyJustPressed(key); }
}