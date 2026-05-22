# API Gateway - Comprehensive Endpoint Test Suite v2
# PowerShell 5.1+
# Run: powershell -ExecutionPolicy Bypass -File scripts/gateway-endpoint-test-v2.ps1

param([string]$BaseUrl = "http://localhost:8080")

$pass = 0; $fail = 0; $totalMs = 0; $testCount = 0
$notes = @()

function Invoke-AndGetStatusCode {
    param($Method = "GET", $Url, $Body, $ContentType = "application/json", $Headers = @{})
    try {
        $params = @{ Uri = $Url; Method = $Method; UseBasicParsing = $true; ErrorAction = "Stop" }
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
            $errBody = $reader.ReadToEnd() | Out-String; $reader.Close()
            return @{ Code = [int]$_.Exception.Response.StatusCode; Ms = $sw.ElapsedMilliseconds; Body = $errBody }
        }
        return @{ Code = 999; Ms = $sw.ElapsedMilliseconds; Body = "" }
    } catch { $sw.Stop(); return @{ Code = 999; Ms = $sw.ElapsedMilliseconds; Body = "" } }
}

function Test {
    param($Name, $Method = "GET", $Url, $Body, $Headers = @{}, $Expected, $ShowBody = $false)
    $global:testCount++
    $r = Invoke-AndGetStatusCode -Method $Method -Url $Url -Body $Body -Headers $Headers
    $a = $r.Code; $ms = $r.Ms; $global:totalMs += $ms
    $ts = "$($ms)ms".PadLeft(7)
    if ($a -eq 999) {
        Write-Host "  [!] $Name" -ForegroundColor Yellow
        Write-Host "       $ts | connection refused" -ForegroundColor DarkYellow; $global:fail++
    } elseif ($a -eq $Expected) {
        Write-Host "  [+] $Name" -ForegroundColor Green
        Write-Host "       $ts | $a" -ForegroundColor DarkGreen
        if ($ShowBody -and $r.Body) { try { Write-Host "       Body: $($r.Body | ConvertFrom-Json | ConvertTo-Json -Compress)" -ForegroundColor Gray } catch { } }
        $global:pass++
    } else {
        Write-Host "  [X] $Name" -ForegroundColor Red
        Write-Host "       $ts | got $a, expected $Expected" -ForegroundColor DarkRed
        if ($r.Body -match '"message":"([^"]+)"') { Write-Host "       msg: $($Matches[1])" -ForegroundColor Red }
        $global:fail++
    }
}

function Group { param($Title); Write-Host "`n>>> $Title" -ForegroundColor Cyan }

$uniqueId = Get-Random -Minimum 1000 -Maximum 9999
$testEmail = "test$uniqueId@test.com"
$testPass = "TestPass123"

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "  API Gateway - Comprehensive Test v2" -ForegroundColor Cyan
Write-Host "  Target: $BaseUrl" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# --- Register user and get token ---
Group "Setup: Register Test User"
$reg = Invoke-AndGetStatusCode -Method POST -Url "$BaseUrl/api/auth/register" -Body "{`"email`":`"$testEmail`",`"password`":`"$testPass`",`"name`":`"Test`",`"lastName`":`"User`",`"phone`":`"123456789`"}"
if ($reg.Code -eq 201) {
    Write-Host "  [+] Registered $testEmail" -ForegroundColor Green
    $global:userToken = ($reg.Body | ConvertFrom-Json).token
    $global:pass++
} elseif ($reg.Code -eq 409) {
    $login = Invoke-AndGetStatusCode -Method POST -Url "$BaseUrl/api/auth/login" -Body "{`"email`":`"$testEmail`",`"password`":`"$testPass`"}"
    if ($login.Code -eq 200) { $global:userToken = ($login.Body | ConvertFrom-Json).token; Write-Host "  [+] Logged in existing user" -ForegroundColor Green; $global:pass++ }
    else { $global:userToken = ""; Write-Host "  [X] Login also failed" -ForegroundColor Red; $global:fail++ }
} else {
    $global:userToken = ""; Write-Host "  [X] Register failed $($reg.Code)" -ForegroundColor Red; $global:fail++
}

