# TarroBuild - Full Integration Test Suite
# Tests all 11 services directly to find bugs in the running environment
# Run: powershell -ExecutionPolicy Bypass -File scripts/full-integration-test.ps1

param(
    [switch]$Quiet
)

$pass = 0; $fail = 0; $totalMs = 0; $testCount = 0
$bugs = @()  # Collect pre-existing service bugs
$notes = @()

function Invoke-Request {
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
    $r = Invoke-Request -Method $Method -Url $Url -Body $Body -Headers $Headers
    $a = $r.Code; $ms = $r.Ms; $global:totalMs += $ms
    $ts = "$($ms)ms".PadLeft(7)
    if ($a -eq 999) {
        if (-not $Quiet) { Write-Host "  [!] $Name" -ForegroundColor Yellow }
        $global:fail++
    } elseif ($a -eq $Expected) {
        if (-not $Quiet) { Write-Host "  [+] $Name" -ForegroundColor Green }
        $global:pass++
    } else {
        if (-not $Quiet) {
            Write-Host "  [X] $Name" -ForegroundColor Red
            Write-Host "       $ts | got $a, expected $Expected" -ForegroundColor DarkRed
            if ($r.Body -match '"message":"([^"]+)"') { Write-Host "       msg: $($Matches[1])" -ForegroundColor Red }
        }
        $global:fail++
    }
    return $r
}

function Group {
    param($Title)
    if (-not $Quiet) { Write-Host "`n>>> $Title" -ForegroundColor Cyan }
}

function ReportBug {
    param($Service, $Endpoint, $Got, $Expected, $Msg)
    $global:bugs += @{ Service = $Service; Endpoint = $Endpoint; Got = $Got; Expected = $Expected; Msg = $Msg }
}

$G = @{}  # Service URLs
$G.Auth = "http://localhost:8081"
$G.User = "http://localhost:8082"
$G.Product = "http://localhost:8083"
$G.Category = "http://localhost:8084"
$G.Compatibility = "http://localhost:8085"
$G.Provider = "http://localhost:8086"
$G.Build = "http://localhost:8087"
$G.Estimate = "http://localhost:8088"
$G.Advisor = "http://localhost:8089"
$G.Notification = "http://localhost:8090"

if (-not $Quiet) {
    Write-Host "`n============================================" -ForegroundColor Cyan
    Write-Host "  TarroBuild - Full Integration Test Suite" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
}

# ============================================================
# 1. AUTH-SERVICE (8081)
# ============================================================
Group "1. Auth-Service (8081)"

# Register and get a token
$uniqueId = Get-Random -Minimum 10000 -Maximum 99999
$testEmail = "inttest$uniqueId@test.com"
$testPass = "IntTest123"

$reg = Test -Name "POST register new user" -Method POST -Url "$($G.Auth)/api/auth/register" -Body "{`"email`":`"$testEmail`",`"password`":`"$testPass`",`"name`":`"Integration`",`"lastName`":`"Test`",`"phone`":`"123456789`"}" -Expected 201 -ShowBody $true
$token = ""
if ($reg.Code -eq 201) { try { $token = ($reg.Body | ConvertFrom-Json).token } catch { } }

Test -Name "POST register duplicate email" -Method POST -Url "$($G.Auth)/api/auth/register" -Body "{`"email`":`"$testEmail`",`"password`":`"$testPass`",`"name`":`"Dup`",`"lastName`":`"User`",`"phone`":`"123456789`"}" -Expected 409

Test -Name "POST register blank name" -Method POST -Url "$($G.Auth)/api/auth/register" -Body "{`"email`":`"blankname$uniqueId@t.com`",`"password`":`"Test12345`",`"name`":`"`",`"lastName`":`"User`",`"phone`":`"123456789`"}" -Expected 400

Test -Name "POST register blank email" -Method POST -Url "$($G.Auth)/api/auth/register" -Body "{`"email`":`"`",`"password`":`"Test12345`",`"name`":`"Test`",`"lastName`":`"User`",`"phone`":`"123456789`"}" -Expected 400

