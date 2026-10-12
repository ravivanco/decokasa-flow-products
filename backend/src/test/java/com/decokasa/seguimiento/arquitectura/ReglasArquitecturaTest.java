package com.decokasa.seguimiento.arquitectura;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas de arquitectura de documentos/arquitectura.md (sección 2) y ADR-0011.
 * El núcleo (domain y application) es Java puro; las dependencias apuntan siempre hacia adentro.
 */
@AnalyzeClasses(packages = "com.decokasa.seguimiento", importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasArquitecturaTest {

    private static final String DOMINIO = "..domain..";
    private static final String APLICACION = "..application..";

    // R1: domain y application no importan Spring, JPA, Hibernate, PostgreSQL, Google ni librerías de JWT o JSON.
    @ArchTest
    static final ArchRule r1NucleoSinFrameworks = noClasses()
            .that().resideInAnyPackage(DOMINIO, APLICACION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.transaction..",
                    "org.hibernate..",
                    "org.postgresql..",
                    "org.flywaydb..",
                    "com.google..",
                    "io.jsonwebtoken..",
                    "com.nimbusds..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..")
            .because("R1: el dominio no depende de Spring, JPA ni Google (CLAUDE.md, ADR-0011)");

    // R2: las clases del núcleo no llevan anotaciones de Spring; se registran como beans en config/.
    @ArchTest
    static final ArchRule r2NucleoSinAnotacionesDeSpring = noClasses()
            .that().resideInAnyPackage(DOMINIO, APLICACION)
            .should().beAnnotatedWith(DescribedPredicate.describe(
                    "una anotación de Spring",
                    (JavaAnnotation<?> anotacion) -> anotacion.getRawType().getPackageName().startsWith("org.springframework")))
            .because("R2: el núcleo se prueba sin levantar Spring");

    // R3: las entidades JPA viven solo en adapter.out.persistence.
    @ArchTest
    static final ArchRule r3EntidadesSoloEnPersistencia = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage("..adapter.out.persistence..")
            .allowEmptyShould(true)
            .because("R3: el modelo de la base de datos no se filtra al dominio");

    // R4: ningún domain importa el domain de otro módulo; los tipos comunes viven en compartido.domain.
    @ArchTest
    static final ArchRule r4DominiosIndependientes = slices()
            .matching("com.decokasa.seguimiento.(*).domain..")
            .should().notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAPackage("com.decokasa.seguimiento.compartido.domain.."))
            .allowEmptyShould(true)
            .because("R4: un módulo usa a otro solo por sus puertos de entrada o por eventos");

    // R9: no hay dependencias circulares entre módulos.
    @ArchTest
    static final ArchRule r9SinCiclosEntreModulos = slices()
            .matching("com.decokasa.seguimiento.(*)..")
            .should().beFreeOfCycles()
            .because("R9: evitar el acoplamiento entre módulos");

    // Regla de dependencias de la arquitectura hexagonal: siempre hacia adentro.
    @ArchTest
    static final ArchRule dominioNoDependeDeCapasExternas = noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAnyPackage(APLICACION, "..adapter..", "..config..", "..configuracion..")
            .because("el dominio no depende de nada externo (arquitectura.md, sección 1)");

    @ArchTest
    static final ArchRule aplicacionNoDependeDeAdaptadores = noClasses()
            .that().resideInAPackage(APLICACION)
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..", "..configuracion..")
            .because("la aplicación depende solo del dominio y de las interfaces de los puertos");
}
