import * as api from "../api.js";
import { avisar, dia, error, escapar, etiqueta, fechaHora, hora, precio } from "../ui.js";

/* ------------------------------------------------- una función y sus informes */

/**
 * El detalle de una función: cuánto se vendió y cuánto dejó.
 *
 * Los dos informes cuelgan de acá y no de una pantalla propia porque es donde el
 * encargado ya está parado cuando los necesita —viene de la lista de funciones, de la
 * función que le interesa— y porque los dos se piden por `funcionId`: una pantalla
 * separada empezaría pidiendo que eligiera la función de nuevo.
 *
 * Son dos endpoints y no uno partido en dos: el borderó es la declaración que se sube al
 * INCAA y el informe es la plata que dejó la función entre las dos cajas. El informe trae
 * el borderó completo adentro justamente para que los dos no puedan decir números
 * distintos de lo mismo.
 */
export async function vistaFuncion(contenedor, id) {
  const [funcion, bordero, informe] = await Promise.all([
    api.obtenerFuncion(id), api.obtenerBordero(id), api.obtenerInformeDeFuncion(id),
  ]);

  contenedor.innerHTML = `
    <a href="#/funciones" class="text-sm text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100">&larr; Funciones</a>
    <h1 class="mt-2 text-2xl font-bold">${escapar(funcion.pelicula.titulo)}</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
      ${escapar(dia(funcion.inicio))} ${hora(funcion.inicio)} ·
      ${escapar(funcion.sala.nombre)} (${etiqueta(funcion.sala.tipo)}) ·
      ${etiqueta(funcion.proyeccion)} · ${etiqueta(funcion.idioma)} ·
      precio base ${precio(funcion.precio)} ·
      ${funcion.libres} de ${funcion.sala.capacidadSala} butacas libres
    </p>

    <div class="grid gap-4 md:grid-cols-2 md:items-start">
      <section id="bordero" class="rounded border border-slate-300 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
        ${dibujarBordero(bordero)}
      </section>

      <section class="rounded border border-slate-300 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
        ${dibujarInforme(informe)}
      </section>
    </div>
  `;

  contenedor.querySelector("#bordero").addEventListener("click", async (evento) => {
    if (!evento.target.closest("#emitir")) return;
    try {
      // Emitir no es consultar: escribe el archivo que se sube al organismo. Se repinta
      // con lo que devuelve porque ahí viene el `generadoEn` de esta declaración, que es
      // lo que fecha lo declarado.
      const emitido = await api.emitirBordero(funcion.id);
      contenedor.querySelector("#bordero").innerHTML = dibujarBordero(emitido, true);
      avisar(`Borderó emitido: ${emitido.espectadores} espectadores, ${precio(emitido.recaudacionNeta)}`);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

/* ------------------------------------------------------------------ borderó */

/**
 * @param emitido si se acaba de escribir el archivo. Consultar el borderó y declararlo no
 *                son lo mismo, así que la pantalla tampoco los muestra igual.
 */
function dibujarBordero(bordero, emitido = false) {
  const tarifas = Object.entries(bordero.porTarifa);

  return `
    <h2 class="font-semibold">Borderó</h2>
    <p class="mb-3 text-xs text-slate-500 dark:text-slate-400">
      Lo que se declara al INCAA. Cuenta lo <strong>cobrado</strong>: una reserva sin pagar
      retiene butacas pero no vendió ninguna entrada.
    </p>

    ${tarifas.length ? `
      <table class="mb-3 w-full text-sm">
        <thead>
          <tr class="border-b border-slate-300 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:text-slate-400">
            <th class="py-1">Tarifa</th><th class="text-right">Entradas</th><th class="text-right">Total</th>
          </tr>
        </thead>
        <tbody>
          ${tarifas.map(([tarifa, total]) => `
            <tr class="border-b border-slate-200 dark:border-slate-800">
              <td class="py-1">${etiqueta(tarifa)}</td>
              <td class="text-right">${total.cantidad}</td>
              <td class="text-right">${precio(total.total)}</td>
            </tr>`).join("")}
        </tbody>
        <tfoot>
          <tr class="font-semibold">
            <td class="py-2">${bordero.espectadores} espectadores</td>
            <td colspan="2"></td>
          </tr>
        </tfoot>
      </table>` : `
      <p class="mb-3 rounded bg-slate-100 p-3 text-sm text-slate-600 dark:bg-slate-800 dark:text-slate-300">
        Todavía no se cobró ninguna entrada de esta función. No es un error: es un borderó
        en cero, y se puede declarar igual.
      </p>`}

    <dl class="space-y-1 border-t border-slate-200 pt-3 text-sm dark:border-slate-800">
      <div class="flex justify-between">
        <dt class="text-slate-600 dark:text-slate-300">Recaudación bruta</dt>
        <dd>${precio(bordero.recaudacionBruta)}</dd>
      </div>
      <div class="flex justify-between">
        <dt class="text-slate-600 dark:text-slate-300">Descuentos</dt>
        <dd class="${bordero.descuentos > 0 ? "text-amber-700 dark:text-amber-400" : "text-slate-500 dark:text-slate-400"}">− ${precio(bordero.descuentos)}</dd>
      </div>
      <div class="flex justify-between border-t border-slate-200 pt-1 font-semibold dark:border-slate-800">
        <dt>Recaudación neta</dt>
        <dd>${precio(bordero.recaudacionNeta)}</dd>
      </div>
    </dl>
    <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">
      Las tres van separadas porque cuentan cosas distintas: la <strong>bruta</strong> es a
      precio de lista —el valor declarado de cada localidad—, los <strong>descuentos</strong>
      son lo que resignó el cine por una promoción suya, y la <strong>neta</strong> es lo
      que entró en la caja. Con una sola, la diferencia no se podría explicar.
    </p>

    <div class="mt-4 flex flex-wrap items-center gap-3 border-t border-slate-200 pt-3 dark:border-slate-800">
      <button type="button" id="emitir"
        class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">
        Emitir para el INCAA
      </button>
      <p class="text-xs text-slate-500 dark:text-slate-400">
        ${emitido
          ? `Emitido el ${escapar(fechaHora(bordero.generadoEn))}. Se guardó el archivo en el servidor.`
          : `Consultado el ${escapar(fechaHora(bordero.generadoEn))}.`}
      </p>
    </div>
    <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">
      Emitir <strong>escribe el archivo</strong> y pisa el anterior: el borderó de una
      función es uno solo y vale el último, porque las entradas se siguen vendiendo hasta
      que la película arranca.
    </p>`;
}

/* ------------------------------------------------------------------ informe */

function dibujarInforme(informe) {
  return `
    <h2 class="font-semibold">Informe de la función</h2>
    <p class="mb-3 text-xs text-slate-500 dark:text-slate-400">
      Cuánto dejó la función entre las dos cajas del cine: boletería y candy.
    </p>

    <dl class="space-y-1 text-sm">
      <div class="flex justify-between">
        <dt class="text-slate-600 dark:text-slate-300">
          Boletería
          <span class="block text-xs text-slate-500 dark:text-slate-400">
            ${informe.boleteria.espectadores} entradas cobradas, ya con los descuentos
          </span>
        </dt>
        <dd>${precio(informe.boleteria.recaudacionNeta)}</dd>
      </div>
      <div class="flex justify-between">
        <dt class="text-slate-600 dark:text-slate-300">
          Candy
          <span class="block text-xs text-slate-500 dark:text-slate-400">
            ${informe.comprasCandy} ${informe.comprasCandy === 1 ? "compra atribuida" : "compras atribuidas"} a esta función
          </span>
        </dt>
        <dd>${precio(informe.candy)}</dd>
      </div>
      <div class="mt-2 flex justify-between border-t border-slate-200 pt-2 text-lg font-bold dark:border-slate-800">
        <dt>Total</dt>
        <dd>${precio(informe.total)}</dd>
      </div>
    </dl>

    <div class="mt-4 rounded border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300">
      <strong>El candy de mostrador no entra, a propósito.</strong> Solo se le atribuye a
      una función lo que se compró junto con la entrada, porque esa reserva es lo único que
      dice de qué función se trata: quien compra un balde en el mostrador puede estar yendo
      a cualquiera de las cuatro funciones de las 22:00, o a ninguna, y repartirlo sería
      inventar el dato. Consecuencia al leer los números: <strong>la suma de los informes
      de todas las funciones de un día da menos que el arqueo de ese día</strong>, y la
      diferencia es el mostrador. Esa plata se cuenta donde sí es cierta, en el arqueo del
      candy.
    </div>`;
}