Test -Name "POST register short password" -Method POST -Url "$($G.Auth)/api/auth/register" -Body "{`"email`":`"shortpw$uniqueId@t.com`",`"password`":`"12`",`"name`":`"Test`",`"lastName`":`"User`",`"phone`":`"123456789`"}" -Expected 400

Test -Name "POST register empty body" -Method POST -Url "$($G.Auth)/api/auth/register" -Body '{}' -Expected 400

# Login tests
Test -Name "POST login valid" -Method POST -Url "$($G.Auth)/api/auth/login" -Body "{`"email`":`"$testEmail`",`"password`":`"$testPass`"}" -Expected 200

Test -Name "POST login wrong password" -Method POST -Url "$($G.Auth)/api/auth/login" -Body "{`"email`":`"$testEmail`",`"password`":`"wrongpass123`"}" -Expected 401

Test -Name "POST login nonexistent email" -Method POST -Url "$($G.Auth)/api/auth/login" -Body "{`"email`":`"noone$uniqueId@nowhere.com`",`"password`":`"Test12345`"}" -Expected 401

Test -Name "POST login blank email" -Method POST -Url "$($G.Auth)/api/auth/login" -Body "{`"email`":`"`",`"password`":`"Test12345`"}" -Expected 400

Test -Name "POST login empty body" -Method POST -Url "$($G.Auth)/api/auth/login" -Body '{}' -Expected 400

# Validate tests
if ($token) {
    Test -Name "GET validate valid token" -Url "$($G.Auth)/api/auth/validate" -Headers @{Authorization = "Bearer $token"} -Expected 200
}

$validateNoHeader = Invoke-Request -Method GET -Url "$($G.Auth)/api/auth/validate"
if ($validateNoHeader.Code -ne 401) {
    ReportBug -Service "auth-service" -Endpoint "GET /api/auth/validate (no header)" -Got $validateNoHeader.Code -Expected 401 -Msg "Should return 401 when no Authorization header is present"
}

Test -Name "GET validate invalid token" -Url "$($G.Auth)/api/auth/validate" -Headers @{Authorization = "Bearer invalidtoken123"} -Expected 401

# Logout
if ($token) {
    Test -Name "POST logout" -Method POST -Url "$($G.Auth)/api/auth/logout" -Headers @{Authorization = "Bearer $token"} -Expected 200
}

# ============================================================
# 2. USER-SERVICE (8082)
# ============================================================
Group "2. User-Service (8082)"

Test -Name "GET users list" -Url "$($G.User)/api/users" -Expected 200

Test -Name "GET user by id=1" -Url "$($G.User)/api/users/1" -Expected 200

Test -Name "GET user by id=99999" -Url "$($G.User)/api/users/99999" -Expected 404

$userListRes = Invoke-Request -Method GET -Url "$($G.User)/api/users"
$existingUserIds = @()
if ($userListRes.Code -eq 200) {
    try { $userList = $userListRes.Body | ConvertFrom-Json; $existingUserIds = $userList | ForEach-Object { $_.id } } catch { }
}

# Check email uniqueness: try to create user with existing email
if ($userListRes.Code -eq 200) {
    $existingEmail = ""
    try {
        $firstUser = ($userListRes.Body | ConvertFrom-Json) | Select-Object -First 1
        $existingEmail = $firstUser.email
    } catch { }
    
    if ($existingEmail) {
        $dupEmail = Test -Name "POST user duplicate email" -Method POST -Url "$($G.User)/api/users" -Body "{`"name`":`"Dup`",`"lastName`":`"User`",`"email`":`"$existingEmail`",`"phone`":`"123456789`"}" -Expected 409
    }
}

# Validation
Test -Name "POST user null name" -Method POST -Url "$($G.User)/api/users" -Body "{`"lastName`":`"Test`",`"email`":`"noname$uniqueId@test.com`",`"phone`":`"123456789`"}" -Expected 400

Test -Name "POST user null email" -Method POST -Url "$($G.User)/api/users" -Body "{`"name`":`"Test`",`"lastName`":`"Test`",`"phone`":`"123456789`"}" -Expected 400

