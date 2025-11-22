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
 * GameWorld con Strategy completo:
 * - FÁCIL: no cambia ancho/velocidad en level-up, se resetean efectos temporales.
 * - MEDIA/DIFÍCIL: progresión de velocidad y reducción de paleta via Strategy.
 */
public class GameWorld {

    private Plataforma paleta;
    private final List<BolaPing> pelotas = new ArrayList<>();
    private final List<Bloque> bloques = new ArrayList<>();
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

    private final List<PowerUp> powerUps = new ArrayList<>();
    private float probDropPowerUp = 0.25f;

    private boolean bolaExplosivaActiva = false;
    private long bolaExplosivaHastaMs = 0;
    private final float radioExplosionPx = 90f;

    private Float bolaSpeedMultiplicador = null;
    private long bolaSpeedExpiraMs = 0L;

    private Integer paletaAnchoOriginal = null;
    private long paletaTamanoExpiraMs = 0L;

    private DifficultyStrategy difficultyStrategy;

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
        this.difficultyStrategy = strategyFor(initialSettings.dificultad);
        iniciarJuego();
    }

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

        // Si todas cayeron
        boolean algunaEnPantalla = pelotas.stream().anyMatch(b -> b.getY() + b.getRadio() >= 0);
        if (!algunaEnPantalla) {
            vidas--;
            pelotas.clear();
            pelotas.add(new BolaPing(
                    paleta.getX() + paleta.getAncho()/2 - 5,
                    paleta.getY() + paleta.getAlto() + 11,
                    10, velPelotaX, velPelotaY, true
            ));
        }

        Bloque bloqueImpactado = detectarImpactoConAlgúnBloque();
        if (bolaExplosivaActiva && bloqueImpactado != null) {
            aplicarExplosionAlrededorDe(bloqueImpactado);
        }

        // Colisiones
        for (BolaPing bp : pelotas) {
            for (Bloque b : bloques) {
                bp.comprobarColision(b);
            }
            bp.comprobarColision(paleta);
        }

        // Bloques destruidos
        for (int i = 0; i < bloques.size(); i++) {
            Bloque b = bloques.get(i);
            if (b.estaDestruido()) {
                puntaje++;
                intentarSoltarPowerUp(b);
                bloques.remove(i);
                i--;
            }
        }

        actualizarYAplicarPowerUps();

        // Avance de nivel
        if (bloques.isEmpty()) {
            nivel++;

            double probVidaExtra = difficultyStrategy.getExtraLifeProbability(nivel);
            if (Math.random() < probVidaExtra) {
                vidas++;
                mostrarBonificacionVidaHastaMs = TimeUtils.millis() + duracionBonificacionVida;
            }

            // Progresión vía strategy
            LevelProgressionResult prog = difficultyStrategy.applyLevelProgression(velPelotaX, velPelotaY, paleta.getAncho());

            if (prog.resetEffects) {
                // Limpieza total en FÁCIL para asegurar que no arrastre efectos temporales.
                bolaSpeedMultiplicador = null;
                bolaSpeedExpiraMs = 0L;
                if (paletaAnchoOriginal != null) {
                    restaurarTamanoPaletaOriginal();
                }
                // Restablecer velocidades base estrictas de la dificultad
                velPelotaX = settings.velPelotaX;
                velPelotaY = settings.velPelotaY;
            } else {
                velPelotaX = prog.newVelX;
                velPelotaY = prog.newVelY;
                if (prog.newPaddleWidth != paleta.getAncho()) {
                    paleta = new Plataforma(paleta.getX(), paleta.getY(), prog.newPaddleWidth, paleta.getAlto(), texturaPaleta);
                    paleta.setVelPxPorSeg(settings.velocidadPaleta);
                }
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
        batch.begin();
        paleta.dibujar(batch);
        for (Bloque b : bloques) b.dibujar(batch);
        hud.dibujar(batch, ancho, alto, puntaje, vidas, nivel, settings.dificultad,
                mostrarBonificacionVidaHastaMs, TimeUtils.millis());
        batch.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (BolaPing bp : pelotas) bp.dibujar(sr);
        for (PowerUp p : powerUps) p.dibujar(sr);
        sr.end();
    }

    private void intentarSoltarPowerUp(Bloque b) {
        float prob = getProbDropPowerUpForDifficulty();
        if (Math.random() < prob) {
            PowerUpType tipo = sortearTipoPowerUp();
            int size = 22;
            int px = b.getX() + b.getAncho()/2 - size/2;
            int py = Math.max(0, b.getY() - size - 2);
            powerUps.add(new PowerUp(px, py, size, size, tipo));
        }
    }

    private float getProbDropPowerUpForDifficulty() {
        if (difficultyStrategy != null) {
            return probDropPowerUp * difficultyStrategy.getProbDropModifier();
        }
        return probDropPowerUp;
    }

    private PowerUpType sortearTipoPowerUp() {
        PowerUpDistribution dist = (difficultyStrategy != null)
                ? difficultyStrategy.adjustDistributionForLevel(difficultyStrategy.getBaseDistribution(), nivel)
                : new PowerUpDistribution(0.30,0.25,0.15,0.15,0.10,0.03,0.02);

        double suma = dist.grow + dist.shrink + dist.explosive + dist.life + dist.split + dist.speedUp + dist.speedDown;
        if (suma <= 0) return PowerUpType.PADDLE_SHRINK;

        dist.grow     /= suma;
        dist.shrink   /= suma;
        dist.explosive/= suma;
        dist.life     /= suma;
        dist.split    /= suma;
        dist.speedUp  /= suma;
        dist.speedDown/= suma;

        double r = Math.random();
        if (r < dist.grow) return PowerUpType.PADDLE_GROW;
        r -= dist.grow;
        if (r < dist.shrink) return PowerUpType.PADDLE_SHRINK;
        r -= dist.shrink;
        if (r < dist.explosive) return PowerUpType.EXPLOSIVE_BALL;
        r -= dist.explosive;
        if (r < dist.life) return PowerUpType.EXTRA_LIFE;
        r -= dist.life;
        if (r < dist.split) return PowerUpType.SPLIT_BALL;
        r -= dist.split;
        if (r < dist.speedUp) return PowerUpType.SPEED_UP;
        return PowerUpType.SPEED_DOWN;
    }

    private void actualizarYAplicarPowerUps() {
        Rectangle rectPaleta = paleta.getRect();
        for (int i = 0; i < powerUps.size(); i++) {
            PowerUp p = powerUps.get(i);
            p.actualizar();

            if (p.getY() + p.getAlto() < 0) { powerUps.remove(i); i--; continue; }

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
                bolaExplosivaHastaMs = ahora + 8000;
                break;
            case PADDLE_GROW:
                modificarPaleta(+30, ahora);
                break;
            case PADDLE_SHRINK:
                modificarPaleta(-30, ahora);
                break;
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

    private void modificarPaleta(int deltaAncho, long ahora) {
        if (paletaAnchoOriginal == null) paletaAnchoOriginal = paleta.getAncho();
        int nuevoAncho = deltaAncho > 0
                ? Math.min(260, paleta.getAncho() + deltaAncho)
                : Math.max(50, paleta.getAncho() + deltaAncho);
        float centro = paleta.getX() + paleta.getAncho() / 2f;
        int nuevoX = Math.round(centro - nuevoAncho / 2f);
        nuevoX = Math.max(0, Math.min(nuevoX, Gdx.graphics.getWidth() - nuevoAncho));
        paleta = new Plataforma(nuevoX, paleta.getY(), nuevoAncho, paleta.getAlto(), texturaPaleta);
        paleta.setVelPxPorSeg(settings.velocidadPaleta);
        paletaTamanoExpiraMs = ahora + getPaletaDurationMs();
    }

    private void splitBalls() {
        List<BolaPing> nuevas = new ArrayList<>();
        for (BolaPing b : pelotas) {
            int yPos = b.getY();
            int xPos = b.getX();
            int speedX = Math.max(2, Math.abs(velPelotaX));
            int speedY = Math.max(3, Math.abs(velPelotaY));
            BolaPing bl = new BolaPing(xPos, yPos, b.getRadio(), -speedX, speedY, false);
            BolaPing bm = new BolaPing(xPos, yPos, b.getRadio(), 0,       speedY, false);
            BolaPing br = new BolaPing(xPos, yPos, b.getRadio(), speedX,  speedY, false);
            if (bolaSpeedMultiplicador != null) {
                bl.aplicarMultiplicadorVelocidad(bolaSpeedMultiplicador);
                bm.aplicarMultiplicadorVelocidad(bolaSpeedMultiplicador);
                br.aplicarMultiplicadorVelocidad(bolaSpeedMultiplicador);
            }
            nuevas.add(bl); nuevas.add(bm); nuevas.add(br);
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
        return 5000L;
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
    public int getVidas() { return vidas; }
    public int getNivel() { return nivel; }
    public int getPuntaje() { return puntaje; }
    public DifficultySettings getSettings() { return settings; }

    public void reiniciarNivel() {
        crearBloques(filasParaNivel(nivel));
        powerUps.clear();
        // Limpiar efectos temporales (si activos)
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