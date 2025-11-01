package com.cosmic.collision;

import java.util.ArrayList;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Main extends ApplicationAdapter {

    private enum EstadoJuego { MENU, JUGANDO, PAUSADO, TUTORIAL, FIN_DE_JUEGO, CREDITOS }

    private OrthographicCamera camara;
    private SpriteBatch loteSprites;
    private BitmapFont fuente;
    private ShapeRenderer renderizadorFormas;
    private GlyphLayout diseñoGlifo;
    
    private com.badlogic.gdx.graphics.Texture texturaFondo;
    private com.badlogic.gdx.graphics.Texture texturaPaleta;
    private com.badlogic.gdx.graphics.Texture texturaAsteroideNormal; 
    private com.badlogic.gdx.graphics.Texture texturaAsteroideDuro2;
    private com.badlogic.gdx.graphics.Texture texturaAsteroideDuro3; 
    private com.badlogic.gdx.graphics.Texture texturaAsteroideIrrompible;
    
    private long mostrarBonificacionVidaHastaMs = 0;
    private final long duracionBonificacionVida = 1500;

    private final int MAX_PAGINAS_TUTORIAL = 3; 
    private int paginaTutorialActual = 1;
    
    private String textoCreditos;
    private float desplazamientoCreditosY;
    private float velocidadDesplazamientoCreditos = 70f; 
    private float lineaFinCreditos;
    
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
    private int anchoBasePaleta = 120;
    private int velocidadXPelotaActual = 5;
    private int velocidadYPelotaActual = 7;
    private float velocidadPaleta = 200f;

    private int anchoBloque = 70;
    private int altoBloque = 26;
    private int espaciadoHBloque = 10;
    private int espaciadoVBloque = 10;
    private int margenBloqueLR = 10;
    private int margenSuperiorBloque = 10;

    private int columnasObjetivoFacil = 8;

    private float tasaBloquesDuros = 0f;
    private float tasaIrrompibles = 0f;
    private boolean permitirIrrompibles = false;

    private final String[] opcionesPausa = { "Reanudar", "Reiniciar Nivel", "Menú Principal", "Salir" };
    private int opcionPausaSeleccionada = 0;
    private boolean recienEntradoEnPausa = false;
    private long ultimoCambioPausaMs = 0;

    @Override
    public void create () {
        camara = new OrthographicCamera();
        camara.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camara.update();

        loteSprites = new SpriteBatch();
        fuente = new BitmapFont();
        fuente.getData().setScale(2.0f, 2.0f);
        renderizadorFormas = new ShapeRenderer();
        diseñoGlifo = new GlyphLayout();

        texturaFondo = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("espacio.jpg"));
        texturaPaleta = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("nave.png"));
        texturaAsteroideNormal = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("AsteroideE.png"));
        texturaAsteroideDuro2 = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("AsteroideM.png"));
        texturaAsteroideDuro3 = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("AsteroideH.png"));
        texturaAsteroideIrrompible = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("AsteroideI.png"));
        
        estado = EstadoJuego.MENU;
        dificultad = Dificultad.FACIL; 
        puntaje = 0;
        vidas = 3;
        nivel = 1;
        bloques.clear();
        
        StringBuilder sb = new StringBuilder();
        sb.append("LOS DEL MOLINOGEA EN ADA PRESENTAN:\n\n");
        sb.append("BLOCKBREAKER - NUESTRA VERSIÓN\n\n");
        sb.append("------------------------------------------\n\n");
        sb.append("EQUIPO DE PROGRAMACIÓN:\n");
        sb.append("  BRANDON CÁCERES\n");
        sb.append("  JOSUÉ HUAIQUIL\n");
        sb.append("  IGNACIO MUÑOZ\n\n");
        sb.append("DISEÑADOR:\n");
        sb.append("  JOSUÉ HUAIQUIL\n\n");
        sb.append("ESCRITOR:\n");
        sb.append("  IGNACIO MUÑOZ\n\n");
        sb.append("LENGUAJE USADO:\n");
        sb.append("  JAVA\n\n");
        sb.append("HERRAMIENTAS USADAS:\n");
        sb.append("  ECLIPSE\n");
        sb.append("  LIBGDX\n");
        sb.append("  GITHUB\n");
        sb.append("  CHATGPT\n");
        sb.append("  GEMINI\n\n");
        sb.append("------------------------------------------\n\n");
        sb.append("¡GRACIAS POR JUGAR!");
        
        textoCreditos = sb.toString();
        
        reiniciarPosicionCreditos();
    }

    private void reiniciarPosicionCreditos() {
        fuente.getData().setScale(2.0f); 
        diseñoGlifo.setText(fuente, textoCreditos);
        float altoTexto = diseñoGlifo.height;
        
        desplazamientoCreditosY = -altoTexto - 50; 
        
        lineaFinCreditos = camara.viewportHeight + 50f;
    }
    
    private boolean puedeCambiarPausa() {
        long ahora = com.badlogic.gdx.utils.TimeUtils.millis();
        if (ahora - ultimoCambioPausaMs < 200) return false;
        ultimoCambioPausaMs = ahora;
        return true;
    }

    private void aplicarDificultad(Dificultad d) {
        dificultad = d;
        switch (d) {
            case FACIL: 
                anchoBasePaleta = 160;
                velocidadXPelotaActual = 2;
                velocidadYPelotaActual = 3;
                filasBase = 3;
                incrementoFilasPorNivel = 0;
                velocidadPaleta = 700f;

                columnasObjetivoFacil = 8;
                espaciadoHBloque = 16;
                espaciadoVBloque = 14;
                margenBloqueLR = 20;
                margenSuperiorBloque = 20;

                tasaBloquesDuros = 0.0f;
                tasaIrrompibles = 0.0f;
                permitirIrrompibles = false;
                break;

            case MEDIA: 
                anchoBasePaleta = 110;
                velocidadXPelotaActual = 4;
                velocidadYPelotaActual = 5;
                filasBase = 5;
                incrementoFilasPorNivel = 1;
                velocidadPaleta = 1000f;

                anchoBloque = 70;
                altoBloque = 26;
                espaciadoHBloque = 10;
                espaciadoVBloque = 10;
                margenBloqueLR = 10;
                margenSuperiorBloque = 10;

                tasaBloquesDuros = 0.25f;
                tasaIrrompibles = 0.0f;
                permitirIrrompibles = false;
                break;

            case DIFICIL: 
                anchoBasePaleta = 90;
                velocidadXPelotaActual = 5;
                velocidadYPelotaActual = 6;
                filasBase = 7;
                incrementoFilasPorNivel = 2;
                velocidadPaleta = 1500f;

                anchoBloque = 70;
                altoBloque = 26;
                espaciadoHBloque = 10;
                espaciadoVBloque = 10;
                margenBloqueLR = 10;
                margenSuperiorBloque = 10;

                tasaBloquesDuros = 0.40f;
                tasaIrrompibles = 0.15f;
                permitirIrrompibles = true;
                break;
        }
    }

    private void iniciarJuego() {
        puntaje = 0;
        vidas = 3;
        nivel = 1;

        paleta = new Plataforma((int)(camara.viewportWidth/2f - anchoBasePaleta/2f), 40, anchoBasePaleta, 40, texturaPaleta);
        paleta.setVelPxPorSeg(velocidadPaleta);

        pelota = new BolaPing((int)(camara.viewportWidth/2f - 10), paleta.getY() + paleta.getAlto() + 11, 10,
        					  velocidadXPelotaActual, velocidadYPelotaActual, true);
        crearBloques(filasParaNivel(nivel));
        estado = EstadoJuego.JUGANDO; 
    }

    private int filasParaNivel(int nivelActual) {
        return filasBase + Math.max(0, (nivelActual - 1) * incrementoFilasPorNivel);
    }

    public void crearBloques(int filas) {
        bloques.clear();
        int y = (int)camara.viewportHeight - margenSuperiorBloque; 

        com.badlogic.gdx.graphics.Texture txN = texturaAsteroideNormal;
        com.badlogic.gdx.graphics.Texture tx2 = texturaAsteroideDuro2;
        com.badlogic.gdx.graphics.Texture tx3 = texturaAsteroideDuro3;
        com.badlogic.gdx.graphics.Texture txU = texturaAsteroideIrrompible;

        if (dificultad == Dificultad.FACIL) {
            int cols = Math.max(3, columnasObjetivoFacil);
            float anchoMundo = camara.viewportWidth;

            float anchoDisponible = anchoMundo - (2 * margenBloqueLR) - (espaciadoHBloque * (cols - 1));
            int anchoB = Math.max(40, (int)(anchoDisponible / cols));
            int altoB = Math.max(26, (int)(anchoB * 0.38f));

            for (int fila = 0; fila < filas; fila++) {
                y -= (altoB + espaciadoVBloque);
                if (y < 0) break;

                float anchoFila = cols * anchoB + (cols - 1) * espaciadoHBloque;
                int inicioX = (int)Math.round((anchoMundo - anchoFila) / 2f);

                for (int c = 0; c < cols; c++) {
                    int x = inicioX + c * (anchoB + espaciadoHBloque);
                   
                    bloques.add(new Bloque(x, y, anchoB, altoB, txN, tx2, tx3, txU)); 
                }
            }
        } else { 
            int anchoB = anchoBloque;
            int altoB = altoBloque;
            float anchoMundo = camara.viewportWidth;

            float anchoDisponible = anchoMundo - (2 * margenBloqueLR);
            int cols = Math.max(1, (int)Math.floor((anchoDisponible + espaciadoHBloque) / (anchoB + espaciadoHBloque)));
            float anchoFila = cols * anchoB + (cols - 1) * espaciadoHBloque;
            int inicioX = Math.max(margenBloqueLR, (int)Math.round((anchoMundo - anchoFila) / 2f));

            for (int fila = 0; fila < filas; fila++) {
                y -= (altoB + espaciadoVBloque);
                if (y < 0) break;

                for (int c = 0; c < cols; c++) {
                    int x = inicioX + c * (anchoB + espaciadoHBloque);
                    
                    // LOGICA DEL TIPO DE ASTEROIDE
                    boolean hacerIrrompible = permitirIrrompibles && Math.random() < tasaIrrompibles;
                    boolean hacerDuro = !hacerIrrompible && Math.random() < tasaBloquesDuros;
                    
                    if (hacerIrrompible) {
                        bloques.add(new Bloque(x, y, anchoB, altoB, 1, true, txN, tx2, tx3, txU));
                    } else if (hacerDuro) {

                        int hp = (dificultad == Dificultad.DIFICIL) ? (Math.random() < 0.5 ? 3 : 2) : 2; 
                        bloques.add(new Bloque(x, y, anchoB, altoB, hp, false, txN, tx2, tx3, txU)); 
                    } else {
                        bloques.add(new Bloque(x, y, anchoB, altoB, txN, tx2, tx3, txU)); 
                    }
                }
            } 
        }
    }

    private void dibujarMenu() {
        camara.update();
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();

        float anchoMundo = camara.viewportWidth;
        float altoMundo = camara.viewportHeight;

        String titulo = "COSMIC COLLISION";
        String subtitulo = "Elige dificultad:";
        String opt1 = "1 (F1) - FÁCIL   | Paleta grande, bola lenta";
        String opt2 = "2 (F2) - MEDIA   | Más bloques, duros";
        String opt3 = "3 (F3) - DIFÍCIL | Más bloques, duros e irrompibles";
        String opt4 = "4 (F4) - TUTORIAL | Ver controles e instrucciones";
        String opt5 = "5 (F5) - CRÉDITOS | Ver información del desarrollo";
        String controles = "Controles: IZQ/DER, ESPACIO lanzar, ESC pausa";

        float y = altoMundo - 60;
        float interlineado = 48f;

        diseñoGlifo.setText(fuente, titulo);
        fuente.draw(loteSprites, titulo, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= interlineado * 1.2f;

        diseñoGlifo.setText(fuente, subtitulo);
        fuente.draw(loteSprites, subtitulo, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= interlineado;

        diseñoGlifo.setText(fuente, opt1);
        fuente.draw(loteSprites, opt1, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= interlineado;

        diseñoGlifo.setText(fuente, opt2);
        fuente.draw(loteSprites, opt2, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= interlineado;

        diseñoGlifo.setText(fuente, opt3);
        fuente.draw(loteSprites, opt3, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= interlineado;
        
        diseñoGlifo.setText(fuente, opt4);
        fuente.draw(loteSprites, opt4, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= interlineado;
        
        diseñoGlifo.setText(fuente, opt5);
        fuente.draw(loteSprites, opt5, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= interlineado * 1.5f;

        diseñoGlifo.setText(fuente, controles);
        fuente.draw(loteSprites, controles, (anchoMundo - diseñoGlifo.width) / 2f, 80);

        loteSprites.end();
    }

    public void dibujarTextos() {
        fuente.draw(loteSprites, "Puntos: " + puntaje, 10, 25);
        fuente.draw(loteSprites, "Vidas : " + vidas, camara.viewportWidth - 240, 25);
        fuente.draw(loteSprites, "Nivel : " + nivel, camara.viewportWidth/2f - 60, 25);
        fuente.draw(loteSprites, "Dif   : " + dificultad, camara.viewportWidth/2f + 120, 25);
        
        if (com.badlogic.gdx.utils.TimeUtils.millis() < mostrarBonificacionVidaHastaMs) {
            String msg = "¡VIDA EXTRA CONSEGUIDA!";
            diseñoGlifo.setText(fuente, msg);

            fuente.draw(loteSprites, msg, (camara.viewportWidth - diseñoGlifo.width) / 2f, camara.viewportHeight / 2f); 
        }
    }
    
    private void manejarEntradaMenu() {
        boolean facil  = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.F1);
        boolean medio= Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.F2);
        boolean dificil  = Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.F3);
        boolean tutorial = Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.F4);
        boolean creditos = Gdx.input.isKeyJustPressed(Input.Keys.NUM_5) || Gdx.input.isKeyJustPressed(Input.Keys.F5);
            
        if (facil) {
            aplicarDificultad(Dificultad.FACIL); 
            iniciarJuego();
            estado = EstadoJuego.JUGANDO;
        } else if (medio) {
            aplicarDificultad(Dificultad.MEDIA);
            iniciarJuego();
            estado = EstadoJuego.JUGANDO;
        } else if (dificil) {
            aplicarDificultad(Dificultad.DIFICIL); 
            iniciarJuego();
            estado = EstadoJuego.JUGANDO;
        } else if (tutorial) {
        	estado = EstadoJuego.TUTORIAL;
        } else if (creditos) {
        	estado = EstadoJuego.CREDITOS;
        	reiniciarPosicionCreditos();
        }
    }
    
    private void dibujarTutorial() {
        camara.update();
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();

        float anchoMundo = camara.viewportWidth;
        float altoMundo = camara.viewportHeight;

        String titulo = "TUTORIAL DEL JUEGO (" + paginaTutorialActual + " de " + MAX_PAGINAS_TUTORIAL + ")";
        String pistaNavegacion = "IZQ/DER: Cambiar página | ESC/ENTER: Menú Principal";

        float y = altoMundo - 80; 
        float interlineado = 44f; 
        float inicioContenidoX = 80;

        fuente.getData().setScale(3.0f);
        
        diseñoGlifo.setText(fuente, titulo);
        fuente.draw(loteSprites, titulo, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= 70;

        fuente.getData().setScale(1.8f);

        if (paginaTutorialActual == 1) {
            String t1 = "OBJETIVO PRINCIPAL:";
            String c1 = "Destruye TODOS los bloques para pasar de nivel.";
            String t2 = "VIDAS Y FIN DE JUEGO:";
            String c2 = "Empiezas con 3 vidas. Si la bola cae, pierdes una. Sin vidas: Fin de Juego.";
            String t3 = "PROGRESO Y RECOMPENSA:";
            String c3 = "La velocidad de la bola aumenta ligeramente con cada nivel.";
            String c4 = "¡Tienes probabilidad de ganar una vida extra al completar un nivel!";

            fuente.draw(loteSprites, t1, inicioContenidoX, y); y -= interlineado;
            fuente.draw(loteSprites, c1, inicioContenidoX + 30, y); y -= interlineado * 1.5f;
            fuente.draw(loteSprites, t2, inicioContenidoX, y); y -= interlineado;
            fuente.draw(loteSprites, c2, inicioContenidoX + 30, y); y -= interlineado * 1.5f;
            fuente.draw(loteSprites, t3, inicioContenidoX, y); y -= interlineado;
            fuente.draw(loteSprites, c3, inicioContenidoX + 30, y); y -= interlineado;
            fuente.draw(loteSprites, c4, inicioContenidoX + 30, y); y -= interlineado;

        } else if (paginaTutorialActual == 2) {
            String t1 = "TIPOS DE BLOQUES:";
            String c1 = "- Normales: Se rompen con 1 golpe.";
            String c2 = "- Duros (2-3 golpes): Aparecen en MEDIA y DIFÍCIL.";
            String c3 = "- Irrompibles: No se destruyen. Solo en DIFÍCIL.";
            String t2 = "AJUSTES POR DIFICULTAD:";
            String c4 = "- FÁCIL: Pala grande, bola lenta.";
            String c5 = "- MEDIA/DIFÍCIL: Pala se encoge y la bola acelera progresivamente.";
            
            fuente.draw(loteSprites, t1, inicioContenidoX, y); y -= interlineado;
            fuente.draw(loteSprites, c1, inicioContenidoX + 30, y); y -= interlineado;
            fuente.draw(loteSprites, c2, inicioContenidoX + 30, y); y -= interlineado;
            fuente.draw(loteSprites, c3, inicioContenidoX + 30, y); y -= interlineado * 1.5f;
            fuente.draw(loteSprites, t2, inicioContenidoX, y); y -= interlineado;
            fuente.draw(loteSprites, c4, inicioContenidoX + 30, y); y -= interlineado;
            fuente.draw(loteSprites, c5, inicioContenidoX + 30, y); y -= interlineado;

        } else if (paginaTutorialActual == 3) {
            String t1 = "CONTROLES DE JUEGO:";
            String c1 = "MOVER PALETA: Flechas IZQUIERDA / DERECHA";
            String c2 = "LANZAR BOLA: ESPACIO";
            String c3 = "PAUSA / MENÚ: ESCAPE";
            String t2 = "CONTROLES DE MENÚ (PAUSA/PRINCIPAL):";
            String c4 = "NAVEGAR: Flechas ARRIBA / ABAJO";
            String c5 = "SELECCIONAR: ENTER / ESPACIO";

            fuente.draw(loteSprites, t1, inicioContenidoX, y); y -= interlineado;
            fuente.draw(loteSprites, c1, inicioContenidoX + 30, y); y -= interlineado;
            fuente.draw(loteSprites, c2, inicioContenidoX + 30, y); y -= interlineado;
            fuente.draw(loteSprites, c3, inicioContenidoX + 30, y); y -= interlineado * 1.5f;
            fuente.draw(loteSprites, t2, inicioContenidoX, y); y -= interlineado;
            fuente.draw(loteSprites, c4, inicioContenidoX + 30, y); y -= interlineado;
            fuente.draw(loteSprites, c5, inicioContenidoX + 30, y); y -= interlineado;
        }
        
        fuente.getData().setScale(2.5f); 
        diseñoGlifo.setText(fuente, pistaNavegacion);
        fuente.draw(loteSprites, pistaNavegacion, (anchoMundo - diseñoGlifo.width) / 2f, 100);

        loteSprites.end();
    }

    private void manejarEntradaTutorial() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            estado = EstadoJuego.MENU; 
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            paginaTutorialActual = Math.min(paginaTutorialActual + 1, MAX_PAGINAS_TUTORIAL); 
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            paginaTutorialActual = Math.max(paginaTutorialActual - 1, 1); 
        }
    }
    
    private void dibujarPantallaCreditos() {
    	camara.update();
        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();

        float anchoMundo = camara.viewportWidth;

        desplazamientoCreditosY += velocidadDesplazamientoCreditos * Gdx.graphics.getDeltaTime();

        fuente.getData().setScale(2.0f); 
        diseñoGlifo.setText(fuente, textoCreditos); 

        fuente.draw(loteSprites, textoCreditos, (anchoMundo - diseñoGlifo.width) / 2f, desplazamientoCreditosY); 

        String pista = "Presiona ENTER o ESC para volver al menú...";
        fuente.getData().setScale(1.5f);
        diseñoGlifo.setText(fuente, pista); 
        fuente.draw(loteSprites, pista, (anchoMundo - diseñoGlifo.width) / 2f, 80);

        loteSprites.end();
        
        if (desplazamientoCreditosY > lineaFinCreditos) {
            reiniciarPosicionCreditos();
        }
    }

    private void dibujarPantallaFinDeJuego() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        renderizadorFormas.setProjectionMatrix(camara.combined);
        renderizadorFormas.begin(ShapeRenderer.ShapeType.Filled);
        renderizadorFormas.setColor(0, 0, 0, 0.75f); 
        renderizadorFormas.rect(0, 0, camara.viewportWidth, camara.viewportHeight);
        renderizadorFormas.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();

        float anchoMundo = camara.viewportWidth;
        float altoMundo = camara.viewportHeight;

        fuente.getData().setScale(4.0f); 

        String titulo = "¡FIN DEL JUEGO! :(";
        String puntuacion = "Puntuación final: " + puntaje;
        String pista = "Presiona ENTER para volver al menú...";

        float y = altoMundo / 2f + 100; 

        diseñoGlifo.setText(fuente, titulo);
        fuente.draw(loteSprites, titulo, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= 80;

        fuente.getData().setScale(2.5f);
        diseñoGlifo.setText(fuente, puntuacion);
        fuente.draw(loteSprites, puntuacion, (anchoMundo - diseñoGlifo.width) / 2f, y);
        y -= 120;

        fuente.getData().setScale(1.5f);
        diseñoGlifo.setText(fuente, pista);
        fuente.draw(loteSprites, pista, (anchoMundo - diseñoGlifo.width) / 2f, y);

        loteSprites.end();
    }

    private void renderizarMarcoJuego(boolean actualizando) {
    	camara.update();
        
    	loteSprites.setProjectionMatrix(camara.combined);
    	loteSprites.begin();

    	if (texturaFondo != null) {
    	    loteSprites.draw(texturaFondo, 0, 0, camara.viewportWidth, camara.viewportHeight);
    	}

    	if (paleta != null) { 
    	    paleta.dibujar(loteSprites); 
    	}

    	dibujarTextos(); 

    	for (Bloque b : bloques) { 
    	    b.dibujar(loteSprites);
    	}
    	
    	loteSprites.end();
    	
    	renderizadorFormas.setProjectionMatrix(camara.combined);
    	renderizadorFormas.begin(ShapeRenderer.ShapeType.Filled);

    	if (actualizando) paleta.actualizar();

        if (pelota.estaQuieta()) {
            pelota.setXY(paleta.getX() + paleta.getAncho()/2 - 5, paleta.getY() + paleta.getAlto() + 11);
            if (actualizando && Gdx.input.isKeyPressed(Input.Keys.SPACE)) pelota.setEstaQuieta(false);
        } else if (actualizando) {
            pelota.actualizar();
        }

        if (actualizando && pelota.getY() < 0) {
            vidas--;
            pelota = new BolaPing(paleta.getX() + paleta.getAncho()/2 - 5, paleta.getY() + paleta.getAlto() + 11, 10, velocidadXPelotaActual,
            					  velocidadYPelotaActual, true);
        }

        
        if (actualizando && vidas <= 0) {
            estado = EstadoJuego.FIN_DE_JUEGO;
            bloques.clear();
            renderizadorFormas.end();
            return;
        }

        for (Bloque b : bloques) { 
            if (actualizando) pelota.comprobarColision((Colisionable)b);
        }


        if (actualizando) {
            for (int i = 0; i < bloques.size(); i++) {
                Bloque b = bloques.get(i);
                if (b.estaDestruido()) {
                    puntaje++;
                    bloques.remove(i);
                    i--;
                }
            }
        }

        if (actualizando) pelota.comprobarColision((Colisionable)paleta);
        pelota.dibujar(renderizadorFormas);

        renderizadorFormas.end();

        if (actualizando && bloques.size() == 0) {
            nivel++;
            if (dificultad == Dificultad.MEDIA) { 
                velocidadXPelotaActual += (velocidadXPelotaActual > 0 ? 1 : -1);
                velocidadYPelotaActual += (velocidadYPelotaActual > 0 ? 1 : -1);
                int nuevoAncho = Math.max(70, paleta.getAncho() - 8);
                paleta = new Plataforma(paleta.getX(), paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
                paleta.setVelPxPorSeg(velocidadPaleta);
            } else if (dificultad == Dificultad.DIFICIL) { 
                velocidadXPelotaActual += (velocidadXPelotaActual > 0 ? 1 : -1);
                velocidadYPelotaActual += (velocidadYPelotaActual > 0 ? 1 : -1);
                int nuevoAncho = Math.max(60, paleta.getAncho() - 12);
                paleta = new Plataforma(paleta.getX(), paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta); 
                paleta.setVelPxPorSeg(velocidadPaleta);
            }
            
            double probVidaExtra = 0.0;
            
            switch (dificultad) {
                case FACIL: 
                    probVidaExtra = 0.25;
                    break;
                case MEDIA: 
                    probVidaExtra = 0.50;
                    break;
                case DIFICIL: 
                    probVidaExtra = 1.0;
                    break;
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
                velocidadXPelotaActual,
                velocidadYPelotaActual,
                true
            );
        }
    }

    private void dibujarSuperposicionPausaYMenu() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        renderizadorFormas.setProjectionMatrix(camara.combined);
        renderizadorFormas.begin(ShapeRenderer.ShapeType.Filled);
        renderizadorFormas.setColor(0, 0, 0, 0.55f);
        renderizadorFormas.rect(0, 0, camara.viewportWidth, camara.viewportHeight);
        renderizadorFormas.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        loteSprites.setProjectionMatrix(camara.combined);
        loteSprites.begin();
        float ancho = camara.viewportWidth;
        float alto = camara.viewportHeight;

        String titulo = "PAUSA";
        diseñoGlifo.setText(fuente, titulo);
        fuente.draw(loteSprites, titulo, (ancho - diseñoGlifo.width) / 2f, alto - 120);

        float y = alto - 200;
        float interlineado = 44f;
        for (int i = 0; i < opcionesPausa.length; i++) {
            String prefijo = (i == opcionPausaSeleccionada) ? "> " : "  ";
            String texto = prefijo + opcionesPausa[i];
            diseñoGlifo.setText(fuente, texto);
            fuente.draw(loteSprites, texto, (ancho - diseñoGlifo.width) / 2f, y);
            y -= interlineado;
        }

        String pista = "ESC: Reanudar  |  ENTER: Aceptar  |  ARRIBA/ABAJO: Navegar";
        diseñoGlifo.setText(fuente, pista);
        fuente.draw(loteSprites, pista, (ancho - diseñoGlifo.width) / 2f, 120);

        loteSprites.end();
    }

    private void manejarEntradaPausa() {
        if (recienEntradoEnPausa) {
            recienEntradoEnPausa = false;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && puedeCambiarPausa()) {
            estado = EstadoJuego.JUGANDO;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            opcionPausaSeleccionada = (opcionPausaSeleccionada - 1 + opcionesPausa.length) % opcionesPausa.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            opcionPausaSeleccionada = (opcionPausaSeleccionada + 1) % opcionesPausa.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            switch (opcionPausaSeleccionada) {
                case 0:
                    estado = EstadoJuego.JUGANDO;
                    break;
                case 1:
                	crearBloques(filasParaNivel(nivel));
                    pelota = new BolaPing(paleta.getX() + paleta.getAncho()/2 - 5, paleta.getY() + paleta.getAlto() + 11, 10,
                    					velocidadXPelotaActual, velocidadYPelotaActual, true); 
                    paleta = new Plataforma(paleta.getX(), paleta.getY(), paleta.getAncho(), paleta.getAlto(), texturaPaleta); 
                    paleta.setVelPxPorSeg(velocidadPaleta);
                    estado = EstadoJuego.JUGANDO;
                    break;
                case 2:
                    estado = EstadoJuego.MENU;
                    bloques.clear();
                    break;
                case 3:
                    Gdx.app.exit();
                    break;
            }
        }
    }

    @Override
    public void render () {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        switch (estado) {
            case MENU:
                dibujarMenu();
                manejarEntradaMenu();
                break;
            case JUGANDO:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && puedeCambiarPausa()) {
                    estado = EstadoJuego.PAUSADO;
                    recienEntradoEnPausa = true;
                }
                renderizarMarcoJuego(true); 
                break;
            case PAUSADO:
                renderizarMarcoJuego(false); 
                dibujarSuperposicionPausaYMenu();
                manejarEntradaPausa();
                break;
            case TUTORIAL:
            	dibujarTutorial();
            	manejarEntradaTutorial();
            	break;
            case CREDITOS:
            	dibujarPantallaCreditos();
            	if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            		estado = EstadoJuego.MENU;
            	}
            	break;
            case FIN_DE_JUEGO:
                dibujarPantallaFinDeJuego();
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    estado = EstadoJuego.MENU;
                    puntaje = 0;
                    vidas = 3;
                    nivel = 1;
                    bloques.clear();
                    aplicarDificultad(dificultad); 
                }
                break;
        }
    }

    @Override
    public void dispose () {
        loteSprites.dispose();
        fuente.dispose();
        renderizadorFormas.dispose();
        texturaFondo.dispose();
        texturaPaleta.dispose();
        texturaAsteroideNormal.dispose();
        texturaAsteroideDuro2.dispose();
        texturaAsteroideDuro3.dispose();
        texturaAsteroideIrrompible.dispose();
    }
}