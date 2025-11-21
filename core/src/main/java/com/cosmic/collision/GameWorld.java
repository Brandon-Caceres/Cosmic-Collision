package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Mundo de juego completo con:
 * - múltiples pelotas (split)
 * - power-ups (spawn, pickup por paleta, efectos)
 * - bola explosiva (área)
 * - SPEED_UP / SPEED_DOWN (temporal)
 * - PADDLE_GROW / PADDLE_SHRINK (temporal)
 * - lógica por dificultad delegada a DifficultyStrategy
 */
public class GameWorld {

    private Plataforma paleta;
    private List<BolaPing> pelotas = new ArrayList<>(); // ahora manejo varias bolas
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
    private float probDropPowerUp = 0.25f; // 25% base

    // Bola explosiva
    private boolean bolaExplosivaActiva = false;
    private long bolaExplosivaHastaMs = 0;
    private final float radioExplosionPx = 90f; // área de efecto alrededor del bloque impactado

    // Velocidad temporal para bolas
    private Float bolaSpeedMultiplicador = null; // null = no activo
    private long bolaSpeedExpiraMs = 0L;

    // ---- Temporal paddle size (duración según dificultad) ----
    private Integer paletaAnchoOriginal = null;
    private long paletaTamanoExpiraMs = 0L;

    // Strategy
    private DifficultyStrategy difficultyStrategy = null;

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

        // inicializar estrategia según settings
        this.difficultyStrategy = strategyFor(initialSettings.dificultad);

