/*jshint globalstrict:true */
/*global angular:true */
'use strict';

angular.module('photon', [
    'photon.controllers', 'ngSanitize', "leaflet-directive"]).
    config(['$routeProvider', function($routeProvider) {
        $routeProvider
            .when('/search/', {templateUrl: 'search.html', controller: 'SearchCtrl'})
            .otherwise({redirectTo: '/search/'});
    }]);