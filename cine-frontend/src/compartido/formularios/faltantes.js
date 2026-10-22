/** Campos vacíos o solo espacios, con su texto: `required` frena el vacío pero no "   ". */
export function faltantes(campos) {
  return Object.fromEntries(
    Object.entries(campos)
      .filter(([, { valor }]) => !valor.trim())
      .map(([clave, { etiqueta }]) => [clave, `Completá ${etiqueta}`]),
  );
}

/** Lleva el foco al primer campo vacío, en el orden de `vacios`; cada campo se llama (`name`) como su clave. */
export function enfocarPrimero(formulario, vacios) {
  const primero = Object.keys(vacios)[0];
  if (primero) formulario.elements.namedItem(primero)?.focus();
}
