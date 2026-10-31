package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarga;
import com.decokasa.seguimiento.archivos.application.port.in.SubirArchivo;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenCargas;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.archivos.domain.ArchivoDemasiadoGrande;
import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import com.decokasa.seguimiento.archivos.domain.FormatoArchivo;
import com.decokasa.seguimiento.archivos.domain.LimiteDeTamano;
import com.decokasa.seguimiento.archivos.domain.ProblemaCarga;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pasos puros en cuanto a frameworks. CargasConfig define transacciones y mantiene el bloqueo de archivo. */
public final class GestionarCargaCasoDeUso {
    private final CargaRepositorio cargas;
    private final PedidosParaArchivos pedidos;
    private final AlmacenLocal almacen;
    private final AlmacenCargas parciales;
    private final AdjuntoRepositorio adjuntos;
    private final SubirArchivoCasoDeUso subidas;
    private final ResumirUsuarios usuarios;
    private final Reloj reloj;
    private final LimiteDeTamano limite;
    /** Nulo con ARCHIVOS_DESTINO=local; con drive, los fragmentos van directo a Drive (ADR-0034). */
    private final CargaADriveCasoDeUso drive;
    public GestionarCargaCasoDeUso(CargaRepositorio cargas, PedidosParaArchivos pedidos, AlmacenLocal almacen,
            AlmacenCargas parciales, AdjuntoRepositorio adjuntos, SubirArchivoCasoDeUso subidas,
            ResumirUsuarios usuarios, Reloj reloj, LimiteDeTamano limite) {
        this(cargas,pedidos,almacen,parciales,adjuntos,subidas,usuarios,reloj,limite,null);
    }
    /** ADR-0034: {@code drive} es a la vez el almacén de cargas ({@code parciales}) y quien confirma en Drive. */
    public GestionarCargaCasoDeUso(CargaRepositorio cargas, PedidosParaArchivos pedidos, AlmacenLocal almacen,
            AlmacenCargas parciales, AdjuntoRepositorio adjuntos, SubirArchivoCasoDeUso subidas,
            ResumirUsuarios usuarios, Reloj reloj, LimiteDeTamano limite, CargaADriveCasoDeUso drive) {
        this.cargas=cargas; this.pedidos=pedidos; this.almacen=almacen; this.parciales=parciales;
        this.adjuntos=adjuntos; this.subidas=subidas; this.usuarios=usuarios; this.reloj=reloj; this.limite=limite;
        this.drive=drive;
    }
    public boolean directoADrive() { return drive!=null; }

    /**
     * ADR-0034, fuera de transacción: el archivo ya está completo en Drive. Se comprueba en Drive (ID, carpeta, tamaño,
     * marcas), se lee de vuelta verificando el SHA-256 de cada fragmento confirmado y el total, y se pasa la verificación
     * técnica del formato sobre un temporal que se borra enseguida. Si el contenido no es válido o no coincide, se borra
     * de Drive y no se registra nada.
     */
    public PreparadoDrive prepararFinalDrive(GestionarCarga.Vista vista) {
        CargaArchivo carga=vista.carga();
        if (carga.offsetConfirmado()!=carga.tamanoBytes()) { throw ProblemaCarga.incompleta(); }
        CargaADriveCasoDeUso.Confirmado confirmado=drive.confirmado(carga);
        AlmacenLocal.ArchivoRecibido recibido;
        try {
            recibido=subidas.recibir(comando(carga,new FlujoPorFragmentos(parciales.abrir(carga.id()),
                    vista.fragmentos(),carga.tamanoBytes())));
        } catch (com.decokasa.seguimiento.archivos.domain.TipoArchivoNoAceptado rechazo) {
            drive.descartar(carga);
            throw rechazo;
        } catch (java.io.UncheckedIOException error) {
            if (error.getCause() instanceof FlujoPorFragmentos.Distinto) {
                drive.descartar(carga);
                throw ProblemaCarga.reiniciada();
            }
            throw com.decokasa.seguimiento.archivos.domain.DriveNoDisponible.alSubir();
        }
        try {
            boolean huellaGoogle=confirmado.sha256()==null || confirmado.sha256().isBlank()
                    || confirmado.sha256().equalsIgnoreCase(recibido.sha256());
            if (recibido.tamanoBytes()!=carga.tamanoBytes() || !huellaGoogle) {
                drive.descartar(carga);
                throw ProblemaCarga.reiniciada();
            }
            return new PreparadoDrive(recibido.tamanoBytes(),recibido.sha256(),recibido.tipoDetectado(),
                    confirmado.drive());
        } finally {
            // Nada queda en el disco: el único almacén del documento es Drive.
            almacen.descartar(recibido.claveTemporal());
        }
    }

