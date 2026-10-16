var searchPoints = L.geoJson(null, {
        onEachFeature: function (feature, layer) {
            layer.bindPopup(document.createTextNode(feature.properties.name || ''));
        }
    });
function showSearchPoints (geojson) {
    searchPoints.clearLayers();
    searchPoints.addData(geojson);
}
var map = L.map('map', {
    scrollWheelZoom: false,
    zoomControl: false,
    photonControl: true,
    photonControlOptions: {
        resultsHandler: showSearchPoints,
        placeholder: 'Try me…',
        feedbackEmail: null,
        position: 'topleft', url: API_URL}});

map.setView(CENTER, 12);
searchPoints.addTo(map);
var tilelayer = L.tileLayer(TILELAYER, {maxZoom: MAXZOOM, attribution: 'Data \u00a9 <a href="https://www.openstreetmap.org/copyright"> OpenStreetMap Contributors </a> '}).addTo(map);
var zoomControl = new L.Control.Zoom({position: 'topright'}).addTo(map);
