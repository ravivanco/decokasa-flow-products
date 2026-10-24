package com.decokasa.seguimiento.compartido.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;
import com.decokasa.seguimiento.compartido.domain.RequisitoFaltante;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce los errores a application/problem+json (RFC 9457) con el "codigo" estable del contrato (esquema Problema).
 * Los errores del dominio traen su código; los de Spring (404, 405, JSON inválido…) reciben uno según su estado.
 * Un error inesperado responde 500 sin detalles internos y se registra en el log del servidor.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    private static final Map<CodigoError, HttpStatus> ESTADOS = new EnumMap<>(Map.ofEntries(
            Map.entry(CodigoError.VALIDACION, HttpStatus.BAD_REQUEST),
            Map.entry(CodigoError.NO_AUTENTICADO, HttpStatus.UNAUTHORIZED),
            Map.entry(CodigoError.SIN_PERMISO, HttpStatus.FORBIDDEN),
            Map.entry(CodigoError.NO_ENCONTRADO, HttpStatus.NOT_FOUND),
            Map.entry(CodigoError.ETAPA_CAMBIO, HttpStatus.CONFLICT),
            Map.entry(CodigoError.TRANSICION_NO_PERMITIDA, HttpStatus.CONFLICT),
            Map.entry(CodigoError.DUPLICADO, HttpStatus.CONFLICT),
            Map.entry(CodigoError.COPIA_PENDIENTE, HttpStatus.CONFLICT),
            Map.entry(CodigoError.ARCHIVO_DEMASIADO_GRANDE, HttpStatus.CONTENT_TOO_LARGE),
            Map.entry(CodigoError.TIPO_ARCHIVO_NO_ACEPTADO, HttpStatus.UNSUPPORTED_MEDIA_TYPE),
            Map.entry(CodigoError.REQUISITO_FALTANTE, HttpStatus.UNPROCESSABLE_CONTENT),
            Map.entry(CodigoError.CUENTA_BLOQUEADA, HttpStatus.LOCKED),
            Map.entry(CodigoError.VERSION_DESACTUALIZADA, HttpStatus.CONFLICT),
            Map.entry(CodigoError.ARCHIVO_NO_DISPONIBLE, HttpStatus.CONFLICT),
            Map.entry(CodigoError.CARGA_CONFLICTO, HttpStatus.CONFLICT),
            Map.entry(CodigoError.CARGA_INCOMPLETA, HttpStatus.CONFLICT),
            Map.entry(CodigoError.CARGA_OCUPADA, HttpStatus.CONFLICT),
            Map.entry(CodigoError.CARGA_ALMACENAMIENTO, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(CodigoError.CUOTA_ALMACENAMIENTO, HttpStatus.CONFLICT),
            Map.entry(CodigoError.GOOGLE_NO_DISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(CodigoError.IA_NO_DISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(CodigoError.LIMITE_PREGUNTAS, HttpStatus.BAD_REQUEST),
            Map.entry(CodigoError.LIMITE_CONSUMO, HttpStatus.TOO_MANY_REQUESTS),
            Map.entry(CodigoError.CONTEXTO_EXCEDIDO, HttpStatus.BAD_REQUEST)));

    private static final Map<CodigoError, String> TITULOS = new EnumMap<>(Map.ofEntries(
            Map.entry(CodigoError.VALIDACION, "Datos inválidos"),
            Map.entry(CodigoError.NO_AUTENTICADO, "No autenticado"),
            Map.entry(CodigoError.SIN_PERMISO, "Sin permiso"),
            Map.entry(CodigoError.NO_ENCONTRADO, "No encontrado"),
            Map.entry(CodigoError.ETAPA_CAMBIO, "La etapa cambió"),
            Map.entry(CodigoError.TRANSICION_NO_PERMITIDA, "Acción no permitida en esta etapa"),
            Map.entry(CodigoError.DUPLICADO, "Dato duplicado"),
            Map.entry(CodigoError.COPIA_PENDIENTE, "Copia a Drive pendiente"),
            Map.entry(CodigoError.ARCHIVO_DEMASIADO_GRANDE, "Archivo demasiado grande"),
            Map.entry(CodigoError.TIPO_ARCHIVO_NO_ACEPTADO, "Tipo de archivo no aceptado"),
            Map.entry(CodigoError.REQUISITO_FALTANTE, "Falta un requisito"),
            Map.entry(CodigoError.CUENTA_BLOQUEADA, "Cuenta bloqueada"),
            Map.entry(CodigoError.VERSION_DESACTUALIZADA, "Los datos cambiaron"),
            Map.entry(CodigoError.ARCHIVO_NO_DISPONIBLE, "Archivo no disponible"),
            Map.entry(CodigoError.CARGA_CONFLICTO, "La carga no coincide"),
            Map.entry(CodigoError.CARGA_INCOMPLETA, "Carga incompleta"),
            Map.entry(CodigoError.CARGA_OCUPADA, "Carga en curso"),
            Map.entry(CodigoError.CARGA_ALMACENAMIENTO, "Carga no disponible"),
            Map.entry(CodigoError.CUOTA_ALMACENAMIENTO, "Cuota de almacenamiento agotada"),
            Map.entry(CodigoError.GOOGLE_NO_DISPONIBLE, "Conexión Google no disponible"),
            Map.entry(CodigoError.IA_NO_DISPONIBLE, "Asistente no disponible"),
            Map.entry(CodigoError.LIMITE_PREGUNTAS, "Límite de preguntas"),
            Map.entry(CodigoError.LIMITE_CONSUMO, "Límite de consumo"),
            Map.entry(CodigoError.CONTEXTO_EXCEDIDO, "Contexto excedido")));

    @ExceptionHandler(ErrorDeDominio.class)
    public ResponseEntity<ProblemDetail> manejarErrorDeDominio(ErrorDeDominio error) {
        HttpStatus estado = ESTADOS.get(error.codigo());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, error.getMessage());
        completar(problema, error.codigo());
        // El formulario marca el campo: datos inválidos (VALIDACION) y repetidos (DUPLICADO, decisión D9).
        String campo = switch (error) {
            case DatoInvalido invalido -> invalido.campo();
            case DatoDuplicado duplicado -> duplicado.campo();
            default -> null;
        };
        if (campo != null) {
            problema.setProperty("errores", List.of(Map.of("campo", campo, "mensaje", error.getMessage())));
        }
        // Contrato (Problema.faltantes): solo con REQUISITO_FALTANTE, la lista de lo que falta para la acción.
        if (error instanceof RequisitoFaltante requisito) {
            problema.setProperty("faltantes", requisito.faltantes());
        }
        return ResponseEntity.status(estado).body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarErrorInesperado(Exception error) {
        LOG.error("Error inesperado al atender la solicitud", error);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo más tarde.");
        problema.setTitle("Error interno");
        return ResponseEntity.internalServerError().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException error, HttpHeaders cabeceras, HttpStatusCode estado, WebRequest solicitud) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Revisa los campos marcados.");
        completar(problema, CodigoError.VALIDACION);
        List<Map<String, String>> errores = error.getBindingResult().getFieldErrors().stream()
                .map(campo -> Map.of(
                        "campo", campo.getField(),
                        "mensaje", campo.getDefaultMessage() == null ? "Valor inválido" : campo.getDefaultMessage()))
                .toList();
        problema.setProperty("errores", errores);
        return handleExceptionInternal(error, problema, cabeceras, HttpStatus.BAD_REQUEST, solicitud);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException error, HttpHeaders cabeceras, HttpStatusCode estado, WebRequest solicitud) {
        // Spring puede delegar con cuerpo null: construirlo aquí evita perder el código contractual.
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONTENT_TOO_LARGE, "El archivo supera el tamaño máximo permitido.");
        completar(problema, CodigoError.ARCHIVO_DEMASIADO_GRANDE);
        return handleExceptionInternal(error, problema, cabeceras, HttpStatus.CONTENT_TOO_LARGE, solicitud);
    }

    // Errores propios de Spring MVC (ruta inexistente, método no permitido, JSON ilegible…): se les agrega el código.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception error, Object cuerpo, HttpHeaders cabeceras, HttpStatusCode estado, WebRequest solicitud) {
        if (cuerpo instanceof ProblemDetail problema
                && (problema.getProperties() == null || !problema.getProperties().containsKey("codigo"))) {
            CodigoError codigo = switch (estado.value()) {
                case 400 -> CodigoError.VALIDACION;
                case 401 -> CodigoError.NO_AUTENTICADO;
                case 403 -> CodigoError.SIN_PERMISO;
                case 404 -> CodigoError.NO_ENCONTRADO;
                case 413 -> CodigoError.ARCHIVO_DEMASIADO_GRANDE;
                case 415 -> CodigoError.TIPO_ARCHIVO_NO_ACEPTADO;
                default -> null;
            };
            if (codigo != null) {
                completar(problema, codigo);
            }
        }
        return super.handleExceptionInternal(error, cuerpo, cabeceras, estado, solicitud);
    }

    private static void completar(ProblemDetail problema, CodigoError codigo) {
        problema.setTitle(TITULOS.get(codigo));
        problema.setProperty("codigo", codigo.name());
    }
}
