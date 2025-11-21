package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton para gestionar Textures y BitmapFonts.
 * Uso: ResourceManager.getInstance().getTexture("paleta","nave.png");
 */
public final class ResourceManager {
    private static ResourceManager instance;
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, BitmapFont> fonts = new HashMap<>();

    private ResourceManager() {}

    public static synchronized ResourceManager getInstance() {
        if (instance == null) instance = new ResourceManager();
        return instance;
    }

    public Texture getTexture(String key, String assetPath) {
        if (!textures.containsKey(key)) {
            textures.put(key, new Texture(Gdx.files.internal(assetPath)));
        }
        return textures.get(key);
    }

    /**
     * Devuelve una textura ya registrada (puede ser null).
     */
    public Texture getTexture(String key) {
        return textures.get(key);
    }

    public BitmapFont getFont(String key, String assetPath) {
        if (!fonts.containsKey(key)) {
            fonts.put(key, new BitmapFont(Gdx.files.internal(assetPath)));
        }
        return fonts.get(key);
    }

    public BitmapFont getFont(String key) {
        return fonts.get(key);
    }

    /**
     * Libera todos los recursos gestionados.
     */
    public void dispose() {
        for (Texture t : textures.values()) {
            try { t.dispose(); } catch (Exception ignored) {}
        }
        textures.clear();
        for (BitmapFont f : fonts.values()) {
            try { f.dispose(); } catch (Exception ignored) {}
        }
        fonts.clear();
        instance = null;
    }
}