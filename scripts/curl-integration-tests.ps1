#Requires -Version 5.1
<#
.SYNOPSIS
  TarroBuild curl-based integration tests - VERBOSE PRESENTATION MODE
  Shows every curl command, full headers, response body, and HTTP code.
  Run against already-running services behind the API Gateway (port 8080)
#>

param(
    [switch]$Pause
)

$GW = "http://localhost:8080"
$TS = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$PASS = 0; $FAIL = 0; $totalTests = 0; $results = @()

function Show-Banner {
    param($Title, $Subtitle)
    $w = 72
    $line = "=" * $w
    Write-Host "`n$line" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    if ($Subtitle) {
        Write-Host "  $Subtitle" -ForegroundColor DarkCyan
    }
    Write-Host "$line" -ForegroundColor Cyan
}

function Show-Step {
    param($Text)
    Write-Host "`n  >> $Text" -ForegroundColor Magenta
}

function Show-CurlCommand {
    param($Method, $Url, $Body, $Token)
    $global:totalTests++
    Write-Host "`n  -- [$global:totalTests] ----------------------------" -ForegroundColor DarkGray
    Write-Host "  [REQUEST] $Method $Url" -ForegroundColor Yellow
    Write-Host "  HEADERS:" -ForegroundColor DarkYellow
    Write-Host "    Content-Type: application/json" -ForegroundColor DarkYellow
    if ($Token) {
        $short = $Token.Substring(0, [math]::Min(40, $Token.Length)) + "..."
        Write-Host "    Authorization: Bearer $short" -ForegroundColor DarkYellow
    }
    if ($Body) {
        Write-Host "  BODY:" -ForegroundColor DarkYellow
        Write-Host "    $Body" -ForegroundColor DarkYellow
    }
}

function Show-ResponseBody {
    param($Body)
    if ([string]::IsNullOrEmpty($Body)) { return }
    Write-Host "  RESPONSE BODY:" -ForegroundColor DarkGreen
    try {
        $parsed = $Body | ConvertFrom-Json
        $pretty = $parsed | ConvertTo-Json
        $pretty -split "`n" | ForEach-Object { Write-Host "    $_" -ForegroundColor White }
    } catch {
        $Body -split "`n" | ForEach-Object { Write-Host "    $_" -ForegroundColor White }
    }
}

function Pause-IfInteractive {
    if ($Pause) {
        Write-Host "`n  --------------------" -ForegroundColor DarkGray
        Write-Host "  Press Enter to continue..." -ForegroundColor DarkGray
        $null = Read-Host
    }
}

function Test-Curl {
    param($Name, $Method, $Url, $Body, $Token, $ExpectedStatus)

    $headers = @{"Content-Type"="application/json"}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }

    $respFile = [System.IO.Path]::GetTempFileName()
    $argList = @("-s", "-o", $respFile, "-w", "%{http_code}", "-X", $Method)
    if ($Body) {
        $tmpFile = [System.IO.Path]::GetTempFileName()
        $Body | Set-Content -Path $tmpFile -Encoding ASCII
        $argList += @("-d", "@$tmpFile")
    }
    foreach ($h in $headers.Keys) { $argList += @("-H", "$($h): $($headers[$h])") }
    $argList += $Url

    Show-CurlCommand -Method $Method -Url $Url -Body $Body -Token $Token

    try {
        $code = curl.exe @argList 2>$null
        $respBody = Get-Content -Path $respFile -Raw -ErrorAction SilentlyContinue
        if ($Body -and $tmpFile) { Remove-Item -Path $tmpFile -Force -ErrorAction SilentlyContinue }
        Remove-Item -Path $respFile -Force -ErrorAction SilentlyContinue

        if ($code -match "^\d+$") {
            $ok = [int]$code -eq $ExpectedStatus
            if ($ok) { $script:PASS++ } else { $script:FAIL++ }
            $icon = if ($ok) { "OK" } else { "FAIL" }
            $fg = if ($ok) { "Green" } else { "Red" }
            Write-Host "  [STATUS] HTTP $code $([int]$code)" -ForegroundColor $fg
            Show-ResponseBody -Body $respBody
            Write-Host "  [RESULT] $icon $Name" -ForegroundColor $fg
            Write-Host "    expected: $ExpectedStatus | got: $code" -ForegroundColor $fg
            $script:results += @{Name=$Name; Status=if($ok){"PASS"}else{"FAIL"}; Code=$code; Expected=$ExpectedStatus}
        } else {
            $script:FAIL++
            Write-Host "  [STATUS] ERROR (no HTTP code)" -ForegroundColor Red
            Write-Host "  [RESULT] FAIL $Name => curl error: $code" -ForegroundColor Red
            $script:results += @{Name=$Name; Status="FAIL"; Code="ERR: $code"; Expected=$ExpectedStatus}
        }
    } catch {
        $script:FAIL++
        Write-Host "  [RESULT] FAIL $Name => $($_.Exception.Message)" -ForegroundColor Red
        $script:results += @{Name=$Name; Status="FAIL"; Code="EXCEPTION"; Expected=$ExpectedStatus}
    }
}

