package com.decokasa.seguimiento.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class FronterasP20Test {
    @Test void r4PedidosIncluidosAdaptadoresNoUsaDominioNiAdaptadoresDeHistorial() {
        var clases=new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.decokasa.seguimiento");
        noClasses().that().resideInAPackage("com.decokasa.seguimiento.pedidos..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.decokasa.seguimiento.historial.domain..",
                        "com.decokasa.seguimiento.historial.adapter..")
                .because("R4 y P20-R4-01 incluyen respuestas HTTP, firmas y adaptadores de pedidos")
                .check(clases);
    }
    @Test void r9HistorialNoDependeDePedidosParaProducirLosCierres() {
        var clases=new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.decokasa.seguimiento");
        noClasses().that().resideInAPackage("com.decokasa.seguimiento.historial..")
                .should().dependOnClassesThat().resideInAPackage("com.decokasa.seguimiento.pedidos..")
                .because("R9: pedidos consume historial; historial no debe volver a depender de pedidos")
                .check(clases);
    }
}
