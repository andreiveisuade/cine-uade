import * as api from "../api.js";
import { escapar, etiqueta, hora, hoyISO, precio } from "../ui.js";

/* ------------------------------------------------------------ arqueo del día */

export async function vistaCaja(contenedor, fecha = hoyISO()) {
  const arqueo = await api.obtenerArqueo(fecha);
  const medios = Object.entries(arqueo.porMedio);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Arqueo</h1>
    <p class="mb-5 text-sm text-slate-500">Lo cobrado en el día, por medio de pago.</p>

    <div class="mb-5 flex flex-wrap items-end gap-4">
      <label class="text-sm">
        <span class="text-slate-600">Fecha</span>
        <input type="date" id="fecha" value="${arqueo.fecha}"
          class="mt-1 block rounded border border-slate-400 px-2 py-1.5" />
      </label>
      <div class="rounded border border-slate-300 bg-white px-4 py-2">
        <span class="text-xs uppercase text-slate-500">Total cobrado</span>
        <p class="text-2xl font-bold">${precio(arqueo.total)}</p>
      </div>
      <div class="rounded border border-slate-300 bg-white px-4 py-2">
        <span class="text-xs uppercase text-slate-500">Operaciones</span>
        <p class="text-2xl font-bold">${arqueo.pagos.length}</p>
      </div>
      <div class="rounded border border-slate-300 bg-white px-4 py-2">
        <span class="text-xs uppercase text-slate-500">Entradas</span>
        <p class="text-2xl font-bold">${arqueo.entradas}</p>
      </div>
    </div>

    ${medios.length ? `
      <div class="mb-5 flex flex-wrap gap-2">
        ${medios.map(([medio, datos]) => `
          <div class="rounded border border-slate-300 bg-white px-3 py-2 text-sm">
            <span class="font-medium">${etiqueta(medio)}</span>
            <span class="text-slate-500">· ${datos.cantidad}</span>
            <span class="ml-2 font-semibold">${precio(datos.total)}</span>
          </div>`).join("")}
      </div>` : ""}

    <div class="overflow-x-auto rounded border border-slate-300 bg-white">
      <table class="w-full text-sm">
        <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500">
          <tr>
            <th class="p-2">Hora</th><th>Reserva</th><th>Película</th><th>Cliente</th>
            <th>Medio</th><th>Autorización</th><th class="text-right">Descuento</th>
            <th class="text-right">Monto</th>
          </tr>
        </thead>
        <tbody>
          ${arqueo.pagos.length ? arqueo.pagos.map((p) => `
            <tr class="border-b border-slate-200">
              <td class="p-2 whitespace-nowrap">${hora(p.fecha)}</td>
              <td>#${p.reservaId}</td>
              <td>${escapar(p.pelicula?.titulo || "—")}</td>
              <td>${escapar(p.cliente?.nombre || "—")}</td>
              <td>${etiqueta(p.medio)}</td>
              <td class="font-mono text-xs">${escapar(p.codigoAutorizacion || "—")}</td>
              <td class="text-right whitespace-nowrap text-xs ${p.descuento > 0 ? "text-emerald-700" : "text-slate-400"}">
                ${p.descuento > 0 ? "−" + precio(p.descuento) : "—"}
              </td>
              <td class="text-right whitespace-nowrap font-medium">${precio(p.monto)}</td>
            </tr>`).join("")
            : '<tr><td colspan="8" class="p-6 text-center text-slate-500">No se cobró nada ese día.</td></tr>'}
        </tbody>
      </table>
    </div>
  `;

  contenedor.querySelector("#fecha").addEventListener("change", (evento) => {
    vistaCaja(contenedor, evento.target.value);
  });
}