function Get-Token {
    param($Email, $Password, $Label)
    $tmp = [System.IO.Path]::GetTempFileName()
    $respTmp = [System.IO.Path]::GetTempFileName()
    $json = '{"email":"' + $Email + '","password":"' + $Password + '"}'
    $json | Set-Content -Path $tmp -Encoding ASCII

    Write-Host "`n  -- [1] ----------------------------" -ForegroundColor DarkGray
    Write-Host "  [REQUEST] POST $GW/api/auth/login" -ForegroundColor Yellow
    Write-Host "  HEADERS:" -ForegroundColor DarkYellow
    Write-Host "    Content-Type: application/json" -ForegroundColor DarkYellow
    Write-Host "  BODY:" -ForegroundColor DarkYellow
    Write-Host ('    {"email":"' + $Email + '","password":"****"}') -ForegroundColor DarkYellow

    $r = curl.exe -s -o $respTmp -w "%{http_code}" -X POST "$GW/api/auth/login" -H "Content-Type: application/json" -d "@$tmp" 2>$null
    $respBody = Get-Content -Path $respTmp -Raw -ErrorAction SilentlyContinue
    Remove-Item -Path $tmp -Force -ErrorAction SilentlyContinue
    Remove-Item -Path $respTmp -Force -ErrorAction SilentlyContinue

    Write-Host "  [STATUS] HTTP $r 200" -ForegroundColor Green
    Write-Host "  RESPONSE BODY:" -ForegroundColor DarkGreen
    try {
        $respBody -split "`n" | ForEach-Object { Write-Host "    $_" -ForegroundColor White }
    } catch {
        Write-Host "    $respBody" -ForegroundColor White
    }

    try {
        $parsed = $respBody | ConvertFrom-Json
        $tokenPreview = $parsed.token.Substring(0, [math]::Min(40, $parsed.token.Length)) + "..."
        Write-Host "  [RESULT] OK $Label authenticated" -ForegroundColor Green
        Write-Host "    token: $tokenPreview" -ForegroundColor Green
        Write-Host "    role: $($parsed.role)" -ForegroundColor Green
        return $parsed.token
    } catch {
        Write-Host "  [RESULT] FAIL $Label login failed" -ForegroundColor Red
        Write-Host "    response: $respBody" -ForegroundColor Red
        return $null
    }
}


# ==============================================
# 1. GET TOKENS
# ==============================================
Show-Banner -Title "PHASE 0: AUTHENTICATION" -Subtitle "Log in as admin (admin@tarrobuild.cl) and regular user (user@tarrobuild.cl) to obtain JWT tokens"

$adminToken = Get-Token "admin@tarrobuild.cl" "admin123" "Admin"
$userToken  = Get-Token "user@tarrobuild.cl" "test123"  "User"

if (-not $adminToken -or -not $userToken) { exit 1 }
Pause-IfInteractive

# ==============================================
# 2. AUTH SERVICE
# ==============================================
Show-Banner -Title "PHASE 1: AUTH SERVICE (:8081)" -Subtitle "Token validation, login error handling, user registration, logout"

Show-Step "Token validation - verifying a valid JWT returns user info"
Test-Curl -Name "Validate admin token" -Method GET -Url "$GW/api/auth/validate" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "Validate user token" -Method GET -Url "$GW/api/auth/validate" -Token $userToken -ExpectedStatus 200