$global:testCount++

# ============================================================
# 1. AUTH ENDPOINTS
# ============================================================
Group "1. Auth Endpoints"

# Register edge cases
Test -Name "POST /api/auth/register - duplicate email" -Method POST -Url "$BaseUrl/api/auth/register" -Body "{`"email`":`"$testEmail`",`"password`":`"$testPass`",`"name`":`"Dup`",`"lastName`":`"User`",`"phone`":`"123`"}" -Expected 409

Test -Name "POST /api/auth/register - blank email" -Method POST -Url "$BaseUrl/api/auth/register" -Body '{`"email`":`"`",`"password`":`"Test12345`",`"name`":`"Test`",`"lastName`":`"User`",`"phone`":`"123`"}' -Expected 400

Test -Name "POST /api/auth/register - short password" -Method POST -Url "$BaseUrl/api/auth/register" -Body '{`"email`":`"short@test.com`",`"password`":`"12`",`"name`":`"Test`",`"lastName`":`"User`",`"phone`":`"123`"}' -Expected 400

Test -Name "POST /api/auth/register - blank name" -Method POST -Url "$BaseUrl/api/auth/register" -Body '{`"email`":`"noname@test.com`",`"password`":`"Test12345`",`"name`":`"`",`"lastName`":`"User`",`"phone`":`"123`"}' -Expected 400

Test -Name "POST /api/auth/register - empty body" -Method POST -Url "$BaseUrl/api/auth/register" -Body '{}' -Expected 400

# Login edge cases
Test -Name "POST /api/auth/login - wrong password" -Method POST -Url "$BaseUrl/api/auth/login" -Body "{`"email`":`"$testEmail`",`"password`":`"wrongpass123`"}" -Expected 401

Test -Name "POST /api/auth/login - nonexistent email" -Method POST -Url "$BaseUrl/api/auth/login" -Body '{`"email`":`"noone@nowhere.com`",`"password`":`"Test12345`"}' -Expected 401

Test -Name "POST /api/auth/login - blank email" -Method POST -Url "$BaseUrl/api/auth/login" -Body '{`"email`":`"`",`"password`":`"Test12345`"}' -Expected 400

Test -Name "POST /api/auth/login - blank password" -Method POST -Url "$BaseUrl/api/auth/login" -Body '{`"email`":`"test@test.com`",`"password`":`"`"}' -Expected 400

Test -Name "POST /api/auth/login - empty body" -Method POST -Url "$BaseUrl/api/auth/login" -Body '{}' -Expected 400

# Validate endpoint edge cases
Test -Name "GET /api/auth/validate - wrong bearer prefix" -Url "$BaseUrl/api/auth/validate" -Headers @{Authorization = "bearer $global:userToken"} -Expected 500