Test -Name "POST user empty body" -Method POST -Url "$($G.User)/api/users" -Body '{}' -Expected 400

Test -Name "POST user blank name" -Method POST -Url "$($G.User)/api/users" -Body "{`"name`":`"`",`"lastName`":`"Test`",`"email`":`"blankname$uniqueId@test.com`",`"phone`":`"123456789`"}" -Expected 400

Test -Name "PUT update user" -Method PUT -Url "$($G.User)/api/users/1" -Body "{`"name`":`"Updated`",`"lastName`":`"Admin`",`"email`":`"admin@tarrobuild.com`",`"phone`":`"111111111`"}" -Expected 200

# Try numeric path params with non-numeric values
$getUserAbc = Invoke-Request -Method GET -Url "$($G.User)/api/users/abc"
if ($getUserAbc.Code -ne 400) {
    ReportBug -Service "user-service" -Endpoint "GET /api/users/abc" -Got $getUserAbc.Code -Expected 400 -Msg "Non-numeric path param should return 400"
}

# ============================================================
# 3. CATEGORY-SERVICE (8084)
# ============================================================
Group "3. Category-Service (8084)"

Test -Name "GET categories" -Url "$($G.Category)/api/categories" -Expected 200
Test -Name "GET category 1" -Url "$($G.Category)/api/categories/1" -Expected 200
Test -Name "GET category 999" -Url "$($G.Category)/api/categories/999" -Expected 404

# CRUD - first create one
$catBody = "{`"name`":`"TestCat$uniqueId`",`"slug`":`"test-cat-$uniqueId`",`"description`":`"Test`"}"
$createCat = Test -Name "POST create category" -Method POST -Url "$($G.Category)/api/categories" -Body $catBody -Expected 201

$catId = 0
if ($createCat.Code -eq 201) { try { $catId = ($createCat.Body | ConvertFrom-Json).id } catch { } }

if ($catId -gt 0) {
    Test -Name "GET created category" -Url "$($G.Category)/api/categories/$catId" -Expected 200
    Test -Name "PUT update category" -Method PUT -Url "$($G.Category)/api/categories/$catId" -Body "{`"name`":`"UpdatedCat`",`"slug`":`"test-cat-$uniqueId`"}" -Expected 200
}

# Slug uniqueness
$dupSlug = Invoke-Request -Method POST -Url "$($G.Category)/api/categories" -Body "{`"name`":`"DupCat`",`"slug`":`"cpu`"}"
if ($dupSlug.Code -ne 409) {
    ReportBug -Service "category-service" -Endpoint "POST /api/categories duplicate slug" -Got $dupSlug.Code -Expected 409 -Msg "Duplicate slug should return 409"
}

# Validation 
Test -Name "POST blank name" -Method POST -Url "$($G.Category)/api/categories" -Body "{`"slug`":`"test`"}" -Expected 400

# NumberFormatException bug
$getCatAbc = Invoke-Request -Method GET -Url "$($G.Category)/api/categories/abc"
if ($getCatAbc.Code -ne 400) {
    ReportBug -Service "category-service" -Endpoint "GET /api/categories/abc" -Got $getCatAbc.Code -Expected 400 -Msg "Non-numeric path param should return 400, got $($getCatAbc.Code)"
}

Test -Name "GET category 1 attributes" -Url "$($G.Category)/api/categories/1/attributes" -Expected 200
Test -Name "GET category 999 attributes" -Url "$($G.Category)/api/categories/999/attributes" -Expected 404

# Attribute CRUD
$attrBody = "{`"attributeName`":`"TestAttr$uniqueId`",`"valueType`":`"STRING`",`"isRequired`":true}"
$createAttr = Test -Name "POST create attribute" -Method POST -Url "$($G.Category)/api/categories/1/attributes" -Body $attrBody -Expected 201

$attrId = 0
if ($createAttr.Code -eq 201) { try { $attrId = ($createAttr.Body | ConvertFrom-Json).id } catch { } }

# Attribute validation
Test -Name "POST attr blank name" -Method POST -Url "$($G.Category)/api/categories/1/attributes" -Body "{`"valueType`":`"STRING`",`"isRequired`":true}" -Expected 400