Show-Step "Login error cases - wrong password and nonexistent email should return 401"
Test-Curl -Name "Login wrong password" -Method POST -Url "$GW/api/auth/login" -Body '{"email":"admin@tarrobuild.cl","password":"wrongpass"}' -ExpectedStatus 401
Test-Curl -Name "Login nonexistent email" -Method POST -Url "$GW/api/auth/login" -Body '{"email":"nobody@nowhere.com","password":"test123"}' -ExpectedStatus 401

Show-Step "User registration - register a new user, then try duplicating (should be 409)"
$ts1 = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$regEmail = "curltest${ts1}@tarrobuild.cl"
$regBody = '{"email":"' + $regEmail + '","password":"test123","name":"Curl","lastName":"Test","phone":"99999999"}'
Test-Curl -Name "Register new user" -Method POST -Url "$GW/api/auth/register" -Body $regBody -ExpectedStatus 201
Test-Curl -Name "Register duplicate email" -Method POST -Url "$GW/api/auth/register" -Body $regBody -ExpectedStatus 409

Show-Step "Logout - invalidate the current token"
Test-Curl -Name "Logout user" -Method POST -Url "$GW/api/auth/logout" -Token $userToken -ExpectedStatus 200

Pause-IfInteractive

# ==============================================
# 3. USER SERVICE
# ==============================================
Show-Banner -Title "PHASE 2: USER SERVICE (:8082)" -Subtitle "CRUD operations - only admin role can access these endpoints"

Show-Step "GET requests - list, get by ID, by email, by phone"
Test-Curl -Name "List all users (admin)" -Method GET -Url "$GW/api/users" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "List users as USER (should 403)" -Method GET -Url "$GW/api/users" -Token $userToken -ExpectedStatus 403
Test-Curl -Name "Get user by ID 1" -Method GET -Url "$GW/api/users/1" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "Get user by ID nonexistent 999" -Method GET -Url "$GW/api/users/999" -Token $adminToken -ExpectedStatus 404

Show-Step "Lookup by unique fields - email and phone"
Test-Curl -Name "Get user by email" -Method GET -Url "$GW/api/users/email/admin@tarrobuild.cl" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "Get user by phone" -Method GET -Url "$GW/api/users/phone/987654321" -Token $adminToken -ExpectedStatus 200

Show-Step "POST and PUT - creating and updating users"
$userEmail = "newuser${TS}@tarrobuild.cl"
$createUserBody = '{"name":"Test","lastName":"User","email":"' + $userEmail + '","phone":"88888888"}'
Test-Curl -Name "Create user" -Method POST -Url "$GW/api/users" -Token $adminToken -Body $createUserBody -ExpectedStatus 201
Test-Curl -Name "Update user 1" -Method PUT -Url "$GW/api/users/1" -Token $adminToken -Body '{"name":"Admin","lastName":"Updated","phone":"11111111"}' -ExpectedStatus 200

Show-Step "DELETE - trying to delete a nonexistent entity (404 expected)"
Test-Curl -Name "Delete nonexistent user 999" -Method DELETE -Url "$GW/api/users/999" -Token $adminToken -ExpectedStatus 404

Pause-IfInteractive

# ==============================================
# 4. CATEGORY SERVICE
# ==============================================
Show-Banner -Title "PHASE 3: CATEGORY SERVICE (:8084)" -Subtitle "Public GET endpoints, admin-only POST for categories and attribute definitions"

Show-Step "Public GET - anyone can list/view categories and attributes"
Test-Curl -Name "List all categories" -Method GET -Url "$GW/api/categories" -ExpectedStatus 200
Test-Curl -Name "Get category by ID 1 (CPU)" -Method GET -Url "$GW/api/categories/1" -ExpectedStatus 200
Test-Curl -Name "Get category by ID 2 (GPU)" -Method GET -Url "$GW/api/categories/2" -ExpectedStatus 200
Test-Curl -Name "Get category nonexistent 999" -Method GET -Url "$GW/api/categories/999" -ExpectedStatus 404

Show-Step "Attribute definitions per category"
Test-Curl -Name "List attributes for category 1" -Method GET -Url "$GW/api/categories/1/attributes" -ExpectedStatus 200
Test-Curl -Name "List attributes for category 2" -Method GET -Url "$GW/api/categories/2/attributes" -ExpectedStatus 200

