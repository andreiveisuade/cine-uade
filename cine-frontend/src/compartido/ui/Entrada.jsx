import { Box } from "@mantine/core";
import clases from "./Entrada.module.css";

// La forma de una entrada de cine: cuerpo y talón separados por el perforado. Lo que el cliente compra se ve así.
export function Entrada({ children, talon, ...props }) {
  return (
    <Box className={clases.entrada} {...props}>
      <div className={clases.partes}>
        <div className={clases.cuerpo}>{children}</div>
        <div className={clases.talon}>{talon}</div>
      </div>
    </Box>
  );
}
