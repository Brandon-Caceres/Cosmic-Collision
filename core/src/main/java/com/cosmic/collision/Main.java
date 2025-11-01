package com.cosmic.collision;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;

public class Main extends ApplicationAdapter {

    private enum EstadoJuego { MENU, JUGANDO, PAUSADO, TUTORIAL, FIN_DE_JUEGO, CREDITOS }

    private OrthographicCamera camara;
    private SpriteBatch loteSprites;
    private BitmapFont fuente;
    private GlyphLayout gl;
    private ShapeRenderer sr;

    private Texture texFondo, texPaleta, txN, txD2, txD3, txU;

    private long mostrarBonificacionVidaHastaMs = 0;
    private final long duracionBonificacionVida = 1500;

    private final int MAX_PAGINAS_TUTORIAL = 3;
    private int paginaTutorialActual = 1;

    private String textoCreditos = "Cosmic Collision\n\nEquipo de Desarrollo\n\nGracias por jugar!";
    private float desplazamientoCreditosY = -20f;
    private float velocidadDesplazamientoCreditos = 70f;
    private float lineaFinCreditos = 0f;

    private BolaPing pelota;
    private Plataforma paleta;
    private ArrayList<Bloque> bloques = new ArrayList<>();

    private int vidas;
    private int puntaje;
    private int nivel;

    private EstadoJuego estado = EstadoJuego.MENU;
    private Dificultad dificultad = Dificultad.FACIL;

    private int filasBase = 3;
    private int incrementoFilasPorNivel = 0;
    private int anchoBasePaleta = 160;
    private int velX = 2;
    private int velY = 3;
    private float velPaleta = 700f;

    private int anchoBloq = 70, altoBloq = 26, espH = 16, espV = 14, margenLR = 20, margenTop = 20;
    private int colsObjetivoFacil = 8;

    private float tasaDuros = 0f, tasaIrrompibles = 0f; private boolean permitirIrrompibles = false;

    private final String[] opcionesPausa = { "Reanudar", "Reiniciar nivel", "Menu principal", "Salir" };
    private int selPausa = 0; private boolean acabaDeEntrarPausa = false; private long ultTogglePausaMs = 0;

    @Override
    public void create() {
        camara = new OrthographicCamera();
        camara.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        loteSprites = new SpriteBatch();
        fuente = new BitmapFont(); fuente.getData().setScale(2f, 2f);
        gl = new GlyphLayout();
        sr = new ShapeRenderer();

        // Texturas sólidas en memoria
        texFondo = tex(1,1,new Color(0.05f,0.05f,0.08f,1f));
        texPaleta = tex(1,1,new Color(0.2f,0.55f,0.95f,1f));
        txN = tex(1,1,new Color(0.82f,0.82f,0.85f,1f));
        txD2 = tex(1,1,new Color(0.65f,0.65f,0.70f,1f));
        txD3 = tex(1,1,new Color(0.48f,0.48f,0.52f,1f));
        txU = tex(1,1,new Color(0.25f,0.25f,0.28f,1f));

        estado = EstadoJuego.MENU;
        aplicarDificultad(Dificultad.FACIL);
    }

