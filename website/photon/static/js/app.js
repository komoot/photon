var startPoint = [52.3879, 13.0582];
var map = L.map('map', {scrollWheelZoom: false, photonControl: true}).setView(startPoint, 12),
    tilelayer = L.tileLayer('http://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {maxZoom: 20, attribution: 'Data \u00a9 <a href="http://www.openstreetmap.org/copyright"> OpenStreetMap Contributors </a> Tiles \u00a9 HOT'}).addTo(map);
