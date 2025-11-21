package com.cosmic.collision;

import java.util.List;

/**
 * Abstract Factory: interfaz para crear familias de Bloque.
 */
public interface BlockFactory {
    List<Bloque> crearBloques(int filas, DifficultySettings settings, float anchoMundo, float altoMundo);
}