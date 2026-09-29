package ar.uade.cine.swing.api;

// Las Api de todos los subdominios, sobre un solo ClienteHttp; la ve solo quien arma las pantallas.
public record Apis(ApiSesion sesion, ApiCatalogos catalogos, ApiCartelera cartelera, ApiSalas salas,
                   ApiFunciones funciones, ApiProgramaciones programaciones, ApiVentas ventas,
                   ApiPromociones promociones, ApiCandy candy, ApiClientes clientes, ApiInformes informes) {

    /** Todas comparten el cliente: una sola sesión, y un 401 en cualquiera avisa lo mismo. */
    public static Apis sobre(ClienteHttp http) {
        return new Apis(new ApiSesion(http), new ApiCatalogos(http), new ApiCartelera(http), new ApiSalas(http),
                new ApiFunciones(http), new ApiProgramaciones(http), new ApiVentas(http), new ApiPromociones(http),
                new ApiCandy(http), new ApiClientes(http), new ApiInformes(http));
    }
}
