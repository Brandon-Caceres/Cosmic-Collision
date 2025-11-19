package com.cosmic.collision;

/**
 * Clase base abstracta para objetos del juego.
 * Se exige implementar actualizar() para favorecer la extensibilidad.
 */
public abstract class ObjetoJuego {
    protected int x;
    protected int y;
    protected int ancho;
    protected int alto;

    protected ObjetoJuego(int x, int y, int ancho, int alto) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }

    public abstract void actualizar();
}