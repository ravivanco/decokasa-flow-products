package com.decokasa.b2;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.application.usecase.ResumirUsuariosCasoDeUso;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumirUsuariosB2Test {
 @Test void vacioNoLeeUsuarios(){
  var repo=mock(UsuarioRepositorio.class);
  assertThat(new ResumirUsuariosCasoDeUso(repo).resumenes(List.of())).isEmpty();verifyNoInteractions(repo);
 }
 @Test void deduplicaSolicitadosOmiteAusenteYNoListaTodasLasCuentas(){
  var repo=mock(UsuarioRepositorio.class);UUID id=UUID.randomUUID(),ausente=UUID.randomUUID();
  var roles=Set.of(new CodigoRol("logistica"),new CodigoRol("consulta"));
  when(repo.resumirPorIds(Set.of(id,ausente))).thenReturn(List.of(new UsuarioRepositorio.Resumen(id,"Ficticio",roles,true)));
  var resultado=new ResumirUsuariosCasoDeUso(repo).resumenes(List.of(id,id,ausente));
  assertThat(resultado).containsOnlyKeys(id);assertThat(resultado.get(id).roles()).isEqualTo(roles);assertThat(resultado.get(id).activo()).isTrue();
  verify(repo).resumirPorIds(Set.of(id,ausente));verifyNoMoreInteractions(repo);
 }
 @Test void conservaEstadoInactivoYConjuntoVacio(){
  var repo=mock(UsuarioRepositorio.class);UUID id=UUID.randomUUID();
  when(repo.resumirPorIds(Set.of(id))).thenReturn(List.of(new UsuarioRepositorio.Resumen(id,"Sin roles",Set.of(),false)));
  var resultado=new ResumirUsuariosCasoDeUso(repo).resumenes(List.of(id));
  assertThat(resultado.get(id).activo()).isFalse();assertThat(resultado.get(id).roles()).isEmpty();verify(repo,never()).listar();
 }
}
