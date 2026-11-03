// El {"error"} del backend (y de nginx, que también contesta en JSON) va tal cual. Otra cosa (texto, cuerpo vacío)
// no se muestra: va a la consola para depurar y en pantalla queda un mensaje según el código, como en Swing.
export function mensajeDeError(status, datos, texto) {
  if (datos?.error) return datos.error;
  if (texto) console.error(`Respuesta ${status} sin mensaje de error en JSON:`, texto);
  if (status >= 502 && status <= 504) return "El servidor no está disponible: probá de nuevo en un momento";
  if (status >= 500) return "Algo salió mal en el servidor: probá de nuevo en un momento";
  return `El servidor respondió con un error (código ${status})`;
}
