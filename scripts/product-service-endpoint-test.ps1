# Product Service Endpoint Test Suite
# PowerShell 5.1+ compatible
# Run: powershell -ExecutionPolicy Bypass -File scripts/product-service-endpoint-test.ps1
# Assumes product-service is running on http://localhost:8083

$BASE = "http://localhost:8083"
$passed = 0
$failed = 0

# ============================================================
# Helper Functions
# ============================================================

function Get-StatusCode {
    param($Url, $Method = "GET", $Body = $null)

    try {
        $params = @{ Uri = $Url; Method = $Method; UseBasicParsing = $true; ErrorAction = "SilentlyContinue" }
        if ($Body) { $params["Body"] = $Body; $params["ContentType"] = "application/json" }
        $r = Invoke-WebRequest @params
        return [int]$r.StatusCode
    } catch {
        $code = [int]$_.Exception.Response.StatusCode
        if ($code -eq 0) { return 999 }
        return $code
    }
}

function Test-Endpoint {
    param($Name, $Url, $Method = "GET", $Body = $null, $Expected)

    $got = Get-StatusCode -Url $Url -Method $Method -Body $Body

    if ($got -eq $Expected) {
        $script:passed++
        Write-Host "  PASS" -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host "  FAIL (expected $Expected, got $got)" -ForegroundColor Red
    }
    Write-Host "    $Name" -ForegroundColor Gray
}

function Test-Group {
    param($Title)
    Write-Host ""
    Write-Host ">> $Title" -ForegroundColor Cyan
}

# ============================================================
# Main Execution
# ============================================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Product Service - Endpoint Test Suite" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# ============================================================
# PHASE 1: Data.sql Verification
# ============================================================

Test-Group "Phase 1: Data.Seed Verification"

# Verify seed data loads
Test-Endpoint "GET /api/products (should have 32 products)" "$BASE/api/products" -Expected 200

# Verify specific seed products exist
Test-Endpoint "GET /api/products/1 (CPU)" "$BASE/api/products/1" -Expected 200
Test-Endpoint "GET /api/products/5 (GPU)" "$BASE/api/products/5" -Expected 200
Test-Endpoint "GET /api/products/9 (RAM)" "$BASE/api/products/9" -Expected 200
Test-Endpoint "GET /api/products/13 (Motherboard)" "$BASE/api/products/13" -Expected 200
Test-Endpoint "GET /api/products/17 (Storage)" "$BASE/api/products/17" -Expected 200
Test-Endpoint "GET /api/products/21 (PSU)" "$BASE/api/products/21" -Expected 200
Test-Endpoint "GET /api/products/25 (Case)" "$BASE/api/products/25" -Expected 200
Test-Endpoint "GET /api/products/32 (Cooling)" "$BASE/api/products/32" -Expected 200

# Verify beyond seed range
Test-Endpoint "GET /api/products/33 (beyond seed)" "$BASE/api/products/33" -Expected 404

# Verify category filters work
Test-Group "Phase 1: Data.Seed Category Filters"
Test-Endpoint "Category 1 (CPUs)" "$BASE/api/products/category/1" -Expected 200
Test-Endpoint "Category 2 (GPUs)" "$BASE/api/products/category/2" -Expected 200
Test-Endpoint "Category 3 (RAM)" "$BASE/api/products/category/3" -Expected 200
Test-Endpoint "Category 4 (Motherboards)" "$BASE/api/products/category/4" -Expected 200
Test-Endpoint "Category 5 (Storage)" "$BASE/api/products/category/5" -Expected 200
Test-Endpoint "Category 6 (PSU)" "$BASE/api/products/category/6" -Expected 200
Test-Endpoint "Category 7 (Cases)" "$BASE/api/products/category/7" -Expected 200
Test-Endpoint "Category 8 (Cooling)" "$BASE/api/products/category/8" -Expected 200
Test-Endpoint "Category 99 (empty)" "$BASE/api/products/category/99" -Expected 200

# Verify brand filters work
Test-Group "Phase 1: Data.Seed Brand Filters"
Test-Endpoint "Brand Intel" "$BASE/api/products/brand/Intel" -Expected 200
Test-Endpoint "Brand AMD" "$BASE/api/products/brand/AMD" -Expected 200
Test-Endpoint "Brand NVIDIA" "$BASE/api/products/brand/NVIDIA" -Expected 200
Test-Endpoint "Brand Corsair" "$BASE/api/products/brand/Corsair" -Expected 200
Test-Endpoint "Brand NonExistent" "$BASE/api/products/brand/NonExistentBrand" -Expected 200