Show-Step "Admin POST - creating categories and attributes (USER role gets 403)"
$catSlug = "test-cat-${TS}"
$catBody = '{"name":"Test Cat ' + $TS + '","slug":"' + $catSlug + '","description":"A test category"}'
Test-Curl -Name "Create category" -Method POST -Url "$GW/api/categories" -Token $adminToken -Body $catBody -ExpectedStatus 201
Test-Curl -Name "Create category as USER (should 403)" -Method POST -Url "$GW/api/categories" -Token $userToken -Body '{"name":"Should Fail","slug":"should-fail","description":"x"}' -ExpectedStatus 403
$attrBody = '{"attributeName":"TestAttr_' + $TS + '","valueType":"STRING","isRequired":false}'
Test-Curl -Name "Create attribute for category 1" -Method POST -Url "$GW/api/categories/1/attributes" -Token $adminToken -Body $attrBody -ExpectedStatus 201

Pause-IfInteractive

# ==============================================
# 5. PRODUCT SERVICE
# ==============================================
Show-Banner -Title "PHASE 4: PRODUCT SERVICE (:8083)" -Subtitle "Public GET (list/detail/filter by category/brand/price), admin CRUD + PATCH activate/deactivate"

Show-Step "Public GET - list all products and get by ID"
Test-Curl -Name "List all products" -Method GET -Url "$GW/api/products" -ExpectedStatus 200
Test-Curl -Name "Get product by ID 1" -Method GET -Url "$GW/api/products/1" -ExpectedStatus 200
Test-Curl -Name "Get product by ID 5" -Method GET -Url "$GW/api/products/5" -ExpectedStatus 200
Test-Curl -Name "Get product nonexistent 999" -Method GET -Url "$GW/api/products/999" -ExpectedStatus 404

Show-Step "Filtering - by category, by brand, by price range"
Test-Curl -Name "Products by category 1 (CPU)" -Method GET -Url "$GW/api/products/category/1" -ExpectedStatus 200
Test-Curl -Name "Products by category 2 (GPU)" -Method GET -Url "$GW/api/products/category/2" -ExpectedStatus 200
Test-Curl -Name "Products by brand Intel" -Method GET -Url "$GW/api/products/brand/Intel" -ExpectedStatus 200
Test-Curl -Name "Products by brand AMD" -Method GET -Url "$GW/api/products/brand/AMD" -ExpectedStatus 200
Test-Curl -Name "Products by price range (100k-500k)" -Method GET -Url "$GW/api/products/price?minPrice=100000&maxPrice=500000" -ExpectedStatus 200
Test-Curl -Name "Get product 1 attributes" -Method GET -Url "$GW/api/products/1/attributes" -ExpectedStatus 200

Show-Step "Admin POST - creating products and attributes"
Test-Curl -Name "Create product" -Method POST -Url "$GW/api/products" -Token $adminToken -Body '{"name":"Test Product","description":"A test","msrp":50000,"categoryId":1,"brand":"TestBrand","model":"T1000"}' -ExpectedStatus 201
Test-Curl -Name "Create product as USER (should 403)" -Method POST -Url "$GW/api/products" -Token $userToken -Body '{"name":"Should Fail","description":"x","msrp":100,"categoryId":1,"brand":"X","model":"X"}' -ExpectedStatus 403
$prodAttrBody = '{"attributeName":"TestAttr_' + $TS + '","attributeValue":"test-value"}'
Test-Curl -Name "Add attribute to product 1" -Method POST -Url "$GW/api/products/1/attributes" -Token $adminToken -Body $prodAttrBody -ExpectedStatus 201

Show-Step "PUT and PATCH - updating, deactivating, and reactivating a product"
Test-Curl -Name "Update product 1" -Method PUT -Url "$GW/api/products/1" -Token $adminToken -Body '{"name":"Intel Core i7-14700K Updated","description":"Updated desc","msrp":460000,"categoryId":1,"brand":"Intel","model":"BX8071514700K"}' -ExpectedStatus 200
Test-Curl -Name "Deactivate product 1" -Method PATCH -Url "$GW/api/products/1/deactivate" -Token $adminToken -ExpectedStatus 204
Test-Curl -Name "Activate product 1" -Method PATCH -Url "$GW/api/products/1/activate" -Token $adminToken -ExpectedStatus 204

Show-Step "DELETE - nonexistent product returns 404"
Test-Curl -Name "Delete nonexistent product 999" -Method DELETE -Url "$GW/api/products/999" -Token $adminToken -ExpectedStatus 404

Pause-IfInteractive

