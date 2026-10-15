# ADR-0016: Spring Boot 4.1 en lugar de Spring Boot 3

**Fecha**: 2026-10-12
**Estado**: accepted (aprobado por el equipo el 2026-10-12)
**Decisores**: equipo de desarrollo (Richard)

## Contexto

El stack fijo (`CLAUDE.md`, ADR-0001) y el stack recomendado del cliente (`requerimientos.md` 1.6: "Spring Boot 3.x, Java 21 LTS") dicen Spring Boot 3. Al crear el backend en la fase 1 (paso 1.3) se comprobó en fuentes oficiales:

- La API oficial de spring.io (`api.spring.io/projects/spring-boot/generations`, consultada el 2026-10-12):

  | Rama | Fin del soporte gratuito (OSS) | Fin del soporte comercial |
  | --- | --- | --- |
  | 3.5.x (última de la 3) | 2026-06-30 | 2032-06-30 |
  | 4.0.x | 2026-12-31 | 2027-12-31 |
  | 4.1.x | 2027-07-31 | 2028-07-31 |

- Spring Initializr (start.spring.io) ya solo ofrece versiones 4.x; la versión estable es la 4.1.1.
- Requisitos de Spring Boot 4.1.1 (docs.spring.io/spring-boot/system-requirements.html): Java 17 o superior (probado hasta Java 26), Spring Framework 7.0.9 o superior, Gradle 8.14 o superior o Gradle 9.

## Decisión

El backend usa **Spring Boot 4.1.x** (la versión estable más reciente al crear el proyecto) con **Java 21 LTS**, que sigue siendo compatible. El resto del stack no cambia: Gradle (wrapper), Spring Security con JWT y BCrypt, Spring Data JPA, Flyway, PostgreSQL y Docker.

## Alternativas consideradas

### Spring Boot 3.5.x
- **Pros**: respeta el stack fijo tal como estaba escrito.
- **Contras**: sin parches de seguridad gratuitos desde el 2026-06-30; solo soporte comercial de pago.
- **Por qué no**: un sistema nuevo con inicio de sesión y datos del cliente no debe empezar sin parches de seguridad.

### Spring Boot 4.0.x
- **Pros**: primera versión de la rama 4.
- **Contras**: su soporte gratuito termina el 2026-12-31, antes de llegar a producción.
- **Por qué no**: obligaría a migrar enseguida.

## Consecuencias

### Positivas
- Parches de seguridad gratuitos hasta el 2027-07-31; después, migrar a la siguiente versión menor 4.x.

### Negativas
- Spring Boot 4 trae Spring Framework 7 y Spring Security 7, y divide las dependencias en módulos más pequeños (por ejemplo, Flyway necesita su propio starter). Algunos ejemplos y documentación de Spring Boot 3 ya no aplican tal cual.
- Hay que volver a comprobar la compatibilidad de las librerías externas (springdoc-openapi, Testcontainers, ArchUnit) con la rama 4.

### Riesgos
- Que una librería necesaria todavía no soporte Spring Boot 4. Mitigación: verificar cada versión en su documentación oficial al agregarla y anotarla en el PR.
- El soporte gratuito de la 4.1 termina el 2027-07-31: la fase 8 (producción) debe planificar la actualización a la versión menor vigente.

## Diferencias entre fuentes

- `requerimientos.md` 1.6 (stack recomendado por el cliente), ADR-0001, `CLAUDE.md` y el manual decían "Spring Boot 3". Se actualizan con referencia a este ADR. Java 21 no cambia.

## Pendiente del cliente

- Ninguno. Es una decisión técnica del equipo; conviene informarla al cliente porque cambia una versión de su stack recomendado.