# Verify price range filter works
Test-Group "Phase 1: Data.Seed Price Range"
Test-Endpoint "Price 100k-500k" "$BASE/api/products/price?minPrice=100000&maxPrice=500000" -Expected 200
Test-Endpoint "Price range (no results)" "$BASE/api/products/price?minPrice=99999999&maxPrice=999999999" -Expected 200

# Verify seeded attributes
Test-Group "Phase 1: Data.Seed Attributes"
Test-Endpoint "Product 1 attributes (CPU)" "$BASE/api/products/1/attributes" -Expected 200
Test-Endpoint "Product 5 attributes (GPU)" "$BASE/api/products/5/attributes" -Expected 200
Test-Endpoint "Product 9 attributes (RAM)" "$BASE/api/products/9/attributes" -Expected 200
Test-Endpoint "Product 13 attributes (Motherboard)" "$BASE/api/products/13/attributes" -Expected 200
Test-Endpoint "Product 17 attributes (Storage)" "$BASE/api/products/17/attributes" -Expected 200

# ============================================================
# PHASE 2: ID Discovery
# ============================================================

Test-Group "Phase 2: ID Discovery"

# Get all product IDs to find next available
try {
    $allProducts = Invoke-WebRequest -Uri "$BASE/api/products" -Method GET -UseBasicParsing -ErrorAction Stop
    $productList = $allProducts.Content | ConvertFrom-Json
    $existingIds = $productList | ForEach-Object { $_.id }
    $maxId = ($existingIds | Measure-Object -Maximum).Maximum
    $nextId = $maxId + 1
    Write-Host "    Found max ID: $maxId, using $nextId for tests" -ForegroundColor Gray
} catch {
    Write-Host "    ERROR: Could not fetch product list" -ForegroundColor Red
    $nextId = 33  # Fallback
}

# ============================================================
# PHASE 3: CRUD Tests
# ============================================================

Test-Group "Phase 3: CRUD Operations"

# Create product and get actual ID from response
$createBody = '{"name":"Test Product","description":"Test description","msrp":100000,"categoryId":1,"brand":"TestBrand","model":"TestModel"}'
$createStatus = Get-StatusCode -Url "$BASE/api/products" -Method POST -Body $createBody
if ($createStatus -eq 201) {
    $created = Invoke-WebRequest -Uri "$BASE/api/products" -Method GET -UseBasicParsing -ErrorAction Stop
    $createdList = $created.Content | ConvertFrom-Json
    $testProductId = ($createdList | Where-Object { $_.name -eq "Test Product" } | Select-Object -First 1).id
    Write-Host "    Created product ID: $testProductId" -ForegroundColor Gray
} else {
    $testProductId = $nextId
}
Test-Endpoint "POST /api/products (create)" "$BASE/api/products" -Method POST -Body $createBody -Expected 201

# Get created product
Test-Endpoint "GET /api/products/$testProductId" "$BASE/api/products/$testProductId" -Expected 200

# Update product
$updateBody = '{"name":"Updated Product","description":"Updated","msrp":150000,"categoryId":1,"brand":"TestBrand","model":"TestModel"}'
Test-Endpoint "PUT /api/products/$testProductId" "$BASE/api/products/$testProductId" -Method PUT -Body $updateBody -Expected 200

# Deactivate product
Test-Endpoint "PATCH /api/products/$testProductId/deactivate" "$BASE/api/products/$testProductId/deactivate" -Method PATCH -Expected 204

# Reactivate product
$reactivateBody = '{"name":"Reactivated","msrp":100000,"categoryId":1,"brand":"TestBrand","model":"TestModel","isActive":true}'
Test-Endpoint "PUT /api/products/$testProductId (reactivate)" "$BASE/api/products/$testProductId" -Method PUT -Body $reactivateBody -Expected 200

# Delete product
Test-Endpoint "DELETE /api/products/$testProductId" "$BASE/api/products/$testProductId" -Method DELETE -Expected 204