    /** ADR-0034, en la transacción final: registra el adjunto COPIADO con sus identificadores de Drive. */
    public AdjuntoVista registrarFinalDrive(Solicitante solicitante, UUID id, PreparadoDrive preparado) {
        CargaArchivo carga=autorizar(solicitante,id,true);
        if (carga.estado()==CargaArchivo.Estado.COMPLETADA) { return adjuntoCompletado(carga); }
        if (carga.offsetConfirmado()!=carga.tamanoBytes()) { throw ProblemaCarga.incompleta(); }
        AdjuntoVista adjunto=subidas.registrarEnDrive(solicitante,comando(carga,InputStream.nullInputStream()),
                preparado.tamanoBytes(),preparado.sha256(),preparado.tipoDetectado(),carga.adjuntoReservadoId(),
                preparado.drive());
        Objects.requireNonNull(adjunto.eventoId(),"El adjunto debe tener su evento de subida");
        cargas.completar(id,adjunto.adjunto().id(),adjunto.eventoId(),reloj.ahora());
        return adjunto;
    }

    /** Resultado verificado de una carga directa a Drive, listo para registrarse. */
    public record PreparadoDrive(long tamanoBytes, String sha256, String tipoDetectado,
            com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio.EnDrive drive) { }
    public GestionarCarga.Vista iniciar(Solicitante solicitante, GestionarCarga.Inicio comando) {
        if (comando.pedidoId()==null || comando.entradaEsperadaId()==null || comando.claveIdempotencia()==null
                || comando.etapa()<1 || comando.etapa()>30 || comando.tamanoBytes()<1) {
            throw new DatoInvalido("Revisa los datos de la carga.");
        }
        if (comando.tamanoBytes()>Math.min(limite.bytes(),CargaArchivo.MAXIMO_BYTES)) {
            throw new ArchivoDemasiadoGrande("El archivo supera el tamaño máximo permitido.");
        }
        String nombre=Adjunto.limpiarNombre(comando.nombre());
        // ADR-0026: la extensión se comprueba antes de reservar nada, sobre el mismo nombre guardado que se vuelve a
        // comprobar al finalizar (prepararFinal → recibir), junto con el contenido.
        FormatoArchivo.desdeNombre(nombre);
        String tipo=Adjunto.validarTipoDocumento(comando.tipoDocumento());
        pedidos.autorizarSubida(solicitante,comando.pedidoId(),comando.etapa(),comando.entradaEsperadaId());
        Instant ahora=reloj.ahora();
        CargaArchivo actual=cargas.crearSiAusente(new CargaArchivo(UUID.randomUUID(),solicitante.usuarioId(),
                comando.claveIdempotencia(),comando.pedidoId(),comando.etapa(),comando.entradaEsperadaId(),
                nombre,tipo,comando.tamanoBytes(),0,CargaArchivo.Estado.RECIBIENDO,UUID.randomUUID(),null,null,ahora,ahora));
        if (!actual.pedidoId().equals(comando.pedidoId()) || actual.etapa()!=comando.etapa()
                || !actual.entradaId().equals(comando.entradaEsperadaId()) || !actual.nombre().equals(nombre)
                || !actual.tipoDocumento().equals(tipo) || actual.tamanoBytes()!=comando.tamanoBytes()) {
            throw ProblemaCarga.conflicto();
        }
        return vista(actual);
    }
    public CargaArchivo autorizar(Solicitante solicitante, UUID id, boolean bloquear) {
        CargaArchivo carga=cargas.porId(id,false).orElseThrow(() -> new NoEncontrado("No existe esa carga."));
        if (!carga.usuarioId().equals(solicitante.usuarioId())) { throw new NoEncontrado("No existe esa carga."); }
        // Orden de bloqueos constante: pedido, después carga. Nunca un admin retoma una carga ajena.
        if (carga.estado()==CargaArchivo.Estado.COMPLETADA) {
            pedidos.autorizarConsulta(solicitante,carga.pedidoId());
        } else {
            pedidos.autorizarSubida(solicitante,carga.pedidoId(),carga.etapa(),carga.entradaId());
        }
        // La autorización pudo esperar a otro commit; releer evita mezclar offset antiguo con huellas nuevas.
        return cargas.porId(id,bloquear).orElseThrow(() -> new NoEncontrado("No existe esa carga."));
    }
    public GestionarCarga.Vista vista(CargaArchivo carga) {
        return new GestionarCarga.Vista(carga,cargas.fragmentos(carga.id()));
    }
    public CargaArchivo.Fragmento validarBloque(CargaArchivo carga, GestionarCarga.Bloque bloque) {
        byte[] bytes=bloque.contenido();
        if (bytes==null || bytes.length<1 || bytes.length>CargaArchivo.FRAGMENTO_BYTES) {
            throw new ArchivoDemasiadoGrande("El fragmento debe contener entre 1 byte y 4 MiB.");
        }
        if (carga.estado()!=CargaArchivo.Estado.RECIBIENDO || bloque.total()!=carga.tamanoBytes()
                || bloque.inicio()<0 || bloque.inicio()%CargaArchivo.FRAGMENTO_BYTES!=0
                || bloque.inicio()>carga.tamanoBytes()-bytes.length || bloque.inicio()>carga.offsetConfirmado()
                || bytes.length!=Math.min(CargaArchivo.FRAGMENTO_BYTES,carga.tamanoBytes()-bloque.inicio())
                || bloque.sha256()==null || !bloque.sha256().matches("[0-9a-f]{64}")) {
            throw ProblemaCarga.conflicto();
        }
        try {
            String real=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!real.equals(bloque.sha256())) { throw ProblemaCarga.conflicto(); }
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 no disponible"); }
        CargaArchivo.Fragmento fragmento=new CargaArchivo.Fragmento(bloque.inicio(),bytes.length,bloque.sha256());
        if (bloque.inicio()<carga.offsetConfirmado()
                && !cargas.fragmentos(carga.id()).contains(fragmento)) { throw ProblemaCarga.conflicto(); }
        return fragmento;
    }
    public GestionarCarga.Vista confirmarBloque(Solicitante solicitante, UUID id, GestionarCarga.Bloque bloque) {
        CargaArchivo carga=autorizar(solicitante,id,true);
        CargaArchivo.Fragmento fragmento=validarBloque(carga,bloque);
        if (bloque.inicio()==carga.offsetConfirmado()) {
            cargas.avanzar(id,carga.offsetConfirmado(),fragmento,reloj.ahora());
        }
        return vista(cargas.porId(id,false).orElseThrow(() -> new NoEncontrado("No existe esa carga.")));
    }
    /** Fuera de transacción. El parcial se conserva también si falla el commit del registro. */
    public AlmacenLocal.ArchivoRecibido prepararFinal(GestionarCarga.Vista vista) {
        CargaArchivo carga=vista.carga();
        if (carga.offsetConfirmado()!=carga.tamanoBytes()) { throw ProblemaCarga.incompleta(); }
        parciales.reconciliar(carga.id(),carga.offsetConfirmado());
        parciales.validar(carga.id(),vista.fragmentos(),carga.tamanoBytes());
        var recibido=subidas.recibir(comando(carga,parciales.abrir(carga.id())));
        try {
            if (recibido.tamanoBytes()!=carga.tamanoBytes()) { throw ProblemaCarga.conflicto(); }
            String destino=SubirArchivoCasoDeUso.claveFinal(comando(carga,InputStream.nullInputStream()),carga.adjuntoReservadoId());
            almacen.confirmar(recibido,destino);
            return new AlmacenLocal.ArchivoRecibido(destino,recibido.tamanoBytes(),recibido.sha256(),recibido.tipoDetectado());
        } finally {
            // Solo el temporal de trabajo. Nunca borrar el definitivo reservado después de un rollback.
            almacen.descartar(recibido.claveTemporal());
        }
    }
    public AdjuntoVista registrarFinal(Solicitante solicitante, UUID id, AlmacenLocal.ArchivoRecibido preparado) {
        CargaArchivo carga=autorizar(solicitante,id,true);
        if (carga.estado()==CargaArchivo.Estado.COMPLETADA) { return adjuntoCompletado(carga); }
        if (carga.offsetConfirmado()!=carga.tamanoBytes()) { throw ProblemaCarga.incompleta(); }
        AdjuntoVista adjunto=subidas.registrar(solicitante,comando(carga,InputStream.nullInputStream()),preparado,
                carga.adjuntoReservadoId());
        Objects.requireNonNull(adjunto.eventoId(),"El adjunto debe tener su evento de subida");
        cargas.completar(id,adjunto.adjunto().id(),adjunto.eventoId(),reloj.ahora());
        return adjunto;
    }
    public AdjuntoVista adjuntoCompletado(CargaArchivo carga) {
        Adjunto adjunto=adjuntos.porId(carga.adjuntoId()).orElseThrow(ProblemaCarga::almacenamiento);
        UUID autor=adjunto.subidoPor();
        return new AdjuntoVista(adjunto,carga.eventoId(),
                PersonasDeArchivos.persona(autor,usuarios.resumenes(List.of(autor)).get(autor)));
    }
    private static SubirArchivo.Comando comando(CargaArchivo c, InputStream contenido) {
        return new SubirArchivo.Comando(c.pedidoId(),c.etapa(),c.entradaId(),c.tipoDocumento(),c.nombre(),contenido);
    }
}
