var startPoint = [52.3879, 13.0582],
    searchPoints = L.geoJson(null, {
        onEachFeature: function (feature, layer) {
            layer.bindPopup(feature.properties.name);
        }
    });
function showSearchPoints (geojson) {
    searchPoints.clearLayers();
    searchPoints.addData(geojson);
}
var map = L.map('map', {scrollWheelZoom: false, photonControl: true, photonControlOptions: {resultsHandler: showSearchPoints}});
map.setView(startPoint, 12);
searchPoints.addTo(map);
var tilelayer = L.tileLayer('http://{s}.tile.komoot.de/komoot/{z}/{x}/{y}.png', {maxZoom: 20, attribution: 'Data \u00a9 <a href="http://www.openstreetmap.org/copyright"> OpenStreetMap Contributors </a> Tiles \u00a9 Komoot'}).addTo(map);
