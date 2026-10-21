import { UnstyledButton } from "@mantine/core";
import { m } from "motion/react";

const SIMBOLO = { VIP: "*", PAREJA: "&", ACCESIBLE: "+", ESTANDAR: "" };
const POP = { type: "spring", stiffness: 500, damping: 15 };

// Cómo se ve lo decide quien usa el mapa (`pintar`); la butaca solo sabe su número y su tipo.
export function BotonButaca({ asiento, estilo, deshabilitado, titulo, alElegir }) {
  return (
    <UnstyledButton component={m.button} title={titulo} disabled={deshabilitado}
      whileTap={deshabilitado ? undefined : { scale: 0.8 }} transition={POP}
      onClick={() => alElegir?.(asiento)}
      style={{ height: 32, width: asiento.tipo === "PAREJA" ? 56 : 32, borderRadius: "6px 6px 3px 3px", fontSize: 11,
               fontWeight: 550, fontVariantNumeric: "tabular-nums", textAlign: "center", flexShrink: 0, ...estilo }}>
      {asiento.numero}{SIMBOLO[asiento.tipo]}
    </UnstyledButton>
  );
}