# ==============================================
# 6. BUILD SERVICE
# ==============================================
Show-Banner -Title "PHASE 5: BUILD SERVICE (:8087)" -Subtitle "Full CRUD on builds + build items, PATCH status - requires USER or ADMIN role"

Show-Step "GET builds - list all, get by ID, by user"
Test-Curl -Name "List builds (user)" -Method GET -Url "$GW/api/builds" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "List builds (admin)" -Method GET -Url "$GW/api/builds" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "List builds without auth (should 401)" -Method GET -Url "$GW/api/builds" -ExpectedStatus 401
Test-Curl -Name "Get build by ID 1" -Method GET -Url "$GW/api/builds/1" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Get build by ID nonexistent 999" -Method GET -Url "$GW/api/builds/999" -Token $userToken -ExpectedStatus 404
Test-Curl -Name "Builds by user 1" -Method GET -Url "$GW/api/builds/user/1" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Builds by user 2" -Method GET -Url "$GW/api/builds/user/2" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Build 1 for user 1" -Method GET -Url "$GW/api/builds/user/1/1" -Token $userToken -ExpectedStatus 200

Show-Step "Build items - list items within a build, get individual item"
Test-Curl -Name "List items for build 1" -Method GET -Url "$GW/api/builds/1/items" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "List items for build 2" -Method GET -Url "$GW/api/builds/2/items" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Get item 1 for build 1" -Method GET -Url "$GW/api/builds/1/items/1" -Token $userToken -ExpectedStatus 200

Show-Step "POST - create a new build and add items to it"
Test-Curl -Name "Create build" -Method POST -Url "$GW/api/builds" -Token $userToken -Body '{"userId":2,"name":"Curl Test Build"}' -ExpectedStatus 201
Test-Curl -Name "Add item to build 1" -Method POST -Url "$GW/api/builds/1/items" -Token $userToken -Body '{"productId":3,"quantity":1}' -ExpectedStatus 201

Show-Step "PUT and PATCH - update build name, update item quantity, change status"
Test-Curl -Name "Update build 1" -Method PUT -Url "$GW/api/builds/1" -Token $userToken -Body '{"userId":1,"name":"Updated Gaming Beast"}' -ExpectedStatus 200
Test-Curl -Name "Update item 1 for build 1" -Method PUT -Url "$GW/api/builds/1/items/1" -Token $userToken -Body '{"productId":2,"quantity":2}' -ExpectedStatus 200
Test-Curl -Name "Update build status to DRAFT" -Method PATCH -Url "$GW/api/builds/1/status" -Token $userToken -Body '{"status":"DRAFT"}' -ExpectedStatus 200

Show-Step "DELETE - remove an item from a build, try deleting nonexistent build"
Test-Curl -Name "Remove item from build 1" -Method DELETE -Url "$GW/api/builds/1/items/3" -Token $userToken -ExpectedStatus 204
Test-Curl -Name "Delete nonexistent build 999" -Method DELETE -Url "$GW/api/builds/999" -Token $userToken -ExpectedStatus 404

Pause-IfInteractive

# ==============================================
# 7. COMPATIBILITY SERVICE
# ==============================================
Show-Banner -Title "PHASE 6: COMPATIBILITY SERVICE (:8085)" -Subtitle "Public POST /check endpoint, admin CRUD for compatibility rules"

Show-Step "Public POST /check - detect incompatible component combinations"
Test-Curl -Name "Check incompatible build (CPU + mobo mismatch)" -Method POST -Url "$GW/api/compatibility/check" -Body '{"buildId":3,"productIds":[2,13]}' -ExpectedStatus 201
Test-Curl -Name "Check compatible build" -Method POST -Url "$GW/api/compatibility/check" -Body '{"buildId":1,"productIds":[2,15]}' -ExpectedStatus 201
Test-Curl -Name "Check with single product (bad request)" -Method POST -Url "$GW/api/compatibility/check" -Body '{"buildId":1,"productIds":[2]}' -ExpectedStatus 400

Show-Step "Admin GET - retrieve check results and rules"
Test-Curl -Name "Latest check for build 3" -Method GET -Url "$GW/api/compatibility/check/3" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "Get check by ID 1" -Method GET -Url "$GW/api/compatibility/check/id/1" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "List rules" -Method GET -Url "$GW/api/compatibility/rules" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "Get rule by ID 1" -Method GET -Url "$GW/api/compatibility/rules/1" -Token $adminToken -ExpectedStatus 200