Test -Name "POST attr invalid valueType" -Method POST -Url "$($G.Category)/api/categories/1/attributes" -Body "{`"attributeName`":`"BadEnum`",`"valueType`":`"INVALID_TYPE`",`"isRequired`":false}" -Expected 400

Test -Name "POST attr to nonexistent category" -Method POST -Url "$($G.Category)/api/categories/999/attributes" -Body "{`"attributeName`":`"Test`",`"valueType`":`"STRING`",`"isRequired`":false}" -Expected 404

# ============================================================
# 4. PRODUCT-SERVICE (8083)
# ============================================================
Group "4. Product-Service (8083)"

Test -Name "GET products" -Url "$($G.Product)/api/products" -Expected 200
Test -Name "GET product 1" -Url "$($G.Product)/api/products/1" -Expected 200
Test -Name "GET product 99999" -Url "$($G.Product)/api/products/99999" -Expected 404

# NumberFormatException bug
$getProdAbc = Invoke-Request -Method GET -Url "$($G.Product)/api/products/abc"
if ($getProdAbc.Code -ne 400) {
    ReportBug -Service "product-service" -Endpoint "GET /api/products/abc" -Got $getProdAbc.Code -Expected 400 -Msg "Non-numeric path param should return 400"
}

# Category filter
Test -Name "GET products/category/1" -Url "$($G.Product)/api/products/category/1" -Expected 200
Test -Name "GET products/category/99" -Url "$($G.Product)/api/products/category/99" -Expected 200

$getProdCatAbc = Invoke-Request -Method GET -Url "$($G.Product)/api/products/category/abc"
if ($getProdCatAbc.Code -ne 400) {
    ReportBug -Service "product-service" -Endpoint "GET /api/products/category/abc" -Got $getProdCatAbc.Code -Expected 400 -Msg "Non-numeric category filter should return 400"
}

# Brand filter
Test -Name "GET products/brand/Intel" -Url "$($G.Product)/api/products/brand/Intel" -Expected 200
Test -Name "GET products/brand/NonExistent" -Url "$($G.Product)/api/products/brand/NonExistent" -Expected 200

# Price filter
Test -Name "GET products/price min-max" -Url "$($G.Product)/api/products/price?minPrice=100000&maxPrice=500000" -Expected 200

$getProdPrice = Invoke-Request -Method GET -Url "$($G.Product)/api/products/price?minPrice=abc"
if ($getProdPrice.Code -ne 400) {
    ReportBug -Service "product-service" -Endpoint "GET /api/products/price?minPrice=abc" -Got $getProdPrice.Code -Expected 400 -Msg "Non-numeric price should return 400"
}

