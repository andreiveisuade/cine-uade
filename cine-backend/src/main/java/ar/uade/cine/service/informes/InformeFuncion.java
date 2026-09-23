package ar.uade.cine.service.informes;

import ar.uade.cine.model.dinero.Dinero;

public record InformeFuncion(Bordero bordero, int comprasCandy, Dinero candy, Dinero total) {
}
