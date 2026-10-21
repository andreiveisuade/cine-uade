import { Alert } from "@mantine/core";

// Tal cual: el mensaje lo escribió el gestor para que lo lea una persona.
export function ErrorCaja({ children }) {
  return (
    <Alert color="red" variant="light">
      {children}
    </Alert>
  );
}
