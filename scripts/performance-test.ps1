param(
    [string]$env = "local",
    [string]$route = "direct",
    [string]$service = "products"
)

$base = @{
    local = @{ direct = "http://localhost:8083"; gateway = "http://localhost:8080"; tarro = "http://localhost:8080" }
    render = @{ direct = "https://product-service-e903.onrender.com"; gateway = "https://api-gateway-tzkw.onrender.com"; tarro = "https://tarrobuild.onrender.com" }
}

$prefix = @{
    products = "/api/v1/products"
    categories = "/api/v1/categories"
    builds = "/api/v1/builds"
    providers = "/api/v1/providers"
}

$url = $base[$env][$route] + $prefix[$service]

$result = curl.exe -o NUL -s -w "%{http_code}|%{time_total}" $url
$line = "$(Get-Date -f 'yyyy-MM-dd HH:mm:ss') | $env/$route/$service | HTTP $result"

$line | Add-Content logs/perf.log
Write-Output $line