# Product CRUD
$prodBody = "{`"name`":`"IntTestProd$uniqueId`",`"description`":`"Integration test`",`"msrp`":50000,`"categoryId`":1,`"brand`":`"TestBrand`",`"model`":`"TM-$uniqueId`"}"
$createProd = Test -Name "POST create product" -Method POST -Url "$($G.Product)/api/products" -Body $prodBody -Expected 201

$prodId = 0
if ($createProd.Code -eq 201) { try { $prodId = ($createProd.Body | ConvertFrom-Json).id } catch { } }

if ($prodId -gt 0) {
    Test -Name "GET created product" -Url "$($G.Product)/api/products/$prodId" -Expected 200
    Test -Name "PUT update product" -Method PUT -Url "$($G.Product)/api/products/$prodId" -Body "{`"name`":`"UpdatedProd`",`"msrp`":75000,`"categoryId`":1,`"brand`":`"TestBrand`",`"model`":`"TM-$uniqueId`"}" -Expected 200
    
    # Attribute CRUD
    $attr1 = Test -Name "POST add attribute" -Method POST -Url "$($G.Product)/api/products/$prodId/attributes" -Body "{`"attributeName`":`"Color`",`"attributeValue`":`"Red`"}" -Expected 201
    $attr1Id = 0
    if ($attr1.Code -eq 201) { try { $attr1Id = ($attr1.Body | ConvertFrom-Json).id } catch { } }
    
    Test -Name "POST duplicate attribute name → 409" -Method POST -Url "$($G.Product)/api/products/$prodId/attributes" -Body "{`"attributeName`":`"Color`",`"attributeValue`":`"Blue`"}" -Expected 409
    
    Test -Name "GET product attributes" -Url "$($G.Product)/api/products/$prodId/attributes" -Expected 200
    
    if ($attr1Id -gt 0) {
        Test -Name "PUT update attribute" -Method PUT -Url "$($G.Product)/api/products/$prodId/attributes/$attr1Id" -Body "{`"attributeName`":`"Color`",`"attributeValue`":`"Blue`"}" -Expected 200
        Test -Name "DELETE attribute" -Method DELETE -Url "$($G.Product)/api/products/$prodId/attributes/$attr1Id" -Expected 204
    }
    
    # Child attribute 404 bug
    $getAttr99999 = Invoke-Request -Method GET -Url "$($G.Product)/api/products/$prodId/attributes/99999"
    if ($getAttr99999.Code -ne 404) {
        ReportBug -Service "product-service" -Endpoint "GET /api/products/$prodId/attributes/99999" -Got $getAttr99999.Code -Expected 404 -Msg "Nonexistent child attribute should return 404"
    }
    
    # Deactivate / reactivate
    Test -Name "PATCH deactivate" -Method PATCH -Url "$($G.Product)/api/products/$prodId/deactivate" -Expected 204
    Test -Name "DELETE product" -Method DELETE -Url "$($G.Product)/api/products/$prodId" -Expected 204
}

# Product validation
Test -Name "POST product null name" -Method POST -Url "$($G.Product)/api/products" -Body "{`"msrp`":100000,`"categoryId`":1,`"brand`":`"T`",`"model`":`"T`"}" -Expected 400

Test -Name "POST product null price" -Method POST -Url "$($G.Product)/api/products" -Body "{`"name`":`"Test`",`"categoryId`":1,`"brand`":`"T`",`"model`":`"T`"}" -Expected 400

Test -Name "POST product negative price" -Method POST -Url "$($G.Product)/api/products" -Body "{`"name`":`"Test`",`"msrp`":-100,`"categoryId`":1,`"brand`":`"T`",`"model`":`"T`"}" -Expected 400

Test -Name "POST product invalid category → 404" -Method POST -Url "$($G.Product)/api/products" -Body "{`"name`":`"Test`",`"msrp`":100000,`"categoryId`":999,`"brand`":`"T`",`"model`":`"T`"}" -Expected 404

Test -Name "POST product empty body" -Method POST -Url "$($G.Product)/api/products" -Body '{}' -Expected 400

# ============================================================
# 5. COMPATIBILITY-SERVICE (8085)
# ============================================================
Group "5. Compatibility-Service (8085)"

Test -Name "POST check compatibility" -Method POST -Url "$($G.Compatibility)/api/compatibility/check" -Body "{`"buildId`":1,`"productIds`":[1,2,3]}" -Expected 201

Test -Name "POST check null buildId" -Method POST -Url "$($G.Compatibility)/api/compatibility/check" -Body "{`"productIds`":[1]}" -Expected 400

Test -Name "POST check empty productIds" -Method POST -Url "$($G.Compatibility)/api/compatibility/check" -Body "{`"buildId`":1,`"productIds`":[]}" -Expected 400

Test -Name "POST check empty body" -Method POST -Url "$($G.Compatibility)/api/compatibility/check" -Body '{}' -Expected 400

Test -Name "GET check by build 1" -Url "$($G.Compatibility)/api/compatibility/check/1" -Expected 200
Test -Name "GET check by build 99999" -Url "$($G.Compatibility)/api/compatibility/check/99999" -Expected 404

# Rules CRUD
$ruleBody = "{`"sourceCategory`":`"CPU`",`"sourceAttributeName`":`"socket`",`"operator`":`"NOT_EQUALS`",`"targetCategory`":`"MOTHERBOARD`",`"targetAttributeName`":`"socket`",`"incompatibilityReason`":`"Socket mismatch $uniqueId`"}"
$createRule = Test -Name "POST create rule" -Method POST -Url "$($G.Compatibility)/api/compatibility/rules" -Body $ruleBody -Expected 201

