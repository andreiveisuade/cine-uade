// Solo nombre y email, para precompletar la próxima compra en este navegador.
const CLAVE_CLIENTE = "cine.cliente";

export function clienteRecordado() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_CLIENTE)) || null;
  } catch {
    return null;
  }
}

export function recordarCliente(cliente) {
  localStorage.setItem(CLAVE_CLIENTE, JSON.stringify({ nombre: cliente.nombre, email: cliente.email }));
}
