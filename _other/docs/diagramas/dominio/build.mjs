// Diagrama de clases integrado: lee los bloques de clases.puml (dominio y una capa por
// bloque), dibuja la vista de dominio ya dispuesta y embebe el motor (motor.js + ELK) para que
// el navegador arme las demas combinaciones de capas al prender los toggles.
// Uso: npm install && node build.mjs
import { readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import ELK from "elkjs/lib/elk.bundled.js";

const AQUI = dirname(fileURLToPath(import.meta.url));
const PUML = join(AQUI, "..", "clases.puml");
const SALIDA = join(AQUI, "..", "clases-dominio.svg");
const MOTOR = readFileSync(join(AQUI, "motor.js"), "utf8");
const ELK_JS = readFileSync(createRequire(import.meta.url).resolve("elkjs/lib/elk.bundled.js"), "utf8");

// Bloques del puml y la capa que aporta cada uno; dominio se ve siempre y sale ya dibujado
const BLOQUES = { "clases-modelo": "dominio" };
const CAPAS = {};
// Patrones GoF: rol de cada clase (pastilla en la caja) y ficha del boton de vista
const PATRONES = {
  Composite: {
    color: "#c2410c",
    roles: { Producto: "Component", ProductoSuelto: "Leaf", Combo: "Composite", ItemCombo: "Child", ItemCompra: "Client" },
    problema: "El precio suelto y el ahorro se preguntan igual a un producto suelto o a un combo: la hoja contesta lo suyo y el combo suma sus componentes, sin preguntar esCombo().",
  },
  "State Reserva": {
    color: "#0f766e",
    roles: { Reserva: "Context", EstadoReserva: "State" },
    problema: "Qué se puede hacer con una reserva (cobrar, cancelar, vencer, entrar) depende de su estado: cada constante decide su transición y Reserva solo asigna la que le devuelven.",
  },
  "State Revisión": {
    color: "#7c3aed",
    roles: { Pelicula: "Context", EstadoRevision: "State" },
    problema: "Si una película se publica, se programa o la ve el público lo decide su estado de revisión, no un if sobre el estado en cada operación.",
  },
  "State Importación": {
    color: "#4338ca",
    roles: { Importacion: "Context", EstadoImportacion: "State" },
    problema: "Solo una corrida en curso puede terminar o fallar: el estado devuelve el siguiente o rechaza, y la importación lo asigna.",
  },
  "Factory Method": {
    color: "#a16207",
    roles: { TipoPromocion: "Creator", Promocion: "Product", PromocionPorcentaje: "ConcreteProduct", PromocionMontoFijo: "ConcreteProduct", PromocionNxM: "ConcreteProduct" },
    problema: "Cada tipo de promoción sabe crear su subclase: sumar un tipo es una constante y una subclase, sin switch ni instanceof en controller y vistas.",
  },
};
// GRASP: se marca a nivel metodo (no es una estructura de clases sino un criterio de asignacion)
const GRASP = {
  Expert: {
    color: "#0369a1", relaciones: false,
    metodos: {
      Reserva: ["getTotal", "pagar", "cancelar", "expirar", "impedimentoParaCobrar", "estaVencida"],
      Funcion: ["precioEn", "precioDe", "yaEmpezo"], FormatoDeFuncion: ["exigirProyectableEn"], Proyeccion: ["sePuedeProyectarEn"],
      Asiento: ["getCodigo", "estaFueraDeServicio", "exigirEnServicio"], Programacion: ["horariosSinGenerar", "estaAlDia"],
      Importacion: ["terminar", "fallar", "quedoColgada"], Pelicula: ["dejarPendiente", "confirmar", "descartar"],
      MedioPago: ["autorizacion", "seCobraEnCaja"], Producto: ["estaDisponible", "editar"], Combo: ["getPrecioSuelto", "getAhorro"],
      TipoProducto: ["esCombo"],
    },
    problema: "La responsabilidad va a la clase que tiene la información: la reserva suma sus entradas y decide sus transiciones, la función calcula el precio con su formato y su sala, el asiento sabe su código y si está en servicio, el medio de pago qué autorización lleva y el combo cuánto ahorra.",
  },
  Creator: {
    color: "#be185d", relaciones: true,
    metodos: { Reserva: ["Reserva"], Entrada: [], ButacaPedida: [], Sala: ["generarAsientos"], Asiento: [], Combo: ["armar"], ItemCombo: [], CompraCandy: ["CompraCandy"], ItemCompra: [] },
    problema: "Crea quien contiene al objeto creado y tiene los datos para inicializarlo: la reserva crea sus entradas con el precio de su función, la sala sus butacas, el combo sus componentes y la compra del candy sus renglones.",
  },
  Polymorphism: {
    color: "#4d7c0f", relaciones: true,
    metodos: {
      Promocion: ["getTipo", "getParametros", "calcularDescuento", "beneficio", "cambiarParametros"],
      PromocionPorcentaje: ["getTipo", "getParametros", "calcularDescuento", "beneficio", "cambiarParametros"],
      PromocionMontoFijo: ["getTipo", "getParametros", "calcularDescuento", "beneficio", "cambiarParametros"],
      PromocionNxM: ["getTipo", "getParametros", "calcularDescuento", "beneficio", "cambiarParametros"],
      TipoPromocion: ["crear"], EstadoReserva: ["pagar", "cancelar", "expirar", "ingresar", "esperaPago", "ocupaButacas", "estaPagada"],
      EstadoRevision: ["exigirPublicable", "exigirProgramable", "seMuestraAlPublico"],
      TipoSala: ["aplicarA"], TipoAsiento: ["aplicarA"], TipoTarifa: ["aplicarA"],
    },
    problema: "Lo que cambia según el tipo lo resuelve el tipo, sin un switch: cada promoción calcula su descuento y dice su beneficio, cada constante de TipoPromocion crea su subclase, cada estado decide su transición y cada tipo de sala, butaca y tarifa aplica su multiplicador.",
  },
};

// ---------- lectura del puml ----------
// Una clase puede figurar en varios bloques: la define el que trae cuerpo y los demas
// solo la nombran (class X sin llaves) para sumarla a su capa.
function leerModelo() {
  const texto = readFileSync(PUML, "utf8");
  const clases = new Map(), relaciones = [], vistas = new Set();
  for (const [bloque, capa] of Object.entries(BLOQUES)) {
    const ini = texto.indexOf("@startuml " + bloque);
    if (ini < 0) continue;
    const fin = texto.indexOf("@enduml", ini);
    const lineas = texto.slice(ini, fin).split("\n").map((l) => l.trim());
    let paquete = null, clase = null;
    for (const l of lineas) {
      let m;
      if (clase) {
        if (l === "}") { clase = null; continue; }
        if (l) clase.miembros.push(l);
        continue;
      }
      if ((m = l.match(/^package "([^"]+)" \{$/))) {
        paquete = m[1];
      } else if (l === "}" && paquete) {
        paquete = null;
      } else if ((m = l.match(/^(abstract class|class|interface|enum) (?:"([^"]+)" as )?(\w+)(?: <<(.+?)>>)?( \{)?$/))) {
        const id = m[3];
        const c = clases.get(id) || { id, nombre: id, tipo: m[1], estereotipo: null, miembros: [], paquete, capas: new Set() };
        c.capas.add(capa);
        clases.set(id, c);
        if (!m[5]) continue;
        // Gana la primera definicion con cuerpo: una capa no puede pisar una clase del dominio
        if (c.definida) {
          console.warn(`${id} ya estaba definida; se ignora la definicion de ${bloque}`);
          clase = { miembros: [] };
          continue;
        }
        Object.assign(c, { nombre: m[2] || id, tipo: m[1], estereotipo: m[4] || null, paquete, definida: true });
        clase = c;
      } else if ((m = l.match(/^(\w+)\s+(?:"([^"]+)"\s+)?(<\|\.\.|<\|--|o-->|\*-->|\*--|-->|\.\.>)\s+(?:"([^"]+)"\s+)?(\w+)\s*:\s*(.+?)\s*>?$/))) {
        const clave = m[1] + m[3] + m[5];
        if (!vistas.has(clave)) relaciones.push({ a: m[1], multA: m[2], flecha: m[3], multB: m[4], b: m[5], verbo: m[6] });
        vistas.add(clave);
      }
    }
  }
  for (const r of relaciones) for (const id of [r.a, r.b]) if (!clases.has(id)) throw new Error(`Relacion con clase sin declarar: ${id}`);
  return { clases, relaciones };
}


const modelo = leerModelo();
const presentes = new Set([...modelo.clases.values()].flatMap((c) => [...c.capas]));
const D = {
  clases: [...modelo.clases.values()].map(({ definida, ...c }) => ({ ...c, capas: [...c.capas] })),
  relaciones: modelo.relaciones,
  patrones: Object.fromEntries(Object.entries(PATRONES).map(([n, p]) => [n, { color: p.color, capa: p.capa, roles: p.roles }])),
  anchoCabecera: 0,
};
const motor = new Function("ELK", "D", MOTOR + "\nreturn motorDominio(ELK, D);")(ELK, D);
const { C, FAMILIA, MONO, SANS, SERIF, MARGEN, TITULO, esc } = motor;

// Cabecera comun a todas las vistas: titulo, leyenda, toggles de capa y botones de patrones
function cabeceraSvg() {
  const out = [];
  let derecha = 0;
  out.push(`<text x="${MARGEN}" y="${MARGEN + 28}" font-family="${SERIF}" font-size="34" fill="${C.ink}">Diagrama de clases</text>`);
  out.push(`<text x="${MARGEN}" y="${MARGEN + 52}" font-family="${MONO}" font-size="12" fill="${C.soft}" letter-spacing="1">CINE UADE · DOMINIO · CADA RELACIÓN SE LEE COMO ORACIÓN, DEL ORIGEN AL DESTINO</text>`);
  let lx = MARGEN;
  for (const f of Object.values(FAMILIA)) {
    out.push(`<line x1="${lx}" y1="${MARGEN + 78}" x2="${lx + 28}" y2="${MARGEN + 78}" stroke="${f.color}" stroke-width="2"${f === FAMILIA.uso ? ` stroke-dasharray="4,3"` : ""}/>`);
    out.push(`<text x="${lx + 36}" y="${MARGEN + 82.5}" font-family="${MONO}" font-size="13" fill="${C.ink}">${esc(f.nombre)}</text>`);
    lx += 72 + f.nombre.length * 7.8;
  }
  const fila = (y, rotulo) => {
    out.push(`<text x="${MARGEN}" y="${y + 22}" font-family="${MONO}" font-size="13" fill="${C.soft}" letter-spacing="1">${rotulo}</text>`);
    return MARGEN + 150;
  };
  const boton = (bx, by, nombre, color, attrs, ficha) => {
    const w = nombre.length * 11 + 40;
    out.push(`<g class="btn-patron" data-patron="${nombre}" data-color="${color}" ${attrs} data-ficha="${esc(ficha).replace(/"/g, "&quot;")}">` +
      `<rect x="${bx}" y="${by}" width="${w}" height="34" rx="17" fill="${color}"/>` +
      `<text x="${bx + w / 2}" y="${by + 23}" text-anchor="middle" font-family="${SANS}" font-size="16" font-weight="600" fill="#ffffff">${nombre}</text></g>`);
    derecha = Math.max(derecha, bx + w);
    return w + 12;
  };
  const titulo = (nombre, color, problema) =>
    `<div style='font:600 15px Geist,sans-serif;color:${color}'>${nombre}</div><div style='margin:4px 0 6px'>${problema}</div>`;

  let bx = fila(MARGEN + 100, "CAPAS");
  for (const [capa, nombre] of [["dominio", "Dominio"], ...Object.entries(CAPAS).filter(([k]) => presentes.has(k))]) {
    const w = nombre.length * 10 + 44;
    const fija = capa === "dominio";
    out.push(`<g class="btn-capa${fija ? " on fija" : ""}" data-capa="${capa}">` +
      `<rect x="${bx}" y="${MARGEN + 100}" width="${w}" height="34" rx="6" stroke="${C.ink}" stroke-width="1.5"/>` +
      `<text x="${bx + w / 2}" y="${MARGEN + 123}" text-anchor="middle" font-family="${SANS}" font-size="16" font-weight="600">${fija ? "" : "+ "}${nombre}</text></g>`);
    bx += w + 12;
  }
  out.push(`<text class="estado" x="${bx + 8}" y="${MARGEN + 122}" font-family="${MONO}" font-size="13" fill="${C.soft}"></text>`);
  bx = fila(MARGEN + 146, "PATRONES GOF");
  for (const [nombre, p] of Object.entries(PATRONES)) {
    const roles = {};
    for (const [clase, rol] of Object.entries(p.roles)) (roles[rol] = roles[rol] || []).push(clase);
    const ficha = titulo(nombre, p.color, p.problema) +
      Object.entries(roles).map(([rol, cs]) => `<div><b>${rol}</b>: ${cs.join(", ")}</div>`).join("");
    const capa = p.capa ? ` data-capa="${p.capa}"` : "";
    bx += boton(bx, MARGEN + 146, nombre, p.color, `data-clases="${Object.keys(p.roles).join(",")}" data-rel="1"${capa}`, ficha);
  }
  bx = fila(MARGEN + 192, "GRASP");
  for (const [nombre, p] of Object.entries(GRASP)) {
    const metodos = Object.entries(p.metodos).flatMap(([c, ms]) => ms.map((m) => `${c}.${m}`));
    const ficha = titulo(nombre, p.color, p.problema) +
      Object.entries(p.metodos).filter(([, ms]) => ms.length).map(([c, ms]) => `<div><b>${c}</b>: ${ms.map((m) => m + "()").join(", ")}</div>`).join("");
    bx += boton(bx, MARGEN + 192, nombre, p.color,
      `data-clases="${Object.keys(p.metodos).join(",")}" data-metodos="${metodos.join(",")}" data-rel="${p.relaciones ? 1 : 0}"`, ficha);
  }
  return { svg: out.join("\n"), ancho: derecha + MARGEN };
}

function dibujar(inicial, cab) {
  const datos = JSON.stringify(D);
  for (const t of [datos, MOTOR, ELK_JS]) if (t.includes("]]>")) throw new Error("]]> dentro de un script");
  const out = [];
  out.push(`<svg xmlns="http://www.w3.org/2000/svg" data-dominio="" viewBox="0 0 ${inicial.w} ${inicial.h}" width="${inicial.w}" height="${inicial.h}" font-family="${SANS}">`);
  out.push(`<style>@import url('https://fonts.googleapis.com/css2?family=Geist:wght@400;600&amp;family=Geist+Mono:wght@400&amp;family=Instrument+Serif:ital@0;1&amp;display=swap');</style>`);
  out.push(`<rect width="100%" height="100%" fill="${C.paper}"/>`);
  out.push(cab.svg);
  out.push(`<g class="lienzo" transform="translate(${MARGEN},${MARGEN + TITULO})">`);
  out.push(inicial.html.replace('class="vista"', 'class="vista visible"'));
  out.push(`</g>`);
  out.push(`<style>${ESTILO}</style>`);
  // ELK y el motor, una sola vez por pagina: arman en el navegador las vistas con mas capas
  out.push(`<script><![CDATA[if (!window.ELK) {\n${ELK_JS}\n}]]></script>`);
  out.push(`<script><![CDATA[if (!window.__dominioMotor && window.ELK) {\n${MOTOR}\nwindow.__dominioMotor = motorDominio(window.ELK, ${datos});\n}]]></script>`);
  out.push(`<script><![CDATA[${INTERACCION}]]></script>`);
  out.push(`</svg>`);
  return out.join("\n");
}

const ESTILO = `
svg[data-dominio] .vista { display: none; }
svg[data-dominio] .vista.visible { display: inline; }
svg[data-dominio] .clase { cursor: pointer; }
svg[data-dominio].foco .clase:not(.activa), svg[data-dominio].foco .rel:not(.activa) { opacity: .1; }
svg[data-dominio].foco .rel.activa path { stroke-width: 2.6; }
.flujo-dominio { fill: none; stroke: ${C.paper}; stroke-width: 3.2; stroke-dasharray: 5 13; pointer-events: none; animation: flujo-dominio .8s linear infinite; }
.flujo-dominio.inv { animation-name: flujo-dominio-inv; }
@keyframes flujo-dominio { from { stroke-dashoffset: 18; } to { stroke-dashoffset: 0; } }
@keyframes flujo-dominio-inv { from { stroke-dashoffset: 0; } to { stroke-dashoffset: 18; } }
svg[data-dominio] .clase.elegida > rect:first-child { stroke: #bf4520; stroke-width: 2.4; }
svg[data-dominio] .btn-patron, svg[data-dominio] .btn-capa:not(.fija) { cursor: pointer; }
svg[data-dominio] .btn-patron.elegido rect { stroke: ${C.ink}; stroke-width: 2.5; }
svg[data-dominio] .btn-capa rect { fill: #ffffff; }
svg[data-dominio] .btn-capa text { fill: ${C.ink}; }
svg[data-dominio] .btn-capa.on rect { fill: ${C.ink}; }
svg[data-dominio] .btn-capa.on text { fill: #ffffff; }
svg[data-dominio] .btn-capa.fija rect { fill: ${C.soft}; stroke: ${C.soft}; }
svg[data-dominio].patron .rel.activa path { stroke: var(--patron); stroke-width: 2.8; }
svg[data-dominio].patron .clase.activa > rect:first-child { stroke: var(--patron); stroke-width: 2.6; }
svg[data-dominio].patron .resalte.marcado { opacity: .2; }
svg[data-dominio].patron text.miembro.marcado { fill: var(--patron); font-weight: 600; }
`;

// Hover: deja a la vista la clase, sus vecinas y sus relaciones. Clic fija; Esc, otro clic
// en la misma clase o un clic en el fondo la sueltan. Arrastrar (para moverse) no cuenta como clic.
// Listeners delegados en document: sirven tambien para la copia que abre el zoom del reporte.
const INTERACCION = `
(function () {
  if (window.__dominio) return;
  window.__dominio = true;
  var fijada = null, panel = null, abajo = null;
  function svgDe(el) { return el && el.closest ? el.closest("svg[data-dominio]") : null; }
  function vista(svg) { return svg.querySelector(".vista.visible") || svg; }
  function nombre(svg, id) { var c = vista(svg).querySelector(".clase[data-id='" + id + "']"); return c ? c.getAttribute("data-nombre") : id; }
  function mostrarPanel(titulo, rels, id) {
    if (!document.body) return;
    if (!panel) {
      panel = document.createElement("div");
      panel.className = "panel-dominio";
      panel.style.cssText = "position:fixed;right:16px;bottom:16px;max-width:460px;max-height:60vh;overflow:auto;z-index:10000;" +
        "background:#fff;border:1px solid #bfc0c0;border-radius:6px;padding:10px 14px;font:13px/1.5 'Geist Mono',ui-monospace,Menlo,monospace;color:#2d3142";
      document.body.appendChild(panel);
    }
    var sal = [], ent = [];
    rels.forEach(function (r) { (r.getAttribute("data-sujeto") === id ? sal : ent).push(r.getAttribute("data-frase")); });
    function lista(t, xs) { return xs.length ? "<div style='margin-top:6px;color:#5a6580'>" + t + "</div>" + xs.map(function (x) { return "<div>" + x.replace(/</g, "&lt;") + "</div>"; }).join("") : ""; }
    panel.innerHTML = "<div style='font:600 15px Geist,sans-serif'>" + titulo + (fijada ? " <span style='font-weight:400;color:#5a6580'>(fijada: Esc o clic en el fondo para soltar)</span>" : "") + "</div>" +
      lista("Qué hace " + titulo, sal) + lista("Quién usa a " + titulo, ent);
    panel.style.display = "block";
  }
  function limpiar(svg) {
    svg.classList.remove("foco", "patron");
    svg.querySelectorAll(".btn-patron.elegido").forEach(function (b) { b.classList.remove("elegido"); });
    svg.querySelectorAll(".activa, .elegida, .marcado").forEach(function (e) { e.classList.remove("activa", "elegida", "marcado"); });
    limpiarFlujos();
    if (panel) panel.style.display = "none";
  }
  function soltar() { fijada = null; enFoco = null; clearTimeout(espera); document.querySelectorAll("svg[data-dominio]").forEach(limpiar); }
  function enfocar(svg, id) {
    limpiar(svg);
    svg.classList.add("foco");
    var vecinas = {}; vecinas[id] = true;
    var trazos = [], v = vista(svg);
    v.querySelectorAll(".rel").forEach(function (r) {
      var a = r.getAttribute("data-a"), b = r.getAttribute("data-b");
      if (a === id || b === id) {
        r.classList.add("activa"); vecinas[a] = vecinas[b] = true;
        if (r.hasAttribute("data-frase")) trazos.push(r);
      }
    });
    v.querySelectorAll(".clase").forEach(function (c) {
      if (vecinas[c.getAttribute("data-id")]) c.classList.add("activa");
      if (c.getAttribute("data-id") === id) c.classList.add("elegida");
    });
    flujos(svg, trazos);
    mostrarPanel(nombre(svg, id), trazos, id);
  }
  var sinMovimiento = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  // Los huecos que corren hacia la punta se dibujan en un SVG aparte, superpuesto al diagrama:
  // animados dentro del diagrama obligaban a Chrome a volver a pintarlo entero en cada cuadro y,
  // con todas las capas, trababan todo. Si el SVG se abre suelto, van dentro del diagrama.
  var NS = "http://www.w3.org/2000/svg";
  function limpiarFlujos() { document.querySelectorAll(".capa-flujo, .flujo-dominio").forEach(function (e) { e.remove(); }); }
  function flujos(svg, rels) {
    limpiarFlujos();
    if (sinMovimiento || !rels.length) return;
    var host = svg.parentNode, destino = vista(svg);
    if (host && host.style && host.getBoundingClientRect) {
      if (getComputedStyle(host).position === "static") host.style.position = "relative";
      var r = svg.getBoundingClientRect(), h = host.getBoundingClientRect();
      var o = document.createElementNS(NS, "svg");
      o.setAttribute("class", "capa-flujo");
      o.setAttribute("viewBox", svg.getAttribute("viewBox"));
      // en porcentajes: sigue alineada si el zoom del reporte cambia el tamano con una clase fijada
      o.style.cssText = "position:absolute;pointer-events:none;will-change:transform;overflow:visible;" +
        "left:" + (r.left - h.left + host.scrollLeft) / host.scrollWidth * 100 + "%;top:" + (r.top - h.top + host.scrollTop) / host.scrollHeight * 100 + "%;" +
        "width:" + r.width / host.scrollWidth * 100 + "%;height:" + r.height / host.scrollHeight * 100 + "%";
      destino = document.createElementNS(NS, "g");
      destino.setAttribute("transform", svg.querySelector(".lienzo").getAttribute("transform"));
      o.appendChild(destino);
      host.appendChild(o);
    }
    rels.forEach(function (rel) {
      var f = document.createElementNS(NS, "path");
      f.setAttribute("d", rel.querySelector("path").getAttribute("d"));
      f.setAttribute("class", "flujo-dominio" + (rel.getAttribute("data-inv") ? " inv" : ""));
      destino.appendChild(f);
    });
  }



  // La vista de dominio viene dibujada; las demas combinaciones las arma el motor la primera vez
  function capasOn(svg) {
    var cs = [];
    svg.querySelectorAll(".btn-capa.on:not(.fija)").forEach(function (b) { cs.push(b.getAttribute("data-capa")); });
    return cs;
  }
  function activar(svg, v) {
    svg.querySelectorAll(".vista").forEach(function (x) { x.classList.toggle("visible", x === v); });
    var w = v.getAttribute("data-w"), h = v.getAttribute("data-h");
    svg.setAttribute("viewBox", "0 0 " + w + " " + h);
    if (svg.hasAttribute("width")) { svg.setAttribute("width", w); svg.setAttribute("height", h); }
    svg.dispatchEvent(new CustomEvent("dominio-vista", { bubbles: true }));
  }
  function estado(svg, t) { var e = svg.querySelector(".estado"); if (e) e.textContent = t; }
  function mostrarVista(svg) {
    var capas = capasOn(svg), clave = ["dominio"].concat(capas).join("+");
    var v = svg.querySelector(".vista[data-vista='" + clave + "']");
    if (v) { activar(svg, v); return Promise.resolve(); }
    if (!window.__dominioMotor) { estado(svg, "no se pudo cargar el motor del diagrama"); return Promise.resolve(); }
    var cache = window.__dominioCache = window.__dominioCache || {};
    estado(svg, "calculando la vista…");
    if (!cache[clave]) cache[clave] = new Promise(function (r) { setTimeout(r, 30); }).then(function () { return window.__dominioMotor.vista(capas); });
    return cache[clave].then(function (res) {
      var v = svg.querySelector(".vista[data-vista='" + clave + "']");
      if (!v) {
        var doc = new DOMParser().parseFromString("<svg xmlns='http://www.w3.org/2000/svg'>" + res.html + "</svg>", "image/svg+xml");
        v = svg.ownerDocument.importNode(doc.documentElement.firstElementChild, true);
        svg.querySelector(".lienzo").appendChild(v);
      }
      if (["dominio"].concat(capasOn(svg)).join("+") === clave) { activar(svg, v); estado(svg, ""); }
    }, function (err) { estado(svg, "error al calcular la vista: " + err); });
  }
  function prenderCapa(svg, capa) {
    var b = svg.querySelector(".btn-capa[data-capa='" + capa + "']");
    if (b && !b.classList.contains("on")) { b.classList.add("on"); return mostrarVista(svg); }
    return Promise.resolve();
  }
  // El foco por hover espera a que el mouse se quede quieto: recalcularlo en cada caja que se
  // cruza obliga a repintar todo el diagrama varias veces por segundo.
  var espera = null, enFoco = null;
  document.addEventListener("mouseover", function (e) {
    var c = e.target.closest && e.target.closest(".clase"), svg = svgDe(c);
    if (!svg || fijada || abajo) return;
    var id = c.getAttribute("data-id");
    clearTimeout(espera);
    if (enFoco === id) return;
    espera = setTimeout(function () { if (!fijada) { enFoco = id; enfocar(svg, id); } }, 90);
  });
  document.addEventListener("mouseout", function (e) {
    var c = e.target.closest && e.target.closest(".clase"), svg = svgDe(c);
    if (!svg || fijada || abajo || (e.relatedTarget && c.contains(e.relatedTarget))) return;
    clearTimeout(espera);
    var siguiente = e.relatedTarget && e.relatedTarget.closest && e.relatedTarget.closest(".clase");
    if (enFoco && !siguiente) espera = setTimeout(function () { if (!fijada) { enFoco = null; limpiar(svg); } }, 90);
  });
  function enfocarPatron(svg, btn) {
    limpiar(svg);
    svg.classList.add("foco", "patron");
    svg.style.setProperty("--patron", btn.getAttribute("data-color"));
    btn.classList.add("elegido");
    var en = {}, v = vista(svg);
    btn.getAttribute("data-clases").split(",").forEach(function (id) { en[id] = true; });
    v.querySelectorAll(".clase").forEach(function (c) { if (en[c.getAttribute("data-id")]) c.classList.add("activa"); });
    if (btn.getAttribute("data-rel") === "1")
      v.querySelectorAll(".rel").forEach(function (r) { if (en[r.getAttribute("data-a")] && en[r.getAttribute("data-b")]) r.classList.add("activa"); });
    (btn.getAttribute("data-metodos") || "").split(",").filter(Boolean).forEach(function (cm) {
      var p = cm.split(".");
      v.querySelectorAll(".clase[data-id='" + p[0] + "'] [data-m='" + p[1] + "']").forEach(function (e) { e.classList.add("marcado"); });
    });
    if (!document.body) return;
    mostrarPanel("", [], "");
    panel.innerHTML = btn.getAttribute("data-ficha") + "<div style='margin-top:6px;color:#5a6580'>Esc o clic en el fondo para salir</div>";
  }
  document.addEventListener("mousedown", function (e) { abajo = { x: e.clientX, y: e.clientY }; }, true);
  document.addEventListener("mouseup", function () { setTimeout(function () { abajo = null; }, 0); }, true);
  document.addEventListener("click", function (e) {
    var arrastre = abajo && Math.abs(e.clientX - abajo.x) + Math.abs(e.clientY - abajo.y) > 5;
    if (arrastre) { e.stopPropagation(); return; }
    var t = e.target;
    if (t.closest && (t.closest(".panel-dominio") || (t.closest(".lb-bar") && !t.closest("#lbClose")))) return;
    var k = t.closest && t.closest(".btn-capa"), sk = svgDe(k);
    if (sk) {
      e.stopPropagation();
      if (k.classList.contains("fija")) return;
      soltar();
      k.classList.toggle("on");
      mostrarVista(sk);
      return;
    }
    var b = t.closest && t.closest(".btn-patron"), sb = svgDe(b);
    if (sb) {
      e.stopPropagation();
      var nombreP = "patron:" + b.getAttribute("data-patron");
      if (fijada === nombreP) { soltar(); return; }
      soltar();
      fijada = nombreP;
      (b.hasAttribute("data-capa") ? prenderCapa(sb, b.getAttribute("data-capa")) : Promise.resolve()).then(function () {
        if (fijada === nombreP) enfocarPatron(sb, b);
      });
      return;
    }
    var c = t.closest && t.closest(".clase"), svg = svgDe(c);
    if (svg) {
      e.stopPropagation();
      var id = c.getAttribute("data-id");
      if (fijada === id) { soltar(); return; }
      fijada = id;
      enfocar(svg, id);
    } else if (fijada) {
      if (svgDe(t)) e.stopPropagation();
      soltar();
    }
  }, true);
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" && fijada) soltar();
  });
})();
`;

const cab = cabeceraSvg();
D.anchoCabecera = cab.ancho;
const inicial = await motor.vista([]);
writeFileSync(SALIDA, dibujar(inicial, cab));
console.log(`dominio: ${inicial.clases} clases, ${inicial.relaciones} relaciones, ${inicial.w}x${inicial.h}`);
console.log(`capas en el puml: ${[...presentes].join(", ")} -> ${SALIDA}`);
