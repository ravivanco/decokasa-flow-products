void (() => {
  if (location.origin !== 'http://localhost:8080') throw new Error('Origen local incorrecto');
  const dialogo = document.createElement('dialog');
  const formulario = document.createElement('form');
  const usuario = document.createElement('input');
  usuario.placeholder = 'Admin del entorno aislado'; usuario.autocomplete = 'username';
  const clave = document.createElement('input');
  clave.type = 'password'; clave.autocomplete = 'current-password';
  clave.placeholder = 'Contraseña local';
  const boton = document.createElement('button');
  boton.type = 'submit'; boton.textContent = 'Iniciar autorización Google';
  const mensaje = document.createElement('p');
  formulario.append(usuario, clave, boton, mensaje); dialogo.append(formulario);
  document.body.append(dialogo); dialogo.showModal();
  formulario.onsubmit = async evento => {
    evento.preventDefault(); boton.disabled = true;
    let sesion;
    try {
      const peticion = fetch('/api/v1/auth/login', {
        method: 'POST', credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identificador: usuario.value, contrasena: clave.value }),
      });
      clave.value = '';
      const acceso = await peticion;
      if (!acceso.ok) throw new Error();
      sesion = await acceso.json();
      const inicio = await fetch('/api/v1/integraciones/google/oauth2/inicio', {
        method: 'POST', credentials: 'same-origin',
        headers: { Authorization: `Bearer ${sesion.tokenAcceso}` },
      });
      if (!inicio.ok) throw new Error();
      const destino = new URL((await inicio.json()).urlAutorizacion);
      if (destino.origin !== 'https://accounts.google.com'
          || destino.pathname !== '/o/oauth2/v2/auth') throw new Error();
      sesion = null; dialogo.remove(); location.assign(destino.href);
    } catch {
      sesion = null; clave.value = ''; boton.disabled = false;
      mensaje.textContent = 'No se inició la conexión. Revisa el entorno y el admin, sin compartir secretos.';
    }
  };
})();