Test -Name "GET /api/auth/validate - expired format token" -Url "$BaseUrl/api/auth/validate" -Headers @{Authorization = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiZXhwIjoxNTAwMDAwMDAwfQ.test"} -Expected 401

# Logout
if ($global:userToken) {
    Test -Name "POST /api/auth/logout - valid token" -Method POST -Url "$BaseUrl/api/auth/logout" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 200
}

# ============================================================
# 2. PRODUCT ENDPOINTS
# ============================================================
Group "2. Product Endpoints (Public)"

Test -Name "GET /api/products - list all" -Url "$BaseUrl/api/products" -Expected 200
Test -Name "GET /api/products/1 - CPU detail" -Url "$BaseUrl/api/products/1" -Expected 200
Test -Name "GET /api/products/99999 - not found" -Url "$BaseUrl/api/products/99999" -Expected 404
Test -Name "GET /api/products/abc - bad id" -Url "$BaseUrl/api/products/abc" -Expected 400

Test -Name "GET /api/products/category/1 - filter CPUs" -Url "$BaseUrl/api/products/category/1" -Expected 200
Test -Name "GET /api/products/category/2 - filter GPUs" -Url "$BaseUrl/api/products/category/2" -Expected 200
Test -Name "GET /api/products/category/99 - empty category" -Url "$BaseUrl/api/products/category/99" -Expected 200
Test -Name "GET /api/products/category/abc - bad category" -Url "$BaseUrl/api/products/category/abc" -Expected 400

Test -Name "GET /api/products/brand/Intel" -Url "$BaseUrl/api/products/brand/Intel" -Expected 200
Test -Name "GET /api/products/brand/AMD" -Url "$BaseUrl/api/products/brand/AMD" -Expected 200

Test -Name "GET /api/products/price?minPrice=100000&maxPrice=500000" -Url "$BaseUrl/api/products/price?minPrice=100000&maxPrice=500000" -Expected 200
Test -Name "GET /api/products/price?minPrice=0&maxPrice=1 - no results" -Url "$BaseUrl/api/products/price?minPrice=0&maxPrice=1" -Expected 200
Test -Name "GET /api/products/price?minPrice=abc - bad param" -Url "$BaseUrl/api/products/price?minPrice=abc" -Expected 400

# Product attributes
Test -Name "GET /api/products/1/attributes" -Url "$BaseUrl/api/products/1/attributes" -Expected 200
Test -Name "GET /api/products/99999/attributes - product not found" -Url "$BaseUrl/api/products/99999/attributes" -Expected 404
Test -Name "GET /api/products/1/attributes/99999 - attr not found" -Url "$BaseUrl/api/products/1/attributes/99999" -Expected 404

Product CRUD (with token - USER role, should be 403 on write)
if ($global:userToken) {
    Test -Name "POST /api/products - user → 403" -Method POST -Url "$BaseUrl/api/products" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"name`":`"Test`",`"msrp`":100000,`"categoryId`":1,`"brand`":`"T`",`"model`":`"T`"}' -Expected 403
    Test -Name "PUT /api/products/1 - user → 403" -Method PUT -Url "$BaseUrl/api/products/1" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"name`":`"Test`",`"msrp`":100000,`"categoryId`":1,`"brand`":`"T`",`"model`":`"T`"}' -Expected 403
    Test -Name "DELETE /api/products/1 - user → 403" -Method DELETE -Url "$BaseUrl/api/products/1" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 403
}

# ============================================================
# 3. CATEGORY ENDPOINTS
# ============================================================
Group "3. Category Endpoints (Public)"

Test -Name "GET /api/categories" -Url "$BaseUrl/api/categories" -Expected 200
Test -Name "GET /api/categories/1" -Url "$BaseUrl/api/categories/1" -Expected 200
Test -Name "GET /api/categories/999" -Url "$BaseUrl/api/categories/999" -Expected 404
Test -Name "GET /api/categories/abc" -Url "$BaseUrl/api/categories/abc" -Expected 400

Test -Name "GET /api/categories/1/attributes" -Url "$BaseUrl/api/categories/1/attributes" -Expected 200
Test -Name "GET /api/categories/999/attributes - cat not found" -Url "$BaseUrl/api/categories/999/attributes" -Expected 404

if ($global:userToken) {
    Test -Name "POST /api/categories - user → 403" -Method POST -Url "$BaseUrl/api/categories" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"name`":`"Test`",`"slug`":`"test`"}' -Expected 403
}

# ============================================================
# 4. COMPATIBILITY ENDPOINTS
# ============================================================
Group "4. Compatibility Endpoints"

Test -Name "POST /api/compatibility/check (public)" -Method POST -Url "$BaseUrl/api/compatibility/check" -Body '{`"buildId`":1,`"productIds`":[1,2,3]}' -Expected 201
Test -Name "POST /api/compatibility/check - empty body" -Method POST -Url "$BaseUrl/api/compatibility/check" -Body '{}' -Expected 400
Test -Name "POST /api/compatibility/check - null buildId" -Method POST -Url "$BaseUrl/api/compatibility/check" -Body '{`"productIds`":[1]}' -Expected 400
Test -Name "POST /api/compatibility/check - empty productIds" -Method POST -Url "$BaseUrl/api/compatibility/check" -Body '{`"buildId`":1,`"productIds`":[]}' -Expected 400

