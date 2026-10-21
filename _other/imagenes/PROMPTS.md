# Prompts de imágenes (Gemini)

## Pasos
1. Abrí https://aistudio.google.com y elegí el modelo de imagen de Gemini.
2. Pegá el prompt del ítem (inglés) y, al final, la guía de estilo.
3. Aspect ratio en el selector del modelo: promos `16:9`, productos y combos `1:1`.
4. Guardá cada resultado con el nombre de la tabla en `_other/imagenes/originales/` (png o jpg).
5. Corré `./procesar.sh`: deja los webp en `listas/`.

## Guía de estilo (pegar al final de cada prompt)
EN: Dark cinema theater atmosphere with blue and violet neon lights and golden-orange accents. Cinematic product photography, high contrast, shallow depth of field, blurred dark background. No text, no logos, no real brands, no recognizable people.

ES: Ambiente de sala de cine oscura con luces de neón azul y violeta y acentos dorados y naranja. Fotografía de producto cinematográfica, alto contraste, poca profundidad de campo, fondo oscuro desenfocado. Sin texto, sin logos, sin marcas reales, sin personas reconocibles.

## Seed (lo que crea `datos-de-ejemplo.sh`)

| Archivo | Formato | EN (principal) | ES |
|---|---|---|---|
| `promo-miercoles-2x1.png` | 16:9 | Two cinema tickets side by side glowing under blue neon, a golden "two for one" feeling conveyed by a duplicated ticket stub, empty red theater seats behind. | Dos entradas de cine lado a lado bajo neón azul, sensación de "dos por uno" con una entrada duplicada, butacas rojas vacías al fondo. |
| `producto-pochoclos-grandes.png` | 1:1 | A large striped bucket overflowing with fresh buttery popcorn, warm golden light from the side. | Un balde grande a rayas rebosante de pochoclos recién hechos con manteca, luz dorada cálida lateral. |
| `producto-pochoclos-medianos.png` | 1:1 | A medium striped popcorn box filled with popcorn, a few kernels scattered on a dark surface. | Una caja mediana a rayas llena de pochoclos, algunos granos sueltos sobre una superficie oscura. |
| `producto-gaseosa-500ml.png` | 1:1 | A 500ml soda bottle with condensation droplets, unbranded plain label, ice splashes, violet rim light. | Una botella de gaseosa de 500ml con gotas de condensación, etiqueta lisa sin marca, salpicaduras de hielo, luz violeta de contorno. |
| `producto-agua-500ml.png` | 1:1 | A 500ml water bottle with a plain blank label, cold condensation, cool blue backlight. | Una botella de agua de 500ml con etiqueta lisa en blanco, condensación fría, contraluz azul. |
| `producto-chocolate.png` | 1:1 | A chocolate bar partially unwrapped in plain dark foil, broken squares in the foreground, warm orange glow. | Una barra de chocolate a medio desenvolver en papel oscuro liso, cuadraditos partidos en primer plano, brillo naranja cálido. |
| `combo-clasico.png` | 1:1 | A large popcorn bucket next to a soda cup with a straw, arranged as a combo on a dark counter, neon reflections. | Un balde de pochoclos junto a un vaso de gaseosa con pajita, armados como combo sobre un mostrador oscuro, reflejos de neón. |

## Plantillas de promos (16:9)

| Archivo | EN | ES |
|---|---|---|
| `promo-2x1.png` | Two cinema tickets glowing under blue neon, duplicated ticket stub, empty seats behind. | Dos entradas de cine brillando bajo neón azul, talón duplicado, butacas vacías al fondo. |
| `promo-porcentaje-off.png` | A single golden cinema ticket with a sparkling discount-tag shape (no digits), blue and violet neon bokeh. | Una entrada de cine dorada con una etiqueta de descuento brillante (sin números), bokeh de neón azul y violeta. |
| `promo-monto-fijo.png` | A cinema ticket next to a stack of golden coins, warm orange glow against a dark theater. | Una entrada de cine junto a una pila de monedas doradas, brillo naranja cálido en una sala oscura. |
| `promo-matine.png` | Bright afternoon light streaming into a dark empty cinema aisle, warm golden beams, a ticket in the foreground. | Luz de tarde entrando a un pasillo de cine oscuro y vacío, haces dorados cálidos, una entrada en primer plano. |
| `promo-dia-semana.png` | A tear-off calendar page (no readable text) beside a cinema ticket, violet neon backdrop. | Una hoja de almanaque (sin texto legible) junto a una entrada de cine, fondo de neón violeta. |
| `promo-medio-pago.png` | A smartphone showing an abstract QR-like pattern and a payment card beside cinema tickets, blue neon glow. | Un celular con un patrón abstracto tipo QR y una tarjeta junto a entradas de cine, brillo de neón azul. |
| `promo-vacaciones.png` | Popcorn, 3D glasses and tickets arranged like a holiday flat lay, warm orange and golden light, festive confetti. | Pochoclos, anteojos 3D y entradas dispuestos como en vacaciones, luz naranja y dorada cálida, confeti festivo. |

## Plantillas de combos (16:9: van en el carrusel)

| Archivo | EN | ES |
|---|---|---|
| `combo-pareja.png` (16:9) | One popcorn bucket and two soda cups with straws, romantic violet and blue neon, shared. | Un balde de pochoclos y dos vasos de gaseosa con pajita, neón violeta y azul romántico, para compartir. |
| `combo-familiar.png` | Two large popcorn buckets, four soda cups and candy, abundant spread on a dark counter, golden light. | Dos baldes grandes de pochoclos, cuatro vasos de gaseosa y golosinas, mesa abundante sobre mostrador oscuro, luz dorada. |
| `combo-dulce.png` | Popcorn with caramel drizzle, chocolate bars and candy, soda cup, warm orange glow. | Pochoclos con caramelo, barras de chocolate y golosinas, vaso de gaseosa, brillo naranja cálido. |

Para un ítem nuevo: mismo nombre que el ítem en el sistema, en minúsculas, sin acentos, con guiones, prefijo `promo-`, `producto-` o `combo-`.
