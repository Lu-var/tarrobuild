# Notification Service Endpoint Test Suite
# PowerShell 5.1+ compatible
# Run: powershell -ExecutionPolicy Bypass -File scripts/notification-service-test.ps1
# Assumes notification-service is running on http://localhost:8090

param(
    [string]$BaseUrl = "http://localhost:8090"
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
Write-Host "  Notification Service - Endpoint Test Suite" -ForegroundColor Cyan
Write-Host "  Target: $BaseUrl" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# --- Connection Check ---
$ping = Invoke-AndGetStatusCode -Url "$BaseUrl/api/notifications/logs"
if ($ping.Code -eq 999) {
    Write-Host ""
    Write-Host "ERROR: Cannot reach $BaseUrl" -ForegroundColor Red
    Write-Host "       Make sure notification-service is running (port 8090)." -ForegroundColor Yellow
    Write-Host ""
    exit 1
}
Write-Host "`nService is alive (${BaseUrl})" -ForegroundColor Green

# -----------------------------------------------------------
# Send Notification
# -----------------------------------------------------------
Test-Group "Send Notification"

Test-Endpoint -Name "POST send notification (INFO status)" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"userId":1,"type":"ESTIMATE_READY","content":"Your budget estimate for build #5 is ready.","status":"INFO"}' -Expected 201 -ShowBody $true

Test-Endpoint -Name "POST send notification (WARNING status)" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"userId":1,"type":"COMPATIBILITY_WARNING","content":"Build #5 has incompatible components: CPU socket LGA1700 does not match motherboard socket AM5.","status":"WARNING"}' -Expected 201 -ShowBody $true

Test-Endpoint -Name "POST send notification (SUCCESS status)" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"userId":2,"type":"RECOMMENDATION_READY","content":"3 upgrade suggestions available for your build.","status":"SUCCESS"}' -Expected 201 -ShowBody $true

Test-Endpoint -Name "POST send notification (ERROR status)" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"userId":1,"type":"BUILD_ERROR","content":"Build #12 contains incompatible components: CPU LGA1700 is not compatible with cooler AM5.","status":"ERROR"}' -Expected 201 -ShowBody $true

# -----------------------------------------------------------
# Validation Errors (expected 400)
# -----------------------------------------------------------
Test-Group "Validation Errors (expected 400)"

Test-Endpoint -Name "POST null userId" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"type":"test","content":"test"}' -Expected 400

Test-Endpoint -Name "POST blank type" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"userId":1,"type":"","content":"test"}' -Expected 400

Test-Endpoint -Name "POST blank content" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"userId":1,"type":"test","content":""}' -Expected 400

Test-Endpoint -Name "POST empty body" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{}' -Expected 400

Test-Endpoint -Name "POST invalid status (BOGUS_STATUS)" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"userId":1,"type":"test","content":"test","status":"BOGUS_STATUS"}' -Expected 400 -ShowBody $true

Test-Endpoint -Name "POST missing status field" -Method POST -Url "$BaseUrl/api/notifications/send" -Body '{"userId":1,"type":"test","content":"test"}' -Expected 400

# -----------------------------------------------------------
# Query Logs
# -----------------------------------------------------------
Test-Group "Query Notification Logs"

Test-Endpoint -Name "GET all logs" -Url "$BaseUrl/api/notifications/logs" -Expected 200 -ShowBody $true

Test-Endpoint -Name "GET logs by userId=1" -Url "$BaseUrl/api/notifications/logs/user/1" -Expected 200

Test-Endpoint -Name "GET logs by userId=2" -Url "$BaseUrl/api/notifications/logs/user/2" -Expected 200

Test-Endpoint -Name "GET logs by userId=3" -Url "$BaseUrl/api/notifications/logs/user/3" -Expected 200

Test-Endpoint -Name "GET logs by userId=9999 (no results)" -Url "$BaseUrl/api/notifications/logs/user/9999" -Expected 200

Test-Endpoint -Name "GET log by id=1" -Url "$BaseUrl/api/notifications/logs/1" -Expected 200 -ShowBody $true

Test-Endpoint -Name "GET log by id=2" -Url "$BaseUrl/api/notifications/logs/2" -Expected 200

# -----------------------------------------------------------
# Not Found (expected 404)
# -----------------------------------------------------------
Test-Group "Not Found (expected 404)"

Test-Endpoint -Name "GET log by id=9999" -Url "$BaseUrl/api/notifications/logs/9999" -Expected 404

Test-Endpoint -Name "GET log by id=-1" -Url "$BaseUrl/api/notifications/logs/-1" -Expected 404

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
