package com.cosmic.collision;

import com.badlogic.gdx.graphics.Texture;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera bloques para un nivel dado respetando lógica original.
 */
public class BlockFactory {

    private final Texture txNormal;
    private final Texture txDuro2;
    private final Texture txDuro3;
    private final Texture txIrrompible;

    public BlockFactory(Texture normal, Texture duro2, Texture duro3, Texture irrompible) {
        this.txNormal = normal;
        this.txDuro2 = duro2;
        this.txDuro3 = duro3;
        this.txIrrompible = irrompible;
    }

    public List<Bloque> crearBloques(int filas, DifficultySettings settings, float anchoMundo, float altoMundo) {
        List<Bloque> lista = new ArrayList<>();
        int y = (int)altoMundo - settings.margenSuperior;

        if (settings.dificultad == Dificultad.FACIL) {
            int cols = Math.max(3, settings.columnasObjetivoFacil);
            float anchoDisponible = anchoMundo - (2 * settings.margenLR) - (settings.espaciadoHBloque * (cols - 1));
            int anchoB = Math.max(40, (int)(anchoDisponible / cols));
            int altoB = Math.max(26, (int)(anchoB * 0.38f));

            for (int fila = 0; fila < filas; fila++) {
                y -= (altoB + settings.espaciadoVBloque);
                if (y < 0) break;
                float anchoFila = cols * anchoB + (cols - 1) * settings.espaciadoHBloque;
                int inicioX = (int)Math.round((anchoMundo - anchoFila) / 2f);
                for (int c = 0; c < cols; c++) {
                    int x = inicioX + c * (anchoB + settings.espaciadoHBloque);
                    lista.add(new Bloque(x, y, anchoB, altoB, txNormal, txDuro2, txDuro3, txIrrompible));
                }
            }
        } else {
            int anchoB = settings.anchoBloque;
            int altoB = settings.altoBloque;
            float anchoDisponible = anchoMundo - (2 * settings.margenLR);
            int cols = Math.max(1, (int)Math.floor((anchoDisponible + settings.espaciadoHBloque) / (anchoB + settings.espaciadoHBloque)));
            float anchoFila = cols * anchoB + (cols - 1) * settings.espaciadoHBloque;
            int inicioX = Math.max(settings.margenLR, (int)Math.round((anchoMundo - anchoFila) / 2f));

            for (int fila = 0; fila < filas; fila++) {
                y -= (altoB + settings.espaciadoVBloque);
                if (y < 0) break;
                for (int c = 0; c < cols; c++) {
                    int x = inicioX + c * (anchoB + settings.espaciadoHBloque);
                    boolean irrompible = settings.permitirIrrompibles && Math.random() < settings.tasaIrrompibles;
                    boolean duro = !irrompible && Math.random() < settings.tasaBloquesDuros;
                    if (irrompible) {
                        lista.add(new Bloque(x, y, anchoB, altoB, 1, true, txNormal, txDuro2, txDuro3, txIrrompible));
                    } else if (duro) {
                        int hp = (settings.dificultad == Dificultad.DIFICIL)
                                ? (Math.random() < 0.5 ? 3 : 2) : 2;
                        lista.add(new Bloque(x, y, anchoB, altoB, hp, false, txNormal, txDuro2, txDuro3, txIrrompible));
                    } else {
                        lista.add(new Bloque(x, y, anchoB, altoB, txNormal, txDuro2, txDuro3, txIrrompible));
                    }
                }
            }
        }
        return lista;
    }
}