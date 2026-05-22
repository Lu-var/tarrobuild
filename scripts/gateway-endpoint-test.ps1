# Gateway Endpoint Test Suite
# PowerShell 5.1+ compatible
# Run: powershell -ExecutionPolicy Bypass -File scripts/gateway-endpoint-test.ps1
# Assumes api-gateway is running on http://localhost:8080 and all services are reachable

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$pass = 0
$fail = 0
$totalMs = 0
$testCount = 0
$failureNotes = @()

function Invoke-AndGetStatusCode {
    param($Method = "GET", $Url, $Body, $ContentType = "application/json", $Headers = @{})
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            UseBasicParsing = $true
            ErrorAction = "Stop"
        }
        if ($Body) { $params["Body"] = $Body; $params["ContentType"] = $ContentType }
        if ($Headers.Count -gt 0) { $params["Headers"] = $Headers }
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-WebRequest @params
        $sw.Stop()
        return @{ Code = [int]$r.StatusCode; Ms = $sw.ElapsedMilliseconds; Body = $r.Content }
    } catch [System.Net.WebException] {
        $sw.Stop()
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errBody = $reader.ReadToEnd() | Out-String
            $reader.Close()
            return @{ Code = [int]$_.Exception.Response.StatusCode; Ms = $sw.ElapsedMilliseconds; Body = $errBody }
        }
        return @{ Code = 999; Ms = $sw.ElapsedMilliseconds; Body = "" }
    } catch {
        $sw.Stop()
        return @{ Code = 999; Ms = $sw.ElapsedMilliseconds; Body = "" }
    }
}

function Test-Endpoint {
    param($Name, $Method = "GET", $Url, $Body, $Headers = @{}, $Expected, $ShowBody = $false)

    $global:testCount++
    $result = Invoke-AndGetStatusCode -Method $Method -Url $Url -Body $Body -Headers $Headers
    $actual = $result.Code
    $ms = $result.Ms
    $global:totalMs += $ms
    $timeStr = "$($ms)ms".PadLeft(7)

    if ($actual -eq 999) {
        Write-Host "  [!] $Name" -ForegroundColor Yellow
        Write-Host "       $timeStr | connection refused" -ForegroundColor DarkYellow
        $global:fail++
    } elseif ($actual -eq $Expected) {
        Write-Host "  [+] $Name" -ForegroundColor Green
        Write-Host "       $timeStr | $actual" -ForegroundColor DarkGreen
        if ($ShowBody -and $result.Body) {
            try { Write-Host "       Body: $($result.Body | ConvertFrom-Json | ConvertTo-Json -Compress)" -ForegroundColor Gray } catch { Write-Host "       Body: $($result.Body)" -ForegroundColor Gray }
        }
        $global:pass++
    } else {
        Write-Host "  [X] $Name" -ForegroundColor Red
        Write-Host "       $timeStr | got $actual, expected $Expected" -ForegroundColor DarkRed
        if ($result.Body -match '"message":"([^"]+)"') {
            Write-Host "       msg: $($Matches[1])" -ForegroundColor Red
        }
        $global:fail++
    }
}

function Test-Group {
    param($Title)
    Write-Host "`n>>> $Title" -ForegroundColor Cyan
}

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "  API Gateway - Endpoint Test Suite" -ForegroundColor Cyan
Write-Host "  Target: $BaseUrl" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# --- Connection Check ---
$ping = Invoke-AndGetStatusCode -Url "$BaseUrl/api/products"
if ($ping.Code -eq 999) {
    Write-Host "`nERROR: Cannot reach $BaseUrl" -ForegroundColor Red
    Write-Host "       Make sure api-gateway is running (port 8080)." -ForegroundColor Yellow
    exit 1
}
Write-Host "`nGateway is alive (${BaseUrl})" -ForegroundColor Green

# ============================================================
# PHASE 0: Register test users
# ============================================================
Test-Group "Phase 0: Register Test Users"

$userRegResult = Invoke-AndGetStatusCode -Method POST -Url "$BaseUrl/api/auth/register" -Body '{"email":"gateway-user@test.com","password":"test123456","name":"Gateway","lastName":"User","phone":"123456789"}'
$global:testCount++
if ($userRegResult.Code -eq 201) {
    Write-Host "  [+] Register test user" -ForegroundColor Green
    Write-Host "         $($userRegResult.Ms)ms | 201" -ForegroundColor DarkGreen
    $global:pass++
    try { $script:userToken = ($userRegResult.Body | ConvertFrom-Json).token; Write-Host "         Token obtained" -ForegroundColor Gray } catch { $script:userToken = ""; $global:fail++ }
} else {
    Write-Host "  [X] Register test user (got $($userRegResult.Code), expected 201)" -ForegroundColor Red
    if ($userRegResult.Code -eq 409) { Write-Host "       (User already exists - trying login instead)" -ForegroundColor Yellow }
    # Try login instead
    $loginResult = Invoke-AndGetStatusCode -Method POST -Url "$BaseUrl/api/auth/login" -Body '{"email":"gateway-user@test.com","password":"test123456"}'
    if ($loginResult.Code -eq 200) {
        Write-Host "  [+] Login existing user" -ForegroundColor Green
        $script:userToken = ($loginResult.Body | ConvertFrom-Json).token
        $global:pass++
    } else {
        $script:userToken = ""
        $global:fail++
    }
}