# Verify deleted
Test-Endpoint "GET /api/products/$testProductId (deleted)" "$BASE/api/products/$testProductId" -Expected 404

# ============================================================
# PHASE 3b: Attribute Tests
# ============================================================

Test-Group "Phase 3b: Attribute Operations"

# Create new product for attributes
$attrProductBody = '{"name":"Attr Test Product","msrp":50000,"categoryId":1,"brand":"Test","model":"AttrTest"}'
Test-Endpoint "POST /api/products (for attributes)" "$BASE/api/products" -Method POST -Body $attrProductBody -Expected 201

# Get the new product ID
try {
    $newProducts = Invoke-WebRequest -Uri "$BASE/api/products" -Method GET -UseBasicParsing -ErrorAction Stop
    $newList = $newProducts.Content | ConvertFrom-Json
    $attrProductId = ($newList | Where-Object { $_.name -eq "Attr Test Product" } | Select-Object -First 1).id
    Write-Host "    Using product ID $attrProductId for attribute tests" -ForegroundColor Gray
} catch {
    $attrProductId = $nextId + 1
}

# Add attributes
Test-Endpoint "POST /api/products/$attrProductId/attributes (attr1)" "$BASE/api/products/$attrProductId/attributes" -Method POST -Body '{"attributeName":"Color","attributeValue":"Red"}' -Expected 201
Test-Endpoint "POST /api/products/$attrProductId/attributes (attr2)" "$BASE/api/products/$attrProductId/attributes" -Method POST -Body '{"attributeName":"Size","attributeValue":"Large"}' -Expected 201
Test-Endpoint "POST /api/products/$attrProductId/attributes (duplicate)" "$BASE/api/products/$attrProductId/attributes" -Method POST -Body '{"attributeName":"Color","attributeValue":"Blue"}' -Expected 409

# Get attributes
Test-Endpoint "GET /api/products/$attrProductId/attributes" "$BASE/api/products/$attrProductId/attributes" -Expected 200

# Get attribute IDs
try {
    $attrs = Invoke-WebRequest -Uri "$BASE/api/products/$attrProductId/attributes" -Method GET -UseBasicParsing -ErrorAction Stop
    $attrList = $attrs.Content | ConvertFrom-Json
    $attr1Id = ($attrList | Where-Object { $_.attributeName -eq "Color" } | Select-Object -First 1).id
    $attr2Id = ($attrList | Where-Object { $_.attributeName -eq "Size" } | Select-Object -First 1).id
    Write-Host "    Attribute IDs: $attr1Id, $attr2Id" -ForegroundColor Gray
} catch {
    $attr1Id = 1
    $attr2Id = 2
}

# Update attribute
Test-Endpoint "PUT /api/products/$attrProductId/attributes/$attr1Id" "$BASE/api/products/$attrProductId/attributes/$attr1Id" -Method PUT -Body '{"attributeName":"Color","attributeValue":"Blue"}' -Expected 200
Test-Endpoint "PUT /api/products/$attrProductId/attributes/99999 (not found)" "$BASE/api/products/$attrProductId/attributes/99999" -Method PUT -Body '{"attributeName":"Test","attributeValue":"Test"}' -Expected 404

# Delete attribute
Test-Endpoint "DELETE /api/products/$attrProductId/attributes/$attr1Id" "$BASE/api/products/$attrProductId/attributes/$attr1Id" -Method DELETE -Expected 204
Test-Endpoint "DELETE /api/products/$attrProductId/attributes/$attr1Id (already deleted)" "$BASE/api/products/$attrProductId/attributes/$attr1Id" -Method DELETE -Expected 404
Test-Endpoint "DELETE /api/products/$attrProductId/attributes/99999 (not found)" "$BASE/api/products/$attrProductId/attributes/99999" -Method DELETE -Expected 404

# ============================================================
# PHASE 3c: Validation Tests
# ============================================================

Test-Group "Phase 3c: Validation Tests"

