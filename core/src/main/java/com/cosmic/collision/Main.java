package com.cosmic.collision;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.Random;

public class Main extends ApplicationAdapter {

    private enum Estado { MENU, JUGANDO, PAUSADO }

    private OrthographicCamera cam;
    private SpriteBatch batch;
    private BitmapFont fuente;
    private GlyphLayout gl;
    private ShapeRenderer sr;

    private BolaPing bola;
    private Plataforma plat;
    private ArrayList<Bloque> bloques = new ArrayList<>();

    private int vidas;
    private int puntos;
    private int nivel;

    private Estado estado = Estado.MENU;
    private Dificultad dificultad = Dificultad.FACIL;

    // Parámetros por dificultad
    private int filasBase = 3;
    private int filasPorNivel = 0;
    private int anchoPlatBase = 160;
    private int velBolaX = 2;
    private int velBolaY = 3;
    private float velPlat = 700f;

    // Geometría de bloques
    private int anchoBloq = 70;
    private int altoBloq = 26;
    private int espH = 16;
    private int espV = 14;
    private int margenLR = 20;
    private int margenTop = 20;

    private int colsObjetivoFacil = 8;

    // Tasas
    private float tasaDuros = 0.0f;
    private float tasaIrrompibles = 0.0f;
    private boolean permitirIrrompibles = false;

    // Pausa
    private final String[] opcionesPausa = { "Reanudar", "Reiniciar nivel", "Menu principal", "Salir" };
    private int selPausa = 0;
    private boolean acabaDeEntrarPausa = false;
    private long ultTogglePausaMs = 0;

    @Override
    public void create() {
        cam = new OrthographicCamera();
        cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch = new SpriteBatch();
        fuente = new BitmapFont(); fuente.getData().setScale(2f, 2f);
        gl = new GlyphLayout();
        sr = new ShapeRenderer();
        estado = Estado.MENU;
    }

    private boolean puedeTogglePausa() {
        long ahora = com.badlogic.gdx.utils.TimeUtils.millis();
        if (ahora - ultTogglePausaMs < 200) return false;
        ultTogglePausaMs = ahora;
        return true;
    }

    private void aplicarDificultad(Dificultad d) {
        dificultad = d;
        switch (d) {
            case FACIL:
                anchoPlatBase = 160; velBolaX = 2; velBolaY = 3; filasBase = 3; filasPorNivel = 0; velPlat = 700f;
                colsObjetivoFacil = 8; espH = 16; espV = 14; margenLR = 20; margenTop = 20;
                tasaDuros = 0.0f; tasaIrrompibles = 0.0f; permitirIrrompibles = false;
                break;
            case MEDIA:
                anchoPlatBase = 110; velBolaX = 4; velBolaY = 5; filasBase = 5; filasPorNivel = 1; velPlat = 1000f;
                anchoBloq = 70; altoBloq = 26; espH = 10; espV = 10; margenLR = 10; margenTop = 10;
                tasaDuros = 0.25f; tasaIrrompibles = 0.0f; permitirIrrompibles = false;
                break;
            case DIFICIL:
                anchoPlatBase = 90; velBolaX = 5; velBolaY = 6; filasBase = 7; filasPorNivel = 2; velPlat = 1500f;
                anchoBloq = 70; altoBloq = 26; espH = 10; espV = 10; margenLR = 10; margenTop = 10;
                tasaDuros = 0.40f; tasaIrrompibles = 0.15f; permitirIrrompibles = true;
                break;
        }
    }

    private void iniciarJuego() {
        puntos = 0; vidas = 3; nivel = 1;
        plat = new Plataforma((int)(cam.viewportWidth/2f - anchoPlatBase/2f), 40, anchoPlatBase, 12);
        plat.setVelPxPorSeg(velPlat);
        bola = new BolaPing((int)(cam.viewportWidth/2f - 10), plat.getY() + plat.getAlto() + 11, 10, velBolaX, velBolaY, true);
        crearBloques(calcularFilasNivel(nivel));
    }

    private int calcularFilasNivel(int n) { return filasBase + Math.max(0, (n - 1) * filasPorNivel); }

