// Único punto de acceso a datos. Las vistas importan siempre desde acá.
//
// Antes esto era un selector entre dos implementaciones (mock y http): el mock
// duplicaba en JavaScript reglas de negocio que ya viven probadas en el backend
// (precios, promociones, arqueo, códigos de reserva), y mantener sincronizadas
// las dos a mano —cada operación nueva sumada en ambos lados— era trabajo puro
// sin beneficio, porque el mock nunca se probó y estaba apagado. Con un solo
// backend no hace falta el selector: reexportar agrega una operación tocando
// un solo archivo, `api-http.js`, en vez de tres.
export * from "./api-http.js";
