package com.decokasa.seguimiento.archivos.adapter.out.drive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** HttpClient.ofInputStream deja de aplicar el timeout de la petición al recibir las cabeceras. Acota cada lectura. */
final class FlujoDriveConEspera extends FilterInputStream {
    private static final ScheduledThreadPoolExecutor RELOJ = reloj();
    private final long espera;
    private final AtomicBoolean vencido = new AtomicBoolean();
    FlujoDriveConEspera(InputStream origen, Duration espera) {
        super(origen);
        if(espera.isZero() || espera.isNegative())throw new IllegalArgumentException("Espera inválida");
        this.espera=espera.toNanos();
    }
    private static ScheduledThreadPoolExecutor reloj() {
        var ejecutor=new ScheduledThreadPoolExecutor(1,r->{var t=new Thread(r,"espera-lectura-drive");t.setDaemon(true);return t;});
        ejecutor.setRemoveOnCancelPolicy(true);return ejecutor;
    }
    private int leer(Lectura lectura)throws IOException {
        if(vencido.get())throw new IOException("Drive no respondió a tiempo.");
        var alarma=RELOJ.schedule(()->{vencido.set(true);try{in.close();}catch(IOException ignorado){ /* Se interrumpe la lectura. */ }},espera,TimeUnit.NANOSECONDS);
        try {
            int n=lectura.leer();
            if(vencido.get())throw new IOException("Drive no respondió a tiempo.");
            return n;
        } catch(IOException fallo) {
            throw new IOException(vencido.get()?"Drive no respondió a tiempo.":"No se pudo leer el documento de Drive.");
        } finally {alarma.cancel(false);}
    }
    @Override public int read()throws IOException{return leer(()->in.read());}
    @Override public int read(byte[] b,int desde,int cantidad)throws IOException {
        java.util.Objects.checkFromIndexSize(desde,cantidad,b.length);
        if(cantidad==0)return 0;
        return leer(()->in.read(b,desde,cantidad));
    }
    @FunctionalInterface private interface Lectura {int leer()throws IOException;}
}
