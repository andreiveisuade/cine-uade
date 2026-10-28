import { etiqueta } from "@/api/etiquetas.js";
import { diaDelMes, hora, precio } from "@/compartido/formato.js";
import { avisoDe, CODIGO_POR_ESTADO, codigoLegible, conAcreditacion } from "@/ticket/estadoReserva.js";

// Los tonos de tema.js y tema.css sobre papel blanco: se imprime sin gastar tinta en fondos.
const COLOR = {
  ambar: "#f5b301", ambarTexto: "#8a6100", texto: "#15171d", tenue: "#5b606d", borde: "#dcd9d1",
  hueco: "#ebe9e3", pagada: "#2f9e44",
};

const MARGEN = 15;
const ANCHO = 210 - 2 * MARGEN;
const TALON = 62;
const RELLENO = 9;

export const nombreDelPdf = (reserva) => `entrada-cine-uade-${reserva.codigo}.pdf`;

// Las celdas del QR que ya dibujó qrcode.react: el PDF lo traza en vectores, nítido al imprimir.
export function leerQr(svg) {
  const celdas = Number(svg.getAttribute("viewBox").split(" ")[3]);
  const trazo = svg.querySelectorAll("path")[1].getAttribute("d");
  const tramos = [...trazo.matchAll(/M(\d+)[ ,](\d+) ?h(\d+)/g)].map((m) => m.slice(1).map(Number));
  return { celdas, tramos };
}

export async function descargarEntradaPdf(reserva, svgQr) {
  // Se baja recién al tocar el botón, como el confetti: la mayoría no descarga nada.
  const [{ jsPDF }, poster] = await Promise.all([import("jspdf"), cargarPoster(reserva.pelicula.posterUrl)]);
  const doc = new jsPDF({ unit: "mm", format: "a4" });
  const enlace = `${location.origin}${location.pathname}#/ticket/${reserva.codigo}`;
  dibujarEntrada(doc, reserva, { qr: leerQr(svgQr), poster, enlace });
  doc.save(nombreDelPdf(reserva));
}

// El poster va pasado a JPEG por un canvas; si el servidor no da CORS o tarda, la entrada sale sin él.
function cargarPoster(url) {
  if (!url) return Promise.resolve(null);
  return new Promise((resolver) => {
    const imagen = new Image();
    imagen.crossOrigin = "anonymous";
    imagen.onload = () => {
      try {
        const canvas = document.createElement("canvas");
        canvas.width = 300;
        canvas.height = 450;
        canvas.getContext("2d").drawImage(imagen, 0, 0, canvas.width, canvas.height);
        resolver(canvas.toDataURL("image/jpeg", 0.85));
      } catch {
        resolver(null);
      }
    };
    imagen.onerror = () => resolver(null);
    setTimeout(() => resolver(null), 4000);
    // Otra URL que la del <img> de la página: la copia en caché vino sin CORS y ensuciaría el canvas.
    imagen.src = url + (url.includes("?") ? "&" : "?") + "pdf";
  });
}

export function dibujarEntrada(doc, reserva, { qr, poster, enlace }) {
  doc.setLineHeightFactor(1.2);
  marca(doc, reserva);
  const arriba = 34;
  const corte = MARGEN + ANCHO - TALON;
  const abajo = Math.max(cuerpo(doc, reserva, poster, arriba, corte), arriba + 120);
  talon(doc, reserva, qr, corte, arriba, abajo);
  contorno(doc, corte, arriba, abajo);
  pie(doc, enlace, abajo + 8);
  return doc;
}

function marca(doc, reserva) {
  doc.setFillColor(COLOR.ambar);
  doc.roundedRect(MARGEN, MARGEN, 6, 6, 1.2, 1.2, "F");
  letra(doc, "bold", 14, COLOR.texto);
  doc.text("CINE UADE", MARGEN + 9, MARGEN + 3, { baseline: "middle", charSpace: 0.6 });
  letra(doc, "normal", 9, COLOR.tenue);
  doc.text(`Entrada · Reserva #${reserva.id}`, MARGEN + ANCHO, MARGEN + 3, { align: "right", baseline: "middle" });
  doc.setDrawColor(COLOR.ambar);
  doc.setLineWidth(0.8);
  doc.line(MARGEN, MARGEN + 11, MARGEN + ANCHO, MARGEN + 11);
}