$ruleId = 0
if ($createRule.Code -eq 201) { try { $ruleId = ($createRule.Body | ConvertFrom-Json).id } catch { } }

Test -Name "GET all rules" -Url "$($G.Compatibility)/api/compatibility/rules" -Expected 200

if ($ruleId -gt 0) {
    Test -Name "GET rule by id" -Url "$($G.Compatibility)/api/compatibility/rules/$ruleId" -Expected 200
    Test -Name "PUT update rule" -Method PUT -Url "$($G.Compatibility)/api/compatibility/rules/$ruleId" -Body "{`"sourceCategory`":`"CPU`",`"sourceAttributeName`":`"socket`",`"operator`":`"NOT_EQUALS`",`"targetCategory`":`"MOTHERBOARD`",`"targetAttributeName`":`"socket`",`"incompatibilityReason`":`"Updated: Socket mismatch`"}" -Expected 200
    Test -Name "DELETE rule" -Method DELETE -Url "$($G.Compatibility)/api/compatibility/rules/$ruleId" -Expected 204
}

Test -Name "GET rule 99999" -Url "$($G.Compatibility)/api/compatibility/rules/99999" -Expected 404

# ============================================================
# 6. PROVIDER-SERVICE (8086)
# ============================================================
Group "6. Provider-Service (8086)"

Test -Name "GET providers" -Url "$($G.Provider)/api/providers" -Expected 200
Test -Name "GET provider 1" -Url "$($G.Provider)/api/providers/1" -Expected 200
Test -Name "GET provider 99999" -Url "$($G.Provider)/api/providers/99999" -Expected 404

$provBody = "{`"name`":`"IntTestProv$uniqueId`",`"contact`":`"inttest@prov.com`",`"website`":`"https://inttest.com`"}"
$createProv = Test -Name "POST create provider" -Method POST -Url "$($G.Provider)/api/providers" -Body $provBody -Expected 201

$provId = 0
if ($createProv.Code -eq 201) { try { $provId = ($createProv.Body | ConvertFrom-Json).id } catch { } }

if ($provId -gt 0) {
    Test -Name "GET created provider" -Url "$($G.Provider)/api/providers/$provId" -Expected 200
    Test -Name "PUT update provider" -Method PUT -Url "$($G.Provider)/api/providers/$provId" -Body "{`"name`":`"UpdatedProv`",`"contact`":`"upd@prov.com`",`"website`":`"https://upd.com`"}" -Expected 200
    Test -Name "DELETE provider" -Method DELETE -Url "$($G.Provider)/api/providers/$provId" -Expected 204
}

Test -Name "POST provider null name" -Method POST -Url "$($G.Provider)/api/providers" -Body "{`"contact`":`"t@t.com`",`"website`":`"https://t.com`"}" -Expected 400

# ProviderProduct CRUD
if ($provId -gt 0) {
    $ppBody = "{`"productId`":1,`"externalReference`":`"EXT-REF-$uniqueId`"}"
    $createPp = Test -Name "POST provider-product" -Method POST -Url "$($G.Provider)/api/providers/$provId/products" -Body $ppBody -Expected 201
    
    $ppId = 0
    if ($createPp.Code -eq 201) { try { $ppId = ($createPp.Body | ConvertFrom-Json).id } catch { } }
    
    if ($ppId -gt 0) {
        Test -Name "GET provider-product by id" -Url "$($G.Provider)/api/providers/$provId/products/$ppId" -Expected 200
        Test -Name "DELETE provider-product" -Method DELETE -Url "$($G.Provider)/api/providers/$provId/products/$ppId" -Expected 204
    }
    
    Test -Name "GET all provider-products" -Url "$($G.Provider)/api/providers/$provId/products" -Expected 200
}

# ============================================================
# 7. BUILD-SERVICE (8087)
# ============================================================
Group "7. Build-Service (8087)"

$buildBody = "{`"userId`":1,`"name`":`"IntTestBuild $uniqueId`"}"
$createBuild = Test -Name "POST create build" -Method POST -Url "$($G.Build)/api/builds" -Body $buildBody -Expected 201