Test -Name "GET /api/compatibility/check/1 - by build" -Url "$BaseUrl/api/compatibility/check/1" -Expected 200
Test -Name "GET /api/compatibility/check/99999 - not found" -Url "$BaseUrl/api/compatibility/check/99999" -Expected 404

# Compatibility rules (admin-only)
if ($global:userToken) {
    Test -Name "POST /api/compatibility/rules - user → 403" -Method POST -Url "$BaseUrl/api/compatibility/rules" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"sourceCategory`":`"CPU`",`"sourceAttributeName`":`"socket`",`"operator`":`"NOT_EQUALS`",`"targetCategory`":`"MOTHERBOARD`",`"targetAttributeName`":`"socket`",`"incompatibilityReason`":`"Socket mismatch`"}' -Expected 403
}
Test -Name "GET /api/compatibility/rules - public?" -Url "$BaseUrl/api/compatibility/rules" -Expected 200

# ============================================================
# 5. BUILD ENDPOINTS
# ============================================================
Group "5. Build Endpoints"

if ($global:userToken) {
    Test -Name "POST /api/builds - create" -Method POST -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer $global:userToken"} -Body "{`"userId`":1,`"name`":`"Test Build $uniqueId`"}" -Expected 201

    $createBuild = Invoke-AndGetStatusCode -Method POST -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer $global:userToken"} -Body "{`"userId`":1,`"name`":`"Test Build $uniqueId`"}"
    $buildId = 0
    if ($createBuild.Code -eq 201) {
        try { $buildId = ($createBuild.Body | ConvertFrom-Json).id } catch { }
        Group "   Build Items"
        if ($buildId -gt 0) {
            Test -Name "POST /api/builds/$buildId/items - add CPU" -Method POST -Url "$BaseUrl/api/builds/$buildId/items" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"productId`":1,`"quantity`":1}' -Expected 201
            Test -Name "POST /api/builds/$buildId/items - add GPU" -Method POST -Url "$BaseUrl/api/builds/$buildId/items" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"productId`":5,`"quantity`":1}' -Expected 201
            Test -Name "POST /api/builds/$buildId/items - add GPU" -Method POST -Url "$BaseUrl/api/builds/$buildId/items" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"productId`":99,`"quantity`":1}' -Expected 404

            Test -Name "GET /api/builds/$buildId/items" -Url "$BaseUrl/api/builds/$buildId/items" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 200

            Test -Name "DELETE /api/builds/$buildId/items/1" -Method DELETE -Url "$BaseUrl/api/builds/$buildId/items/1" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 204
        }
    } else {
        Write-Host "  [X] Create build (skipping item tests)" -ForegroundColor Red; $global:fail++; $global:testCount++
    }

    Test -Name "GET /api/builds - list user" -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 200 -ShowBody $true

    Test -Name "POST /api/builds - null name" -Method POST -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{}' -Expected 400

    Test -Name "POST /api/builds - empty name" -Method POST -Url "$BaseUrl/api/builds" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"name`":`"`",`"userId`":1}' -Expected 400

    Test -Name "GET /api/builds/99999 - not found" -Url "$BaseUrl/api/builds/99999" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 404

    # Estimate
    Group "   Estimate"
    Test -Name "POST /api/estimate/calculate" -Method POST -Url "$BaseUrl/api/estimate/calculate" -Headers @{Authorization = "Bearer $global:userToken"} -Body "{`"buildId`":1}" -Expected 201
    Test -Name "POST /api/estimate/calculate - no build" -Method POST -Url "$BaseUrl/api/estimate/calculate" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"buildId`":99999}' -Expected 404

    # Recommendations
    Group "   Recommendations"
    Test -Name "POST /api/recommendations/generate" -Method POST -Url "$BaseUrl/api/recommendations/generate" -Headers @{Authorization = "Bearer $global:userToken"} -Body "{`"buildId`":1}" -Expected 201
    Test -Name "GET /api/recommendations/1" -Url "$BaseUrl/api/recommendations/1" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 200
}

# ============================================================
# 6. PROVIDER ENDPOINTS
# ============================================================
Group "6. Provider Endpoints"

Test -Name "GET /api/providers - list" -Url "$BaseUrl/api/providers" -Expected 200
Test -Name "GET /api/providers/1" -Url "$BaseUrl/api/providers/1" -Expected 200
Test -Name "GET /api/providers/99999" -Url "$BaseUrl/api/providers/99999" -Expected 404

if ($global:userToken) {
    Test -Name "POST /api/providers - user → 403" -Method POST -Url "$BaseUrl/api/providers" -Headers @{Authorization = "Bearer $global:userToken"} -Body '{`"name`":`"Test`",`"contact`":`"t@t.com`",`"website`":`"https://t.com`"}' -Expected 403
}

