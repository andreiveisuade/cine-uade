import * as api from "../api.js";
import { campo, filaTabla, panel, tabla } from "../componentes.js";
import { escapar } from "../dom.js";
import { etiqueta } from "../etiquetas.js";
import { hora, hoyISO, precio } from "../formato.js";

/* ------------------------------------------------------------ arqueo del día */

export async function vistaCaja(contenedor, fecha = hoyISO()) {
  const arqueo = await api.obtenerArqueo(fecha);
  const medios = Object.entries(arqueo.porMedio);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Arqueo</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">Lo cobrado en el día, por medio de pago.</p>

    <div class="mb-5 flex flex-wrap items-end gap-4">
      ${campo({ nombre: "fecha", etiqueta: "Fecha", tipo: "date", valor: arqueo.fecha, ancho: "block" })}
      ${panel(`
        <span class="text-xs uppercase text-slate-500 dark:text-slate-400">Total cobrado</span>
        <p class="text-2xl font-bold">${precio(arqueo.total)}</p>`, "px-4 py-2")}
      ${panel(`
        <span class="text-xs uppercase text-slate-500 dark:text-slate-400">Operaciones</span>
        <p class="text-2xl font-bold">${arqueo.pagos.length}</p>`, "px-4 py-2")}
      ${panel(`
        <span class="text-xs uppercase text-slate-500 dark:text-slate-400">Entradas</span>
        <p class="text-2xl font-bold">${arqueo.entradas}</p>`, "px-4 py-2")}
    </div>

    ${medios.length ? `
      <div class="mb-5 flex flex-wrap gap-2">
        ${medios.map(([medio, datos]) => panel(`
            <span class="font-medium">${etiqueta(medio)}</span>
            <span class="text-slate-500 dark:text-slate-400">· ${datos.cantidad}</span>
            <span class="ml-2 font-semibold">${precio(datos.total)}</span>`, "px-3 py-2 text-sm")).join("")}
      </div>` : ""}

    ${panel(tabla(`
          <tr>
            <th class="p-2">Hora</th><th>Reserva</th><th>Película</th><th>Cliente</th>
            <th>Medio</th><th>Autorización</th><th class="text-right">Descuento</th>
            <th class="text-right">Monto</th>
          </tr>`, arqueo.pagos.length ? arqueo.pagos.map((p) => `
            <tr class="${filaTabla()}">
              <td class="p-2 whitespace-nowrap">${hora(p.fecha)}</td>
              <td>#${p.reservaId}</td>
              <td>${escapar(p.pelicula?.titulo || "—")}</td>
              <td>${escapar(p.cliente?.nombre || "—")}</td>
              <td>${etiqueta(p.medio)}</td>
              <td class="font-mono text-xs">${escapar(p.codigoAutorizacion || "—")}</td>
              <td class="text-right whitespace-nowrap text-xs ${p.descuento > 0 ? "text-emerald-700 dark:text-emerald-400" : "text-slate-400 dark:text-slate-500"}">
                ${p.descuento > 0 ? "−" + precio(p.descuento) : "—"}
              </td>
              <td class="text-right whitespace-nowrap font-medium">${precio(p.monto)}</td>
            </tr>`).join("")
            : '<tr><td colspan="8" class="p-6 text-center text-slate-500 dark:text-slate-400">No se cobró nada ese día.</td></tr>'), "overflow-x-auto")}
  `;

  contenedor.querySelector("#fecha").addEventListener("change", (evento) => {
    vistaCaja(contenedor, evento.target.value);
  });
}
