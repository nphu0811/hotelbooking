param(
    [string]$InputFile = "data/raw/osm_vietnam_hotels.json",
    [string]$OutFile = "data/processed/osm_hotels.csv"
)

if (!(Test-Path -LiteralPath $InputFile)) {
    throw "Input file not found: $InputFile"
}

$json = Get-Content -Raw -Encoding UTF8 -LiteralPath $InputFile | ConvertFrom-Json
New-Item -ItemType Directory -Force -Path (Split-Path $OutFile) | Out-Null
$rows = foreach ($element in $json.elements) {
    $tags = $element.tags
    $name = if ($tags.name) { $tags.name } else { "OSM Hotel $($element.id)" }
    $city = if ($tags.'addr:city') { $tags.'addr:city' } else { "Việt Nam" }
    $address = (($tags.'addr:housenumber', $tags.'addr:street', $tags.'addr:district') | Where-Object { $_ }) -join " "
    if (!$address) { $address = "OSM element $($element.type)/$($element.id)" }
    [pscustomobject]@{
        hotel_name = $name
        city = $city
        province = $city
        address = $address
        room_name = "Standard Room"
        room_type = "Standard"
        capacity = 2
        area_sqm = 28
        price_per_night = 900000
        amenities = "Wi-Fi"
        image_url = "/css/room-placeholder.svg"
        source = "OpenStreetMap ODbL"
    }
}
$rows | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $OutFile
Write-Host "Transformed OSM data to $OutFile"