// Devuelve dónde termina, para que el talón y el contorno tengan el alto de lo que entró.
function cuerpo(doc, reserva, poster, arriba, corte) {
  const x = MARGEN + RELLENO;
  const ancho = corte - x - RELLENO;
  let y = arriba + RELLENO;

  const posterAncho = 28;
  const posterAlto = 42;
  if (poster) {
    doc.addImage(poster, "JPEG", x, y, posterAncho, posterAlto);
  } else {
    doc.setFillColor(COLOR.hueco);
    doc.roundedRect(x, y, posterAncho, posterAlto, 1.5, 1.5, "F");
    letra(doc, "bold", 26, COLOR.tenue);
    doc.text(reserva.pelicula.titulo.charAt(0).toUpperCase(), x + posterAncho / 2, y + posterAlto / 2,
      { align: "center", baseline: "middle" });
  }

  const tx = x + posterAncho + 6;
  const tAncho = ancho - posterAncho - 6;
  let ty = y;
  insignia(doc, etiqueta(reserva.estado), tx, ty, reserva.estado === "PAGADA" ? COLOR.pagada : COLOR.ambar);
  ty += 9;
  // Un título largo achica la letra hasta entrar en tres renglones en vez de cortarse.
  let tamano = 22;
  let titulo;
  do {
    tamano -= 2;
    letra(doc, "bold", tamano, COLOR.texto);
    titulo = doc.splitTextToSize(reserva.pelicula.titulo, tAncho);
  } while (titulo.length > 3 && tamano > 12);
  doc.text(titulo, tx, ty, { baseline: "top" });
  ty += titulo.length * tamano * 0.4233 + 1;
  letra(doc, "normal", 9, COLOR.tenue);
  const aviso = doc.splitTextToSize(avisoDe(reserva), tAncho);
  doc.text(aviso, tx, ty, { baseline: "top" });
  ty += aviso.length * 4.4;
  y = Math.max(y + posterAlto, ty) + 8;

  const sala = reserva.sala || reserva.funcion.sala;
  const dia = diaDelMes(reserva.funcion.inicio);
  dato(doc, "Día", dia.charAt(0).toUpperCase() + dia.slice(1), x, y, true);
  dato(doc, "Hora", hora(reserva.funcion.inicio), x + 64, y, true);
  y += 15;
  dato(doc, "Sala", `${sala.nombre} · ${etiqueta(sala.tipo)}`, x, y);
  dato(doc, "Formato", `${etiqueta(reserva.funcion.proyeccion)} · ${etiqueta(reserva.funcion.idioma)}`, x + 50, y);
  y += 15;

  rotulo(doc, "Butacas", x, y);
  y += 5;
  for (const entrada of reserva.entradas) {
    butaca(doc, entrada, x, y, ancho);
    y += 8;
  }

  const cantidad = reserva.entradas.length;
  letra(doc, "bold", 11, COLOR.texto);
  doc.text(`Total · ${cantidad} entrada${cantidad === 1 ? "" : "s"}`, x, y + 8, { baseline: "bottom" });
  letra(doc, "bold", 22, COLOR.texto);
  doc.text(precio(reserva.total), x + ancho, y + 9, { align: "right", baseline: "bottom" });
  return y + 9 + RELLENO;
}

function talon(doc, reserva, qr, corte, arriba, abajo) {
  const centro = corte + TALON / 2;
  const lado = 44;
  letra(doc, "normal", 8.5, COLOR.tenue);
  const nota = doc.splitTextToSize(CODIGO_POR_ESTADO[reserva.estado], TALON - 14);
  const alto = 5 + lado + 6 + 10 + nota.length * 4;
  let y = arriba + (abajo - arriba - alto) / 2;

  if (reserva.estado === "PAGADA") sello(doc, "PAGADA", corte + TALON - 14, arriba + 9, COLOR.pagada);

  rotulo(doc, "Código de acceso", centro, y, true);
  y += 5;
  const lado1 = lado / qr.celdas;
  doc.setFillColor(COLOR.texto);
  // Un pelo de solape para que el visor no deje rayas blancas entre filas.
  for (const [cx, cy, largo] of qr.tramos) {
    doc.rect(centro - lado / 2 + cx * lado1, y + cy * lado1, largo * lado1 + 0.02, lado1 + 0.02, "F");
  }
  y += lado + 6;
  letra(doc, "bold", 20, COLOR.texto, "courier");
  espaciado(doc, codigoLegible(reserva.codigo), centro, y, 0.8, true);
  y += 10;
  letra(doc, "normal", 8.5, COLOR.tenue);
  doc.text(nota, centro, y, { align: "center", baseline: "top" });
}

