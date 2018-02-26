define(["require", "exports", "angular", "../module-name", "pascalprecht.translate"], function (require, exports, angular, module_name_1) {
    "use strict";
    var _this = this;
    Object.defineProperty(exports, "__esModule", { value: true });
    angular.module(module_name_1.moduleName).directive("kyloTimer", ['$interval', '$filter', function () {
            return {
                restrict: "EA",
                scope: {
                    startTime: "=",
                    refreshTime: "@",
                    truncatedFormat: '=?',
                    addAgoSuffix: '=?'
                },
                link: function ($scope, element, attrs) {
                    $scope.truncatedFormat = angular.isDefined($scope.truncatedFormat) ? $scope.truncatedFormat : false;
                    $scope.addAgoSuffix = angular.isDefined($scope.addAgoSuffix) ? $scope.addAgoSuffix : false;
                    $scope.time = $scope.startTime;
                    $scope.previousDisplayStr = '';
                    $scope.$watch('startTime', function (newVal, oldVal) {
                        $scope.time = $scope.startTime;
                        _this.format();
                    });
                    _this.format();
                    var seconds = 0;
                    var minutes = 0;
                    var hours = 0;
                    var days = 0;
                    var months = 0;
                    var years = 0;
                    if ($scope.refreshTime == undefined) {
                        $scope.refreshTime = 1000;
                    }
                    function update() {
                        $scope.time += $scope.refreshTime;
                        //format it
                        this.format();
                    }
                    function format() {
                        var ms = $scope.time;
                        var displayStr = this.DateTimeUtils(this.$filter('translate')).formatMillisAsText(ms, $scope.truncatedFormat, false);
                        if ($scope.addAgoSuffix) {
                            displayStr += " ago";
                        }
                        if ($scope.previousDisplayStr == '' || $scope.previousDisplayStr != displayStr) {
                            element.html(displayStr);
                            element.attr('title', displayStr);
                        }
                        $scope.previousDisplayStr = displayStr;
                    }
                    var interval = _this.$interval(update, $scope.refreshTime);
                    var clearInterval = function () {
                        _this.$interval.cancel(interval);
                        interval = null;
                    };
                    $scope.$on('$destroy', function () {
                        clearInterval();
                    });
                }
            };
        }
    ]);
});
//# sourceMappingURL=kylo-timer.js.map