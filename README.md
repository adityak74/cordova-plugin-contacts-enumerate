# @silverstreet/cordova-plugin_apple_cn-contact-store_enumerate-contacts

Cordova plugin implementing the Apple CNContactStore enumerateContacts method.

## Installation

Add the plugin to your cordova project:

```
cordova plugin add @silverstreet/cordova-plugin_apple_cn-contact-store_enumerate-contacts
```

## Usage

```
import {
  APPLE_CN_CONTACT_KEY_EMAIL_ADDRESSES,
  APPLE_CN_CONTACT_KEY_FAMILY_NAME,
  APPLE_CN_CONTACT_KEY_GIVEN_NAME,
  APPLE_CN_CONTACT_KEY_PHONE_NUMBERS,
  APPLE_CN_CONTACT_KEY_THUMBNAIL_IMAGE_DATA,
  APPLE_CN_CONTACT_SORT_ORDER_FAMILY_NAME,
  appleCNContactPredicateForContactsMatchingName,
  appleCNContactStoreEnumerateStart,
  appleCNContactStoreEnumerateStop,
} from '@silverstreet/cordova-plugin_apple_cn-contact-store_enumerate-contacts';

const id = // ...

const req = {
  keysToFetch: [
    APPLE_CN_CONTACT_KEY_EMAIL_ADDRESSES,
    APPLE_CN_CONTACT_KEY_FAMILY_NAME,
    APPLE_CN_CONTACT_KEY_GIVEN_NAME,
    APPLE_CN_CONTACT_KEY_PHONE_NUMBERS,
    APPLE_CN_CONTACT_KEY_THUMBNAIL_IMAGE_DATA,
  ],
  predicate: appleCNContactPredicateForContactsMatchingName('John'),
  sortOrder: APPLE_CN_CONTACT_SORT_ORDER_FAMILY_NAME,
};

appleCNContactStoreEnumerateStart(
  cordova,
  id,
  req,
  contact => console.log('next:', contact),
  err => console.error(err);
  () => console.log('complete'),
);

setTimeout(() => {
  appleCNContactStoreEnumerateStop(cordova, id);
}, 5000);
```
