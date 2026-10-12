# Imagen del backend de Decokasa (paso 1.4). Multietapa: compila con el JDK y ejecuta con el JRE.
# Se construye desde backend/ (docker compose lo hace con `context: ./backend`).

# --- Compilación ---
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /fuente

# Primero el wrapper y la configuración de Gradle: así la descarga de dependencias queda en caché
# mientras no cambie build.gradle.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src ./src
# Las pruebas no corren aquí: necesitan Docker (Testcontainers) y se ejecutan con ./gradlew check.
RUN ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/seguimiento-*.jar /fuente/aplicacion.jar

# --- Ejecución ---
FROM eclipse-temurin:21-jre
WORKDIR /aplicacion

# Usuario sin privilegios: la aplicación no corre como root.
RUN groupadd --system decokasa && useradd --system --gid decokasa --no-create-home decokasa
COPY --from=compilacion --chown=decokasa:decokasa /fuente/aplicacion.jar ./aplicacion.jar
USER decokasa

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/aplicacion/aplicacion.jar"]
