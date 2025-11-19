package com.cosmic.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Pantalla de Tutorial con save/restore de escala de fuente.
 */
public class TutorialScreen {

    public interface Listener { void onSalirMenu(); }

    private final int MAX_PAGINAS = 3;
    private int pagina = 1;

    private final BitmapFont fuente;
    private final GlyphLayout layout = new GlyphLayout();
    private final Listener listener;

    public TutorialScreen(BitmapFont fuente, Listener listener) {
        this.fuente = fuente;
        this.listener = listener;
    }

    public void render(SpriteBatch batch, float ancho, float alto) {
        float oldX = fuente.getData().scaleX, oldY = fuente.getData().scaleY;

        batch.begin();
        batch.setColor(1f,1f,1f,1f);

        // Título
        fuente.getData().setScale(3.0f);
        String titulo = "TUTORIAL DEL JUEGO (" + pagina + " de " + MAX_PAGINAS + ")";
        layout.setText(fuente, titulo);
        fuente.draw(batch, titulo, (ancho - layout.width)/2f, alto - 80);

        float y = alto - 150;
        float inter = 44f;
        float x0 = 80;

        // Contenido
        fuente.getData().setScale(1.8f);
        if (pagina == 1) {
            draw(batch, "OBJETIVO PRINCIPAL:", x0, y); y -= inter;
            draw(batch, "Destruye TODOS los bloques para pasar de nivel.", x0 + 30, y); y -= inter * 1.5f;
            draw(batch, "VIDAS Y FIN DE JUEGO:", x0, y); y -= inter;
            draw(batch, "Empiezas con 3 vidas. Si la bola cae, pierdes una. Sin vidas: Fin de Juego.", x0 + 30, y); y -= inter * 1.5f;
            draw(batch, "PROGRESO Y RECOMPENSA:", x0, y); y -= inter;
            draw(batch, "La velocidad de la bola aumenta ligeramente con cada nivel.", x0 + 30, y); y -= inter;
            draw(batch, "¡Tienes probabilidad de ganar una vida extra al completar un nivel!", x0 + 30, y); y -= inter;
        } else if (pagina == 2) {
            draw(batch, "TIPOS DE BLOQUES:", x0, y); y -= inter;
            draw(batch, "- Normales: Se rompen con 1 golpe.", x0 + 30, y); y -= inter;
            draw(batch, "- Duros (2-3 golpes): Aparecen en MEDIA y DIFÍCIL.", x0 + 30, y); y -= inter;
            draw(batch, "- Irrompibles: No se destruyen. Solo en DIFÍCIL.", x0 + 30, y); y -= inter * 1.5f;
            draw(batch, "AJUSTES POR DIFICULTAD:", x0, y); y -= inter;
            draw(batch, "- FÁCIL: Pala grande, bola lenta.", x0 + 30, y); y -= inter;
            draw(batch, "- MEDIA/DIFÍCIL: Pala se encoge y la bola acelera progresivamente.", x0 + 30, y); y -= inter;
        } else if (pagina == 3) {
            draw(batch, "CONTROLES DE JUEGO:", x0, y); y -= inter;
            draw(batch, "MOVER PALETA: Flechas IZQUIERDA / DERECHA", x0 + 30, y); y -= inter;
            draw(batch, "LANZAR BOLA: ESPACIO", x0 + 30, y); y -= inter;
            draw(batch, "PAUSA / MENÚ: ESCAPE", x0 + 30, y); y -= inter * 1.5f;
            draw(batch, "CONTROLES DE MENÚ (PAUSA/PRINCIPAL):", x0, y); y -= inter;
            draw(batch, "NAVEGAR: Flechas ARRIBA / ABAJO", x0 + 30, y); y -= inter;
            draw(batch, "SELECCIONAR: ENTER / ESPACIO", x0 + 30, y); y -= inter;
        }

        // Pista navegación
        fuente.getData().setScale(2.5f);
        String pista = "IZQ/DER: Cambiar página | ESC/ENTER: Menú Principal";
        layout.setText(fuente, pista);
        fuente.draw(batch, pista, (ancho - layout.width)/2f, 100);

        batch.end();

        // Restaurar
        fuente.getData().setScale(oldX, oldY);
    }

    private void draw(SpriteBatch batch, String txt, float x, float y) {
        fuente.draw(batch, txt, x, y);
    }

    public void handleInput() {
        if (just(Input.Keys.ESCAPE) || just(Input.Keys.ENTER)) listener.onSalirMenu();
        if (just(Input.Keys.RIGHT)) pagina = Math.min(pagina + 1, MAX_PAGINAS);
        if (just(Input.Keys.LEFT))  pagina = Math.max(pagina - 1, 1);
    }

    private boolean just(int key) { return Gdx.input.isKeyJustPressed(key); }
}