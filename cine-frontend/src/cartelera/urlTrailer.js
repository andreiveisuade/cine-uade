// El navegador solo deja arrancar solo un video muteado; el sonido se prende después con la IFrame API por postMessage.
export function urlTrailer(id, { autoplay = true, controles = true } = {}) {
  const params = new URLSearchParams({
    autoplay: autoplay ? "1" : "0", mute: "1", playsinline: "1", rel: "0", modestbranding: "1",
    loop: "1", playlist: id, enablejsapi: "1", controls: controles ? "1" : "0",
  });
  return `https://www.youtube-nocookie.com/embed/${id}?${params}`;
}
