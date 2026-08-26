# Deja el sistema andando desde cero. Idempotente: se puede correr de nuevo sin romper nada.
#
#     .\setup.ps1
#
# Si Windows lo bloquea por politica de ejecucion:
#     powershell -ExecutionPolicy Bypass -File .\setup.ps1
#
# Equivalente para macOS y Linux: setup.sh

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Ok    ($m) { Write-Host "  + $m" -ForegroundColor Green }
function Info  ($m) { Write-Host "  . $m" -ForegroundColor Blue }
function Aviso ($m) { Write-Host "  ! $m" -ForegroundColor Yellow }
function Fatal ($m) { Write-Host "  x $m" -ForegroundColor Red; exit 1 }
function Paso  ($m) { Write-Host ""; Write-Host $m -ForegroundColor White }

function Nueva-Clave {
  # 24 caracteres alfanumericos. Sin simbolos: van a un .env que leen varios programas.
  -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
}

function Set-Variable-Env($archivo, $clave, $valor) {
  (Get-Content $archivo) -replace "^$clave=.*", "$clave=$valor" | Set-Content $archivo -Encoding UTF8
}

# Indexar .Matches[0] directo revienta cuando no hay match, y con ErrorActionPreference
# en Stop eso corta el script. Devolver "" es lo que el resto del script espera.
function Get-Variable-Env($archivo, $clave) {
  $linea = Select-String -Path $archivo -Pattern "^$clave=(.*)$" | Select-Object -First 1
  if ($linea) { return $linea.Matches[0].Groups[1].Value.Trim() }
  return ""
}

# ---------------------------------------------------------------- 1. requisitos
Paso "1/5  Requisitos"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Fatal "No encontre docker. Instala Docker Desktop: https://www.docker.com/products/docker-desktop/"
}
docker info *> $null
if ($LASTEXITCODE -ne 0) {
  Fatal "Docker esta instalado pero no corriendo. Abri Docker Desktop y espera a que arranque."
}
Ok "Docker corriendo"

# ------------------------------------------------------------------- 2. el .env
Paso "2/5  Configuracion (.env)"

if (Test-Path .env) {
  Ok ".env ya existe, no lo toco"
} else {
  Copy-Item .env.example .env
  # Las contrasenas son de tu MySQL, adentro de tu Docker: no las tenes que recordar
  # ni compartir con nadie. Las genero para que nadie se quede con la de ejemplo.
  Set-Variable-Env .env 'MYSQL_ROOT_PASSWORD' (Nueva-Clave)
  Set-Variable-Env .env 'DB_PASSWORD'         (Nueva-Clave)
  Ok ".env creado con contrasenas nuevas"
}

# --------------------------------------------------------------- 3. token TMDB
Paso "3/5  Token de TMDB (opcional)"

$actual = Get-Variable-Env .env 'TMDB_TOKEN'

if ($actual) {
  Ok "Ya tenes un token cargado"
} else {
  Write-Host "  Sirve para importar la cartelera real de Argentina. El sistema anda sin el:"
  Write-Host "  lo unico que no vas a poder hacer es traer peliculas desde el importador."
  Write-Host ""
  Write-Host "  Andrei te lo pasa por privado. Si todavia no lo tenes, saltealo con Enter:"
  Write-Host "  se agrega despues editando .env y reiniciando con 'docker compose up -d backend'."
  Write-Host ""
  $seguro = Read-Host "  Pegalo aca y Enter, o Enter solo para saltearlo" -AsSecureString
  $token  = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
              [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro))
  if ($token) {
    Set-Variable-Env .env 'TMDB_TOKEN' $token
    Ok "Token guardado en .env (que no se versiona)"
  } else {
    Info "Sin token. Lo podes agregar despues editando .env"
  }
}

# --------------------------------------------------------------- 4. levantar
Paso "4/5  Levantando"

Info "La primera vez tarda varios minutos: Maven baja las dependencias del backend."
docker compose up -d --build
if ($LASTEXITCODE -ne 0) { Fatal "Fallo el build. Mira el error de arriba." }

Write-Host -NoNewline "  Esperando a que los servicios esten sanos"
$sanos = 0
foreach ($i in 1..60) {
  $sanos = @(docker compose ps --format '{{.Health}}' 2>$null | Where-Object { $_ -eq 'healthy' }).Count
  if ($sanos -ge 2) { break }
  Write-Host -NoNewline "."
  Start-Sleep -Seconds 5
}
Write-Host ""

if ($sanos -lt 2) {
  Aviso "Tardaron mas de 5 minutos. Mira que pasa con:"
  Write-Host "      docker compose ps"
  Write-Host "      docker compose logs backend"
  exit 1
}
Ok "mysql y backend sanos"

# ------------------------------------------------------------------- 5. datos
Paso "5/5  Datos de ejemplo"

# Windows no trae shell POSIX ni curl garantizados, asi que el seed corre adentro de un
# contenedor conectado a la red del compose. No depende de Git Bash.
$cuenta = docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -Nse "SELECT COUNT(*) FROM pelicula"' 2>$null
$hayDatos = $false
if ($cuenta) { $n = 0; if ([int]::TryParse(("$cuenta").Trim(), [ref]$n)) { $hayDatos = $n -gt 0 } }

if ($hayDatos) {
  Ok "Ya hay datos cargados, no siembro de nuevo"
} else {
  $red = (docker network ls --format '{{.Name}}' | Where-Object { $_ -like '*_web' } | Select-Object -First 1)
  if (-not $red) {
    Aviso "No encontre la red del compose. Sembra a mano: ./seed/datos-de-ejemplo.sh"
  } else {
    docker run --rm --network $red -v "${PWD}/seed:/seed:ro" -e API=http://frontend:8080/api `
      --entrypoint sh curlimages/curl:latest /seed/datos-de-ejemplo.sh *> $null
    if ($LASTEXITCODE -eq 0) { Ok "4 peliculas, 6 salas y 8 funciones" }
    else { Aviso "Fallo el sembrado. Proba a mano: ./seed/datos-de-ejemplo.sh" }
  }
}

$puerto  = Get-Variable-Env .env 'PUERTO_WEB'
$adminer = Get-Variable-Env .env 'PUERTO_ADMINER'
if (-not $puerto)  { $puerto  = '8080' }
if (-not $adminer) { $adminer = '8081' }

Write-Host ""
Write-Host "Listo" -ForegroundColor White
Write-Host ""
Write-Host "  Cliente     http://localhost:$puerto"
Write-Host "  Panel       http://localhost:$puerto/admin.html   encargado@cine.uade.ar / cine2026"
Write-Host "  Puerta      el mismo panel                        puerta@cine.uade.ar / cine2026"
Write-Host "  Adminer     http://localhost:$adminer   servidor: mysql"
Write-Host ""
Write-Host "  Para bajarlo:  docker compose down"
Write-Host "  Guia completa: ..\_other\COMO-LEVANTARLO.md"
Write-Host ""
