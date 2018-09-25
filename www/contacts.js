var iOSContacts = require('../src/ios/web/js/code/index');
var androidContacts = require('../src/android/web/js/code/index');

module.exports = function(context) {
  var platformContactsPluginModule;
  var platforms = context.requireCordovaModule('cordova-lib/src/cordova/util').listPlatforms(context.opts.projectRoot);

  if (platforms.indexOf('android') >= 0) {
    platformContactsPluginModule = androidContacts;
  } else if (platforms.indexOf('ios') >= 0) {
    platformContactsPluginModule = iOSContacts;
  }
  return platformContactsPluginModule;
};