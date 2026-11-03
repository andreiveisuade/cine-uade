// Un rechazo del backend: el mensaje se muestra tal cual y la pantalla decide por el código, no por el texto.
// Al reservar, el 409 es solo la butaca que ganó otra compra; el resto de lo que se rechaza es 400.
export class ErrorApi extends Error {
  constructor(mensaje, status) {
    super(mensaje);
    this.status = status;
  }
}
