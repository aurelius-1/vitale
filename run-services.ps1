<#
.SYNOPSIS
    Helper pentru pornirea infrastructurii si a serviciilor din proiectul vitale.

.DESCRIPTION
    Wrapper peste Docker Compose si Maven ca sa nu mai retii comenzile pe de rost.
    Fiecare serviciu Spring Boot porneste intr-o fereastra noua de PowerShell,
    astfel incat log-urile lor sa nu se amestece.

    Detecteaza automat un JDK 21+ de pe disc (proiectul cere Java 21) daca JAVA_HOME
    implicit din shell e mai vechi, ca sa eviti erori de compilare / UnsupportedClassVersionError.

.EXAMPLE
    ./run-services.ps1 infra        # porneste postgres, redis, rabbitmq, minio, pgadmin
    ./run-services.ps1 build        # mvn clean install (fara books-ui, rapid) - instaleaza common-api in .m2
    ./run-services.ps1 vitale       # porneste vitale_app (catalog), port 9001
    ./run-services.ps1 dispatcher   # porneste dispatcher-service, port 9003
    ./run-services.ps1 edge         # porneste edge-service (gateway), port 9080
    ./run-services.ps1 ui           # porneste books-ui (Angular dev server), port 4200
    ./run-services.ps1 all          # infra + build + toate serviciile Java, fiecare in fereastra proprie
    ./run-services.ps1 status       # verifica rapid ce ruleaza (containere Docker + porturi)
    ./run-services.ps1 stop-infra   # opreste containerele
    ./run-services.ps1 stop-services # opreste ferestrele cu vitale_app/dispatcher/edge/books-ui pornite de acest script
    ./run-services.ps1 stop-all     # stop-services + stop-infra (opreste tot)
    ./run-services.ps1 build-images # mvn spring-boot:build-image pentru vitale/edge/dispatcher (imagini Docker locale)
    ./run-services.ps1 full-docker  # infra + toate serviciile Java containerizate (docker compose --profile full)
#>

param(
    [Parameter(Position = 0, Mandatory = $true)]
    [ValidateSet("infra", "stop-infra", "build", "vitale", "dispatcher", "edge", "ui", "all", "status", "build-images", "full-docker", "stop-services", "stop-all")]
    [string]$Target
)

$RepoRoot   = $PSScriptRoot
$ComposeDir = Join-Path $RepoRoot "polar-deployment\docker"

# Proiectul cere Java 21 (vezi <java.version> in pom.xml radacina), dar JAVA_HOME implicit
# din shell poate fi mai vechi (ex. un JDK 19 instalat separat de IDE). Daca Maven compileaza/
# porneste cu un JDK sub 21 primesti fie erori de compilare, fie UnsupportedClassVersionError
# la runtime pe clase compilate anterior cu 21. Detectam automat un JDK >=21 de pe disc
# (inclusiv cel descarcat de Gradle in ~/.gradle/jdks, ramas din trecutul Gradle al proiectului)
# si il folosim doar pentru comenzile Maven pornite de acest script.
function Test-JavaVersionAtLeast21([string]$JavaExe) {
    try {
        $out = & $JavaExe -version 2>&1 | Out-String
        if ($out -match 'version "(\d+)') {
            return [int]$Matches[1] -ge 21
        }
    } catch {}
    return $false
}

function Resolve-BuildJavaHome {
    if ($env:JAVA_HOME) {
        $exe = Join-Path $env:JAVA_HOME "bin\java.exe"
        if ((Test-Path $exe) -and (Test-JavaVersionAtLeast21 $exe)) { return $env:JAVA_HOME }
    }
    $searchRoots = @(
        (Join-Path $env:USERPROFILE ".gradle\jdks"),
        (Join-Path $env:USERPROFILE ".jdks"),
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium"
    )
    foreach ($root in $searchRoots) {
        if (-not (Test-Path $root)) { continue }
        foreach ($dir in Get-ChildItem $root -Directory -ErrorAction SilentlyContinue) {
            $exe = Join-Path $dir.FullName "bin\java.exe"
            if ((Test-Path $exe) -and (Test-JavaVersionAtLeast21 $exe)) { return $dir.FullName }
        }
    }
    return $null
}