// La forma de la entrada impresa: contorno redondeado, perforado y las dos muescas del corte.
function contorno(doc, corte, arriba, abajo) {
  doc.setDrawColor(COLOR.borde);
  doc.setLineWidth(0.4);
  doc.roundedRect(MARGEN, arriba, ANCHO, abajo - arriba, 4, 4, "S");
  const muesca = 3.5;
  doc.setLineDashPattern([1.6, 1.4], 0);
  doc.line(corte, arriba + muesca + 1.5, corte, abajo - muesca - 1.5);
  doc.setLineDashPattern([], 0);
  doc.setFillColor("#ffffff");
  for (const [y, afuera] of [[arriba, -1], [abajo, 1]]) {
    doc.circle(corte, y, muesca, "FD");
    doc.rect(corte - muesca - 0.5, afuera < 0 ? y - muesca - 0.5 : y, 2 * muesca + 1, muesca + 0.5, "F");
  }
}

function pie(doc, enlace, y) {
  letra(doc, "normal", 8.5, COLOR.tenue);
  doc.text("Mostrala impresa o en el celular. El QR sirve una sola vez.", MARGEN, y, { baseline: "top" });
  doc.text(enlace, MARGEN + ANCHO, y, { align: "right", baseline: "top" });
}

function dato(doc, titulo, valor, x, y, grande) {
  rotulo(doc, titulo, x, y);
  letra(doc, "bold", grande ? 18 : 11, COLOR.texto);
  doc.text(valor, x, y + 4, { baseline: "top" });
}

function butaca(doc, entrada, x, y, ancho) {
  doc.setDrawColor(COLOR.borde);
  doc.setLineWidth(0.3);
  doc.roundedRect(x, y + 1, 13, 6, 1, 1, "S");
  letra(doc, "bold", 9, COLOR.texto);
  doc.text(entrada.codigo, x + 6.5, y + 4, { align: "center", baseline: "middle" });
  const acredita = conAcreditacion(entrada);
  letra(doc, acredita ? "bold" : "normal", 9.5, acredita ? COLOR.ambarTexto : COLOR.tenue);
  const tarifa = etiqueta(entrada.tarifa || "GENERAL");
  doc.text(tarifa, x + 17, y + 4, { baseline: "middle" });
  if (acredita) {
    const nota = doc.getTextWidth(tarifa);
    letra(doc, "normal", 8.5, COLOR.tenue);
    doc.text("· traé el carnet", x + 18.5 + nota, y + 4, { baseline: "middle" });
  }
  letra(doc, "bold", 10, COLOR.texto);
  doc.text(precio(entrada.precio), x + ancho, y + 4, { align: "right", baseline: "middle" });
  doc.setLineDashPattern([0.8, 0.8], 0);
  doc.line(x, y + 8, x + ancho, y + 8);
  doc.setLineDashPattern([], 0);
}

function insignia(doc, texto, x, y, color) {
  letra(doc, "bold", 8, color === COLOR.ambar ? COLOR.texto : "#ffffff");
  const ancho = doc.getTextWidth(texto.toUpperCase()) + 5;
  doc.setFillColor(color);
  doc.roundedRect(x, y, ancho, 5.5, 1, 1, "F");
  doc.text(texto.toUpperCase(), x + 2.5, y + 2.85, { baseline: "middle" });
}

// El sello de pagada, inclinado como en la pantalla.
function sello(doc, texto, cx, cy, color) {
  const angulo = (8 * Math.PI) / 180;
  const [cos, sen] = [Math.cos(angulo), Math.sin(angulo)];
  const girar = (dx, dy) => [cx + dx * cos + dy * sen, cy - dx * sen + dy * cos];
  letra(doc, "bold", 10, color);
  const ancho = doc.getTextWidth(texto) + 5;
  const esquinas = [[-ancho / 2, -3.5], [ancho / 2, -3.5], [ancho / 2, 3.5], [-ancho / 2, 3.5]].map(([dx, dy]) => girar(dx, dy));
  doc.setDrawColor(color);
  doc.setLineWidth(0.6);
  const tramos = esquinas.slice(1).map(([px, py], i) => [px - esquinas[i][0], py - esquinas[i][1]]);
  doc.lines(tramos, esquinas[0][0], esquinas[0][1], [1, 1], "S", true);
  const [ax, ay] = girar(-(ancho - 5) / 2, 0);
  doc.text(texto, ax, ay, { angle: 8, baseline: "middle" });
}

function rotulo(doc, texto, x, y, centrado) {
  letra(doc, "bold", 7.5, COLOR.tenue);
  espaciado(doc, texto.toUpperCase(), x, y, 0.4, centrado);
}

// jsPDF centra sin contar el espacio entre letras: se centra a mano.
function espaciado(doc, texto, x, y, charSpace, centrado) {
  const ancho = doc.getTextWidth(texto) + charSpace * (texto.length - 1);
  doc.text(texto, centrado ? x - ancho / 2 : x, y, { baseline: "top", charSpace });
}

function letra(doc, peso, tamano, color, familia = "helvetica") {
  doc.setFont(familia, peso);
  doc.setFontSize(tamano);
  doc.setTextColor(color);
}
