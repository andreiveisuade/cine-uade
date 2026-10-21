import { useState } from "react";
import { Box, Collapse, Stack, Text, UnstyledButton } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { fecha } from "@/promos/diasPromo.js";

export function CondicionesPromo({ promo }) {
  const [abiertas, setAbiertas] = useState(false);
  return (
    <Box>
      <UnstyledButton onClick={() => setAbiertas((a) => !a)} c="dimmed" fz="sm" fw={600} aria-expanded={abiertas}>
        {abiertas ? "Ocultar condiciones ▲" : "Ver condiciones ▼"}
      </UnstyledButton>
      <Collapse expanded={abiertas}>
        <Stack gap={2} pt={6}>
          <Text size="sm" c="dimmed">Del {fecha(promo.vigenciaDesde)} al {fecha(promo.vigenciaHasta)}</Text>
          {promo.horaDesde && <Text size="sm" c="dimmed">Funciones de {promo.horaDesde.slice(0, 5)} a {promo.horaHasta.slice(0, 5)}</Text>}
          <Text size="sm" c="dimmed">
            {promo.mediosPago.length === 0 ? "Cualquier medio de pago" : `Pagando con ${promo.mediosPago.map(etiqueta).join(", ").toLowerCase()}`}
          </Text>
          {promo.tarifasExcluidas.length > 0 && (
            <Text size="sm" c="dimmed">No suma con las tarifas {promo.tarifasExcluidas.map(etiqueta).join(", ").toLowerCase()}</Text>
          )}
        </Stack>
      </Collapse>
    </Box>
  );
}