$script:BuildJavaHome = Resolve-BuildJavaHome
if (-not $script:BuildJavaHome) {
    Write-Host "Atentie: nu am gasit niciun JDK 21+ pe disc (nici in JAVA_HOME, nici in .jdks/.gradle/jdks/Program Files). Maven va folosi JAVA_HOME curent si probabil va esua - instaleaza un JDK 21." -ForegroundColor Yellow
} elseif ($script:BuildJavaHome -ne $env:JAVA_HOME) {
    Write-Host "Folosesc JDK 21 detectat automat la: $script:BuildJavaHome" -ForegroundColor DarkGray
}

function Set-BuildJavaEnv {
    if ($script:BuildJavaHome) {
        $env:JAVA_HOME = $script:BuildJavaHome
        $env:Path = (Join-Path $script:BuildJavaHome "bin") + ";" + $env:Path
    }
}

function Get-JavaEnvPrefix {
    if ($script:BuildJavaHome) {
        return "`$env:JAVA_HOME = '$script:BuildJavaHome'; `$env:Path = '$script:BuildJavaHome\bin;' + `$env:Path; "
    }
    return ""
}

function Start-Infra {
    Write-Host "Pornesc infrastructura (postgres, redis, rabbitmq, minio, pgadmin)..." -ForegroundColor Cyan
    Push-Location $ComposeDir
    docker compose up -d polar-postgres polar-redis polar-rabbitmq polar-minio pgadmin
    Pop-Location
}

function Stop-Infra {
    Write-Host "Opresc infrastructura..." -ForegroundColor Cyan
    Push-Location $ComposeDir
    docker compose down
    Pop-Location
}

function Build-All {
    Write-Host "Build (mvn clean install, fara books-ui) - instaleaza common-api in .m2 pentru celelalte module..." -ForegroundColor Cyan
    Set-BuildJavaEnv
    Push-Location $RepoRoot
    mvn clean install -DskipTests "-pl" "!books-ui"
    Pop-Location
}

function Build-Images {
    Write-Host "Construiesc imaginile Docker locale (vitale, edge-service, dispatcher-service) via spring-boot:build-image..." -ForegroundColor Cyan
    Set-BuildJavaEnv
    Push-Location $RepoRoot
    mvn spring-boot:build-image "-pl" "vitale_app,edge-service,dispatcher-service" "-am" -DskipTests
    Pop-Location
}

function Start-FullDocker {
    Write-Host "Pornesc infrastructura + toate serviciile Java containerizate (profil 'full')..." -ForegroundColor Cyan
    Push-Location $ComposeDir
    docker compose --profile full up -d polar-postgres polar-redis polar-rabbitmq polar-minio pgadmin vitale edge-service dispatcher-service
    Pop-Location
}

function Test-Port([string]$ComputerName, [int]$Port, [int]$TimeoutMs = 300) {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $result = $client.BeginConnect($ComputerName, $Port, $null, $null)
        $success = $result.AsyncWaitHandle.WaitOne($TimeoutMs)
        if ($success -and $client.Connected) { $client.Close(); return $true }
        $client.Close()
        return $false
    } catch {
        return $false
    }
}