    public void crearBloques(int filas) {
        bloques.clear();
        int y = (int)cam.viewportHeight - margenTop;

        if (dificultad == Dificultad.FACIL) {
            int cols = Math.max(3, colsObjetivoFacil);
            float w = cam.viewportWidth;
            float disponibleW = w - (2 * margenLR) - (espH * (cols - 1));
            int bw = Math.max(40, (int)(disponibleW / cols));
            int bh = Math.max(26, (int)(bw * 0.38f));
            for (int f = 0; f < filas; f++) {
                y -= (bh + espV); if (y < 0) break;
                float anchoFila = cols * bw + (cols - 1) * espH;
                int startX = (int)Math.round((w - anchoFila) / 2f);
                for (int c = 0; c < cols; c++) {
                    int x = startX + c * (bw + espH);
                    bloques.add(new Bloque(x, y, bw, bh));
                }
            }
        } else {
            int bw = anchoBloq, bh = altoBloq;
            float w = cam.viewportWidth;
            float disponibleW = w - (2 * margenLR);
            int cols = Math.max(1, (int)Math.floor((disponibleW + espH) / (bw + espH)));
            float anchoFila = cols * bw + (cols - 1) * espH;
            int startX = Math.max(margenLR, (int)Math.round((w - anchoFila) / 2f));

            Random r = new Random(nivel * 9001L);
            for (int f = 0; f < filas; f++) {
                y -= (bh + espV); if (y < 0) break;
                for (int c = 0; c < cols; c++) {
                    int x = startX + c * (bw + espH);
                    boolean mkIrromp = permitirIrrompibles && (r.nextFloat() < tasaIrrompibles);
                    boolean mkDuro = !mkIrromp && (r.nextFloat() < tasaDuros);
                    if (mkIrromp) bloques.add(new Bloque(x, y, bw, bh, 1, true));
                    else if (mkDuro) {
                        int hp = (dificultad == Dificultad.DIFICIL) ? (r.nextFloat() < 0.5f ? 3 : 2) : 2;
                        bloques.add(new Bloque(x, y, bw, bh, hp, false));
                    } else bloques.add(new Bloque(x, y, bw, bh));
                }
            }
        }
    }

    private void dibujarMenu() {
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        float w = cam.viewportWidth, h = cam.viewportHeight;
        String titulo = "COSMIC COLLISION";
        String sub = "Elige dificultad:";
        String o1 = "1 (F1) - FACIL   | Paleta grande, bola lenta";
        String o2 = "2 (F2) - MEDIA   | Mas bloques, duros";
        String o3 = "3 (F3) - DIFICIL | Mas bloques, duros e irrompibles";
        String cont = "Controles: LEFT/RIGHT, SPACE lanzar, ESC pausa";

        float y = h - 60, linea = 48f;

        gl.setText(fuente, titulo); fuente.draw(batch, titulo, (w - gl.width) / 2f, y); y -= linea * 1.2f;
        gl.setText(fuente, sub);    fuente.draw(batch, sub,    (w - gl.width) / 2f, y); y -= linea;
        gl.setText(fuente, o1);     fuente.draw(batch, o1,     (w - gl.width) / 2f, y); y -= linea;
        gl.setText(fuente, o2);     fuente.draw(batch, o2,     (w - gl.width) / 2f, y); y -= linea;
        gl.setText(fuente, o3);     fuente.draw(batch, o3,     (w - gl.width) / 2f, y); y -= linea * 1.5f;
        gl.setText(fuente, cont);   fuente.draw(batch, cont,   (w - gl.width) / 2f, 80);

        batch.end();
    }

