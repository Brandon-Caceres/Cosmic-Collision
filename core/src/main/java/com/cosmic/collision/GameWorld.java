package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.List;

/**
 * Mundo de juego: maneja paleta, pelota, bloques, avance de nivel y vidas extra.
 */
public class GameWorld {

    private Plataforma paleta;
    private BolaPing pelota;
    private List<Bloque> bloques = new ArrayList<>();
    private final BlockFactory blockFactory;
    private DifficultySettings settings;
    private final HUD hud;

    private int puntaje;
    private int vidas;
    private int nivel;

    private int velPelotaX;
    private int velPelotaY;

    private long mostrarBonificacionVidaHastaMs;
    private final long duracionBonificacionVida;

    private com.badlogic.gdx.graphics.Texture texturaPaleta;

    public GameWorld(BlockFactory factory, HUD hud, DifficultySettings initialSettings,
                     long duracionBonificacionVida, com.badlogic.gdx.graphics.Texture texturaPaleta) {
        this.blockFactory = factory;
        this.hud = hud;
        this.settings = initialSettings;
        this.duracionBonificacionVida = duracionBonificacionVida;
        this.texturaPaleta = texturaPaleta;
        iniciarJuego();
    }

    public void aplicarDificultad(DifficultySettings nueva) {
        this.settings = nueva;
    }

    public void iniciarJuego() {
        puntaje = 0;
        vidas = 3;
        nivel = 1;
        velPelotaX = settings.velPelotaX;
        velPelotaY = settings.velPelotaY;

        paleta = new Plataforma(
                (int)(Gdx.graphics.getWidth()/2f - settings.anchoBasePaleta/2f),
                40,
                settings.anchoBasePaleta,
                40,
                texturaPaleta
        );
        paleta.setVelPxPorSeg(settings.velocidadPaleta);

        pelota = new BolaPing(
                (int)(Gdx.graphics.getWidth()/2f - 10),
                paleta.getY() + paleta.getAlto() + 11,
                10,
                velPelotaX,
                velPelotaY,
                true
        );
        crearBloques(filasParaNivel(nivel));
    }

    private int filasParaNivel(int nivelActual) {
        return settings.filasBase + Math.max(0, (nivelActual - 1) * settings.incrementoFilasPorNivel);
    }

    public void crearBloques(int filas) {
        bloques.clear();
        bloques.addAll(blockFactory.crearBloques(
                filas,
                settings,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        ));
    }

    public void actualizar() {
        paleta.actualizar();

        if (pelota.estaQuieta()) {
            pelota.setXY(
                    paleta.getX() + paleta.getAncho()/2 - 5,
                    paleta.getY() + paleta.getAlto() + 11
            );
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) pelota.setEstaQuieta(false);
        } else {
            pelota.actualizar();
        }

        if (pelota.getY() < 0) {
            vidas--;
            pelota = new BolaPing(
                    paleta.getX() + paleta.getAncho()/2 - 5,
                    paleta.getY() + paleta.getAlto() + 11,
                    10,
                    velPelotaX,
                    velPelotaY,
                    true
            );
        }

        for (Bloque b : bloques) {
            pelota.comprobarColision(b);
        }
        pelota.comprobarColision(paleta);

        for (int i = 0; i < bloques.size(); i++) {
            Bloque b = bloques.get(i);
            if (b.estaDestruido()) {
                puntaje++;
                bloques.remove(i);
                i--;
            }
        }

        if (bloques.isEmpty()) {
            nivel++;
            if (settings.dificultad == Dificultad.MEDIA) {
                velPelotaX += (velPelotaX > 0 ? 1 : -1);
                velPelotaY += (velPelotaY > 0 ? 1 : -1);
                int nuevoAncho = Math.max(70, paleta.getAncho() - 8);
                paleta = new Plataforma(paleta.getX(), paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
                paleta.setVelPxPorSeg(settings.velocidadPaleta);
            } else if (settings.dificultad == Dificultad.DIFICIL) {
                velPelotaX += (velPelotaX > 0 ? 1 : -1);
                velPelotaY += (velPelotaY > 0 ? 1 : -1);
                int nuevoAncho = Math.max(60, paleta.getAncho() - 12);
                paleta = new Plataforma(paleta.getX(), paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
                paleta.setVelPxPorSeg(settings.velocidadPaleta);
            }

            double probVidaExtra;
            switch (settings.dificultad) {
                case FACIL:  probVidaExtra = 0.25; break;
                case MEDIA:  probVidaExtra = 0.50; break;
                case DIFICIL: probVidaExtra = 1.0;  break;
                default: probVidaExtra = 0.0;
            }
            if (Math.random() < probVidaExtra) {
                vidas++;
                mostrarBonificacionVidaHastaMs = com.badlogic.gdx.utils.TimeUtils.millis() + duracionBonificacionVida;
            }

            crearBloques(filasParaNivel(nivel));
            pelota = new BolaPing(
                    paleta.getX() + paleta.getAncho()/2 - 5,
                    paleta.getY() + paleta.getAlto() + 11,
                    10,
                    velPelotaX,
                    velPelotaY,
                    true
            );
        }
    }

    public void dibujar(SpriteBatch batch, ShapeRenderer sr, float ancho, float alto) {
        batch.begin();
        paleta.dibujar(batch);
        for (Bloque b : bloques) {
            b.dibujar(batch);
        }
        hud.dibujar(batch, ancho, alto, puntaje, vidas, nivel, settings.dificultad,
                mostrarBonificacionVidaHastaMs, com.badlogic.gdx.utils.TimeUtils.millis());
        batch.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        pelota.dibujar(sr);
        sr.end();
    }

    public int getVidas() { return vidas; }
    public int getNivel() { return nivel; }
    public int getPuntaje() { return puntaje; }
    public DifficultySettings getSettings() { return settings; }

    public void reiniciarNivel() {
        crearBloques(filasParaNivel(nivel));
        pelota = new BolaPing(
                paleta.getX() + paleta.getAncho()/2 - 5,
                paleta.getY() + paleta.getAlto() + 11,
                10,
                velPelotaX,
                velPelotaY,
                true
        );
        paleta = new Plataforma(paleta.getX(), paleta.getY(), paleta.getAncho(), paleta.getAlto(), texturaPaleta);
        paleta.setVelPxPorSeg(settings.velocidadPaleta);
    }
}