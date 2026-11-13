package com.stan.poc.spi2iso;

import com.stan.poc.spi2iso.service.Spi1ToIso20022Service;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

/**
 * Punto de entrada del PoC. Al arrancar (con el perfil "demo" o pasando el argumento
 * --demo.file=examples/spi1-demo.txt) procesa automaticamente el archivo SPI1 de ejemplo
 * y deja levantada la API REST en http://localhost:8080 con la consola H2 en /h2-console.
 */
@SpringBootApplication
public class Spi1Iso20022PocApplication {

    public static void main(String[] args) {
        SpringApplication.run(Spi1Iso20022PocApplication.class, args);
    }

    @Bean
    public CommandLineRunner demoRunner(Spi1ToIso20022Service service) {
        return args -> {
            String demoFile = System.getProperty("demo.file");
            if (demoFile == null) {
                return;
            }
            System.out.println("=== PoC SPI1 -> ISO 20022: procesando archivo de demo: " + demoFile + " ===");
            var resultados = service.procesarArchivo(Path.of(demoFile));
            resultados.forEach(r -> System.out.println(
                    "  [" + r.numeroInstruccion() + "] estado=" + r.estado() + " -> " + r.detalle()));
            System.out.println("=== Fin de demo. " + resultados.size() + " transacciones procesadas. ===");
        };
    }
}
