// Patrón Facade: la única puerta de las pantallas al backend, como Apis en Swing. Cada operación vive en el
// módulo de su subdominio y el transporte en http/; los tests mockean solo este módulo.
export * from "./catalogos.js";
export * from "./cartelera.js";
export * from "./funciones.js";
export * from "./sesion.js";
export * from "./clientes.js";
export * from "./ventas.js";
export * from "./promociones.js";
export * from "./candy.js";
