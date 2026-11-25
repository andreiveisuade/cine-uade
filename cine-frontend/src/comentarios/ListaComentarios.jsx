import { Avatar, Group, Rating, Stack, Text } from "@mantine/core";
import { fechaHora } from "@/compartido/formato.js";

export function ListaComentarios({ comentarios }) {
  return (
    <Stack gap="lg" component="ul" p={0} m={0} style={{ listStyle: "none" }}>
      {comentarios.map((c) => (
        <Group key={c.id} component="li" gap="md" wrap="nowrap" align="flex-start" pb="lg"
          style={{ borderBottom: "1px solid var(--cine-borde)" }}>
          <Avatar name={c.autor} color="initials" radius="xl" alt="" />
          <Stack gap="xs" flex={1} miw={0}>
            <div>
              <Text fw={600} style={{ overflowWrap: "anywhere" }}>{c.autor}</Text>
              <Group gap="xs">
                <Rating value={c.puntaje} readOnly size="xs" color="var(--cine-foco)" aria-label={`${c.puntaje} de 5`} />
                <Text size="xs" c="dimmed">{fechaHora(c.fecha)}</Text>
              </Group>
            </div>
            {/* Texto de un usuario: como hijo de React va escapado, nunca como HTML. */}
            <Text style={{ whiteSpace: "pre-line", overflowWrap: "anywhere" }}>{c.texto}</Text>
          </Stack>
        </Group>
      ))}
    </Stack>
  );
}
