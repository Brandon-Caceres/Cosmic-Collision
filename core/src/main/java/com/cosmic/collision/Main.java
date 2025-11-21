package com.cosmic.collision;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Main
 * - ResourceManager (Singleton) para assets.
 * - DefaultBlockFactory (Abstract Factory) para crear bloques.
 * - DifficultyStrategy (Strategy) ya integrado en GameWorld.
 * - Pantallas migradas a AbstractScreen (Template Method): menu.render(delta), tutorial.render(delta), credits.render(delta).
 */
public class Main extends ApplicationAdapter {

    private OrthographicCamera camara;
    private SpriteBatch lote;
    private ShapeRenderer formas;

    // Dos fuentes para aislar escalas
    private BitmapFont fuenteUI;
    private BitmapFont fuenteHUD;

    private com.badlogic.gdx.graphics.Texture texturaFondo;
    private com.badlogic.gdx.graphics.Texture texturaPaleta;
    private com.badlogic.gdx.graphics.Texture texturaAsteroideNormal;
    private com.badlogic.gdx.graphics.Texture texturaAsteroideDuro2;
    private com.badlogic.gdx.graphics.Texture texturaAsteroideDuro3;
    private com.badlogic.gdx.graphics.Texture texturaAsteroideIrrompible;

    private GameState estado = GameState.MENU;
    private Dificultad dificultadActual = Dificultad.FACIL;
    private DifficultySettings ajustes;

    private HUD hud;
    private BlockFactory blockFactory;
    private GameWorld mundo;
    private MenuScreen menu;
    private PauseOverlay pausa;
    private TutorialScreen tutorial;
    private CreditsScreen creditos;

    private final long duracionBonificacionVida = 1500;

