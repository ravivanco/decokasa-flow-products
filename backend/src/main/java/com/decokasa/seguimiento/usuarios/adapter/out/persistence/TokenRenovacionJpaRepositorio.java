package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.domain.SesionRenovable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adaptador JPA del puerto {@link TokenRenovacionRepositorio} (ADR-0002, D3). */
@Component
public class TokenRenovacionJpaRepositorio implements TokenRenovacionRepositorio {

    private final TokenRenovacionDatos datos;

    public TokenRenovacionJpaRepositorio(TokenRenovacionDatos datos) {
        this.datos = datos;
    }

    @Override
    public Optional<SesionRenovable> buscarPorHash(String tokenHash) {
        return datos.findByTokenHash(tokenHash).map(TokenRenovacionJpaRepositorio::aDominio);
    }

    @Override
    public void guardar(SesionRenovable sesion) {
        TokenRenovacionEntity entidad = datos.findById(sesion.id())
                .orElseGet(() -> new TokenRenovacionEntity(sesion.id(), sesion.usuarioId(), sesion.familiaId(),
                        sesion.tokenHash(), sesion.creadoEn(), sesion.expiraEn()));
        entidad.setRevocadoEn(sesion.revocadoEn());
        datos.save(entidad);
    }

    @Override
    public boolean consumir(UUID id, Instant ahora) {
        return datos.consumir(id, ahora) == 1;
    }

    @Override
    public void revocarFamilia(UUID familiaId, Instant ahora) {
        datos.revocarFamilia(familiaId, ahora);
    }

    @Override
    public void revocarDeUsuario(UUID usuarioId, Instant ahora) {
        datos.revocarDeUsuario(usuarioId, ahora);
    }

    private static SesionRenovable aDominio(TokenRenovacionEntity entidad) {
        return new SesionRenovable(entidad.getId(), entidad.getUsuarioId(), entidad.getFamiliaId(),
                entidad.getTokenHash(), entidad.getCreadoEn(), entidad.getExpiraEn(), entidad.getRevocadoEn());
    }
}
