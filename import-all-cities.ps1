$jar = ".\build\libs\HotelBooking-0.0.1-SNAPSHOT.jar"
$cities = @("saigon", "hanoi", "da nang", "da lat", "nha trang", "vung tau", "phu quoc", "hoi an", "hue")

foreach ($city in $cities) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Importing: $city (limit=200)" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    & java -jar $jar --spring.profiles.active=prod --server.port=0 --app.import-hotels=true --app.import-hotels.exit=true "--source=geoapify" "--city=$city" "--limit=200"
    Write-Host "  Finished: $city (exit code: $LASTEXITCODE)" -ForegroundColor Green
    Write-Host ""
}

Write-Host "ALL IMPORTS COMPLETE" -ForegroundColor Yellow
