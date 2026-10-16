package com.decokasa.seguimiento.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class FronterasFaseDosTest {
    @Test void r4PedidosSoloUsaOtrosModulosPorPuertosDeEntradaOEventosCompartidos() {
        var clases=new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.decokasa.seguimiento");
        noClasses().that().resideInAPackage("..pedidos.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..flujos.domain..", "..flujos.application.port.out..", "..flujos.application.usecase..", "..flujos.adapter..", "..flujos.config..",
                        "..usuarios.domain..", "..usuarios.application.port.out..", "..usuarios.application.usecase..", "..usuarios.adapter..", "..usuarios.config..",
                        "..plazos.domain..", "..plazos.application.port.out..", "..plazos.application.usecase..", "..plazos.adapter..", "..plazos.config..",
                        "..historial.domain..", "..historial.application.port.out..", "..historial.application.usecase..", "..historial.adapter..", "..historial.config..")
                .because("R4: un módulo usa a otro solo por sus puertos de entrada o eventos; arquitectura.md §2")
                .check(clases);
    }
}
