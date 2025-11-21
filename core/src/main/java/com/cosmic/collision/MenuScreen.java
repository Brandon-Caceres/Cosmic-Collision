package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * MenuScreen refactorizada para extender AbstractScreen (Template Method).
 */
public class MenuScreen extends AbstractScreen {

    public interface Listener {
        void onElegirDificultad(Dificultad d);
        void onTutorial();
        void onCreditos();
    }

    private final BitmapFont fuente;
    private final GlyphLayout layout = new GlyphLayout();
    private final Listener listener;

    public MenuScreen(SpriteBatch batch, BitmapFont fuente, Listener listener) {
        super(batch);
        this.fuente = fuente;
        this.listener = listener;
    }

    @Override
    protected void onUpdate(float delta) {
        if (just(Input.Keys.NUM_1) || just(Input.Keys.F1)) listener.onElegirDificultad(Dificultad.FACIL);
        else if (just(Input.Keys.NUM_2) || just(Input.Keys.F2)) listener.onElegirDificultad(Dificultad.MEDIA);
        else if (just(Input.Keys.NUM_3) || just(Input.Keys.F3)) listener.onElegirDificultad(Dificultad.DIFICIL);
        else if (just(Input.Keys.NUM_4) || just(Input.Keys.F4)) listener.onTutorial();
        else if (just(Input.Keys.NUM_5) || just(Input.Keys.F5)) listener.onCreditos();
    }

    @Override
    protected void onDraw(SpriteBatch batch, float delta) {
        float oldX = fuente.getData().scaleX, oldY = fuente.getData().scaleY;
        fuente.getData().setScale(2.0f);

        batch.begin();
        batch.setColor(1f,1f,1f,1f);

        drawCentered(batch, "COSMIC COLLISION", Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - 60);
        drawCentered(batch, "Elige dificultad:", Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - 108);
        drawCentered(batch, "1 (F1) - FÁCIL   | Paleta grande, bola lenta", Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - 156);
        drawCentered(batch, "2 (F2) - MEDIA   | Más bloques, duros", Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - 204);
        drawCentered(batch, "3 (F3) - DIFÍCIL | Más bloques, duros e irrompibles", Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - 252);
        drawCentered(batch, "4 (F4) - TUTORIAL | Ver controles e instrucciones", Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - 300);
        drawCentered(batch, "5 (F5) - CRÉDITOS | Ver información del desarrollo", Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - 348);
        drawCentered(batch, "Controles: IZQ/DER, ESPACIO lanzar, ESC pausa", Gdx.graphics.getWidth(), 80);
        batch.end();

        fuente.getData().setScale(oldX, oldY);
    }

    private void drawCentered(SpriteBatch batch, String text, float ancho, float y) {
        layout.setText(fuente, text);
        fuente.draw(batch, text, (ancho - layout.width) / 2f, y);
    }

    private boolean just(int key) {
        return Gdx.input.isKeyJustPressed(key);
    }
}