var exec = require('cordova/exec');

exports.connect = function (ip_address, port, amount,success, error) {
    exec(success, error, 'cordovaPluginIngenico', 'connect', [ip_address, port, amount]);
};
