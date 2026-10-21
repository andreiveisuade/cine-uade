import { Skeleton, Stack } from "@mantine/core";

export function TicketEsqueleto() {
  return <Stack gap="md"><Skeleton h={420} radius="lg" /><Skeleton h={48} w={320} /></Stack>;
}