    private Texture tex(int w,int h, Color c){
        Pixmap pm = new Pixmap(w,h, Pixmap.Format.RGBA8888);
        pm.setColor(c); pm.fill();
        Texture t = new Texture(pm); pm.dispose();
        return t;
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
                anchoBasePaleta = 160; velX = 2; velY = 3; filasBase = 3; incrementoFilasPorNivel = 0; velPaleta = 700f;
                colsObjetivoFacil = 8; espH = 16; espV = 14; margenLR = 20; margenTop = 20;
                tasaDuros = 0.0f; tasaIrrompibles = 0.0f; permitirIrrompibles = false;
                break;
            case MEDIA:
                anchoBasePaleta = 110; velX = 4; velY = 5; filasBase = 5; incrementoFilasPorNivel = 1; velPaleta = 1000f;
                anchoBloq = 70; altoBloq = 26; espH = 10; espV = 10; margenLR = 10; margenTop = 10;
                tasaDuros = 0.25f; tasaIrrompibles = 0.0f; permitirIrrompibles = false;
                break;
            case DIFICIL:
                anchoBasePaleta = 90; velX = 5; velY = 6; filasBase = 7; incrementoFilasPorNivel = 2; velPaleta = 1500f;
                anchoBloq = 70; altoBloq = 26; espH = 10; espV = 10; margenLR = 10; margenTop = 10;
                tasaDuros = 0.40f; tasaIrrompibles = 0.15f; permitirIrrompibles = true;
                break;
        }
    }

    private void iniciarJuego() {
        puntaje = 0; vidas = 3; nivel = 1;
        paleta = new Plataforma((int)(camara.viewportWidth/2f - anchoBasePaleta/2f), 40, anchoBasePaleta, 12, texPaleta);
        paleta.setVelPxPorSeg(velPaleta);
        pelota = new BolaPing((int)(camara.viewportWidth/2f - 10), paleta.getY() + paleta.getAlto() + 11, 10, velX, velY, true);
        crearBloques(calcularFilasNivel(nivel));
    }

    private int calcularFilasNivel(int n) { return filasBase + Math.max(0, (n - 1) * incrementoFilasPorNivel); }

    public void crearBloques(int filas) {
        bloques.clear();
        int y = (int)camara.viewportHeight - margenTop;

        if (dificultad == Dificultad.FACIL) {
            int cols = Math.max(3, colsObjetivoFacil);
            float w = camara.viewportWidth;
            float disponibleW = w - (2 * margenLR) - (espH * (cols - 1));
            int bw = Math.max(40, (int)(disponibleW / cols));
            int bh = Math.max(26, (int)(bw * 0.38f));
            for (int f = 0; f < filas; f++) {
                y -= (bh + espV); if (y < 0) break;
                float anchoFila = cols * bw + (cols - 1) * espH;
                int startX = (int)Math.round((w - anchoFila) / 2f);
                for (int c = 0; c < cols; c++) {
                    int x = startX + c * (bw + espH);
                    bloques.add(new Bloque(x, y, bw, bh, txN, txD2, txD3, txU));
                }
            }
        } else {
            int bw = anchoBloq, bh = altoBloq;
            float w = camara.viewportWidth;
            float disponibleW = w - (2 * margenLR);
            int cols = Math.max(1, (int)Math.floor((disponibleW + espH) / (bw + espH)));
            float anchoFila = cols * bw + (cols - 1) * espH;
            int startX = Math.max(margenLR, (int)Math.round((w - anchoFila) / 2f));

            for (int f = 0; f < filas; f++) {
                y -= (bh + espV); if (y < 0) break;
                for (int c = 0; c < cols; c++) {
                    int x = startX + c * (bw + espH);
                    boolean mkIrromp = permitirIrrompibles && Math.random() < tasaIrrompibles;
                    boolean mkDuro = !mkIrromp && Math.random() < tasaDuros;
                    if (mkIrromp) bloques.add(new Bloque(x, y, bw, bh, 1, true, txN, txD2, txD3, txU));
                    else if (mkDuro) {
                        int hp = (dificultad == Dificultad.DIFICIL) ? (Math.random() < 0.5 ? 3 : 2) : 2;
                        bloques.add(new Bloque(x, y, bw, bh, hp, false, txN, txD2, txD3, txU));
                    } else bloques.add(new Bloque(x, y, bw, bh, txN, txD2, txD3, txU));
                }
            }
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        switch (estado) {
            case MENU:
                dibujarMenu(); manejarInputMenu(); break;
            case JUGANDO:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && puedeTogglePausa()) { estado = EstadoJuego.PAUSADO; selPausa = 0; acabaDeEntrarPausa = true; }
                renderFrameJuego(true); break;
            case PAUSADO:
                renderFrameJuego(false); dibujarOverlayPausa(); manejarInputPausa(); break;
            case TUTORIAL:
                dibujarTutorial(); manejarInputTutorial(); break;
            case FIN_DE_JUEGO:
                dibujarFinJuego(); manejarInputFinJuego(); break;
            case CREDITOS:
                dibujarCreditos(); manejarInputCreditos(); break;
        }
    }

    private void renderFrameJuego(boolean actualizar) {
        camara.update();

        // fondo
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();
        loteSprites.draw(texFondo, 0, 0, camara.viewportWidth, camara.viewportHeight);
        paleta.dibujar(loteSprites);
        for (Bloque b : bloques) b.dibujar(loteSprites);
        loteSprites.end();

        // bola con shape para legibilidad
        sr.setProjectionMatrix(camara.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        if (actualizar) paleta.actualizar();

        if (pelota.estaQuieta()) {
            pelota.setXY(paleta.getX() + paleta.getAncho()/2 - 5, paleta.getY() + paleta.getAlto() + 11);
            if (actualizar && Gdx.input.isKeyPressed(Input.Keys.SPACE)) pelota.setEstaQuieta(false);
        } else if (actualizar) {
            pelota.actualizar();
        }

        if (actualizar && pelota.getY() < 0) {
            vidas--;
            pelota = new BolaPing(paleta.getX() + paleta.getAncho()/2 - 5, paleta.getY() + paleta.getAlto() + 11, 10, velX, velY, true);
            if (vidas <= 0) { estado = EstadoJuego.FIN_DE_JUEGO; sr.end(); return; }
        }

        if (actualizar) {
            for (Bloque b : bloques) pelota.comprobarColision(b);
            for (int i = 0; i < bloques.size(); i++) {
                if (bloques.get(i).estaDestruido()) {
                    puntaje++;
                    bloques.remove(i);
                    i--;
                    if (puntaje % 10 == 0) {
                        vidas++;
                        mostrarBonificacionVidaHastaMs = com.badlogic.gdx.utils.TimeUtils.millis() + duracionBonificacionVida;
                    }
                }
            }
            pelota.comprobarColision(paleta);
        }

        pelota.dibujar(sr);
        sr.end();

        dibujarHUD();

        if (actualizar && bloques.isEmpty()) {
            nivel++;
            if (dificultad != Dificultad.FACIL) {
                velX += (velX > 0 ? 1 : -1);
                velY += (velY > 0 ? 1 : -1);
                int dec = (dificultad == Dificultad.DIFICIL) ? 12 : 8;
                int nuevoAncho = Math.max(60, paleta.getAncho() - dec);
                paleta = new Plataforma(paleta.getX(), paleta.getY(), nuevoAncho, paleta.getAlto(), texPaleta);
                paleta.setVelPxPorSeg(velPaleta);
            }
            crearBloques(calcularFilasNivel(nivel));
            pelota = new BolaPing(paleta.getX() + paleta.getAncho()/2 - 5, paleta.getY() + paleta.getAlto() + 11, 10, velX, velY, true);
        }
    }

    private void dibujarHUD() {
        camara.update();
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();
        fuente.draw(loteSprites, "Puntos: " + puntaje, 10, 25);
        fuente.draw(loteSprites, "Vidas : " + vidas, camara.viewportWidth - 240, 25);
        fuente.draw(loteSprites, "Nivel : " + nivel, camara.viewportWidth/2f - 60, 25);
        fuente.draw(loteSprites, "Dif   : " + dificultad, camara.viewportWidth/2f + 120, 25);

        long ahora = com.badlogic.gdx.utils.TimeUtils.millis();
        if (ahora < mostrarBonificacionVidaHastaMs) {
            String bonus = "+1 Vida!";
            gl.setText(fuente, bonus);
            fuente.setColor(Color.GOLD);
            fuente.draw(loteSprites, bonus, camara.viewportWidth - 180, camara.viewportHeight - 40);
            fuente.setColor(Color.WHITE);
        }
        loteSprites.end();
    }

    private void dibujarMenu() {
        camara.update();
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();

        float w = camara.viewportWidth, h = camara.viewportHeight;
        String titulo = "COSMIC COLLISION";
        String sub = "Elige dificultad:";
        String o1 = "1 (F1) - FACIL   | Paleta grande, bola lenta";
        String o2 = "2 (F2) - MEDIA   | Mas bloques, duros";
        String o3 = "3 (F3) - DIFICIL | Mas bloques, duros e irrompibles";
        String extra = "T: Tutorial | C: Creditos";
        String cont = "Controles: LEFT/RIGHT, SPACE lanzar, ESC pausa";

        float y = h - 60, linea = 48f;

        gl.setText(fuente, titulo); fuente.draw(loteSprites, titulo, (w - gl.width) / 2f, y); y -= linea * 1.2f;
        gl.setText(fuente, sub);    fuente.draw(loteSprites, sub,    (w - gl.width) / 2f, y); y -= linea;
        gl.setText(fuente, o1);     fuente.draw(loteSprites, o1,     (w - gl.width) / 2f, y); y -= linea;
        gl.setText(fuente, o2);     fuente.draw(loteSprites, o2,     (w - gl.width) / 2f, y); y -= linea;
        gl.setText(fuente, o3);     fuente.draw(loteSprites, o3,     (w - gl.width) / 2f, y); y -= linea * 1.5f;
        gl.setText(fuente, extra);  fuente.draw(loteSprites, extra,  (w - gl.width) / 2f, y); y -= linea;
        gl.setText(fuente, cont);   fuente.draw(loteSprites, cont,   (w - gl.width) / 2f, 80);

        loteSprites.end();
    }

    private void manejarInputMenu() {
        boolean f1 = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.F1);
        boolean f2 = Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.F2);
        boolean f3 = Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.F3);

        if (f1) { aplicarDificultad(Dificultad.FACIL); iniciarJuego(); estado = EstadoJuego.JUGANDO; }
        else if (f2) { aplicarDificultad(Dificultad.MEDIA); iniciarJuego(); estado = EstadoJuego.JUGANDO; }
        else if (f3) { aplicarDificultad(Dificultad.DIFICIL); iniciarJuego(); estado = EstadoJuego.JUGANDO; }

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) { estado = EstadoJuego.TUTORIAL; paginaTutorialActual = 1; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) { estado = EstadoJuego.CREDITOS; desplazamientoCreditosY = -20f; }
    }

    private void dibujarOverlayPausa() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.setProjectionMatrix(camara.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.55f);
        sr.rect(0, 0, camara.viewportWidth, camara.viewportHeight);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();
        float w = camara.viewportWidth, h = camara.viewportHeight;

        String titulo = "PAUSA";
        gl.setText(fuente, titulo);
        fuente.draw(loteSprites, titulo, (w - gl.width) / 2f, h - 120);

        float y = h - 200, linea = 44f;
        for (int i = 0; i < opcionesPausa.length; i++) {
            String pref = (i == selPausa) ? "> " : "  ";
            String txt = pref + opcionesPausa[i];
            gl.setText(fuente, txt);
            fuente.draw(loteSprites, txt, (w - gl.width) / 2f, y);
            y -= linea;
        }

        String pista = "ESC: Reanudar  |  ENTER: Aceptar  |  UP/DOWN: Navegar";
        gl.setText(fuente, pista);
        fuente.draw(loteSprites, pista, (w - gl.width) / 2f, 120);

        loteSprites.end();
    }

    private void manejarInputPausa() {
        if (acabaDeEntrarPausa) { acabaDeEntrarPausa = false; return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && puedeTogglePausa()) { estado = EstadoJuego.JUGANDO; return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))   selPausa = (selPausa - 1 + opcionesPausa.length) % opcionesPausa.length;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) selPausa = (selPausa + 1) % opcionesPausa.length;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            switch (selPausa) {
                case 0: estado = EstadoJuego.JUGANDO; break;
                case 1:
                    crearBloques(calcularFilasNivel(nivel));
                    pelota = new BolaPing(paleta.getX() + paleta.getAncho()/2 - 5, paleta.getY() + paleta.getAlto() + 11, 10, velX, velY, true);
                    estado = EstadoJuego.JUGANDO;
                    break;
                case 2: estado = EstadoJuego.MENU; bloques.clear(); break;
                case 3: Gdx.app.exit(); break;
            }
        }
    }

    private void dibujarTutorial() {
        camara.update();
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();

        String titulo = "TUTORIAL (" + paginaTutorialActual + "/" + MAX_PAGINAS_TUTORIAL + ")";
        gl.setText(fuente, titulo);
        fuente.draw(loteSprites, titulo, (camara.viewportWidth - gl.width)/2f, camara.viewportHeight - 80);

        String pagina;
        if (paginaTutorialActual == 1) {
            pagina = "Objetivo: Destruye todos los bloques.\nMueve la plataforma con LEFT/RIGHT.\nLanza con SPACE.";
        } else if (paginaTutorialActual == 2) {
            pagina = "Tipos de bloque:\n- Normal: 1 impacto\n- Duro: 2-3 impactos\n- Irrompible: obstaculo";
        } else {
            pagina = "Pausa (ESC). Dificultad afecta paleta, velocidad,\nfilas y tipos de bloque. ¡Suerte!";
        }

        float y = camara.viewportHeight - 160;
        for (String linea : pagina.split("\n")) {
            gl.setText(fuente, linea);
            fuente.draw(loteSprites, linea, (camara.viewportWidth - gl.width)/2f, y);
            y -= 40;
        }

        String pista = "LEFT/RIGHT: navegar | ESC: volver";
        gl.setText(fuente, pista);
        fuente.draw(loteSprites, pista, (camara.viewportWidth - gl.width)/2f, 100);

        loteSprites.end();
    }

    private void manejarInputTutorial() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) paginaTutorialActual = Math.min(MAX_PAGINAS_TUTORIAL, paginaTutorialActual + 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  paginaTutorialActual = Math.max(1, paginaTutorialActual - 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) estado = EstadoJuego.MENU;
    }

    private void dibujarFinJuego() {
        camara.update();
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();
        String titulo = "FIN DE JUEGO";
        gl.setText(fuente, titulo);
        fuente.draw(loteSprites, titulo, (camara.viewportWidth - gl.width)/2f, camara.viewportHeight - 120);

        String linea = "ENTER: Volver al menu";
        gl.setText(fuente, linea);
        fuente.draw(loteSprites, linea, (camara.viewportWidth - gl.width)/2f, camara.viewportHeight - 200);
        loteSprites.end();
    }

    private void manejarInputFinJuego() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) { estado = EstadoJuego.MENU; }
    }

    private void dibujarCreditos() {
        camara.update();
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();

        float y = desplazamientoCreditosY <= 0 ? camara.viewportHeight : desplazamientoCreditosY;
        for (String linea : textoCreditos.split("\n")) {
            gl.setText(fuente, linea);
            fuente.draw(loteSprites, linea, (camara.viewportWidth - gl.width)/2f, y);
            y -= 40;
        }
        lineaFinCreditos = y;

        loteSprites.end();

        desplazamientoCreditosY += Gdx.graphics.getDeltaTime() * velocidadDesplazamientoCreditos;
        if (lineaFinCreditos < 0) desplazamientoCreditosY = -20f; // loop
    }

    private void manejarInputCreditos() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) estado = EstadoJuego.MENU;
    }

    @Override
    public void dispose() {
        texFondo.dispose(); texPaleta.dispose(); txN.dispose(); txD2.dispose(); txD3.dispose(); txU.dispose();
        loteSprites.dispose(); sr.dispose(); fuente.dispose();
    }
}