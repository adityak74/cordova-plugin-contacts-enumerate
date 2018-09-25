import * as iOSContacts from '../src/ios/web/js/code/index';
import * as androidContacts from '../src/android/web/js/code/index';

const contactsInit = () => {
  if (iOSContacts || androidContacts) {
    return true;
  } else {
    return false;
  }
};

module.exports = contactsInit;