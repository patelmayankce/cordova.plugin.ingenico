var exec = require('cordova/exec');

exports.connect = function (ip_address, port, amount, invoiceNo, success, error) {
    exec(success, error, 'cordovaPluginIngenico', 'connect', [ip_address, port, amount, invoiceNo]);
};

exports.void = function (ip_address, port, ref_no, success, error) {
    exec(success, error, 'cordovaPluginIngenico', 'void', [ip_address, port, ref_no]);
};

exports.refund = function (ip_address, port, amount, invoiceNo, success, error) {
    exec(success, error, 'cordovaPluginIngenico', 'refund', [ip_address, port, amount, invoiceNo]);
};

exports.addTip = function (ip_address, port, invoiceNo, success, error) {
    exec(success, error, 'cordovaPluginIngenico', 'addTip', [ip_address, port, invoiceNo]);
};