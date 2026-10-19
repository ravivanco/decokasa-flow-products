package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.application.port.in.ConsultarArchivosDrive;
import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.out.*;
import com.decokasa.seguimiento.archivos.application.usecase.ConsultarArchivosDriveCasoDeUso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@Configuration(proxyBeanMethods=false)
public class ConsultaDriveConfig {
    @Bean ConsultarArchivosDrive consultarArchivosDrive(PlatformTransactionManager gestor,AdjuntoRepositorio adjuntos,
            ConsultaCopias copias,PedidosParaArchivos pedidos,ConsultarResponsables responsables,ResumirUsuarios usuarios) {
        var tx=new TransactionTemplate(gestor); tx.setReadOnly(true);
        var caso=new ConsultarArchivosDriveCasoDeUso(adjuntos,copias,pedidos,responsables,usuarios);
        return new ConsultarArchivosDrive() {
            public String enlace(Solicitante s,UUID id) { return tx.execute(e->caso.enlace(s,id)); }
            public List<AdjuntoVista> fallidos(Solicitante s,int p,int t) { return tx.execute(e->caso.fallidos(s,p,t)); }
        };
    }
}
