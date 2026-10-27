package com.decokasa.seguimiento.archivos.adapter.out.google;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.archivos.domain.GoogleNoDisponible;
import com.decokasa.seguimiento.archivos.application.port.out.ConexionOAuthGoogle;
import com.decokasa.seguimiento.compartido.domain.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** OAuth protocol/state tests, not real authorization: HTTP and JWT signature decoder are doubles.
 * Claim checks and local token encryption remain production behavior under test. */
@SuppressWarnings({"unchecked","rawtypes"})
class OAuthFlujoLocalTest {
    @TempDir Path disco;
    final String clienteId="cliente-ficticio.apps.googleusercontent.com",correo="ficticio@gmail.com",vinculo="a".repeat(43);
    final UUID admin=UUID.randomUUID();final AtomicReference<Instant> tiempo=new AtomicReference<>(Instant.parse("2026-10-19T15:00:00Z"));
    HttpClient http=mock(HttpClient.class);HttpResponse<byte[]> respuesta=mock(HttpResponse.class);
    NimbusJwtDecoder decoder=mock(NimbusJwtDecoder.class);GoogleOAuthUsuario oauth;Path cliente,tokens,llave;
    int estado=200;String cuerpo="{}";
    String scopes="openid https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/drive.file";
    @BeforeEach void preparar() throws Exception {
        FixtureGoogle.privado(disco);cliente=disco.resolve("cliente-ficticio.json");tokens=disco.resolve("tokens-ficticios.enc");llave=disco.resolve("llave-ficticia");
        ArchivosPrivadosGoogle.escribirAtomico(llave,new byte[32]);cliente(null);
        oauth=crear(true);
        when(respuesta.statusCode()).thenAnswer(i->estado);when(respuesta.body()).thenAnswer(i->cuerpo.getBytes(StandardCharsets.UTF_8));
        when(http.sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(i->CompletableFuture.completedFuture(respuesta));
    }
    GoogleOAuthUsuario crear(boolean habilitada) {
        var builder=mock(HttpClient.Builder.class,RETURNS_SELF);when(builder.build()).thenReturn(http);
        try(var fabrica=mockStatic(HttpClient.class)) {
            fabrica.when(HttpClient::newBuilder).thenReturn(builder);
            var resultado=new GoogleOAuthUsuario(habilitada,cliente.toString(),tokens.toString(),llave.toString(),correo,GoogleOAuthUsuario.CALLBACK,
                JsonMapper.builder().build(),tiempo::get,id->assertThat(id).isEqualTo(admin));
            ReflectionTestUtils.setField(resultado,"idTokens",decoder);return resultado;
        }
    }
    void cliente(String redirects) throws Exception {
        ArchivosPrivadosGoogle.escribirAtomico(cliente,("{\"web\":{\"client_id\":\""+clienteId+"\",\"client_secret\":\"secreto-ficticio\""
            +(redirects==null?"":",\"redirect_uris\":"+redirects)+"}}").getBytes(StandardCharsets.UTF_8));
    }
    Map<String,String> parametros(String url) {
        Map<String,String> p=new HashMap<>();for(String item:URI.create(url).getRawQuery().split("&")) {
            var par=item.split("=",2);p.put(par[0],URLDecoder.decode(par[1],StandardCharsets.UTF_8));}
        return p;
    }
    ConexionOAuthGoogle.Pendiente pendiente() {var inicio=oauth.iniciar(admin,vinculo);return oauth.consumir(parametros(inicio.urlAutorizacion()).get("state"),vinculo);}
    void identidad(ConexionOAuthGoogle.Pendiente p,String email,String nonce) {
        var jwt=Jwt.withTokenValue("id-token-ficticio").header("alg","RS256").issuer("https://accounts.google.com").subject("cuenta-ficticia")
            .audience(List.of(clienteId)).issuedAt(tiempo.get()).expiresAt(tiempo.get().plusSeconds(3600))
            .claim("nonce",nonce).claim("email",email).claim("email_verified",true).build();
        when(decoder.decode("id-token-ficticio")).thenReturn(jwt);
        cuerpo="{\"id_token\":\"id-token-ficticio\",\"access_token\":\"acceso-ficticio\",\"refresh_token\":\"refresh-ficticio\",\"expires_in\":3600,\"token_type\":\"Bearer\"}";
    }
    @Test void inicioOfflineCallbackFijoPkceNonceYStateDeUnSoloUsoSinRed() {
        var inicio=oauth.iniciar(admin,vinculo);var p=parametros(inicio.urlAutorizacion());
        assertThat(p).containsEntry("redirect_uri",GoogleOAuthUsuario.CALLBACK).containsEntry("access_type","offline").containsEntry("code_challenge_method","S256");
        assertThat(p.get("scope").split(" ")).containsExactlyInAnyOrder(scopes.split(" "));
        assertThat(p.get("nonce")).hasSize(43);assertThat(p.get("code_challenge")).hasSize(43);
        var pendiente=oauth.consumir(p.get("state"),vinculo);assertThat(pendiente.adminId()).isEqualTo(admin);
        assertThatThrownBy(()->oauth.consumir(p.get("state"),vinculo)).isInstanceOf(DatoInvalido.class);verifyNoInteractions(http);
    }
    @Test void navegadorAjenoNoConsumeStateYCaducidadNoSeExtiende() {
        var p=parametros(oauth.iniciar(admin,vinculo).urlAutorizacion());
        assertThatThrownBy(()->oauth.consumir(p.get("state"),"b".repeat(43))).isInstanceOf(DatoInvalido.class);
        tiempo.updateAndGet(t->t.plusSeconds(300));assertThatThrownBy(()->oauth.consumir(p.get("state"),vinculo)).isInstanceOf(DatoInvalido.class);
        assertThatThrownBy(()->oauth.iniciar(admin,"invalido")).isInstanceOf(DatoInvalido.class);verifyNoInteractions(http);
    }
    @Test void metadatosRedirectOpcionalesPeroSiPresentesDebenCoincidir() throws Exception {
        assertThat(oauth.iniciar(admin,vinculo)).isNotNull();cliente("[\""+GoogleOAuthUsuario.CALLBACK+"\"]");assertThat(oauth.iniciar(admin,vinculo)).isNotNull();
        cliente("[\"http://localhost:9999/otro\"]");assertThatThrownBy(()->oauth.iniciar(admin,vinculo)).isInstanceOf(GoogleNoDisponible.class);verifyNoInteractions(http);
    }
    @Test void canjeSimuladoValidaCuentaNonceYPersisteGrantDeSieteDias() throws Exception {
        var p=pendiente();identidad(p,correo,p.nonce());oauth.conectar(p,"codigo-ficticio");
        assertThat(oauth.credencial().tokenAcceso()).isEqualTo("acceso-ficticio");
        var almacen=new AlmacenTokensGoogle(tokens,llave,JsonMapper.builder().build());
        assertThat(almacen.vigente(tiempo.get(),x->x).autorizacionHasta()).isEqualTo(tiempo.get().plusSeconds(7*86400));
        verify(http,times(1)).sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @ParameterizedTest @ValueSource(strings={"nonce","cuenta","scope","token_type","expires_in"})
    void canjeConIdentidadOPermisosIncorrectosNoEscribeTokens(String caso) {
        var p=pendiente();identidad(p,"cuenta".equals(caso)?"otra@gmail.com":correo,"nonce".equals(caso)?"nonce-ajeno":p.nonce());
        if("scope".equals(caso))cuerpo=cuerpo.replace("\"expires_in\":3600","\"scope\":\"openid\",\"expires_in\":3600");
        if("token_type".equals(caso))cuerpo=cuerpo.replace("Bearer","invalido");
        if("expires_in".equals(caso))cuerpo=cuerpo.replace("3600","0");
        assertThatThrownBy(()->oauth.conectar(p,"codigo-ficticio")).isInstanceOf(GoogleNoDisponible.class);
        assertThat(tokens).doesNotExist();
    }
    @Test void renovarOfflineNoProlongaGrantYCaducidadNoLlamaAlProveedor() throws Exception {
        var p=pendiente();identidad(p,correo,p.nonce());oauth.conectar(p,"codigo-ficticio");tiempo.updateAndGet(t->t.plusSeconds(3590));
        cuerpo="{\"access_token\":\"renovado-ficticio\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"refresh_token_expires_in\":9999999}";
        assertThat(oauth.credencial().tokenAcceso()).isEqualTo("renovado-ficticio");
        var almacen=new AlmacenTokensGoogle(tokens,llave,JsonMapper.builder().build());
        assertThat(almacen.vigente(tiempo.get(),x->x).autorizacionHasta()).isEqualTo(Instant.parse("2026-10-26T15:00:00Z"));
        clearInvocations(http);tiempo.set(Instant.parse("2026-10-26T15:00:00Z"));
        assertThatThrownBy(()->oauth.credencial()).isInstanceOf(FalloIntegracionGoogle.class);verifyNoInteractions(http);
    }
    @ParameterizedTest @CsvSource({"429,CUOTA","500,TRANSITORIA","401,AUTORIZACION_REQUERIDA"})
    void renovacionFallidaConservaClasificacionYSinPersistirCredencialNueva(int status,String causa) throws Exception {
        var p=pendiente();identidad(p,correo,p.nonce());oauth.conectar(p,"codigo-ficticio");tiempo.updateAndGet(t->t.plusSeconds(3590));estado=status;cuerpo="{}";
        assertThatThrownBy(()->oauth.credencial()).isInstanceOfSatisfying(FalloIntegracionGoogle.class,e->assertThat(e.causa().name()).isEqualTo(causa));
    }
    @Test void desactivadoNoLeeFicherosNiLlamaAlProveedor() {
        oauth=crear(false);assertThatThrownBy(()->oauth.credencial()).isInstanceOfSatisfying(FalloIntegracionGoogle.class,e->assertThat(e.causa()).isEqualTo(FalloIntegracionGoogle.Causa.DESACTIVADA));
        assertThatThrownBy(()->oauth.iniciar(admin,vinculo)).isInstanceOf(GoogleNoDisponible.class);verifyNoInteractions(http);
    }
}
