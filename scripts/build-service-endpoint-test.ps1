# Build Service Endpoint Test Suite
# PowerShell 5.1+ compatible
# Run: powershell -ExecutionPolicy Bypass -File scripts/build-service-endpoint-test.ps1
# Assumes build-service is running on http://localhost:8087

param(
    [string]$BaseUrl = "http://localhost:8087"
)

$pass = 0
$fail = 0
$totalMs = 0
$testCount = 0

function Invoke-AndGetStatusCode {
    param($Method = "GET", $Url, $Body, $ContentType = "application/json")
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            UseBasicParsing = $true
            ErrorAction = "Stop"
        }
        if ($Body) {
            $params["Body"] = $Body
            $params["ContentType"] = $ContentType
        }
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
    param(
        $Name,
        $Method = "GET",
        $Url,
        $Body,
        $Expected,
        $ShowBody = $false
    )

    $global:testCount++
    $result = Invoke-AndGetStatusCode -Method $Method -Url $Url -Body $Body
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
Write-Host "  Build Service - Endpoint Test Suite" -ForegroundColor Cyan
Write-Host "  Target: $BaseUrl" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# --- Connection Check ---
$ping = Invoke-AndGetStatusCode -Url "$BaseUrl/api/builds"
if ($ping.Code -eq 999) {
    Write-Host ""
    Write-Host "ERROR: Cannot reach $BaseUrl" -ForegroundColor Red
    Write-Host "       Make sure build-service is running (port 8087)." -ForegroundColor Yellow
    Write-Host ""
    exit 1
}
Write-Host "`nService is alive (${BaseUrl})" -ForegroundColor Green

# -----------------------------------------------------------
# Build CRUD
# -----------------------------------------------------------
Test-Group "Build CRUD"

Test-Endpoint -Name "GET all builds" -Url "$BaseUrl/api/builds" -Expected 200 -ShowBody $true

Test-Endpoint -Name "GET build by id=1" -Url "$BaseUrl/api/builds/1" -Expected 200 -ShowBody $true

Test-Endpoint -Name "GET build by id=9999 (not found)" -Url "$BaseUrl/api/builds/9999" -Expected 404

Test-Endpoint -Name "GET builds by userId=1" -Url "$BaseUrl/api/builds/user/1" -Expected 200

Test-Endpoint -Name "GET build by id=1 and userId=1" -Url "$BaseUrl/api/builds/user/1/1" -Expected 200

Test-Endpoint -Name "GET build by id=1 and userId=9999 (not found)" -Url "$BaseUrl/api/builds/user/9999/1" -Expected 404

Test-Endpoint -Name "POST create build" -Method POST -Url "$BaseUrl/api/builds" -Body '{"userId":1,"name":"Test Build"}' -Expected 201 -ShowBody $true

Test-Endpoint -Name "POST create build invalid (blank name)" -Method POST -Url "$BaseUrl/api/builds" -Body '{"userId":1,"name":""}' -Expected 400

Test-Endpoint -Name "PUT update build" -Method PUT -Url "$BaseUrl/api/builds/1" -Body '{"userId":1,"name":"Updated Build"}' -Expected 200 -ShowBody $true

Test-Endpoint -Name "PATCH update build status" -Method Patch -Url "$BaseUrl/api/builds/1/status" -Body '{"status":"VALIDATED"}' -Expected 200 -ShowBody $true

Test-Endpoint -Name "PATCH update build status invalid value" -Method Patch -Url "$BaseUrl/api/builds/1/status" -Body '{"status":"BOGUS"}' -Expected 400

# -----------------------------------------------------------
# Build Items
# -----------------------------------------------------------
Test-Group "Build Items"

Test-Endpoint -Name "GET items by buildId=1" -Url "$BaseUrl/api/builds/1/items" -Expected 200 -ShowBody $true

Test-Endpoint -Name "GET items by buildId=9999 (not found)" -Url "$BaseUrl/api/builds/9999/items" -Expected 404

Test-Endpoint -Name "GET item by id=1 and buildId=1" -Url "$BaseUrl/api/builds/1/items/1" -Expected 200 -ShowBody $true

Test-Endpoint -Name "GET item by id=9999 and buildId=1 (not found)" -Url "$BaseUrl/api/builds/1/items/9999" -Expected 404

Test-Endpoint -Name "POST create item" -Method POST -Url "$BaseUrl/api/builds/1/items" -Body '{"productId":4,"quantity":1}' -Expected 201 -ShowBody $true

Test-Endpoint -Name "POST create item invalid quantity (0)" -Method POST -Url "$BaseUrl/api/builds/1/items" -Body '{"productId":4,"quantity":0}' -Expected 400

Test-Endpoint -Name "POST create item build not found" -Method POST -Url "$BaseUrl/api/builds/9999/items" -Body '{"productId":4,"quantity":1}' -Expected 404

Test-Endpoint -Name "PUT update item" -Method PUT -Url "$BaseUrl/api/builds/1/items/1" -Body '{"productId":4,"quantity":2}' -Expected 200 -ShowBody $true

Test-Endpoint -Name "DELETE item" -Method DELETE -Url "$BaseUrl/api/builds/1/items/1" -Expected 204

Test-Endpoint -Name "DELETE item not found" -Method DELETE -Url "$BaseUrl/api/builds/1/items/9999" -Expected 404

# -----------------------------------------------------------
# Delete Build
# -----------------------------------------------------------
Test-Group "Delete Build"

Test-Endpoint -Name "DELETE build" -Method DELETE -Url "$BaseUrl/api/builds/5" -Expected 204

Test-Endpoint -Name "DELETE build not found" -Method DELETE -Url "$BaseUrl/api/builds/9999" -Expected 404

# -----------------------------------------------------------
# Summary
# -----------------------------------------------------------
$total = $pass + $fail
$avgMs = if ($testCount -gt 0) { [math]::Round($totalMs / $testCount) } else { 0 }

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Summary" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Passed:  $pass / $total" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "  Failed:  $fail" -ForegroundColor $(if ($fail -eq 0) { "Gray" } else { "Red" })
Write-Host "  Total:   ${totalMs}ms ($avgMs ms avg)" -ForegroundColor Gray
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

if ($fail -eq 0) { exit 0 } else { exit 1 }