$buildId = 0
if ($createBuild.Code -eq 201) { try { $buildId = ($createBuild.Body | ConvertFrom-Json).id } catch { } }

Test -Name "GET builds" -Url "$($G.Build)/api/builds" -Expected 200

if ($buildId -gt 0) {
    Test -Name "GET build by id" -Url "$($G.Build)/api/builds/$buildId" -Expected 200
    Test -Name "PATCH build status" -Method PATCH -Url "$($G.Build)/api/builds/$buildId/status" -Body "{`"status`":`"VALIDATED`"}" -Expected 200

    # Build items
    $itemBody = "{`"productId`":1,`"quantity`":1}"
    $createItem = Test -Name "POST add item" -Method POST -Url "$($G.Build)/api/builds/$buildId/items" -Body $itemBody -Expected 201

    $itemId = 0
    if ($createItem.Code -eq 201) { try { $itemId = ($createItem.Body | ConvertFrom-Json).id } catch { } }
    
    Test -Name "GET build items" -Url "$($G.Build)/api/builds/$buildId/items" -Expected 200
    
    if ($itemId -gt 0) {
        Test -Name "PUT update item" -Method PUT -Url "$($G.Build)/api/builds/$buildId/items/$itemId" -Body "{`"productId`":2,`"quantity`":2}" -Expected 200
        Test -Name "DELETE item" -Method DELETE -Url "$($G.Build)/api/builds/$buildId/items/$itemId" -Expected 204
    }

    Test -Name "POST item nonexistent product → 404" -Method POST -Url "$($G.Build)/api/builds/$buildId/items" -Body "{`"productId`":99999,`"quantity`":1}" -Expected 404
    
    Test -Name "DELETE build" -Method DELETE -Url "$($G.Build)/api/builds/$buildId" -Expected 204
}

# Validation
Test -Name "POST build null userId" -Method POST -Url "$($G.Build)/api/builds" -Body "{`"name`":`"Test`"}" -Expected 400

Test -Name "POST build empty name" -Method POST -Url "$($G.Build)/api/builds" -Body "{`"userId`":1,`"name`":`"`"}" -Expected 400

Test -Name "GET build 99999" -Url "$($G.Build)/api/builds/99999" -Expected 404

# POST build with no body
Test -Name "POST build empty body" -Method POST -Url "$($G.Build)/api/builds" -Body '{}' -Expected 400

# ============================================================
# 8. ESTIMATE-SERVICE (8088)
# ============================================================
Group "8. Estimate-Service (8088)"

# First create a build to estimate
$estBuildBody = "{`"userId`":1,`"name`":`"EstTest $uniqueId`"}"
$estBuild = Invoke-Request -Method POST -Url "$($G.Build)/api/builds" -Body $estBuildBody
$estBuildId = 0
if ($estBuild.Code -eq 201) { try { $estBuildId = ($estBuild.Body | ConvertFrom-Json).id } catch { } }

if ($estBuildId -gt 0) {
    # Add items so the build has products
    Invoke-Request -Method POST -Url "$($G.Build)/api/builds/$estBuildId/items" -Body "{`"productId`":1,`"quantity`":1}"
    Invoke-Request -Method POST -Url "$($G.Build)/api/builds/$estBuildId/items" -Body "{`"productId`":2,`"quantity`":1}"
    
    Test -Name "POST calculate estimate" -Method POST -Url "$($G.Estimate)/api/estimate/calculate" -Body "{`"buildId`":$estBuildId}" -Expected 201
    
    Test -Name "GET estimate by build" -Url "$($G.Estimate)/api/estimate/$estBuildId" -Expected 200
    
    Test -Name "GET estimate 99999" -Url "$($G.Estimate)/api/estimate/99999" -Expected 404
}

Test -Name "POST calculate empty body" -Method POST -Url "$($G.Estimate)/api/estimate/calculate" -Body '{}' -Expected 400

Test -Name "POST calculate no buildId" -Method POST -Url "$($G.Estimate)/api/estimate/calculate" -Body "{`"buildId`":99999}" -Expected 404

