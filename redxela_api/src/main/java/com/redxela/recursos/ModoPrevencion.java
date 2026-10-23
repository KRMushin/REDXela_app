package com.redxela.recursos;

/**
 * Modos de asignación de recursos en el sistema.
 * 
 * Concepto de SO: Estrategias de manejo de interbloqueos.
 * PREVENCION_ACTIVA: Aplica la regla de Havender (adquisición ordenada globalmente)
 * para invalidar la condición de Coffman de espera circular.
 * SIN_PREVENCION: Permite retención y espera arbitraria en orden natural, admitiendo deadlocks.
 */
public enum ModoPrevencion {
    /** Ordena las solicitudes según el rango global de los recursos antes de adquirirlos. */
    PREVENCION_ACTIVA,

    /** Solicita los recursos en el orden natural del tipo de mercancía, reteniendo los previos. */
    SIN_PREVENCION
}
