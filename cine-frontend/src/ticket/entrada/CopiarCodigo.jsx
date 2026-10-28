import { ActionIcon, Tooltip } from "@mantine/core";
import { IconCopy } from "@tabler/icons-react";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";

export function CopiarCodigo({ codigo }) {
  const avisar = useAvisar();

  async function copiar() {
    // Sin permiso o fuera de HTTPS el navegador rechaza la escritura.
    try {
      await navigator.clipboard.writeText(codigo);
      avisar("Código copiado");
    } catch {
      avisar("No pudimos copiar. Seleccionalo y copialo a mano.", "error");
    }
  }

  return (
    <Tooltip label="Copiar código" withArrow>
      <ActionIcon variant="subtle" color="gray" size="lg" aria-label="Copiar código" onClick={copiar}>
        <IconCopy size={18} />
      </ActionIcon>
    </Tooltip>
  );
}