    @Override
    public void create() {
        camara = new OrthographicCamera();
        camara.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        lote = new SpriteBatch();
        formas = new ShapeRenderer();

        // Inicializa dos fuentes independientes
        fuenteUI = new BitmapFont();
        fuenteUI.getData().setScale(2.0f);

        fuenteHUD = new BitmapFont();
        fuenteHUD.getData().setScale(2.0f);

        // Carga texturas vía ResourceManager (singleton)
        ResourceManager rm = ResourceManager.getInstance();
        texturaFondo = rm.getTexture("fondo", "espacio.jpg");
        texturaPaleta = rm.getTexture("paleta", "nave.png");
        texturaAsteroideNormal = rm.getTexture("ast_normal", "AsteroideE.png");
        texturaAsteroideDuro2 = rm.getTexture("ast_duro2", "AsteroideM.png");
        texturaAsteroideDuro3 = rm.getTexture("ast_duro3", "AsteroideH.png");
        texturaAsteroideIrrompible = rm.getTexture("ast_unb", "AsteroideI.png");

        ajustes = new DifficultySettings(dificultadActual);
        hud = new HUD(fuenteHUD);

        // Usar fábrica concreta pasando texturas ya cargadas (evita duplicar cargas)
        blockFactory = new DefaultBlockFactory(texturaAsteroideNormal, texturaAsteroideDuro2, texturaAsteroideDuro3, texturaAsteroideIrrompible);

        mundo = new GameWorld(blockFactory, hud, ajustes, duracionBonificacionVida, texturaPaleta);

        // Crear pantallas que extienden AbstractScreen (reciben lote como SpriteBatch)
        menu = new MenuScreen(lote, fuenteUI, new MenuScreen.Listener() {
            @Override public void onElegirDificultad(Dificultad d) {
                dificultadActual = d;
                ajustes = new DifficultySettings(dificultadActual);
                mundo.aplicarDificultad(ajustes);
                mundo.iniciarJuego();
                estado = GameState.JUGANDO;
            }
            @Override public void onTutorial() { estado = GameState.TUTORIAL; }
            @Override public void onCreditos() {
                creditos.reiniciar(camara.viewportWidth, camara.viewportHeight);
                estado = GameState.CREDITOS;
            }
        });

        pausa = new PauseOverlay(fuenteUI, new PauseOverlay.Listener() {
            @Override public void onReanudar() { estado = GameState.JUGANDO; }
            @Override public void onReiniciarNivel() { mundo.reiniciarNivel(); estado = GameState.JUGANDO; }
            @Override public void onMenuPrincipal() { estado = GameState.MENU; }
            @Override public void onSalir() { Gdx.app.exit(); }
        });

        tutorial = new TutorialScreen(lote, fuenteUI, () -> estado = GameState.MENU);
        creditos = new CreditsScreen(lote, fuenteUI, () -> estado = GameState.MENU);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camara.update();
        lote.setProjectionMatrix(camara.combined);
        formas.setProjectionMatrix(camara.combined);

        switch (estado) {
            case MENU:
                // AbstractScreen: render(delta) invoca onUpdate/onDraw internamente
                menu.render(delta);
                break;

            case JUGANDO:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    estado = GameState.PAUSADO;
                    break;
                }
                dibujarFondo();
                mundo.actualizar();
                mundo.dibujar(lote, formas, camara.viewportWidth, camara.viewportHeight);
                if (mundo.getVidas() <= 0) {
                    estado = GameState.FIN_DE_JUEGO;
                }
                break;

            case PAUSADO:
                dibujarFondo();
                // Dibujar estado actual sin actualizar
                mundo.dibujar(lote, formas, camara.viewportWidth, camara.viewportHeight);
                pausa.render(lote, formas, camara.viewportWidth, camara.viewportHeight);
                pausa.handleInput();
                break;

            case TUTORIAL:
                dibujarFondo();
                tutorial.render(delta);
                break;

            case CREDITOS:
                dibujarFondo();
                creditos.render(delta);
                break;

            case FIN_DE_JUEGO:
                dibujarFondo();
                dibujarPantallaFinDeJuego();
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    estado = GameState.MENU;
                    ajustes = new DifficultySettings(dificultadActual);
                    mundo.aplicarDificultad(ajustes);
                    mundo.iniciarJuego();
                }
                break;
        }
    }

    private void dibujarFondo() {
        lote.begin();
        lote.setColor(1f,1f,1f,1f);
        if (texturaFondo != null) {
            lote.draw(texturaFondo, 0, 0, camara.viewportWidth, camara.viewportHeight);
        }
        lote.end();
    }

    private void dibujarPantallaFinDeJuego() {
        // Overlay
        formas.begin(ShapeRenderer.ShapeType.Filled);
        formas.setColor(0, 0, 0, 0.75f);
        formas.rect(0, 0, camara.viewportWidth, camara.viewportHeight);
        formas.end();

        // Guardar escala actual y restaurar al final
        float oldX = fuenteUI.getData().scaleX, oldY = fuenteUI.getData().scaleY;

        lote.begin();
        lote.setColor(1f,1f,1f,1f);

        fuenteUI.getData().setScale(4.0f);
        String titulo = "¡FIN DEL JUEGO! :(";
        String puntuacion = "Puntuación final: " + mundo.getPuntaje();
        String pista = "Presiona ENTER para volver al menú...";

        float anchoMundo = camara.viewportWidth;
        float y = camara.viewportHeight / 2f + 100;

        com.badlogic.gdx.graphics.g2d.GlyphLayout gl = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        gl.setText(fuenteUI, titulo);
        fuenteUI.draw(lote, titulo, (anchoMundo - gl.width)/2f, y);
        y -= 80;

        fuenteUI.getData().setScale(2.5f);
        gl.setText(fuenteUI, puntuacion);
        fuenteUI.draw(lote, puntuacion, (anchoMundo - gl.width)/2f, y);
        y -= 120;

        fuenteUI.getData().setScale(1.5f);
        gl.setText(fuenteUI, pista);
        fuenteUI.draw(lote, pista, (anchoMundo - gl.width)/2f, y);
        lote.end();

        // Restaurar escala
        fuenteUI.getData().setScale(oldX, oldY);
    }

    @Override
    public void dispose() {
        lote.dispose();
        formas.dispose();
        fuenteUI.dispose();
        fuenteHUD.dispose();
        // Dispose centralizado del ResourceManager
        ResourceManager.getInstance().dispose();
    }
}