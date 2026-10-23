package com.redxela;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Clase principal de inicio de la aplicación REDXela.
 * 
 * Concepto de SO: Proceso raíz del sistema operativo simulado que inicializa
 * el despachador de tareas, la administración de recursos y el espacio de memoria compartida.
 */
@SpringBootApplication(scanBasePackages = "com.redxela")
@ConfigurationPropertiesScan("com.redxela.config")
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
