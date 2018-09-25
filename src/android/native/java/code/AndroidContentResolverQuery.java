package io.silverstreet.content_resolver.query;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.*;
import android.text.TextUtils;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AndroidContentResolverQuery extends CordovaPlugin {

  private Thread contactsWorker;
  private List<String> enumerateIDs = new ArrayList<>();

  boolean IS_INT = false;
  static String TAG = "CONTACTS_ENUMERATE";

  private static final Map<String, String> dbMap = new HashMap<String, String>();

  static {
    dbMap.put("id", ContactsContract.Data.CONTACT_ID);
    dbMap.put("displayName", ContactsContract.Contacts.DISPLAY_NAME);
    dbMap.put("name", CommonDataKinds.StructuredName.DISPLAY_NAME);
    dbMap.put("name.formatted", CommonDataKinds.StructuredName.DISPLAY_NAME);
    dbMap.put("name.familyName", CommonDataKinds.StructuredName.FAMILY_NAME);
    dbMap.put("name.givenName", CommonDataKinds.StructuredName.GIVEN_NAME);
    dbMap.put("name.middleName", CommonDataKinds.StructuredName.MIDDLE_NAME);
    dbMap.put("name.honorificPrefix", CommonDataKinds.StructuredName.PREFIX);
    dbMap.put("name.honorificSuffix", CommonDataKinds.StructuredName.SUFFIX);
    dbMap.put("nickname", CommonDataKinds.Nickname.NAME);
    dbMap.put("phoneNumbers", CommonDataKinds.Phone.NUMBER);
    dbMap.put("phoneNumbers.value", CommonDataKinds.Phone.NUMBER);
    dbMap.put("emails", CommonDataKinds.Email.DATA);
    dbMap.put("emails.value", CommonDataKinds.Email.DATA);
    dbMap.put("addresses", CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS);
    dbMap.put("addresses.formatted", CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS);
    dbMap.put("addresses.streetAddress", CommonDataKinds.StructuredPostal.STREET);
    dbMap.put("addresses.locality", CommonDataKinds.StructuredPostal.CITY);
    dbMap.put("addresses.region", CommonDataKinds.StructuredPostal.REGION);
    dbMap.put("addresses.postalCode", CommonDataKinds.StructuredPostal.POSTCODE);
    dbMap.put("addresses.country", CommonDataKinds.StructuredPostal.COUNTRY);
    dbMap.put("ims", CommonDataKinds.Im.DATA);
    dbMap.put("ims.value", CommonDataKinds.Im.DATA);
    dbMap.put("organizations", CommonDataKinds.Organization.COMPANY);
    dbMap.put("organizations.name", CommonDataKinds.Organization.COMPANY);
    dbMap.put("organizations.department", CommonDataKinds.Organization.DEPARTMENT);
    dbMap.put("organizations.title", CommonDataKinds.Organization.TITLE);
    dbMap.put("birthday", CommonDataKinds.Event.CONTENT_ITEM_TYPE);
    dbMap.put("note", CommonDataKinds.Note.NOTE);
    dbMap.put("photos.value", CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
    //dbMap.put("categories.value", null);
    dbMap.put("urls", CommonDataKinds.Website.URL);
    dbMap.put("urls.value", CommonDataKinds.Website.URL);
  }

  public AndroidContentResolverQuery() {
    
  }

  private String[] getStringArray(JSONArray jsonArray) {
    if (jsonArray == null)
      return null;
    String[] responseArray = new String[jsonArray.length()];
    int length = jsonArray.length();
    for (int i = 0; i < length; i++)
      responseArray[i] = jsonArray.optString(i);
    return responseArray;
  }

  private String buildOptionsString(String selection) {
    String[] optStringArr = selection.split(" ");
    String _selectionKey = optStringArr[0];
    for (Map.Entry<String, String> constants : dbMap.entrySet()) {
      if (_selectionKey.equals(constants.getKey())) {
        optStringArr[0] =  _selectionKey.replace(constants.getKey(), constants.getValue());
      }
    }
    return TextUtils.join(" ", optStringArr);
  }

  private void onComplete(CallbackContext cbContext) {
      cordova.getActivity().runOnUiThread(() -> {
          final PluginResult result = new PluginResult(PluginResult.Status.OK, PluginResult.MESSAGE_TYPE_NULL);
          cbContext.sendPluginResult(result);
      });
  }

  private void getContactList(CallbackContext cbContext, JSONArray args) {

    String enumerateID = null, uri = null, selection = null, sortOrder = null;
    String[] projection = null, selectionArgs = null;

    JSONObject contact = new JSONObject();

    JSONArray organizations = new JSONArray();
    JSONArray addresses = new JSONArray();
    JSONArray phones = new JSONArray();
    JSONArray emails = new JSONArray();
    JSONArray ims = new JSONArray();
    JSONArray websites = new JSONArray();
    JSONArray photos = new JSONArray();

    JSONArray options = new JSONArray();

    try {
      enumerateID = args.getString(0);
      uri = args.getString(1);
      projection = getStringArray(args.getJSONArray(2));
      selection = buildOptionsString(args.getString(3));
      selectionArgs = getStringArray(args.getJSONArray(4));
      sortOrder = buildOptionsString(args.getString(5));
      options = args.getJSONArray(2);
    } catch (JSONException ex) {
      Log.e(TAG, ex.getLocalizedMessage());
    }

    ContentResolver cr = cordova.getActivity().getContentResolver();
    Cursor cur;
      // projections are not column names/actual columns
    cur = cr.query(Contacts.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            sortOrder
    );

    HashMap<String, Boolean> populate = buildPopulationSet(options);

    // Determine which columns we should be fetching.
    HashSet<String> columnsToFetch = new HashSet<String>();
    columnsToFetch.add(ContactsContract.Data.CONTACT_ID);
    columnsToFetch.add(ContactsContract.Data.RAW_CONTACT_ID);
    columnsToFetch.add(ContactsContract.Data.MIMETYPE);

    if (isRequired("displayName", populate)) {
      columnsToFetch.add(CommonDataKinds.StructuredName.DISPLAY_NAME);
    }
    if (isRequired("name", populate)) {
      columnsToFetch.add(CommonDataKinds.StructuredName.FAMILY_NAME);
      columnsToFetch.add(CommonDataKinds.StructuredName.GIVEN_NAME);
      columnsToFetch.add(CommonDataKinds.StructuredName.MIDDLE_NAME);
      columnsToFetch.add(CommonDataKinds.StructuredName.PREFIX);
      columnsToFetch.add(CommonDataKinds.StructuredName.SUFFIX);
    }
    if (isRequired("phoneNumbers", populate)) {
      columnsToFetch.add(CommonDataKinds.Phone._ID);
      columnsToFetch.add(CommonDataKinds.Phone.NUMBER);
      columnsToFetch.add(CommonDataKinds.Phone.TYPE);
      columnsToFetch.add(CommonDataKinds.Phone.LABEL);
    }
    if (isRequired("emails", populate)) {
      columnsToFetch.add(CommonDataKinds.Email._ID);
      columnsToFetch.add(CommonDataKinds.Email.DATA);
      columnsToFetch.add(CommonDataKinds.Email.TYPE);
      columnsToFetch.add(CommonDataKinds.Email.LABEL);
    }
    if (isRequired("addresses", populate)) {
      columnsToFetch.add(CommonDataKinds.StructuredPostal._ID);
      columnsToFetch.add(CommonDataKinds.Organization.TYPE);
      columnsToFetch.add(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS);
      columnsToFetch.add(CommonDataKinds.StructuredPostal.STREET);
      columnsToFetch.add(CommonDataKinds.StructuredPostal.CITY);
      columnsToFetch.add(CommonDataKinds.StructuredPostal.REGION);
      columnsToFetch.add(CommonDataKinds.StructuredPostal.POSTCODE);
      columnsToFetch.add(CommonDataKinds.StructuredPostal.COUNTRY);
      columnsToFetch.add(CommonDataKinds.StructuredPostal.LABEL);
    }
    if (isRequired("organizations", populate)) {
      columnsToFetch.add(CommonDataKinds.Organization._ID);
      columnsToFetch.add(CommonDataKinds.Organization.TYPE);
      columnsToFetch.add(CommonDataKinds.Organization.DEPARTMENT);
      columnsToFetch.add(CommonDataKinds.Organization.COMPANY);
      columnsToFetch.add(CommonDataKinds.Organization.TITLE);
      columnsToFetch.add(CommonDataKinds.Organization.LABEL);
    }
    if (isRequired("ims", populate)) {
      columnsToFetch.add(CommonDataKinds.Im._ID);
      columnsToFetch.add(CommonDataKinds.Im.DATA);
      columnsToFetch.add(CommonDataKinds.Im.TYPE);
    }
    if (isRequired("note", populate)) {
      columnsToFetch.add(CommonDataKinds.Note.NOTE);
    }
    if (isRequired("nickname", populate)) {
      columnsToFetch.add(CommonDataKinds.Nickname.NAME);
    }
    if (isRequired("urls", populate)) {
      columnsToFetch.add(CommonDataKinds.Website._ID);
      columnsToFetch.add(CommonDataKinds.Website.URL);
      columnsToFetch.add(CommonDataKinds.Website.TYPE);
      columnsToFetch.add(CommonDataKinds.Website.LABEL);
    }
    if (isRequired("birthday", populate)) {
      columnsToFetch.add(CommonDataKinds.Event.START_DATE);
      columnsToFetch.add(CommonDataKinds.Event.TYPE);
    }
    if (isRequired("photos", populate)) {
      columnsToFetch.add(CommonDataKinds.Photo._ID);
    }
    if ((cur != null ? cur.getCount() : 0) > 0) {
      while (cur.moveToNext() && enumerateIDs.contains(enumerateID)) {
          JSONObject rcontact;
          try{
              rcontact = getContactById(
                  cur.getString(cur.getColumnIndex(Contacts._ID)),
                  options
              );
              if (rcontact != null) {
                  cordova.getActivity().runOnUiThread(() -> {
                      final PluginResult result = new PluginResult(PluginResult.Status.OK, rcontact);
                      result.setKeepCallback(true);
                      cbContext.sendPluginResult(result);
                  });
              }
          } catch (Exception ex) {
            Log.e(TAG, ex.getLocalizedMessage());
          }
      }
      onComplete(cbContext);
    } else {
        onComplete(cbContext);
    }
    if(cur!=null){
      cur.close();
    }

  }


    public JSONObject getContactById(String id, JSONArray desiredFields) throws JSONException {
        // Do the id query
        ContentResolver cr = cordova.getActivity().getContentResolver();
        Cursor c = cr.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                Data.CONTACT_ID + " = ? ",
                new String[] { id },
                ContactsContract.Data.CONTACT_ID + " ASC");

        HashMap<String, Boolean> populate = buildPopulationSet(desiredFields);
        JSONArray contacts = populateContactArray(1, populate, c);
        if (!c.isClosed()) {
            c.close();
        }

        if (contacts.length() == 1) {
            return contacts.getJSONObject(0);
        } else {
            return null;
        }
    }

  /**
   * Check to see if the data associated with the key is required to
   * be populated in the Contact object.
   * @param key
   * @param map created by running buildPopulationSet.
   * @return true if the key data is required
   */
  protected boolean isRequired(String key, HashMap<String,Boolean> map) {
    Boolean retVal = map.get(key);
    return (retVal == null) ? false : retVal.booleanValue();
  }


    private JSONArray populateContactArray(int limit,
                                           HashMap<String, Boolean> populate, Cursor c) {

        String contactId = "";
        String rawId = "";
        String oldContactId = "";
        boolean newContact = true;
        String mimetype = "";

        JSONArray contacts = new JSONArray();
        JSONObject contact = new JSONObject();
        JSONArray organizations = new JSONArray();
        JSONArray addresses = new JSONArray();
        JSONArray phones = new JSONArray();
        JSONArray emails = new JSONArray();
        JSONArray ims = new JSONArray();
        JSONArray websites = new JSONArray();
        JSONArray photos = new JSONArray();

        // Column indices
        int colContactId = c.getColumnIndex(ContactsContract.Data.CONTACT_ID);
        int colRawContactId = c.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
        int colMimetype = c.getColumnIndex(ContactsContract.Data.MIMETYPE);
        int colDisplayName = c.getColumnIndex(CommonDataKinds.StructuredName.DISPLAY_NAME);
        int colNote = c.getColumnIndex(CommonDataKinds.Note.NOTE);
        int colNickname = c.getColumnIndex(CommonDataKinds.Nickname.NAME);
        int colEventType = c.getColumnIndex(CommonDataKinds.Event.TYPE);

        if (c.getCount() > 0) {
            while (c.moveToNext() && (contacts.length() <= (limit - 1))) {
                try {
                    contactId = c.getString(colContactId);
                    rawId = c.getString(colRawContactId);

                    // If we are in the first row set the oldContactId
                    if (c.getPosition() == 0) {
                        oldContactId = contactId;
                    }

                    // When the contact ID changes we need to push the Contact object
                    // to the array of contacts and create new objects.
                    if (!oldContactId.equals(contactId)) {
                        // Populate the Contact object with it's arrays
                        // and push the contact into the contacts array
                        contacts.put(populateContact(contact, organizations, addresses, phones,
                                emails, ims, websites, photos));

                        // Clean up the objects
                        contact = new JSONObject();
                        organizations = new JSONArray();
                        addresses = new JSONArray();
                        phones = new JSONArray();
                        emails = new JSONArray();
                        ims = new JSONArray();
                        websites = new JSONArray();
                        photos = new JSONArray();

                        // Set newContact to true as we are starting to populate a new contact
                        newContact = true;
                    }

                    // When we detect a new contact set the ID and display name.
                    // These fields are available in every row in the result set returned.
                    if (newContact) {
                        newContact = false;
                        contact.put("id", contactId);
                        contact.put("rawId", rawId);
                    }

                    // Grab the mimetype of the current row as it will be used in a lot of comparisons
                    mimetype = c.getString(colMimetype);

                    if (mimetype.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE) && isRequired("displayName", populate)) {
                        contact.put("displayName", c.getString(colDisplayName));
                    }

                    if (mimetype.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            && isRequired("name", populate)) {
                        contact.put("name", nameQuery(c));
                    }
                    else if (mimetype.equals(CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            && isRequired("phoneNumbers", populate)) {
                        phones.put(phoneQuery(c));
                    }
                    else if (mimetype.equals(CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            && isRequired("emails", populate)) {
                        emails.put(emailQuery(c));
                    }
                    else if (mimetype.equals(CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                            && isRequired("addresses", populate)) {
                        addresses.put(addressQuery(c));
                    }
                    else if (mimetype.equals(CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                            && isRequired("organizations", populate)) {
                        organizations.put(organizationQuery(c));
                    }
                    else if (mimetype.equals(CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                            && isRequired("ims", populate)) {
                        ims.put(imQuery(c));
                    }
                    else if (mimetype.equals(CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                            && isRequired("note", populate)) {
                        contact.put("note", c.getString(colNote));
                    }
                    else if (mimetype.equals(CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                            && isRequired("nickname", populate)) {
                        contact.put("nickname", c.getString(colNickname));
                    }
                    else if (mimetype.equals(CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                            && isRequired("urls", populate)) {
                        websites.put(websiteQuery(c));
                    }
                    else if (mimetype.equals(CommonDataKinds.Event.CONTENT_ITEM_TYPE)) {
                        if (isRequired("birthday", populate) &&
                                CommonDataKinds.Event.TYPE_BIRTHDAY == c.getInt(colEventType)) {

                            Date birthday = getBirthday(c);
                            if (birthday != null) {
                                contact.put("birthday", birthday.getTime());
                            }
                        }
                    }
                    else if (mimetype.equals(CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            && isRequired("photos", populate)) {
                        JSONObject photo = photoQuery(c, contactId);
                        if (photo != null) {
                            photos.put(photo);
                        }
                    }
                } catch (JSONException e) {
                    LOG.e(TAG, e.getMessage(), e);
                }

                // Set the old contact ID
                oldContactId = contactId;

            }

            // Push the last contact into the contacts array
            if (contacts.length() < limit) {
                contacts.put(populateContact(contact, organizations, addresses, phones,
                        emails, ims, websites, photos));
            }
        }
        c.close();
        return contacts;
    }

  private JSONObject populateContact(JSONObject contact, JSONArray organizations,
                                     JSONArray addresses, JSONArray phones, JSONArray emails,
                                     JSONArray ims, JSONArray websites, JSONArray photos) {
    try {
      // Only return the array if it has at least one entry
      if (organizations.length() > 0) {
        contact.put("organizations", organizations);
      } else {
        contact.put("organizations", JSONObject.NULL);
      }
      if (addresses.length() > 0) {
        contact.put("addresses", addresses);
      } else {
          contact.put("addresses", JSONObject.NULL);
      }
      if (phones.length() > 0) {
        contact.put("phoneNumbers", phones);
      } else {
          contact.put("phoneNumbers", JSONObject.NULL);
      }
      if (emails.length() > 0) {
        contact.put("emails", emails);
      } else {
        contact.put("emails", JSONObject.NULL);
      }
      if (ims.length() > 0) {
        contact.put("ims", ims);
      } else {
          contact.put("ims", JSONObject.NULL);
      }
      if (websites.length() > 0) {
        contact.put("urls", websites);
      } else {
          contact.put("urls", JSONObject.NULL);
      }
      if (photos.length() > 0) {
        contact.put("photos", photos);
      } else {
          contact.put("photos", JSONObject.NULL);
      }
    } catch (JSONException e) {
      LOG.e(TAG, e.getMessage(), e);
    }

    return contact;
  }

    private Date getBirthday(Cursor c) {

        try {
            int colBirthday = c.getColumnIndexOrThrow(CommonDataKinds.Event.START_DATE);
            return Date.valueOf(c.getString(colBirthday));
        } catch (IllegalArgumentException e) {
            LOG.e(TAG, "Failed to get birthday for contact from cursor", e);
            return null;
        }
    }

    private JSONObject websiteQuery(Cursor cursor) {
        JSONObject website = new JSONObject();
        try {
            int typeCode = cursor.getInt(cursor.getColumnIndexOrThrow(CommonDataKinds.Website.TYPE));
            String typeLabel = cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Website.LABEL));
            String type = (typeCode == CommonDataKinds.Website.TYPE_CUSTOM) ? typeLabel : getContactType(typeCode);
            website.put("id", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Website._ID)));
            website.put("pref", false); // Android does not store pref attribute
            website.put("value", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Website.URL)));
            website.put("type", type);
        } catch (JSONException e) {
            LOG.e(TAG, e.getMessage(), e);
        }
        return website;
    }

    private JSONObject imQuery(Cursor cursor) {
        JSONObject im = new JSONObject();
        try {
            im.put("id", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Im._ID)));
            im.put("pref", false); // Android does not store pref attribute
            im.put("value", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Im.DATA)));
            String protocol = cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Im.PROTOCOL));
            if (!isInteger(protocol) || Integer.parseInt(protocol) == CommonDataKinds.Im.PROTOCOL_CUSTOM) {
                // the protocol is custom, get its name and put it into JSON
                protocol = cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Im.CUSTOM_PROTOCOL));
                im.put("type", protocol);
            } else {
                // (the protocol is one of the standard ones) look up its type and then put it into JSON
                im.put("type", getImType(Integer.parseInt(protocol)));
            }
        } catch (JSONException e) {
            LOG.e(TAG, e.getMessage(), e);
        }
        return im;
    }

    private static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        }
        catch(Exception e) {
            return false;
        }
    }

    private int getImType(String string) {
        int type = CommonDataKinds.Im.PROTOCOL_CUSTOM;
        if (string != null) {
            String lowerType = string.toLowerCase(Locale.getDefault());

            if ("aim".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_AIM;
            }
            else if ("google talk".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK;
            }
            else if ("icq".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_ICQ;
            }
            else if ("jabber".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_JABBER;
            }
            else if ("msn".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_MSN;
            }
            else if ("netmeeting".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_NETMEETING;
            }
            else if ("qq".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_QQ;
            }
            else if ("skype".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_SKYPE;
            }
            else if ("yahoo".equals(lowerType)) {
                return CommonDataKinds.Im.PROTOCOL_YAHOO;
            }
        }
        return type;
    }

    @SuppressWarnings("unused")
    private String getImType(int type) {
        String stringType;
        switch (type) {
            case CommonDataKinds.Im.PROTOCOL_AIM:
                stringType = "AIM";
                break;
            case CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK:
                stringType = "Google Talk";
                break;
            case CommonDataKinds.Im.PROTOCOL_ICQ:
                stringType = "ICQ";
                break;
            case CommonDataKinds.Im.PROTOCOL_JABBER:
                stringType = "Jabber";
                break;
            case CommonDataKinds.Im.PROTOCOL_MSN:
                stringType = "MSN";
                break;
            case CommonDataKinds.Im.PROTOCOL_NETMEETING:
                stringType = "NetMeeting";
                break;
            case CommonDataKinds.Im.PROTOCOL_QQ:
                stringType = "QQ";
                break;
            case CommonDataKinds.Im.PROTOCOL_SKYPE:
                stringType = "Skype";
                break;
            case CommonDataKinds.Im.PROTOCOL_YAHOO:
                stringType = "Yahoo";
                break;
            default:
                stringType = "custom";
                break;
        }
        return stringType;
    }

    private JSONObject addressQuery(Cursor cursor) {
        JSONObject address = new JSONObject();
        try {
            int typeCode = cursor.getInt(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.TYPE));
            String typeLabel = cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.LABEL));
            String type = (typeCode == CommonDataKinds.StructuredPostal.TYPE_CUSTOM) ? typeLabel : getAddressType(typeCode);
            address.put("id", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal._ID)));
            address.put("pref", false); // Android does not store pref attribute
            address.put("type", type);
            address.put("formatted", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)));
            address.put("streetAddress", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.STREET)));
            address.put("locality", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.CITY)));
            address.put("region", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.REGION)));
            address.put("postalCode", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.POSTCODE)));
            address.put("country", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.COUNTRY)));
        } catch (JSONException e) {
            LOG.e(TAG, e.getMessage(), e);
        }
        return address;
    }

    private JSONObject organizationQuery(Cursor cursor) {
        JSONObject organization = new JSONObject();
        try {
            int typeCode = cursor.getInt(cursor.getColumnIndexOrThrow(CommonDataKinds.Organization.TYPE));
            String typeLabel = cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Organization.LABEL));
            String type = (typeCode == CommonDataKinds.Organization.TYPE_CUSTOM) ? typeLabel : getOrgType(typeCode);
            organization.put("id", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Organization._ID)));
            organization.put("pref", false); // Android does not store pref attribute
            organization.put("type", type);
            organization.put("department", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Organization.DEPARTMENT)));
            organization.put("name", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Organization.COMPANY)));
            organization.put("title", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Organization.TITLE)));
        } catch (JSONException e) {
            LOG.e(TAG, e.getMessage(), e);
        }
        return organization;
    }

    private String getOrgType(int type) {
        String stringType;
        switch (type) {
            case CommonDataKinds.Organization.TYPE_CUSTOM:
                stringType = "custom";
                break;
            case CommonDataKinds.Organization.TYPE_WORK:
                stringType = "work";
                break;
            case CommonDataKinds.Organization.TYPE_OTHER:
            default:
                stringType = "other";
                break;
        }
        return stringType;
    }

    private String getAddressType(int type) {
        String stringType;
        switch (type) {
            case CommonDataKinds.StructuredPostal.TYPE_HOME:
                stringType = "home";
                break;
            case CommonDataKinds.StructuredPostal.TYPE_WORK:
                stringType = "work";
                break;
            case CommonDataKinds.StructuredPostal.TYPE_OTHER:
            default:
                stringType = "other";
                break;
        }
        return stringType;
    }

    private JSONObject photoQuery(Cursor cursor, String contactId) {
        JSONObject photo = new JSONObject();
        Cursor photoCursor = null;
        try {
            photo.put("id", cursor.getString(cursor.getColumnIndexOrThrow(CommonDataKinds.Photo._ID)));
            photo.put("pref", false);
            photo.put("type", "url");
            Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, (Long.valueOf(contactId)));
            Uri photoUri = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
            photo.put("value", photoUri.toString());

            // Query photo existance
            photoCursor = cordova.getActivity().getContentResolver().query(photoUri, new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
            if (photoCursor == null) return null;
            if (!photoCursor.moveToFirst()) {
                photoCursor.close();
                return null;
            }
            photoCursor.close();
        } catch (JSONException e) {
            LOG.e(TAG, e.getMessage(), e);
        } catch (SQLiteException e) {
            LOG.e(TAG, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOG.e(TAG, e.getMessage(), e);
        } finally {
            if(photoCursor != null && !photoCursor.isClosed()) {
                photoCursor.close();
            }
        }
        return photo;
    }

  private HashMap<String, Boolean> buildPopulationSet(JSONArray options) {
    HashMap<String, Boolean> map = new HashMap<String, Boolean>();

    String key;
    try {
      JSONArray desiredFields = null;
      if (options!=null) {
        desiredFields = options;
      }
      if (desiredFields == null || desiredFields.length() == 0) {
        map.put("displayName", true);
        map.put("name", true);
        map.put("nickname", true);
        map.put("phoneNumbers", true);
        map.put("emails", true);
        map.put("addresses", true);
        map.put("ims", true);
        map.put("organizations", true);
        map.put("birthday", true);
        map.put("note", true);
        map.put("urls", true);
        map.put("photos", true);
        map.put("categories", true);
      } else {
        for (int i = 0; i < desiredFields.length(); i++) {
          key = desiredFields.getString(i);
          if (key.startsWith("displayName")) {
            map.put("displayName", true);
          } else if (key.startsWith("name")) {
            map.put("displayName", true);
            map.put("name", true);
          } else if (key.startsWith("nickname")) {
            map.put("nickname", true);
          } else if (key.startsWith("phoneNumbers")) {
            map.put("phoneNumbers", true);
          } else if (key.startsWith("emails")) {
            map.put("emails", true);
          } else if (key.startsWith("addresses")) {
            map.put("addresses", true);
          } else if (key.startsWith("ims")) {
            map.put("ims", true);
          } else if (key.startsWith("organizations")) {
            map.put("organizations", true);
          } else if (key.startsWith("birthday")) {
            map.put("birthday", true);
          } else if (key.startsWith("note")) {
            map.put("note", true);
          } else if (key.startsWith("urls")) {
            map.put("urls", true);
          } else if (key.startsWith("photos")) {
            map.put("photos", true);
          } else if (key.startsWith("categories")) {
            map.put("categories", true);
          }
        }
      }
    } catch (JSONException e) {
      LOG.e(TAG, e.getMessage(), e);
    }
    return map;
  }

  /**
   * getPhoneType converts an Android phone type into a string
   * @param type
   * @return phone type as string.
   */
    private String getContactType(int type) {
    String stringType;
    switch (type) {
      case ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM:
        stringType = "custom";
        break;
      case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
        stringType = "home";
        break;
      case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
        stringType = "work";
        break;
      case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
        stringType = "mobile";
        break;
      case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
      default:
        stringType = "other";
        break;
    }
    return stringType;
  }

  /**
   * Create a ContactField JSONObject
   * @param cursor the current database row
   * @return a JSONObject representing a ContactField
   */
  private JSONObject emailQuery(Cursor cursor) {
    JSONObject email = new JSONObject();
    try {
      int typeCode = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE));
      String typeLabel = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LABEL));
      String type = (typeCode == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) ? typeLabel : getContactType(typeCode);
      email.put("id", cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email._ID)));
      email.put("pref", false); // Android does not store pref attribute
      email.put("value", cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA)));
      email.put("type", type);
    } catch (JSONException e) {
      LOG.e(TAG, e.getMessage(), e);
    }
    return email;
  }

  /**
   * getPhoneType converts an Android phone type into a string
   * @param type
   * @return phone type as string.
   */
  private String getPhoneType(int type) {
    String stringType;

    switch (type) {
      case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
        stringType = "custom";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
        stringType = "home fax";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
        stringType = "work fax";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
        stringType = "home";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
        stringType = "mobile";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
        stringType = "pager";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
        stringType = "work";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK:
        stringType = "callback";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_CAR:
        stringType = "car";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
        stringType = "company main";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX:
        stringType = "other fax";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_RADIO:
        stringType = "radio";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_TELEX:
        stringType = "telex";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD:
        stringType = "tty tdd";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
        stringType = "work mobile";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER:
        stringType = "work pager";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT:
        stringType = "assistant";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_MMS:
        stringType = "mms";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_ISDN:
        stringType = "isdn";
        break;
      case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
      default:
        stringType = "other";
        break;
    }
    return stringType;
  }

  /**
   * Create a ContactField JSONObject
   * @param cursor the current database row
   * @return a JSONObject representing a ContactField
   */
  private JSONObject phoneQuery(Cursor cursor) {
    JSONObject phoneNumber = new JSONObject();
    try {
      int typeCode = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
      String typeLabel = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL));
      String type = (typeCode == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) ? typeLabel : getPhoneType(typeCode);
      phoneNumber.put("id", cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)));
      phoneNumber.put("pref", false); // Android does not store pref attribute
      phoneNumber.put("value", cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
      phoneNumber.put("type",type);
    } catch (JSONException e) {
      LOG.e(TAG, e.getMessage(), e);
    }
    return phoneNumber;
  }

  /**
   * Create a ContactName JSONObject
   * @param cursor the current database row
   * @return a JSONObject representing a ContactName
   */
  private JSONObject nameQuery(Cursor cursor) {
    JSONObject contactName = new JSONObject();
    try {
      String familyName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
      String givenName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
      String middleName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
      String honorificPrefix = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PREFIX));
      String honorificSuffix = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.SUFFIX));

      // Create the formatted name
      StringBuffer formatted = new StringBuffer("");
      if (!TextUtils.isEmpty(honorificPrefix)) {
        formatted.append(honorificPrefix + " ");
      }
      if (!TextUtils.isEmpty(givenName)) {
        formatted.append(givenName + " ");
      }
      if (!TextUtils.isEmpty(middleName)) {
        formatted.append(middleName + " ");
      }
      if (!TextUtils.isEmpty(familyName)) {
        formatted.append(familyName);
      }
      if (!TextUtils.isEmpty(honorificSuffix)) {
        formatted.append(" " + honorificSuffix);
      }
      if (TextUtils.isEmpty(formatted)) {
        formatted = null;
      }

      contactName.put("familyName", familyName);
      contactName.put("givenName", givenName);
      contactName.put("middleName", middleName);
      contactName.put("honorificPrefix", honorificPrefix);
      contactName.put("honorificSuffix", honorificSuffix);
      contactName.put("formatted", formatted);
    } catch (JSONException e) {
      LOG.e(TAG, e.getMessage(), e);
    }
    return contactName;
  }

  private String getEnumerateInstanceID(JSONArray args) {
      String enumerateInstanceID = "";
      try {
          if (!args.getString(0).equals(""))
              enumerateInstanceID = args.getString(0);
      } catch (JSONException ex) {
          Log.e(TAG, ex.getLocalizedMessage());
      }
      return enumerateInstanceID;
  }

  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
    switch (action) {
        case "start":
            enumerateIDs.add(getEnumerateInstanceID(args));
            this.contactsWorker = new Thread(() -> this.getContactList(callbackContext, args));
            cordova.getThreadPool().submit(this.contactsWorker);
            break;
        case "stop":
            if (enumerateIDs.contains(getEnumerateInstanceID(args)))
                enumerateIDs.remove(getEnumerateInstanceID(args));
        default:
                return false;
    }
    return true;
  }
}
