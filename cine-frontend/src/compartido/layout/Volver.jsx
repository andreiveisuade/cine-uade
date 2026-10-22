import { Anchor, Group } from "@mantine/core";
import { IconArrowLeft } from "@tabler/icons-react";
import { Link } from "react-router";

export function Volver({ a, children, ...props }) {
  return (
    <Anchor component={Link} to={a} size="sm" c="dimmed" fw={500} w="fit-content" {...props}>
      <Group gap={4} wrap="nowrap" component="span"><IconArrowLeft size={16} stroke={1.75} aria-hidden />{children}</Group>
    </Anchor>
  );
}