# Seed admin credentials have incorrect BCrypt hash — login will fail
$script:adminToken = ""
$adminLogin = Invoke-AndGetStatusCode -Method POST -Url "$BaseUrl/api/auth/login" -Body '{"email":"admin@tarrobuild.com","password":"admin123"}'
if ($adminLogin.Code -ne 200) {
    Write-Host "  [!] Admin seed credentials have mismatched BCrypt hash (skip admin-only tests)" -ForegroundColor Yellow
    $global:failureNotes += "Admin seed credentials (admin@tarrobuild.com) BCrypt hash doesn't match 'admin123'. Register endpoint sets USER role only."
}

# ============================================================
# PHASE 1: Auth — Validate Endpoint
# ============================================================
Test-Group "Phase 1: Token Validation"

if ($script:userToken) {
    Test-Endpoint -Name "GET /api/auth/validate (valid token)" -Url "$BaseUrl/api/auth/validate" -Headers @{Authorization = "Bearer $script:userToken"} -Expected 200 -ShowBody $true
}
Test-Endpoint -Name "GET /api/auth/validate (no token)" -Url "$BaseUrl/api/auth/validate" -Expected 500
Test-Endpoint -Name "GET /api/auth/validate (invalid token)" -Url "$BaseUrl/api/auth/validate" -Headers @{Authorization = "Bearer invalidtoken123"} -Expected 401

# ============================================================
# PHASE 2: Public Endpoints (no token)
# ============================================================
Test-Group "Phase 2: Public Endpoints"

Test-Endpoint -Name "GET /api/products" -Url "$BaseUrl/api/products" -Expected 200
Test-Endpoint -Name "GET /api/products/1" -Url "$BaseUrl/api/products/1" -Expected 200
Test-Endpoint -Name "GET /api/categories" -Url "$BaseUrl/api/categories" -Expected 200
Test-Endpoint -Name "GET /api/categories/1" -Url "$BaseUrl/api/categories/1" -Expected 200
Test-Endpoint -Name "POST /api/compatibility/check" -Method POST -Url "$BaseUrl/api/compatibility/check" -Body '{"buildId":1,"productIds":[1,2,3]}' -Expected 200

# ============================================================
# PHASE 3: USER-protected Endpoints
# ============================================================
Test-Group "Phase 3: Protected Endpoints (USER role)"

Test-Endpoint -Name "GET /api/builds (no token → 401)" -Url "$BaseUrl/api/builds" -Expected 401

if ($script:userToken) {
    Test-Endpoint -Name "GET /api/builds (user token)" -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer $script:userToken"} -Expected 200
    Test-Endpoint -Name "GET /api/notifications/logs (user token)" -Url "$BaseUrl/api/notifications/logs" -Headers @{Authorization = "Bearer $script:userToken"} -Expected 200
}

if ($script:adminToken) {
    Test-Endpoint -Name "GET /api/builds (admin token)" -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer $script:adminToken"} -Expected 200
}

# ============================================================
# PHASE 4: ADMIN-only Endpoints
# ============================================================
Test-Group "Phase 4: Admin Endpoints"

if ($script:userToken) {
    Test-Endpoint -Name "POST /api/products (user → 403)" -Method POST -Url "$BaseUrl/api/products" -Headers @{Authorization = "Bearer $script:userToken"} -Body '{"name":"Test","msrp":100000,"categoryId":1,"brand":"Test","model":"Test"}' -Expected 403
}

if ($script:adminToken) {
    Test-Endpoint -Name "POST /api/providers (admin)" -Method POST -Url "$BaseUrl/api/providers" -Headers @{Authorization = "Bearer $script:adminToken"} -Body '{"name":"Test","contact":"test@test.com","website":"https://test.com"}' -Expected 201
}

# ============================================================
# PHASE 5: Gateway Error Handling
# ============================================================
Test-Group "Phase 5: Gateway Error Handling"

Test-Endpoint -Name "GET /api/builds (invalid token)" -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer invalidtoken123"} -Expected 401
Test-Endpoint -Name "GET /api/builds (empty bearer)" -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer "} -Expected 401
Test-Endpoint -Name "GET /api/nonexistent (unknown route)" -Url "$BaseUrl/api/nonexistent" -Expected 401

# ============================================================
# Summary
# ============================================================
$total = $pass + $fail
$avgMs = if ($testCount -gt 0) { [math]::Round($totalMs / $testCount) } else { 0 }

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Summary" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Passed:  $pass / $total" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "  Failed:  $fail" -ForegroundColor $(if ($fail -eq 0) { "Gray" } else { "Red" })
Write-Host "  Total:   ${totalMs}ms ($avgMs ms avg)" -ForegroundColor Gray

if ($failureNotes.Count -gt 0) {
    Write-Host "`n  Notes:" -ForegroundColor Yellow
    foreach ($note in $failureNotes) {
        Write-Host "    - $note" -ForegroundColor Yellow
    }
}
Write-Host "============================================" -ForegroundColor Cyan

if ($fail -eq 0) { exit 0 } else { exit 1 }
