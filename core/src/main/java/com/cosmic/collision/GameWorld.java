package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Mundo de juego con soporte de power-ups que caen al destruir bloques.
 * Ajustes:
 * - Probabilidad por dificultad y sesgo por nivel.
 * - Duración temporal para cambios de tamaño de paleta según dificultad:
 *     FACIL: 7s, MEDIA: 5s, DIFICIL: 3s
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

    // Probabilidad base de que un bloque suelte un power-up (por bloque destruido).
    private float probDropPowerUp = 0.18f; // 18% base

    // Bola explosiva
    private boolean bolaExplosivaActiva = false;
    private long bolaExplosivaHastaMs = 0;
    private final float radioExplosionPx = 90f; // área de efecto alrededor del bloque impactado

    // ---- Temporal paddle size (duración según dificultad) ----
    // Si != null => tamaño temporal activo y contiene el ancho ORIGINAL a restaurar.
    private Integer paletaAnchoOriginal = null;
    // momento en ms en que expira el tamaño temporal
    private long paletaTamanoExpiraMs = 0L;

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

        // limpiar estado temporal de paleta
        paletaAnchoOriginal = null;
        paletaTamanoExpiraMs = 0L;

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
        long ahora = TimeUtils.millis();

        // Expiración de bola explosiva
        if (bolaExplosivaActiva && ahora >= bolaExplosivaHastaMs) {
            bolaExplosivaActiva = false;
        }

        // Restaurar tamaño de paleta si el temporal expiró
        if (paletaAnchoOriginal != null && ahora >= paletaTamanoExpiraMs) {
            restaurarTamanoPaletaOriginal();
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
                mostrarBonificacionVidaHastaMs = TimeUtils.millis() + duracionBonificacionVida;
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
                mostrarBonificacionVidaHastaMs, TimeUtils.millis());
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
        // limpiar estado temporal de paleta al reiniciar
        paletaAnchoOriginal = null;
        paletaTamanoExpiraMs = 0L;

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
        // Ajusta la probabilidad por dificultad
        float prob = getProbDropPowerUpForDifficulty();
        if (Math.random() < prob) {
            PowerUpType tipo = sortearTipoPowerUp();
            int size = 22;
            int px = b.getX() + b.getAncho()/2 - size/2;
            int py = b.getY() + b.getAlto()/2 - size/2;
            powerUps.add(new PowerUp(px, py, size, size, tipo));
        }
    }

    private float getProbDropPowerUpForDifficulty() {
        switch (settings.dificultad) {
            case FACIL:
                return probDropPowerUp * 1.25f; // más frecuentes en fácil
            case MEDIA:
                return probDropPowerUp;         // base para media
            case DIFICIL:
                return probDropPowerUp * 0.7f;  // menos en difícil
            default:
                return probDropPowerUp;
        }
    }

    /**
     * Sortea tipo de power-up según dificultad y nivel.
     */
    private PowerUpType sortearTipoPowerUp() {
        // Probabilidades base por dificultad (suman 1.0)
        double grow, shrink, explosive, life;

        switch (settings.dificultad) {
            case FACIL:
                grow = 0.50;
                shrink = 0.10;
                explosive = 0.20;
                life = 0.20;
                break;
            case MEDIA:
                grow = 0.35;
                shrink = 0.25;
                explosive = 0.20;
                life = 0.20;
                break;
            case DIFICIL:
                grow = 0.20;
                shrink = 0.50;
                explosive = 0.20;
                life = 0.10;
                break;
            default:
                grow = 0.35; shrink = 0.25; explosive = 0.20; life = 0.20;
        }

        // Sesgo por nivel: a más nivel, menos chance de útiles (grow + life) y más a shrink.
        double levelSkew = Math.min(0.25, Math.max(0.0, (nivel - 1) * 0.02));

        double utilesSum = grow + life;
        if (utilesSum > 0 && levelSkew > 0) {
            double reducTotal = utilesSum * levelSkew;
            double growRed = (grow / utilesSum) * reducTotal;
            double lifeRed = (life / utilesSum) * reducTotal;

            grow = Math.max(0.0, grow - growRed);
            life = Math.max(0.0, life - lifeRed);

            shrink += growRed + lifeRed;
        }

        double suma = grow + shrink + explosive + life;
        if (suma <= 0) {
            return PowerUpType.PADDLE_SHRINK;
        }
        grow /= suma; shrink /= suma; explosive /= suma; life /= suma;

        double r = Math.random();
        if (r < grow) return PowerUpType.PADDLE_GROW;
        r -= grow;
        if (r < shrink) return PowerUpType.PADDLE_SHRINK;
        r -= shrink;
        if (r < explosive) return PowerUpType.EXPLOSIVE_BALL;
        return PowerUpType.EXTRA_LIFE;
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
        long ahora = TimeUtils.millis();
        switch (tipo) {
            case EXPLOSIVE_BALL:
                bolaExplosivaActiva = true;
                bolaExplosivaHastaMs = ahora + 8000; // 8s
                break;
            case PADDLE_GROW: {
                // Guardar ancho original si no está guardado
                if (paletaAnchoOriginal == null) paletaAnchoOriginal = paleta.getAncho();

                int nuevoAncho = Math.min(260, paleta.getAncho() + 30);
                float centro = paleta.getX() + paleta.getAncho() / 2f;
                int nuevoX = Math.round(centro - nuevoAncho / 2f);
                nuevoX = Math.max(0, Math.min(nuevoX, Gdx.graphics.getWidth() - nuevoAncho));
                paleta = new Plataforma(nuevoX, paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
                paleta.setVelPxPorSeg(settings.velocidadPaleta);

                // establecer/actualizar expiración según dificultad
                paletaTamanoExpiraMs = ahora + getPaletaDurationMs();
                break;
            }
            case PADDLE_SHRINK: {
                if (paletaAnchoOriginal == null) paletaAnchoOriginal = paleta.getAncho();

                int nuevoAncho = Math.max(50, paleta.getAncho() - 30);
                float centro = paleta.getX() + paleta.getAncho() / 2f;
                int nuevoX = Math.round(centro - nuevoAncho / 2f);
                nuevoX = Math.max(0, Math.min(nuevoX, Gdx.graphics.getWidth() - nuevoAncho));
                paleta = new Plataforma(nuevoX, paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
                paleta.setVelPxPorSeg(settings.velocidadPaleta);

                paletaTamanoExpiraMs = ahora + getPaletaDurationMs();
                break;
            }
            case EXTRA_LIFE:
                vidas++;
                mostrarBonificacionVidaHastaMs = ahora + duracionBonificacionVida;
                break;
        }
    }

    /**
     * Duración en ms de la modificación del tamaño de la paleta según dificultad.
     * FACIL  -> 7000 ms
     * MEDIA  -> 5000 ms
     * DIFICIL -> 3000 ms
     */
    private long getPaletaDurationMs() {
        switch (settings.dificultad) {
            case FACIL: return 7000L;
            case MEDIA: return 5000L;
            case DIFICIL: return 3000L;
            default: return 5000L;
        }
    }

    private void restaurarTamanoPaletaOriginal() {
        if (paletaAnchoOriginal == null) return;
        int anchoOriginal = paletaAnchoOriginal;
        float centro = paleta.getX() + paleta.getAncho() / 2f;
        int nuevoX = Math.round(centro - anchoOriginal / 2f);
        nuevoX = Math.max(0, Math.min(nuevoX, Gdx.graphics.getWidth() - anchoOriginal));
        paleta = new Plataforma(nuevoX, paleta.getY(), anchoOriginal, paleta.getAlto(), texturaPaleta);
        paleta.setVelPxPorSeg(settings.velocidadPaleta);

        paletaAnchoOriginal = null;
        paletaTamanoExpiraMs = 0L;
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

    public void setProbDropPowerUp(float prob) {
        this.probDropPowerUp = Math.max(0f, Math.min(1f, prob));
    }
}