function Show-Status {
    Write-Host "`n== Infrastructura (Docker) ==" -ForegroundColor Cyan
    Push-Location $ComposeDir
    docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
    Pop-Location

    Write-Host "`n== Servicii Java / UI (porturi locale) ==" -ForegroundColor Cyan
    $checks = @(
        @{ Name = "vitale_app (catalog)";     Port = 9001 },
        @{ Name = "dispatcher-service";       Port = 9003 },
        @{ Name = "edge-service (gateway)";   Port = 9080 },
        @{ Name = "books-ui (Angular)";       Port = 4200 }
    )
    foreach ($c in $checks) {
        $open   = Test-Port -ComputerName "localhost" -Port $c.Port
        $status = if ($open) { "UP" } else { "down" }
        $color  = if ($open) { "Green" } else { "DarkGray" }
        Write-Host ("  {0,-28} port {1,-6} {2}" -f $c.Name, $c.Port, $status) -ForegroundColor $color
    }
    Write-Host ""
}

function Start-JavaService([string]$ModulePath, [string]$Title) {
    Write-Host "Pornesc $Title intr-o fereastra noua..." -ForegroundColor Cyan
    $envPrefix = Get-JavaEnvPrefix
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "cd '$RepoRoot'; $envPrefix mvn -pl $ModulePath spring-boot:run"
    ) -WindowStyle Normal
}

function Start-Ui {
    Write-Host "Pornesc books-ui (Angular dev server) pe portul 4200..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "cd '$RepoRoot\books-ui'; npm install; npm start"
    ) -WindowStyle Normal
}

# Ferestrele de servicii sunt pornite cu Start-Process powershell -NoExit, deci un simplu
# Stop-Process pe procesul powershell nu opreste si copiii lui (java/node porniti de mvn/npm).
# Cautam fereastra powershell dupa un fragment unic din linia de comanda si o oprim cu
# "taskkill /T" ca sa moara tot arborele de procese (powershell -> cmd -> mvn -> java, etc).
function Stop-MatchingWindows([string]$Match, [string]$Label) {
    $procs = Get-CimInstance Win32_Process -Filter "Name='powershell.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine -like "*-NoExit*" -and $_.CommandLine -like "*$Match*" }
    if (-not $procs) {
        Write-Host "  $Label - nicio fereastra pornita de script gasita." -ForegroundColor DarkGray
        return
    }
    foreach ($p in $procs) {
        Write-Host "  Opresc $Label (PID $($p.ProcessId))..." -ForegroundColor DarkGray
        & taskkill /PID $p.ProcessId /T /F *> $null
    }
}

function Stop-Services {
    Write-Host "Opresc serviciile Java / UI pornite de acest script (ferestrele lor + procesele copil)..." -ForegroundColor Cyan
    Stop-MatchingWindows -Match "-pl vitale_app"           -Label "vitale_app (catalog)"
    Stop-MatchingWindows -Match "-pl dispatcher-service"   -Label "dispatcher-service"
    Stop-MatchingWindows -Match "-pl edge-service"         -Label "edge-service (gateway)"
    Stop-MatchingWindows -Match "books-ui"                 -Label "books-ui (Angular)"
}

function Stop-All {
    Stop-Services
    Stop-Infra
}

switch ($Target) {
    "infra"      { Start-Infra }
    "stop-infra" { Stop-Infra }
    "build"      { Build-All }
    "vitale"     { Start-JavaService "vitale_app" "vitale_app (catalog, port 9001)" }
    "dispatcher" { Start-JavaService "dispatcher-service" "dispatcher-service (port 9003)" }
    "edge"       { Start-JavaService "edge-service" "edge-service / gateway (port 9080)" }
    "ui"         { Start-Ui }
    "all" {
        Start-Infra
        Build-All
        Start-JavaService "vitale_app" "vitale_app (catalog, port 9001)"
        Start-Sleep -Seconds 5
        Start-JavaService "dispatcher-service" "dispatcher-service (port 9003)"
        Start-JavaService "edge-service" "edge-service / gateway (port 9080)"
        Start-Ui
    }
    "status"        { Show-Status }
    "build-images"  { Build-Images }
    "full-docker"   { Start-FullDocker }
    "stop-services" { Stop-Services }
    "stop-all"      { Stop-All }
}
