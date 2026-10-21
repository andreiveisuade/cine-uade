import { Component } from "react";
import { Link } from "react-router";
import { Button, Stack, Text, Title } from "@mantine/core";

// Una excepción en el render de una pantalla deja este aviso en vez de la página en blanco. Es clase porque
// React solo atrapa errores de render con getDerivedStateFromError: no hay hook para eso.
export class ErrorDePantalla extends Component {
  state = { roto: false };

  static getDerivedStateFromError() {
    return { roto: true };
  }

  componentDidCatch(error, info) {
    console.error(error, info.componentStack);
  }

  render() {
    if (!this.state.roto) return this.props.children;
    return (
      <Stack align="center" py="xl" gap="sm">
        <Title order={1}>Algo salió mal</Title>
        <Text c="dimmed">No pudimos mostrar esta pantalla. Volvé al inicio y probá de nuevo.</Text>
        {/* Volver limpia el error: si no, la pantalla de destino seguiría tapada por este aviso. */}
        <Button component={Link} to="/" onClick={() => this.setState({ roto: false })}>Volver al inicio</Button>
      </Stack>
    );
  }
}
