param(
    [string]$CsvFile = "data/processed/osm_hotels.csv"
)

if (!(Test-Path -LiteralPath $CsvFile)) {
    throw "CSV file not found: $CsvFile"
}
if (!$env:DATABASE_URL -and !$env:SPRING_DATASOURCE_URL) {
    throw "Set DATABASE_URL or SPRING_DATASOURCE_URL before importing. Do not hardcode credentials."
}

Write-Host "Import is intentionally conservative. Review CSV and use application seed/import service or psql COPY with env-only credentials."
Write-Host "CSV ready: $CsvFile"
