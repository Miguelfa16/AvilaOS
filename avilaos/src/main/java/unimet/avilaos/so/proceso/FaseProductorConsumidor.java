package unimet.avilaos.so.proceso;

/**
 * Paso del algoritmo productor-consumidor en el que va un proceso.
 * Productor:  PRODUCIR -> ACCESO_RED -> ESPERAR_VACIOS -> ESPERAR_MUTEX -> INSERTAR -> SENALAR_MUTEX -> SENALAR_LLENOS
 * Consumidor: ACCESO_RED -> ESPERAR_LLENOS -> ESPERAR_MUTEX -> EXTRAER -> SENALAR_MUTEX -> SENALAR_VACIOS -> CONSUMIR
 * (ACCESO_RED se salta si el buffer es local)
 */
public enum FaseProductorConsumidor {
    PRODUCIR, CONSUMIR, ACCESO_RED,
    ESPERAR_VACIOS, ESPERAR_LLENOS, ESPERAR_MUTEX,
    INSERTAR, EXTRAER,
    SENALAR_MUTEX, SENALAR_LLENOS, SENALAR_VACIOS
}
