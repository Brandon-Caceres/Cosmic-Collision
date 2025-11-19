package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Mundo de juego con soporte de power-ups que caen al destruir bloques.
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

    private final com.badlogic.gdx.graphics.Texture texturaPaleta;

    // Power-ups
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final float probDropPowerUp = 0.25f; // 25% por bloque destruido

    // Bola explosiva
    private boolean bolaExplosivaActiva = false;
    private long bolaExplosivaHastaMs = 0;
    private final float radioExplosionPx = 90f; // área de efecto alrededor del bloque impactado

    public GameWorld(BlockFactory factory,
                     HUD hud,
                     DifficultySettings initialSettings,
                     long duracionBonificacionVida,
                     com.badlogic.gdx.graphics.Texture texturaPaleta) {
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
        powerUps.clear();
        bolaExplosivaActiva = false;
        bolaExplosivaHastaMs = 0;

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
        powerUps.clear();
        bloques.addAll(blockFactory.crearBloques(
                filas, settings, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()
        ));
    }

    public void actualizar() {
        // Expiración de bola explosiva
        if (bolaExplosivaActiva && com.badlogic.gdx.utils.TimeUtils.millis() >= bolaExplosivaHastaMs) {
            bolaExplosivaActiva = false;
        }

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

        // Efecto: bola explosiva — detectar bloque impactado y aplicar daño en área
        Bloque bloqueImpactado = detectarImpactoConAlgúnBloque();
        if (bolaExplosivaActiva && bloqueImpactado != null) {
            aplicarExplosionAlrededorDe(bloqueImpactado);
        }

        // Colisiones normales de la bola con bloques
        for (Bloque b : bloques) {
            pelota.comprobarColision(b);
        }
        // y con la paleta
        pelota.comprobarColision(paleta);

        // Procesar bloques destruidos: puntaje, drop de power-up y remover
        for (int i = 0; i < bloques.size(); i++) {
            Bloque b = bloques.get(i);
            if (b.estaDestruido()) {
                puntaje++;
                intentarSoltarPowerUp(b);
                bloques.remove(i);
                i--;
            }
        }

        // Actualizar power-ups cayendo y aplicar si tocan la paleta
        actualizarYAplicarPowerUps();

        // Progreso de nivel
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
                    10, velPelotaX, velPelotaY, true
            );
        }
    }

    public void dibujar(SpriteBatch batch, ShapeRenderer sr, float ancho, float alto) {
        // Batch: paleta, bloques, HUD
        batch.begin();
        paleta.dibujar(batch);
        for (Bloque b : bloques) {
            b.dibujar(batch);
        }
        hud.dibujar(batch, ancho, alto, puntaje, vidas, nivel, settings.dificultad,
                mostrarBonificacionVidaHastaMs, com.badlogic.gdx.utils.TimeUtils.millis());
        batch.end();

        // Shapes: pelota + power-ups
        sr.begin(ShapeRenderer.ShapeType.Filled);
        pelota.dibujar(sr);
        for (PowerUp p : powerUps) {
            p.dibujar(sr);
        }
        sr.end();
    }

    public int getVidas() { return vidas; }
    public int getNivel() { return nivel; }
    public int getPuntaje() { return puntaje; }
    public DifficultySettings getSettings() { return settings; }

    public void reiniciarNivel() {
        crearBloques(filasParaNivel(nivel));
        powerUps.clear();
        pelota = new BolaPing(
                paleta.getX() + paleta.getAncho()/2 - 5,
                paleta.getY() + paleta.getAlto() + 11,
                10, velPelotaX, velPelotaY, true
        );
        paleta = new Plataforma(paleta.getX(), paleta.getY(), paleta.getAncho(), paleta.getAlto(), texturaPaleta);
        paleta.setVelPxPorSeg(settings.velocidadPaleta);
    }

    // --------------------- LÓGICA DE POWER-UPS ----------------------

    private void intentarSoltarPowerUp(Bloque b) {
        if (Math.random() < probDropPowerUp) {
            PowerUpType tipo = sortearTipoPowerUp();
            int size = 22;
            int px = b.getX() + b.getAncho()/2 - size/2;
            int py = b.getY() + b.getAlto()/2 - size/2;
            powerUps.add(new PowerUp(px, py, size, size, tipo));
        }
    }

    private PowerUpType sortearTipoPowerUp() {
        // Distribución simple: ajusta a gusto
        double r = Math.random();
        if (r < 0.30) return PowerUpType.PADDLE_GROW;      // 30%
        if (r < 0.50) return PowerUpType.PADDLE_SHRINK;    // 20%
        if (r < 0.75) return PowerUpType.EXTRA_LIFE;       // 25%
        return PowerUpType.EXPLOSIVE_BALL;                 // 25%
    }

    private void actualizarYAplicarPowerUps() {
        Rectangle rectPaleta = paleta.getRect();

        for (int i = 0; i < powerUps.size(); i++) {
            PowerUp p = powerUps.get(i);
            p.actualizar();

            // Fuera de la pantalla
            if (p.getY() + p.getAlto() < 0) {
                powerUps.remove(i); i--; continue;
            }

            // ¿Toca la paleta?
            if (p.getRect().overlaps(rectPaleta)) {
                aplicarPowerUp(p.getTipo());
                powerUps.remove(i); i--;
            }
        }
    }

    private void aplicarPowerUp(PowerUpType tipo) {
        switch (tipo) {
            case EXPLOSIVE_BALL:
                bolaExplosivaActiva = true;
                bolaExplosivaHastaMs = com.badlogic.gdx.utils.TimeUtils.millis() + 8000; // 8s
                break;
            case PADDLE_GROW: {
                int nuevoAncho = Math.min(260, paleta.getAncho() + 30);
                paleta = new Plataforma(paleta.getX(), paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
                paleta.setVelPxPorSeg(settings.velocidadPaleta);
                break;
            }
            case PADDLE_SHRINK: {
                int nuevoAncho = Math.max(50, paleta.getAncho() - 30);
                paleta = new Plataforma(paleta.getX(), paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
                paleta.setVelPxPorSeg(settings.velocidadPaleta);
                break;
            }
            case EXTRA_LIFE:
                vidas++;
                mostrarBonificacionVidaHastaMs = com.badlogic.gdx.utils.TimeUtils.millis() + duracionBonificacionVida;
                break;
        }
    }

    private Bloque detectarImpactoConAlgúnBloque() {
        if (pelota.estaQuieta()) return null;
        int cx = pelota.getX();
        int cy = pelota.getY();
        int r  = pelota.getRadio();

        for (Bloque b : bloques) {
            if (circleIntersectsRect(cx, cy, r, b.getRect())) {
                return b;
            }
        }
        return null;
    }

    private void aplicarExplosionAlrededorDe(Bloque impactado) {
        float cx = impactado.getX() + impactado.getAncho() / 2f;
        float cy = impactado.getY() + impactado.getAlto() / 2f;
        float r2 = radioExplosionPx * radioExplosionPx;

        for (Bloque b : bloques) {
            if (b == impactado) continue;
            if (b.esIrrompible()) continue;

            float bx = b.getX() + b.getAncho() / 2f;
            float by = b.getY() + b.getAlto() / 2f;
            float dx = bx - cx;
            float dy = by - cy;
            if (dx*dx + dy*dy <= r2) {
                b.destruir();
            }
        }
        // El bloque impactado en sí ya recibirá daño por la colisión normal.
    }

    private boolean circleIntersectsRect(int cx, int cy, int radius, Rectangle r) {
        float closestX = clamp(cx, r.x, r.x + r.width);
        float closestY = clamp(cy, r.y, r.y + r.height);
        float dx = cx - closestX;
        float dy = cy - closestY;
        return (dx*dx + dy*dy) <= (radius * radius);
    }

    private float clamp(float v, float a, float b) {
        if (v < a) return a;
        if (v > b) return b;
        return v;
    }
}