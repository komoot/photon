/*jshint globalstrict:true */
/*global angular:true */
/*global _:true */
'use strict';

angular.module('photon.controllers', [])
    .controller('SearchCtrl', function($scope, $location, $http) {
    $scope.hits = [];
    $scope.searchString = "";
    $scope.center = {
        lat: 48.8,
        lng: 2.7,
        zoom: 4
    };
    $scope.markers = [];
    $scope.tiles = {url: "http://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png"};

    var getLatLng = function (hit) {
        var latlng = hit.coordinate.split(',');
        latlng = {
            lat: parseFloat(latlng[0], 10),
            lng: parseFloat(latlng[1], 10)
        };
        return latlng;
    };

    $scope.search = function () {
        $http.get('/search/?q=' + encodeURIComponent($scope.searchString), {cache: true}).success(function(data) {
            $scope.hits = data.docs;
            $scope.highlight = data.highlight;
            $scope.markers = _.map($scope.hits, function (hit, key, list) {
                return getLatLng(hit);
            });
        });
    };

    $scope.mapCenter = function (hit) {
        var latlng = getLatLng(hit);
        $scope.center = {
            lat: latlng.lat,
            lng: latlng.lng,
            zoom: 15
        };
        $scope.markers = [latlng];
        $scope.hits = [];
        $scope.searchString = "";
    };

    $scope.getTitle = function (hit) {
        var title = [];
        var hl = $scope.highlight[hit.id];
        if (hl.name) {
            title.push(hl.name.join(' '));
        } else {
            title.push(hit.name);
        }
        if (hl.street) {
            title.push(hl.street.join(' '));
        } else {
            title.push(hit.street);
        }
        title.push(hit.country);
        return title.join(', ');
    };

});