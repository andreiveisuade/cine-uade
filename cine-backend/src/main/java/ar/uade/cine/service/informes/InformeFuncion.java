package ar.uade.cine.service.informes;

import ar.uade.cine.model.dinero.Dinero;

// Borderó de una función más su candy; record de resultado de GestorInformes.
public record InformeFuncion(Bordero bordero, int comprasCandy, Dinero candy, Dinero total) {
}
