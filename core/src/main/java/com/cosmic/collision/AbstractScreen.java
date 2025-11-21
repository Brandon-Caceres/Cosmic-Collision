package com.cosmic.collision;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Template Method para pantallas. Define el esqueleto del ciclo.
 * Subclases implementan onUpdate/onDraw e opcionalmente onShow/onHide/onDispose.
 */
public abstract class AbstractScreen implements Screen {

    protected final SpriteBatch batch;

    protected AbstractScreen(SpriteBatch batch) {
        this.batch = batch;
    }

    @Override
    public final void show() {
        onShow();
    }

    @Override
    public final void render(float delta) {
        onUpdate(delta);
        onDraw(batch, delta);
    }

    @Override
    public final void resize(int width, int height) {
        // Hook opcional
    }

    @Override
    public final void pause() {}

    @Override
    public final void resume() {}

    @Override
    public final void hide() {
        onHide();
    }

    @Override
    public final void dispose() {
        onDispose();
    }

    // Métodos que deben / pueden implementar las subclases
    protected void onShow() {}
    protected abstract void onUpdate(float delta);
    protected abstract void onDraw(SpriteBatch batch, float delta);
    protected void onHide() {}
    protected void onDispose() {}
}