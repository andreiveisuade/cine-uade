package ar.uade.cine.controller.usuarios;

import org.springframework.stereotype.Component;

import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Usuario;
import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.usuarios.UsuarioVistaDTO;

// Arma los JSON de cliente y del usuario que entró, sin el hash de la clave; Assembler entre modelo y DTO.
@Component
public class VistasUsuarios {

    public ClienteVistaDTO cliente(Cliente c) {
        return new ClienteVistaDTO(c.getId(), c.getNombre(), c.getEmail());
    }

    public UsuarioVistaDTO usuario(Usuario u) {
        return new UsuarioVistaDTO(u.getId(), u.getNombre(), u.getEmail(), u.getRol().name());
    }
}