Show-Step "Admin CRUD on rules - create, update, delete (nonexistent)"
Test-Curl -Name "Create rule" -Method POST -Url "$GW/api/compatibility/rules" -Token $adminToken -Body '{"sourceCategory":"RAM","sourceAttributeName":"Capacity","operator":"GTE","targetCategory":"Storage","targetAttributeName":"Capacity","incompatibilityReason":"Test rule"}' -ExpectedStatus 201
Test-Curl -Name "Update rule 1" -Method PUT -Url "$GW/api/compatibility/rules/1" -Token $adminToken -Body '{"sourceCategory":"CPU","sourceAttributeName":"Socket","operator":"EQ","targetCategory":"Motherboard","targetAttributeName":"Socket","incompatibilityReason":"Updated: CPU socket mismatch"}' -ExpectedStatus 200
Test-Curl -Name "Delete rule nonexistent 999" -Method DELETE -Url "$GW/api/compatibility/rules/999" -Token $adminToken -ExpectedStatus 404

Pause-IfInteractive

# ==============================================
# 8. PROVIDER SERVICE
# ==============================================
Show-Banner -Title "PHASE 7: PROVIDER SERVICE (:8086)" -Subtitle "Admin-only CRUD for providers and their product catalog links"

Show-Step "GET providers - list, get by ID, list provider products"
Test-Curl -Name "List providers" -Method GET -Url "$GW/api/providers" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "List providers as USER (should 403)" -Method GET -Url "$GW/api/providers" -Token $userToken -ExpectedStatus 403
Test-Curl -Name "Get provider by ID 1 (PcFactory)" -Method GET -Url "$GW/api/providers/1" -Token $adminToken -ExpectedStatus 200
Test-Curl -Name "Get provider nonexistent 999" -Method GET -Url "$GW/api/providers/999" -Token $adminToken -ExpectedStatus 404
Test-Curl -Name "List provider 1 products" -Method GET -Url "$GW/api/providers/1/products" -Token $adminToken -ExpectedStatus 200

Show-Step "POST - create provider, link product to provider"
Test-Curl -Name "Create provider" -Method POST -Url "$GW/api/providers" -Token $adminToken -Body '{"name":"Test Provider","contact":"test@provider.com","website":"testprovider.cl"}' -ExpectedStatus 201
Test-Curl -Name "Link product to provider 1" -Method POST -Url "$GW/api/providers/1/products" -Token $adminToken -Body '{"productId":2,"externalReference":"EXT-002"}' -ExpectedStatus 201

Show-Step "PUT and DELETE - update provider, unlink product, delete nonexistent"
Test-Curl -Name "Update provider 1" -Method PUT -Url "$GW/api/providers/1" -Token $adminToken -Body '{"name":"PcFactory Updated","contact":"contact@pcfactory.cl","website":"pcfactory.cl"}' -ExpectedStatus 200
Test-Curl -Name "Unlink product from provider 1" -Method DELETE -Url "$GW/api/providers/1/products/5" -Token $adminToken -ExpectedStatus 204
Test-Curl -Name "Delete nonexistent provider 999" -Method DELETE -Url "$GW/api/providers/999" -Token $adminToken -ExpectedStatus 404

Pause-IfInteractive

# ==============================================
# 9. ESTIMATE SERVICE
# ==============================================
Show-Banner -Title "PHASE 8: ESTIMATE SERVICE (:8088)" -Subtitle "Calculate and retrieve cost estimates for builds - requires USER or ADMIN role"

Show-Step "POST /calculate - compute cost estimate in CLP and USD"
Test-Curl -Name "Calculate estimate CLP" -Method POST -Url "$GW/api/estimate/calculate" -Token $userToken -Body '{"buildId":1,"currency":"CLP"}' -ExpectedStatus 201
Test-Curl -Name "Calculate estimate USD" -Method POST -Url "$GW/api/estimate/calculate" -Token $adminToken -Body '{"buildId":2,"currency":"USD"}' -ExpectedStatus 201
Test-Curl -Name "Calculate estimate no auth (should 401)" -Method POST -Url "$GW/api/estimate/calculate" -Body '{"buildId":1}' -ExpectedStatus 401

