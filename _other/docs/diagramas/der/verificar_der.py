import re, sys, xml.etree.ElementTree as ET
schema, svgp = (sys.argv[1:3] if len(sys.argv) > 2 else ["cine-backend/src/main/resources/schema.sql", "_other/docs/diagramas/der-design.svg"])
sql = re.sub(r"--[^\n]*", "", open(schema).read())
NS = "{http://www.w3.org/2000/svg}"
root = ET.parse(svgp).getroot()
err = []
def txt(e): return "".join(e.itertext()).replace("\xa0", " ").strip()
drawn = {g.get("data-table"): g for g in root.iter(NS + "g") if g.get("data-table")}
fks_sql = set()
tabs = re.findall(r"CREATE TABLE IF NOT EXISTS (\w+) \((.*?)\);", sql, re.S)
if len(tabs) != 20: err.append(f"schema tiene {len(tabs)} tablas")
if set(drawn) != {t for t, _ in tabs}: err.append(f"tablas distintas: {set(drawn) ^ {t for t,_ in tabs}}")
for t, body in tabs:
    parts = [p.strip() for p in re.split(r",\s*\n", body.strip()) if p.strip()]
    g = drawn.get(t)
    if g is None: continue
    rows = [c for c in g.iter(NS + "g") if c.get("data-col")]
    gtext = txt(g)
    cons = [p for p in parts if re.match(r"(PRIMARY KEY|UNIQUE|FOREIGN KEY)", p)]
    cols = [p for p in parts if p not in cons]
    if [r.get("data-col") for r in rows] != [c.split()[0] for c in cols]:
        err.append(f"{t}: columnas {[r.get('data-col') for r in rows]} vs {[c.split()[0] for c in cols]}")
        continue
    comp_pk = next((re.search(r"\((.*)\)", c).group(1).replace(" ", "").split(",") for c in cons if c.startswith("PRIMARY KEY")), [])
    fk = {}
    for c in cons:
        m = re.match(r"FOREIGN KEY \((\w+)\) REFERENCES (\w+)\(id\)( ON DELETE CASCADE)?", c)
        if m: fk[m.group(1)] = bool(m.group(3)); fks_sql.add(f"{t}.{m.group(1)}->{m.group(2)}")
        if c.startswith(("UNIQUE", "PRIMARY KEY")) and "," in c:
            cl = ", ".join(x.strip() for x in re.search(r"\((.*)\)", c).group(1).split(","))
            if f"{'UQ' if c.startswith('UNIQUE') else 'PK'} ({cl})" not in gtext: err.append(f"{t}: falta constraint {c}")
    for c, r in zip(cols, rows):
        texts = [txt(x) for x in r if x.tag == NS + "text"]
        name, chips, typ = texts[0], set(texts[1:-1]), texts[-1]
        m = re.match(r"(\w+) (\S+)(.*)", c); cn, ct, rest = m.groups()
        exp = set()
        pk = "PRIMARY KEY" in rest or cn in comp_pk
        if pk: exp.add("PK")
        if cn in fk: exp.add("FK")
        if fk.get(cn): exp.add("CASCADE")
        if " UNIQUE" in rest: exp.add("UQ")
        if "NOT NULL" in rest and "PRIMARY KEY" not in rest: exp.add("NN")
        if "AUTO_INCREMENT" in rest: exp.add("AI")
        d = re.search(r"DEFAULT ('[^']*'|\S+)", rest)
        etyp = ct + ("" if "NOT NULL" in rest or "PRIMARY KEY" in rest else " NULL") + (f" = {d.group(1)}" if d else "")
        if name != cn or chips != exp or typ != etyp:
            err.append(f"{t}.{cn}: dibujado {name} {sorted(chips)} '{typ}' / esperado {sorted(exp)} '{etyp}'")
fks_svg = {p.get("data-fk") for p in root.iter(NS + "path") if p.get("data-fk")}
if fks_svg != fks_sql: err.append(f"FK distintas: {fks_svg ^ fks_sql}")
print("\n".join(err) or f"OK: 20 tablas, {sum(len(re.findall(chr(10), b)) for _,b in tabs)} lineas, {len(fks_sql)} FK coinciden")
sys.exit(1 if err else 0)
