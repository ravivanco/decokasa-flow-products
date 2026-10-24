package com.decokasa.seguimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.decokasa.seguimiento.asistente.adapter.out.claude.AsistenteIADesactivado;
import com.decokasa.seguimiento.asistente.adapter.out.claude.ClaudeAsistenteIA;
import com.decokasa.seguimiento.asistente.application.port.in.Preguntar;
import com.decokasa.seguimiento.asistente.application.port.out.AsistenteIA;
import com.decokasa.seguimiento.asistente.application.usecase.ConfiguracionAsistente;
import com.decokasa.seguimiento.asistente.domain.IaNoDisponible;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.CrearPedido;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Revalidación de F7-COMP-01 (2026-10-24): con el asistente compilado e incluido, y IA_HABILITADA=false (el init de la
 * revalidación también vacía DECOKASA_CLAUDE_API_KEY), el adaptador es AsistenteIADesactivado, no existe ningún cliente
 * Claude y preguntar sobre una conversación real y vigente responde IaNoDisponible (503) sin ninguna interacción con el
 * proveedor ni reservas de presupuesto. Aplicación completa con PostgreSQL 17 propio (Testcontainers).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AsistenteDesactivadoRevalidacionIT {

    @MockitoSpyBean AsistenteIA proveedor;
    @Autowired ConfiguracionAsistente configuracion;
    @Autowired Preguntar preguntar;
    @Autowired CrearPedido crearPedido;
    @Autowired JdbcTemplate sql;
    @Autowired ApplicationContext contexto;

    @Test
    void sinIaHabilitadaPreguntarNoLlamaAlProveedor() {
        assertThat(configuracion.habilitada()).isFalse();
        assertThat(proveedor).isInstanceOf(AsistenteIADesactivado.class);
        assertThat(contexto.getBeansOfType(ClaudeAsistenteIA.class)).isEmpty();

        UUID admin = UUID.randomUUID();
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) "
                + "values (?,?,?,?,?,'hash-ficticio',now(),now(),0)", admin, "Admin ficticio IA", admin + "@ejemplo.test",
                "admin-ia-" + admin.toString().substring(0, 8), "+593990000041");
        sql.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,now() from rol where codigo='admin'",
                admin);
        sql.update("insert into usuario_pais(usuario_id,pais_codigo) values (?,'EC')", admin);
        Solicitante solicitante = new Solicitante(admin, Set.of(new CodigoRol("admin")));

        UUID pedido = crearPedido.crear(solicitante,
                new CrearPedido.Comando(TipoFlujo.A, CodigoPais.EC, "Pedido ficticio IA", null, false)).id();
        String numero = sql.queryForObject("select numero from pedido where id = ?", String.class, pedido);
        UUID conversacion = UUID.randomUUID();
        sql.update("insert into conversacion_ia(id,usuario_id,pedido_id,numero_pedido,pais_codigo,creada_en,expira_en,"
                + "preguntas) values (?,?,?,?,'EC',now(),now() + interval '1 day',0)", conversacion, admin, pedido, numero);

        assertThatThrownBy(() -> preguntar.preguntar(solicitante, conversacion, "¿En qué etapa está el pedido?"))
                .isInstanceOf(IaNoDisponible.class);

        verifyNoInteractions(proveedor);
        assertThat(sql.queryForObject("select count(*) from reserva_ia", Long.class)).isZero();
        assertThat(sql.queryForObject("select count(*) from consulta_ia", Long.class)).isZero();
        assertThat(sql.queryForObject("select count(*) from mensaje_ia", Long.class)).isZero();
    }
}