Show-Step "GET - retrieve latest estimate, by ID, all for a build"
Test-Curl -Name "Latest estimate for build 1" -Method GET -Url "$GW/api/estimate/1" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Latest estimate for build 999" -Method GET -Url "$GW/api/estimate/999" -Token $userToken -ExpectedStatus 404
Test-Curl -Name "Get estimate by ID 1" -Method GET -Url "$GW/api/estimate/id/1" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "All estimates for build 1" -Method GET -Url "$GW/api/estimate/all/1" -Token $userToken -ExpectedStatus 200

Pause-IfInteractive

# ==============================================
# 10. HARDWARE ADVISOR SERVICE
# ==============================================
Show-Banner -Title "PHASE 9: HARDWARE ADVISOR SERVICE (:8089)" -Subtitle "Generate and retrieve upgrade recommendations"

Show-Step "POST /generate - create recommendations for builds with issues"
Test-Curl -Name "Generate recs for build 3 (incompatible)" -Method POST -Url "$GW/api/recommendations/generate" -Token $userToken -Body '{"buildId":3}' -ExpectedStatus 201
Test-Curl -Name "Generate recs for build 5 (missing GPU)" -Method POST -Url "$GW/api/recommendations/generate" -Token $userToken -Body '{"buildId":5}' -ExpectedStatus 201
Test-Curl -Name "Generate recs for nonexistent build" -Method POST -Url "$GW/api/recommendations/generate" -Token $userToken -Body '{"buildId":999}' -ExpectedStatus 201

Show-Step "GET - retrieve recommendations by build ID or by recommendation ID"
Test-Curl -Name "Recs for build 3" -Method GET -Url "$GW/api/recommendations/3" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Recs for build 5" -Method GET -Url "$GW/api/recommendations/5" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Recs for nonexistent build" -Method GET -Url "$GW/api/recommendations/999" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Get recommendation by ID 1" -Method GET -Url "$GW/api/recommendations/id/1" -Token $userToken -ExpectedStatus 200

Pause-IfInteractive

# ==============================================
# 11. NOTIFICATION SERVICE
# ==============================================
Show-Banner -Title "PHASE 10: NOTIFICATION SERVICE (:8090)" -Subtitle "Send notifications and retrieve log history"

Show-Step "POST /send - create notifications for users (both roles)"
Test-Curl -Name "Send notification (user)" -Method POST -Url "$GW/api/notifications/send" -Token $userToken -Body '{"userId":2,"type":"TEST","content":"Curl test notification","status":"INFO"}' -ExpectedStatus 201
Test-Curl -Name "Send notification (admin)" -Method POST -Url "$GW/api/notifications/send" -Token $adminToken -Body '{"userId":1,"type":"TEST","content":"Admin test notification","status":"SUCCESS"}' -ExpectedStatus 201

Show-Step "GET /logs - list all, filter by user, get by ID"
Test-Curl -Name "List notification logs" -Method GET -Url "$GW/api/notifications/logs" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Logs for user 1" -Method GET -Url "$GW/api/notifications/logs/user/1" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Logs for user 2" -Method GET -Url "$GW/api/notifications/logs/user/2" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Get log by ID 1" -Method GET -Url "$GW/api/notifications/logs/1" -Token $userToken -ExpectedStatus 200
Test-Curl -Name "Get log nonexistent 999" -Method GET -Url "$GW/api/notifications/logs/999" -Token $userToken -ExpectedStatus 404

# ==============================================
# SUMMARY
# ==============================================
Write-Host "`n"
Show-Banner -Title "RESULTS SUMMARY" -Subtitle "${PASS} passed / ${FAIL} failed"

$total = $PASS + $FAIL
if ($total -gt 0) {
    $pct = [math]::Round($PASS / $total * 100, 1)
    Write-Host "  Pass rate: " -NoNewline
    Write-Host "$pct% " -ForegroundColor $(if($pct -eq 100){"Green"}else{"Yellow"}) -NoNewline
    Write-Host "($PASS of $total)" -ForegroundColor White
}

if ($FAIL -gt 0) {
    Write-Host "`n  Failed tests:" -ForegroundColor Red
    foreach ($r in $results) {
        if ($r.Status -eq "FAIL") {
            Write-Host "    FAIL $($r.Name): got $($r.Code), expected $($r.Expected)" -ForegroundColor Red
        }
    }
}
Write-Host "`n"