# ============================================================
# 9. HARDWARE-ADVISOR (8089)
# ============================================================
Group "9. Hardware-Advisor (8089)"

# Use existing build ID 1 (should have seeded items)
Test -Name "GET recommendations 1" -Url "$($G.Advisor)/api/recommendations/1" -Expected 200

Test -Name "GET recommendations 99999" -Url "$($G.Advisor)/api/recommendations/99999" -Expected 404

# Generate recommendations
Test -Name "POST generate recommendations" -Method POST -Url "$($G.Advisor)/api/recommendations/generate" -Body "{`"buildId`":1}" -Expected 201

Test -Name "POST generate no build" -Method POST -Url "$($G.Advisor)/api/recommendations/generate" -Body "{`"buildId`":99999}" -Expected 404

Test -Name "POST generate empty body" -Method POST -Url "$($G.Advisor)/api/recommendations/generate" -Body '{}' -Expected 400

# ============================================================
# 10. NOTIFICATION-SERVICE (8090)
# ============================================================
Group "10. Notification-Service (8090)"

Test -Name "POST send notification" -Method POST -Url "$($G.Notification)/api/notifications/send" -Body "{`"userId`":1,`"type`":`"TEST`",`"content`":`"Integration test notification $uniqueId`",`"status`":`"INFO`"}" -Expected 201

Test -Name "GET logs" -Url "$($G.Notification)/api/notifications/logs" -Expected 200

Test -Name "GET logs by user 1" -Url "$($G.Notification)/api/notifications/logs/user/1" -Expected 200
Test -Name "GET logs by user 99999" -Url "$($G.Notification)/api/notifications/logs/user/99999" -Expected 200

Test -Name "GET log 99999" -Url "$($G.Notification)/api/notifications/logs/99999" -Expected 404

# Validation
Test -Name "POST null userId" -Method POST -Url "$($G.Notification)/api/notifications/send" -Body "{`"type`":`"TEST`",`"content`":`"test`"}" -Expected 400

Test -Name "POST blank type" -Method POST -Url "$($G.Notification)/api/notifications/send" -Body "{`"userId`":1,`"type`":`"`",`"content`":`"test`"}" -Expected 400

Test -Name "POST invalid status" -Method POST -Url "$($G.Notification)/api/notifications/send" -Body "{`"userId`":1,`"type`":`"TEST`",`"content`":`"test`",`"status`":`"BOGUS`"}" -Expected 400

Test -Name "POST missing status" -Method POST -Url "$($G.Notification)/api/notifications/send" -Body "{`"userId`":1,`"type`":`"TEST`",`"content`":`"test`"}" -Expected 400

Test -Name "POST empty body" -Method POST -Url "$($G.Notification)/api/notifications/send" -Body '{}' -Expected 400

# ============================================================
# SUMMARY
# ============================================================
$total = $pass + $fail
$avgMs = if ($testCount -gt 0) { [math]::Round($totalMs / $testCount) } else { 0 }

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "  Full Integration Test - Summary" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Passed:  $pass / $total" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "  Failed:  $fail" -ForegroundColor $(if ($fail -eq 0) { "Gray" } else { "Red" })
Write-Host "  Total:   ${totalMs}ms ($avgMs ms avg)" -ForegroundColor Gray

Write-Host "`n============================================" -ForegroundColor Yellow
Write-Host "  Pre-existing Service Bugs Found" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

if ($bugs.Count -eq 0) {
    Write-Host "  None!" -ForegroundColor Green
} else {
    foreach ($b in $bugs) {
        Write-Host "`n  [$($b.Service)]" -ForegroundColor Red
        Write-Host "    Endpoint: $($b.Endpoint)" -ForegroundColor Gray
        Write-Host "    Got: $($b.Got) | Expected: $($b.Expected)" -ForegroundColor Gray
        Write-Host "    Msg: $($b.Msg)" -ForegroundColor Gray
    }
}

Write-Host "`n============================================" -ForegroundColor Cyan
exit $(if ($fail -eq 0) { 0 } else { 1 })