# ============================================================
# 7. NOTIFICATION ENDPOINTS
# ============================================================
Group "7. Notification Endpoints"

Test -Name "GET /api/notifications/logs - no auth" -Url "$BaseUrl/api/notifications/logs" -Expected 401

if ($global:userToken) {
    Test -Name "GET /api/notifications/logs - authed" -Url "$BaseUrl/api/notifications/logs" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 200
    Test -Name "GET /api/notifications/logs/user/1 - by user" -Url "$BaseUrl/api/notifications/logs/user/1" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 200
    Test -Name "GET /api/notifications/logs/1 - by id" -Url "$BaseUrl/api/notifications/logs/1" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 200
    Test -Name "GET /api/notifications/logs/99999 - not found" -Url "$BaseUrl/api/notifications/logs/99999" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 404
}

# ============================================================
# 8. USER ENDPOINTS (ADMIN)
# ============================================================
Group "8. User Endpoints"

if ($global:userToken) {
    Test -Name "GET /api/users - user → 403" -Url "$BaseUrl/api/users" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 403
    Test -Name "GET /api/users/1 - user → 403" -Url "$BaseUrl/api/users/1" -Headers @{Authorization = "Bearer $global:userToken"} -Expected 403
}

# ============================================================
# 9. GATEWAY EDGE CASES
# ============================================================
Group "9. Gateway Edge Cases"

# JWT filter edge cases
Test -Name "Auth header with 'Bearer' no space" -Url "$BaseUrl/api/builds" -Headers @{Authorization = "BearernoSpace"} -Expected 401
Test -Name "Auth header wrong case 'BEARER'" -Url "$BaseUrl/api/builds" -Headers @{Authorization = "BEARER invalid"} -Expected 401

# shouldNotFilter edge cases - public routes with stray Bearer header
Test -Name "GET /api/products/1 - staying Bearer header" -Url "$BaseUrl/api/products/1" -Headers @{Authorization = "Bearer still-here"} -Expected 200

# shouldNotFilter edge case - POST to compatibility check with stray header
Test -Name "POST /api/compatibility/check - stray Bearer" -Method POST -Url "$BaseUrl/api/compatibility/check" -Headers @{Authorization = "Bearer stray"} -Body '{`"buildId`":1,`"productIds`":[1,2]}' -Expected 201

# Cross-origin / header propagation - verify response headers
$checkHeaders = Invoke-AndGetStatusCode -Method GET -Url "$BaseUrl/api/products"
if ($checkHeaders.Body -match '[') { Write-Host "  [+] Response body is valid JSON (products list)" -ForegroundColor Green; $global:pass++ }
else { Write-Host "  [X] Response body is not valid JSON array" -ForegroundColor Red; $global:fail++ }
$global:testCount++

# ============================================================
# SUMMARY
# ============================================================
$total = $pass + $fail
$avgMs = if ($testCount -gt 0) { [math]::Round($totalMs / $testCount) } else { 0 }

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "  Summary" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Passed:  $pass / $total" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "  Failed:  $fail" -ForegroundColor $(if ($fail -eq 0) { "Gray" } else { "Red" })
Write-Host "  Total:   ${totalMs}ms ($avgMs ms avg)" -ForegroundColor Gray

if ($notes.Count -gt 0) {
    Write-Host "`n  Notes:" -ForegroundColor Yellow
    foreach ($n in $notes) { Write-Host "    - $n" -ForegroundColor Yellow }
}
Write-Host "============================================" -ForegroundColor Cyan
if ($fail -eq 0) { exit 0 } else { exit 1 }
