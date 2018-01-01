var exec = require('cordova/exec');

exports.connect = function (ip_address, port, amount, success, error) {
    exec(success, error, 'cordovaPluginIngenico', 'connect', [ip_address, port, amount]);
};

exports.void = function (ip_address, port, ref_no, success, error) {
    exec(success, error, 'cordovaPluginIngenico', 'void', [ip_address, port, ref_no]);
};
