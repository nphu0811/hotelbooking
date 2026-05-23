param(
    [string]$OutFile = "data/raw/osm_vietnam_hotels.json"
)

$query = @"
[out:json][timeout:60];
area["ISO3166-1"="VN"][admin_level=2]->.searchArea;
(
  node["tourism"~"hotel|guest_house|hostel|motel"](area.searchArea);
  way["tourism"~"hotel|guest_house|hostel|motel"](area.searchArea);
  relation["tourism"~"hotel|guest_house|hostel|motel"](area.searchArea);
);
out center tags 200;
"@

New-Item -ItemType Directory -Force -Path (Split-Path $OutFile) | Out-Null
Invoke-WebRequest -Uri "https://overpass-api.de/api/interpreter" -Method Post -Body @{ data = $query } -OutFile $OutFile
Write-Host "Downloaded OSM/Overpass hotel data to $OutFile"
