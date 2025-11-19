package com.cosmic.collision;

/**
 * Encapsula parámetros por dificultad.
 */
public class DifficultySettings {

    public final Dificultad dificultad;

    public final int anchoBasePaleta;
    public final int velPelotaX;
    public final int velPelotaY;
    public final int filasBase;
    public final int incrementoFilasPorNivel;
    public final float velocidadPaleta;

    public final int anchoBloque;
    public final int altoBloque;
    public final int espaciadoHBloque;
    public final int espaciadoVBloque;
    public final int margenLR;
    public final int margenSuperior;
    public final int columnasObjetivoFacil;

    public final float tasaBloquesDuros;
    public final float tasaIrrompibles;
    public final boolean permitirIrrompibles;

    public DifficultySettings(Dificultad dif) {
        this.dificultad = dif;
        switch (dif) {
            case FACIL:
                anchoBasePaleta = 160;
                velPelotaX = 2;
                velPelotaY = 3;
                filasBase = 3;
                incrementoFilasPorNivel = 0;
                velocidadPaleta = 700f;

                columnasObjetivoFacil = 8;
                espaciadoHBloque = 16;
                espaciadoVBloque = 14;
                margenLR = 20;
                margenSuperior = 20;

                anchoBloque = 70;
                altoBloque = 26;

                tasaBloquesDuros = 0f;
                tasaIrrompibles = 0f;
                permitirIrrompibles = false;
                break;
            case MEDIA:
                anchoBasePaleta = 110;
                velPelotaX = 4;
                velPelotaY = 5;
                filasBase = 5;
                incrementoFilasPorNivel = 1;
                velocidadPaleta = 1000f;

                anchoBloque = 70;
                altoBloque = 26;
                espaciadoHBloque = 10;
                espaciadoVBloque = 10;
                margenLR = 10;
                margenSuperior = 10;
                columnasObjetivoFacil = 0;

                tasaBloquesDuros = 0.25f;
                tasaIrrompibles = 0.0f;
                permitirIrrompibles = false;
                break;
            case DIFICIL:
                anchoBasePaleta = 90;
                velPelotaX = 5;
                velPelotaY = 6;
                filasBase = 7;
                incrementoFilasPorNivel = 2;
                velocidadPaleta = 1500f;

                anchoBloque = 70;
                altoBloque = 26;
                espaciadoHBloque = 10;
                espaciadoVBloque = 10;
                margenLR = 10;
                margenSuperior = 10;
                columnasObjetivoFacil = 0;

                tasaBloquesDuros = 0.40f;
                tasaIrrompibles = 0.15f;
                permitirIrrompibles = true;
                break;
            default:
                throw new IllegalArgumentException("Dificultad no soportada");
        }
    }
}