package com.cosmic.collision;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.Random;

public class Main extends ApplicationAdapter {

    private OrthographicCamera cam;
    private SpriteBatch batch;
    private BitmapFont fuente;
    private ShapeRenderer sr;

    private BolaPing bola;
    private Plataforma plat;
    private ArrayList<Bloque> bloques = new ArrayList<>();

    private int vidas;
    private int puntos;
    private int nivel;

    // Geometría de bloques (grid centrado)
    private int anchoBloq = 70;
    private int altoBloq = 26;
    private int espH = 12;
    private int espV = 12;
    private int margenLR = 16;
    private int margenTop = 20;

    // Probabilidades base (sin dificultad)
    private float probIrrompible = 0.05f;
    private float probDuro = 0.25f;

    @Override
    public void create() {
        cam = new OrthographicCamera();
        cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch = new SpriteBatch();
        fuente = new BitmapFont();
        fuente.getData().setScale(2.0f, 2.0f);
        sr = new ShapeRenderer();

        nivel = 1;
        puntos = 0;
        vidas = 3;

        int anchoInicial = 140;
        plat = new Plataforma((int)(cam.viewportWidth/2f - anchoInicial/2f), 40, anchoInicial, 12);
        plat.setVelPxPorSeg(580f);

        bola = new BolaPing(plat.getX() + plat.getAncho()/2 - 5, plat.getY() + plat.getAlto() + 11, 10, 4, 6, true);

        crearBloques(3 + Math.max(0, nivel - 1));
    }

    private void crearBloques(int filas) {
        bloques.clear();
        float w = cam.viewportWidth;
        int y = (int)cam.viewportHeight - margenTop;

        // columnas máximas ajustadas al ancho disponible
        int cols = Math.max(3, (int)Math.floor((w - 2*margenLR + espH) / (anchoBloq + espH)));
        float anchoFila = cols * anchoBloq + (cols - 1) * espH;
        int startX = Math.max(margenLR, (int)Math.round((w - anchoFila) / 2f));

        Random r = new Random(nivel * 1337L);
        for (int f = 0; f < filas; f++) {
            y -= (altoBloq + espV);
            if (y < 0) break;

            for (int c = 0; c < cols; c++) {
                int x = startX + c * (anchoBloq + espH);

                boolean mkIrromp = r.nextFloat() < probIrrompible;
                boolean mkDuro = !mkIrromp && (r.nextFloat() < probDuro);
                int hp = mkIrromp ? 1 : (mkDuro ? (r.nextFloat() < 0.5f ? 3 : 2) : 1);

                Color base = new Color(0.20f + r.nextFloat()*0.75f, 0.15f + r.nextFloat()*0.8f, 0.15f + r.nextFloat()*0.8f, 1f);
                bloques.add(new Bloque(x, y, anchoBloq, altoBloq, hp, mkIrromp, base));
            }
        }
    }

    private void dibujaHUD() {
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        fuente.draw(batch, "Puntos: " + puntos, 10, 25);
        fuente.draw(batch, "Vidas : " + vidas, cam.viewportWidth - 240, 25);
        fuente.draw(batch, "Nivel : " + nivel, cam.viewportWidth/2f - 60, 25);
        batch.end();
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // actualizar plataforma
        plat.actualizar();

        // dibujar
        sr.setProjectionMatrix(cam.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // plataforma
        plat.dibujar(sr);

        // bola (pegada hasta SPACE)
        if (bola.estaQuieta()) {
            bola.setXY(plat.getX() + plat.getAncho()/2 - 5, plat.getY() + plat.getAlto() + 11);
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) bola.setEstaQuieta(false);
        } else {
            bola.actualizar();
        }

        // caída por abajo
        if (bola.getY() < 0) {
            vidas--;
            bola = new BolaPing(plat.getX() + plat.getAncho()/2 - 5, plat.getY() + plat.getAlto() + 11, 10, 4 + nivel/3, 6 + nivel/3, true);
        }

        // game over => reinicio simple
        if (vidas <= 0) {
            vidas = 3; nivel = 1; puntos = 0;
            int anchoInicial = 140;
            plat = new Plataforma((int)(cam.viewportWidth/2f - anchoInicial/2f), 40, anchoInicial, 12);
            plat.setVelPxPorSeg(580f);
            crearBloques(3);
            sr.end();
            dibujaHUD();
            return;
        }

        // bloques
        for (Bloque b : bloques) {
            b.dibujar(sr);
            bola.comprobarColision(b);
        }

        // limpiar destruidos
        for (int i = 0; i < bloques.size(); i++) {
            if (bloques.get(i).estaDestruido()) {
                puntos++;
                bloques.remove(i);
                i--;
            }
        }

        // colisión con plataforma y bola
        bola.comprobarColision(plat);
        bola.dibujar(sr);

        sr.end();
        dibujaHUD();

        // siguiente nivel
        if (bloques.isEmpty()) {
            nivel++;
            // subir levemente dificultad con el nivel
            int nuevoAncho = Math.max(70, plat.getAncho() - 8);
            plat = new Plataforma(plat.getX(), plat.getY(), nuevoAncho, plat.getAlto());
            plat.setVelPxPorSeg(580f + nivel * 20);
            crearBloques(3 + Math.max(0, nivel - 1));
            bola = new BolaPing(plat.getX() + plat.getAncho()/2 - 5, plat.getY() + plat.getAlto() + 11, 10, 4 + nivel/3, 6 + nivel/3, true);
        }
    }

    @Override
    public void dispose() {
        // liberar si fuese necesario (batch, sr, fuente)
    }
}