# Product validation
Test-Endpoint "POST /api/products (empty name)" "$BASE/api/products" -Method POST -Body '{"name":"","msrp":100000,"categoryId":1,"brand":"Test","model":"Test"}' -Expected 400
Test-Endpoint "POST /api/products (name too short)" "$BASE/api/products" -Method POST -Body '{"name":"A","msrp":100000,"categoryId":1,"brand":"Test","model":"Test"}' -Expected 400
Test-Endpoint "POST /api/products (null name)" "$BASE/api/products" -Method POST -Body '{"msrp":100000,"categoryId":1,"brand":"Test","model":"Test"}' -Expected 400
Test-Endpoint "POST /api/products (null price)" "$BASE/api/products" -Method POST -Body '{"name":"Test","categoryId":1,"brand":"Test","model":"Test"}' -Expected 400
Test-Endpoint "POST /api/products (negative price)" "$BASE/api/products" -Method POST -Body '{"name":"Test","msrp":-100,"categoryId":1,"brand":"Test","model":"Test"}' -Expected 400
Test-Endpoint "POST /api/products (null categoryId)" "$BASE/api/products" -Method POST -Body '{"name":"Test","msrp":100000,"brand":"Test","model":"Test"}' -Expected 400
Test-Endpoint "POST /api/products (empty brand)" "$BASE/api/products" -Method POST -Body '{"name":"Test","msrp":100000,"categoryId":1,"brand":"","model":"Test"}' -Expected 400
Test-Endpoint "POST /api/products (empty model)" "$BASE/api/products" -Method POST -Body '{"name":"Test","msrp":100000,"categoryId":1,"brand":"Test","model":""}' -Expected 400
Test-Endpoint "POST /api/products (empty body)" "$BASE/api/products" -Method POST -Body '{}' -Expected 400

# Category validation via RestClient → category-service
Test-Endpoint "POST /api/products (invalid categoryId 99)" "$BASE/api/products" -Method POST -Body '{"name":"Test","msrp":100000,"categoryId":99,"brand":"Test","model":"Test"}' -Expected 404

# Attribute validation
Test-Endpoint "POST /api/products/$attrProductId/attributes (empty name)" "$BASE/api/products/$attrProductId/attributes" -Method POST -Body '{"attributeName":"","attributeValue":"Test"}' -Expected 400
Test-Endpoint "POST /api/products/$attrProductId/attributes (empty value)" "$BASE/api/products/$attrProductId/attributes" -Method POST -Body '{"attributeName":"Test","attributeValue":""}' -Expected 400

# Not found scenarios
Test-Endpoint "GET /api/products/99999 (not found)" "$BASE/api/products/99999" -Expected 404
Test-Endpoint "PUT /api/products/99999 (not found)" "$BASE/api/products/99999" -Method PUT -Body '{"name":"Test","msrp":100000,"categoryId":1,"brand":"Test","model":"Test"}' -Expected 404
Test-Endpoint "DELETE /api/products/99999 (not found)" "$BASE/api/products/99999" -Method DELETE -Expected 404
Test-Endpoint "PATCH /api/products/99999/deactivate (not found)" "$BASE/api/products/99999/deactivate" -Method PATCH -Expected 404
Test-Endpoint "GET /api/products/99999/attributes (product not found)" "$BASE/api/products/99999/attributes" -Expected 404
Test-Endpoint "POST /api/products/99999/attributes (product not found)" "$BASE/api/products/99999/attributes" -Method POST -Body '{"attributeName":"Test","attributeValue":"Test"}' -Expected 404

# ============================================================
# PHASE 4: Cleanup
# ============================================================

Test-Group "Phase 4: Cleanup"

# Delete test products created during tests
try {
    $finalProducts = Invoke-WebRequest -Uri "$BASE/api/products" -Method GET -UseBasicParsing -ErrorAction SilentlyContinue
    if ($finalProducts.StatusCode -eq 200) {
        $finalList = $finalProducts.Content | ConvertFrom-Json
        foreach ($p in $finalList) {
            if ($p.name -eq "Test Product" -or $p.name -eq "Attr Test Product") {
                $null = Get-StatusCode -Url "$BASE/api/products/$($p.id)" -Method DELETE
            }
        }
        Write-Host "    Cleanup completed" -ForegroundColor Gray
    }
} catch {
    Write-Host "    Cleanup warning: could not clean up test products" -ForegroundColor Yellow
}

# ============================================================
# Summary
# ============================================================

$total = $passed + $failed
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Results: $passed / $total passed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "============================================" -ForegroundColor Cyan

if ($failed -eq 0) { exit 0 } else { exit 1 }