        iniciarJuego();
    }

    // --- Strategy helper ---
    private DifficultyStrategy strategyFor(Dificultad d) {
        switch (d) {
            case FACIL: return new EasyStrategy();
            case MEDIA: return new MediumStrategy();
            case DIFICIL: return new HardStrategy();
            default: return new MediumStrategy();
        }
    }

    public void aplicarDificultad(DifficultySettings nueva) {
        this.settings = nueva;
        this.difficultyStrategy = strategyFor(nueva.dificultad);
    }

    public void iniciarJuego() {
        puntaje = 0;
        vidas = 3;
        nivel = 1;
        powerUps.clear();
        bolaExplosivaActiva = false;
        bolaExplosivaHastaMs = 0;
        bolaSpeedMultiplicador = null;
        bolaSpeedExpiraMs = 0L;

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

        pelotas.clear();
        pelotas.add(new BolaPing(
                (int)(Gdx.graphics.getWidth()/2f - 10),
                paleta.getY() + paleta.getAlto() + 11,
                10,
                velPelotaX,
                velPelotaY,
                true
        ));
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

        // Expiraciones de efectos temporales
        if (bolaExplosivaActiva && ahora >= bolaExplosivaHastaMs) {
            bolaExplosivaActiva = false;
        }
        if (bolaSpeedMultiplicador != null && ahora >= bolaSpeedExpiraMs) {
            for (BolaPing b : pelotas) b.restaurarVelBase();
            bolaSpeedMultiplicador = null;
            bolaSpeedExpiraMs = 0L;
        }
        if (paletaAnchoOriginal != null && ahora >= paletaTamanoExpiraMs) {
            restaurarTamanoPaletaOriginal();
        }

        paleta.actualizar();

        // Actualizar pelotas
        Iterator<BolaPing> it = pelotas.iterator();
        while (it.hasNext()) {
            BolaPing bp = it.next();
            if (bp.estaQuieta()) {
                bp.setXY(paleta.getX() + paleta.getAncho()/2 - 5, paleta.getY() + paleta.getAlto() + 11);
                if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) bp.setEstaQuieta(false);
            } else {
                bp.actualizar();
            }
        }

        // Si todas las bolas cayeron, perder vida y reiniciar una bola
        boolean algunaEnPantalla = false;
        for (BolaPing bp : pelotas) {
            if (bp.getY() + bp.getRadio() >= 0) { algunaEnPantalla = true; break; }
        }
        if (!algunaEnPantalla) {
            vidas--;
            pelotas.clear();
            pelotas.add(new BolaPing(
                    paleta.getX() + paleta.getAncho()/2 - 5,
                    paleta.getY() + paleta.getAlto() + 11,
                    10, velPelotaX, velPelotaY, true
            ));
        }

        // Efecto: bola explosiva — detectar bloque impactado y aplicar daño en área
        Bloque bloqueImpactado = detectarImpactoConAlgúnBloque();
        if (bolaExplosivaActiva && bloqueImpactado != null) {
            aplicarExplosionAlrededorDe(bloqueImpactado);
        }

        // Colisiones normales de cada bola con bloques y paleta
        for (BolaPing bp : pelotas) {
            for (Bloque b : bloques) {
                bp.comprobarColision(b);
            }
            bp.comprobarColision(paleta);
        }

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

        // Actualizar power-ups cayendo y aplicar si tocan la paleta (solo si p.isActive())
        actualizarYAplicarPowerUps();

        // Progreso de nivel (igual que antes)
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
            pelotas.clear();
            pelotas.add(new BolaPing(
                    paleta.getX() + paleta.getAncho()/2 - 5,
                    paleta.getY() + paleta.getAlto() + 11,
                    10, velPelotaX, velPelotaY, true
            ));
        }
    }

    public void dibujar(SpriteBatch batch, ShapeRenderer sr, float ancho, float alto) {
        // Batch: paleta, bloques, HUD
        batch.begin();
        paleta.dibujar(batch);
        for (Bloque b : bloques) b.dibujar(batch);
        hud.dibujar(batch, ancho, alto, puntaje, vidas, nivel, settings.dificultad,
                mostrarBonificacionVidaHastaMs, TimeUtils.millis());
        batch.end();

        // Shapes: pelotas + power-ups
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (BolaPing bp : pelotas) bp.dibujar(sr);
        for (PowerUp p : powerUps) p.dibujar(sr);
        sr.end();
    }

    // --------------------- power-up spawn / pickup ----------------------

    private void intentarSoltarPowerUp(Bloque b) {
        float prob = getProbDropPowerUpForDifficulty();
        if (Math.random() < prob) {
            PowerUpType tipo = sortearTipoPowerUp();
            int size = 22;
            int px = b.getX() + b.getAncho()/2 - size/2;
            // spawn justo debajo del bloque para evitar solapamiento con la bola que lo golpeó
            int py = Math.max(0, b.getY() - size - 2);
            powerUps.add(new PowerUp(px, py, size, size, tipo));
        }
    }

    private float getProbDropPowerUpForDifficulty() {
        if (difficultyStrategy != null) {
            return probDropPowerUp * difficultyStrategy.getProbDropModifier();
        }
        // fallback
        switch (settings.dificultad) {
            case FACIL: return probDropPowerUp * 1.25f;
            case MEDIA: return probDropPowerUp;
            case DIFICIL: return probDropPowerUp * 0.7f;
            default: return probDropPowerUp;
        }
    }

    /**
     * Sortea tipo de power-up usando la estrategia si está disponible.
     */
    private PowerUpType sortearTipoPowerUp() {
        double grow, shrink, explosive, life, split, sUp, sDown;

        if (difficultyStrategy != null) {
            PowerUpDistribution d = difficultyStrategy.getBaseDistribution();
            grow = d.grow; shrink = d.shrink; explosive = d.explosive; life = d.life;
            split = d.split; sUp = d.speedUp; sDown = d.speedDown;
        } else {
            // fallback values (original)
            switch (settings.dificultad) {
                case FACIL:
                    grow = 0.40; shrink = 0.10; explosive = 0.15; life = 0.20; split = 0.10; sUp = 0.03; sDown = 0.02;
                    break;
                case MEDIA:
                    grow = 0.30; shrink = 0.20; explosive = 0.15; life = 0.15; split = 0.12; sUp = 0.05; sDown = 0.03;
                    break;
                case DIFICIL:
                    grow = 0.15; shrink = 0.45; explosive = 0.15; life = 0.05; split = 0.12; sUp = 0.06; sDown = 0.02;
                    break;
                default:
                    grow = 0.30; shrink = 0.25; explosive = 0.15; life = 0.15; split = 0.10; sUp = 0.03; sDown = 0.02;
            }
        }

        // Sesgo por nivel
        double levelSkew = Math.min(0.25, Math.max(0.0, (nivel - 1) * 0.02));
        double utilesSum = grow + life + sUp;
        if (utilesSum > 0 && levelSkew > 0) {
            double reducTotal = utilesSum * levelSkew;
            double growRed = (grow / utilesSum) * reducTotal;
            double lifeRed = (life / utilesSum) * reducTotal;
            double upRed = (sUp / utilesSum) * reducTotal;

            grow = Math.max(0.0, grow - growRed);
            life = Math.max(0.0, life - lifeRed);
            sUp = Math.max(0.0, sUp - upRed);

            shrink += (growRed + lifeRed + upRed);
        }

        double suma = grow + shrink + explosive + life + split + sUp + sDown;
        if (suma <= 0) return PowerUpType.PADDLE_SHRINK;

        grow /= suma; shrink /= suma; explosive /= suma; life /= suma; split /= suma; sUp /= suma; sDown /= suma;

        double r = Math.random();
        if (r < grow) return PowerUpType.PADDLE_GROW;
        r -= grow;
        if (r < shrink) return PowerUpType.PADDLE_SHRINK;
        r -= shrink;
        if (r < explosive) return PowerUpType.EXPLOSIVE_BALL;
        r -= explosive;
        if (r < life) return PowerUpType.EXTRA_LIFE;
        r -= life;
        if (r < split) return PowerUpType.SPLIT_BALL;
        r -= split;
        if (r < sUp) return PowerUpType.SPEED_UP;
        return PowerUpType.SPEED_DOWN;
    }

    private void actualizarYAplicarPowerUps() {
        Rectangle rectPaleta = paleta.getRect();
        for (int i = 0; i < powerUps.size(); i++) {
            PowerUp p = powerUps.get(i);
            p.actualizar();

            // Fuera de la pantalla -> eliminar
            if (p.getY() + p.getAlto() < 0) { powerUps.remove(i); i--; continue; }

            // Solo permitir pickup por paleta si el powerup ya es activo (pasó el small delay)
            if (p.isActive() && p.getRect().overlaps(rectPaleta)) {
                aplicarPowerUp(p.getTipo());
                powerUps.remove(i);
                i--;
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
                if (paletaAnchoOriginal == null) paletaAnchoOriginal = paleta.getAncho();

                int nuevoAncho = Math.min(260, paleta.getAncho() + 30);
                float centro = paleta.getX() + paleta.getAncho() / 2f;
                int nuevoX = Math.round(centro - nuevoAncho / 2f);
                nuevoX = Math.max(0, Math.min(nuevoX, Gdx.graphics.getWidth() - nuevoAncho));
                paleta = new Plataforma(nuevoX, paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
                paleta.setVelPxPorSeg(settings.velocidadPaleta);

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
            case SPLIT_BALL:
                splitBalls();
                break;
            case SPEED_UP:
                applyBallSpeedMultiplier(1.5f, getPaletaDurationMs());
                break;
            case SPEED_DOWN:
                applyBallSpeedMultiplier(0.6f, getPaletaDurationMs());
                break;
        }
    }

    private void splitBalls() {
        List<BolaPing> nuevas = new ArrayList<>();
        for (BolaPing b : pelotas) {
            int yPos = b.getY();
            int xPos = b.getX();
            int speedX = Math.max(2, Math.abs(velPelotaX));
            int speedY = Math.max(3, Math.abs(velPelotaY));

            BolaPing bl = new BolaPing(xPos, yPos, b.getRadio(), -speedX, speedY, false);
            BolaPing bm = new BolaPing(xPos, yPos, b.getRadio(), 0, speedY, false);
            BolaPing br = new BolaPing(xPos, yPos, b.getRadio(), speedX, speedY, false);

            if (bolaSpeedMultiplicador != null) {
                bl.aplicarMultiplicadorVelocidad(bolaSpeedMultiplicador);
                bm.aplicarMultiplicadorVelocidad(bolaSpeedMultiplicador);
                br.aplicarMultiplicadorVelocidad(bolaSpeedMultiplicador);
            }

            nuevas.add(bl);
            nuevas.add(bm);
            nuevas.add(br);
        }
        pelotas.addAll(nuevas);
    }

    private void applyBallSpeedMultiplier(float mult, long durationMs) {
        bolaSpeedMultiplicador = mult;
        bolaSpeedExpiraMs = TimeUtils.millis() + durationMs;
        for (BolaPing b : pelotas) b.aplicarMultiplicadorVelocidad(mult);
    }

    private long getPaletaDurationMs() {
        if (difficultyStrategy != null) return difficultyStrategy.getPaletaDurationMs();
        // fallback
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
        for (BolaPing b : pelotas) {
            if (b.estaQuieta()) continue;
            int cx = b.getX();
            int cy = b.getY();
            int r  = b.getRadio();
            for (Bloque bl : bloques) {
                if (circleIntersectsRect(cx, cy, r, bl.getRect())) return bl;
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
            if (dx*dx + dy*dy <= r2) b.destruir();
        }
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

    public void setProbDropPowerUp(float prob) { this.probDropPowerUp = Math.max(0f, Math.min(1f, prob)); }

    // ---------- Getters expuestos para Main / HUD ----------
    public int getVidas() { return vidas; }
    public int getNivel() { return nivel; }
    public int getPuntaje() { return puntaje; }
    public DifficultySettings getSettings() { return settings; }

    public void reiniciarNivel() {
        crearBloques(filasParaNivel(nivel));
        powerUps.clear();

        if (paletaAnchoOriginal != null) {
            restaurarTamanoPaletaOriginal();
        }

        bolaSpeedMultiplicador = null;
        bolaSpeedExpiraMs = 0L;

        pelotas.clear();
        pelotas.add(new BolaPing(
                paleta.getX() + paleta.getAncho()/2 - 5,
                paleta.getY() + paleta.getAlto() + 11,
                10, velPelotaX, velPelotaY, true
        ));
    }
}