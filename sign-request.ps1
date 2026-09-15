<#
.SYNOPSIS
    Genereaza header-ele x-signature / x-signature-body-hash cerute de SignatureValidationFilter
    si, optional, trimite direct request-ul catre un serviciu local.

.DESCRIPTION
    SignatureValidationFilter valideaza TOATE request-urile in afara de /actuator/**, inclusiv
    GET-urile cu body gol. Pentru un body dinamic (POST/PUT), header-ele trebuie recalculate de
    fiecare data - de-asta exista acest script, in loc sa incerci sa le tii hardcodate intr-un
    fisier .http static.

    Foloseste, implicit, cheia de test din test_key_signature.pem (radacina repo-ului), care
    corespunde cheii publice din truststore-ul vitale_app (vitale_app/src/main/resources/truststore.p12,
    alias "signature-key") - deci semnaturile generate cu ea vor trece validarea pe orice instanta
    locala a serviciului.

.PARAMETER Body
    Continutul JSON (ca string) al request-ului. Implicit: body gol (potrivit pentru GET).

.PARAMETER KeyPath
    Calea catre cheia privata PEM folosita la semnare. Implicit: test_key_signature.pem din radacina repo-ului.

.PARAMETER Uri
    Daca este specificat, scriptul trimite efectiv request-ul (via Invoke-RestMethod) in loc sa
    doar afiseze header-ele.

.PARAMETER Method
    Metoda HTTP folosita cand -Uri este specificat. Implicit: GET (sau POST daca -Body e dat).

.EXAMPLE
    ./sign-request.ps1
    # afiseaza header-ele pentru un GET cu body gol

.EXAMPLE
    ./sign-request.ps1 -Body '{"isbn":"1234567890","title":"Test","author":"Ana","price":19.99}'
    # afiseaza header-ele pentru acest body - le copiezi manual intr-un fisier .http

.EXAMPLE
    ./sign-request.ps1 -Body '{"isbn":"1234567890","title":"Test","author":"Ana","price":19.99}' `
        -Uri http://localhost:9001/books -Method Post
    # calculeaza header-ele SI trimite request-ul, afisand raspunsul
#>

param(
    [string]$Body = "",
    [string]$KeyPath = (Join-Path $PSScriptRoot "test_key_signature.pem"),
    [string]$Uri,
    [string]$Method
)

$opensslCmd = (Get-Command openssl -ErrorAction SilentlyContinue).Source
if (-not $opensslCmd) {
    # PowerShell "normal" nu are openssl in PATH chiar daca Git for Windows e instalat -
    # cauta-l langa git.exe (Git Bash il are in mingw64/bin sau usr/bin).
    $gitCmd = Get-Command git -ErrorAction SilentlyContinue
    if ($gitCmd) {
        $gitRoot = Split-Path (Split-Path $gitCmd.Source)
        foreach ($candidate in @("mingw64\bin\openssl.exe", "usr\bin\openssl.exe")) {
            $path = Join-Path $gitRoot $candidate
            if (Test-Path $path) { $opensslCmd = $path; break }
        }
    }
}
if (-not $opensslCmd) {
    Write-Error "openssl nu a fost gasit. Vine cu Git for Windows (Git Bash) - instaleaza-l sau ruleaza scriptul din Git Bash."
    exit 1
}

if (-not (Test-Path $KeyPath)) {
    Write-Error "Nu gasesc cheia privata la '$KeyPath'."
    exit 1
}

$tempFile = New-TemporaryFile
$hashFile = "$($tempFile.FullName).hash"
$sigFile  = "$($tempFile.FullName).sig"
try {
    # UTF8 fara BOM - un BOM in fisier ar schimba bytes-ii semnati fata de cei trimisi pe wire.
    # Nu se piping binar intre procese native prin PowerShell: pipeline-ul il trateaza ca text
    # si corupe octetii - fiecare pas scrie/citeste fisiere pe disc in schimb.
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllBytes($tempFile.FullName, $utf8NoBom.GetBytes($Body))

    & $opensslCmd dgst -sha256 -binary -out $hashFile $tempFile.FullName
    & $opensslCmd dgst -sha256 -sign $KeyPath -binary -out $sigFile $tempFile.FullName

    $bodyHash  = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($hashFile))
    $signature = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($sigFile))
} finally {
    Remove-Item $tempFile.FullName, $hashFile, $sigFile -ErrorAction SilentlyContinue
}

Write-Host "x-signature-body-hash: " -NoNewline -ForegroundColor Cyan
Write-Host $bodyHash
Write-Host "x-signature: " -NoNewline -ForegroundColor Cyan
Write-Host $signature

if ($Uri) {
    if (-not $Method) { $Method = if ($Body) { "Post" } else { "Get" } }

    $headers = @{
        "x-signature-body-hash" = $bodyHash
        "x-signature"           = $signature
    }

    Write-Host "`nTrimit $Method $Uri ..." -ForegroundColor Cyan
    try {
        if ($Body) {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers -ContentType "application/json" -Body $Body
        } else {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers
        }
        $response | ConvertTo-Json -Depth 10
    } catch {
        Write-Host "Eroare: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message -ForegroundColor Red }
    }
}
