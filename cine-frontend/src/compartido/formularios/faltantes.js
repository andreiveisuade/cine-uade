/** Campos vacíos o solo espacios, con su texto: `required` frena el vacío pero no "   ". */
export function faltantes(campos) {
  return Object.fromEntries(
    Object.entries(campos)
      .filter(([, { valor }]) => !valor.trim())
      .map(([clave, { etiqueta }]) => [clave, `Completá ${etiqueta}`]),
  );
}
