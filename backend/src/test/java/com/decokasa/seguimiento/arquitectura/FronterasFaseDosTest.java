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
                        "..historial.domain..", "..historial.application.port.out..", "..historial.application.usecase..", "..historial.adapter..", "..historial.config..",
                        "..bodegas.domain..", "..bodegas.application.port.out..", "..bodegas.application.usecase..", "..bodegas.adapter..", "..bodegas.config..",
                        "..archivos.domain..", "..archivos.application.port.out..", "..archivos.application.usecase..", "..archivos.adapter..", "..archivos.config..")
                .because("R4: un módulo usa a otro solo por sus puertos de entrada o eventos; arquitectura.md §2")
                .check(clases);
    }
    @Test void r4BodegasSoloUsaPuertosDeEntradaDeUsuariosYSinDependenciaDePedidos() {
        var clases=new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.decokasa.seguimiento");
        noClasses().that().resideInAPackage("..bodegas.application..")
                .should().dependOnClassesThat().resideInAnyPackage("..pedidos..",
                        "..usuarios.domain..", "..usuarios.application.port.out..", "..usuarios.application.usecase..",
                        "..usuarios.adapter..", "..usuarios.config..")
                .because("R4: bodegas consulta usuarios por sus puertos publicos; R9: no vuelve a pedidos")
                .check(clases);
    }
}