    private void manejarInputMenu() {
        boolean f1 = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.F1);
        boolean f2 = Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.F2);
        boolean f3 = Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.F3);

        if (f1) { aplicarDificultad(Dificultad.FACIL); iniciarJuego(); estado = Estado.JUGANDO; }
        else if (f2) { aplicarDificultad(Dificultad.MEDIA); iniciarJuego(); estado = Estado.JUGANDO; }
        else if (f3) { aplicarDificultad(Dificultad.DIFICIL); iniciarJuego(); estado = Estado.JUGANDO; }
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (estado == Estado.MENU) { dibujarMenu(); manejarInputMenu(); return; }

        if (estado == Estado.JUGANDO && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && puedeTogglePausa()) {
            estado = Estado.PAUSADO; selPausa = 0; acabaDeEntrarPausa = true;
        }

        if (estado == Estado.JUGANDO) { renderFrameJuego(true); return; }

        if (estado == Estado.PAUSADO) {
            renderFrameJuego(false);
            dibujarOverlayPausa();
            manejarInputPausa();
        }
    }

    private void renderFrameJuego(boolean actualizar) {
        cam.update();
        sr.setProjectionMatrix(cam.combined);

        if (actualizar) plat.actualizar();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        plat.dibujar(sr);

        if (bola.estaQuieta()) {
            bola.setXY(plat.getX() + plat.getAncho()/2 - 5, plat.getY() + plat.getAlto() + 11);
            if (actualizar && Gdx.input.isKeyPressed(Input.Keys.SPACE)) bola.setEstaQuieta(false);
        } else if (actualizar) {
            bola.actualizar();
        }

        if (actualizar && bola.getY() < 0) {
            vidas--;
            bola = new BolaPing(plat.getX() + plat.getAncho()/2 - 5, plat.getY() + plat.getAlto() + 11, 10, velBolaX, velBolaY, true);
        }

        if (actualizar && vidas <= 0) {
            estado = Estado.MENU;
            bloques.clear();
            sr.end();
            return;
        }

        for (Bloque b : bloques) {
            b.dibujar(sr);
            if (actualizar) bola.comprobarColision(b);
        }

        if (actualizar) {
            for (int i = 0; i < bloques.size(); i++) {
                if (bloques.get(i).estaDestruido()) {
                    puntos++;
                    bloques.remove(i);
                    i--;
                }
            }
        }

        if (actualizar) bola.comprobarColision(plat);
        bola.dibujar(sr);
        sr.end();

        dibujaHUD();

        if (actualizar && bloques.isEmpty()) {
            nivel++;
            if (dificultad != Dificultad.FACIL) {
                velBolaX += (velBolaX > 0 ? 1 : -1);
                velBolaY += (velBolaY > 0 ? 1 : -1);
                int dec = (dificultad == Dificultad.DIFICIL) ? 12 : 8;
                int nuevoAncho = Math.max(60, plat.getAncho() - dec);
                plat = new Plataforma(plat.getX(), plat.getY(), nuevoAncho, plat.getAlto());
                plat.setVelPxPorSeg(velPlat);
            }
            crearBloques(calcularFilasNivel(nivel));
            bola = new BolaPing(plat.getX() + plat.getAncho()/2 - 5, plat.getY() + plat.getAlto() + 11, 10, velBolaX, velBolaY, true);
        }
    }

    private void dibujaHUD() {
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        fuente.draw(batch, "Puntos: " + puntos, 10, 25);
        fuente.draw(batch, "Vidas : " + vidas, cam.viewportWidth - 240, 25);
        fuente.draw(batch, "Nivel : " + nivel, cam.viewportWidth/2f - 60, 25);
        fuente.draw(batch, "Dif   : " + dificultad, cam.viewportWidth/2f + 120, 25);
        batch.end();
    }

    private void dibujarOverlayPausa() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.setProjectionMatrix(cam.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.55f);
        sr.rect(0, 0, cam.viewportWidth, cam.viewportHeight);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        float w = cam.viewportWidth, h = cam.viewportHeight;

        String titulo = "PAUSA";
        gl.setText(fuente, titulo);
        fuente.draw(batch, titulo, (w - gl.width) / 2f, h - 120);

        float y = h - 200, linea = 44f;
        for (int i = 0; i < opcionesPausa.length; i++) {
            String pref = (i == selPausa) ? "> " : "  ";
            String txt = pref + opcionesPausa[i];
            gl.setText(fuente, txt);
            fuente.draw(batch, txt, (w - gl.width) / 2f, y);
            y -= linea;
        }

        String pista = "ESC: Reanudar  |  ENTER: Aceptar  |  UP/DOWN: Navegar";
        gl.setText(fuente, pista);
        fuente.draw(batch, pista, (w - gl.width) / 2f, 120);

        batch.end();
    }

    private void manejarInputPausa() {
        if (acabaDeEntrarPausa) { acabaDeEntrarPausa = false; return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && puedeTogglePausa()) { estado = Estado.JUGANDO; return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))   selPausa = (selPausa - 1 + opcionesPausa.length) % opcionesPausa.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) selPausa = (selPausa + 1) % opcionesPausa.length;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            switch (selPausa) {
                case 0: estado = Estado.JUGANDO; break;
                case 1:
                    crearBloques(calcularFilasNivel(nivel));
                    bola = new BolaPing(plat.getX() + plat.getAncho()/2 - 5, plat.getY() + plat.getAlto() + 11, 10, velBolaX, velBolaY, true);
                    estado = Estado.JUGANDO;
                    break;
                case 2: estado = Estado.MENU; bloques.clear(); break;
                case 3: Gdx.app.exit(); break;
            }
        }
    }
}