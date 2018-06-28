import Contacts

enum pluginResultType {
    case error
    case ok
    case error_fetch_request
    case error_response
    case error_id
}

enum predicateType {
    case contactsInContainerWithIdentifier
    case contactsInGroupWithIdentifier
    case contactsMatchingName
    case contactsWithIdentifiers
}

var pluginResponseMessages:[String : String] = [
    "PLUGIN_ERROR" : "Unable to get contacts",
    "PLUGIN_ERROR_ID" : "No request id found",
    "PLUGIN_OK": "OK",
    "PLUGIN_PARSE_ERROR": "Unable to parse arguments",
    "PLUGIN_RESPONSE_ERROR": "Invalid reponse format"
]

@objc(AppleCNContactStoreEnumerateContacts)
class AppleCNContactStoreEnumerateContacts: CDVPlugin {
  var ids: [String] = []

  var contactsWorkItem: DispatchWorkItem?
    
    private func getSerializedContact(contact: CNContact, req: NSObject) -> [String : Any] {
        var serializedContact: [String: Any] = [:]
        
        let keysToFetch = req.value(forKey: "keysToFetch") as! [String]
        
        // build the contact here
        for key in keysToFetch {
            
            switch key {
                case CNContactFamilyNameKey:
                    serializedContact[CNContactFamilyNameKey] = contact.familyName
                    break
                case CNContactGivenNameKey:
                    serializedContact[CNContactGivenNameKey] = contact.givenName
                    break
                case CNContactEmailAddressesKey:
                    serializedContact[CNContactEmailAddressesKey] = contact.emailAddresses.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": labeledValue.value
                        ]}
                    break
                case CNContactPhoneNumbersKey:
                    serializedContact[CNContactPhoneNumbersKey] = contact.phoneNumbers.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": labeledValue.value.stringValue
                        ]}
                    break
                case CNContactThumbnailImageDataKey:
                    serializedContact[CNContactThumbnailImageDataKey] = contact.thumbnailImageData?.base64EncodedString()
                    break
                case CNContactBirthdayKey:
                    serializedContact[CNContactBirthdayKey] = contact.birthday.flatMap({ (date) -> String? in
                        return "\(date.month ?? 1)/\(date.day ?? 1)/\(date.year ?? 1970)"
                    })
                    break
                case CNContactPostalAddressesKey:
                    serializedContact[CNContactPostalAddressesKey] = contact.postalAddresses.map { labeledValue in [
                            "identifier": labeledValue.identifier,
                            "label": labeledValue.label!,
                            "value": [
                                "city" : labeledValue.value.city,
                                "country" : labeledValue.value.country,
                                "isoCountryCode" : labeledValue.value.isoCountryCode,
                                "postalCode" : labeledValue.value.postalCode,
                                "state" : labeledValue.value.state,
                                "street" : labeledValue.value.street,
                                "subAdministrativeArea" : labeledValue.value.subAdministrativeArea,
                                "subLocality" : labeledValue.value.subLocality
                            ]
                        ]}
                    break
                case CNContactDepartmentNameKey:
                    serializedContact[CNContactDepartmentNameKey] = contact.departmentName
                    break
                case CNContactJobTitleKey:
                    serializedContact[CNContactJobTitleKey] = contact.jobTitle
                    break
                case CNContactMiddleNameKey:
                    serializedContact[CNContactMiddleNameKey] = contact.middleName
                    break
                case CNContactNamePrefixKey:
                    serializedContact[CNContactNamePrefixKey] = contact.namePrefix
                    break
                case CNContactNameSuffixKey:
                    serializedContact[CNContactNameSuffixKey] = contact.nameSuffix
                    break
                case CNContactNicknameKey:
                    serializedContact[CNContactNicknameKey] = contact.nickname
                    break
                case CNContactNonGregorianBirthdayKey:
                    serializedContact[CNContactNonGregorianBirthdayKey] = contact.nonGregorianBirthday
                    break
                case CNContactNoteKey:
                    serializedContact[CNContactNoteKey] = contact.note
                    break
                case CNContactOrganizationNameKey:
                    serializedContact[CNContactOrganizationNameKey] = contact.organizationName
                    break
                case CNContactPhoneticGivenNameKey:
                    serializedContact[CNContactPhoneticGivenNameKey] = contact.phoneticGivenName
                    break
                case CNContactPhoneticFamilyNameKey:
                    serializedContact[CNContactPhoneticFamilyNameKey] = contact.phoneticFamilyName
                    break
                case CNContactPhoneticMiddleNameKey:
                    serializedContact[CNContactPhoneticMiddleNameKey] = contact.phoneticMiddleName
                    break
                case CNContactPreviousFamilyNameKey:
                    serializedContact[CNContactPreviousFamilyNameKey] = contact.previousFamilyName
                    break
                case CNContactSocialProfilesKey:
                    serializedContact[CNContactSocialProfilesKey] = contact.socialProfiles.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": [
                            "service":  labeledValue.value.service,
                            "urlString" : labeledValue.value.urlString,
                            "userIdentifier" : labeledValue.value.userIdentifier,
                            "username" : labeledValue.value.username,
                            ]
                        ]}
                    break
                case CNContactUrlAddressesKey:
                    serializedContact[CNContactUrlAddressesKey] = contact.urlAddresses.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": labeledValue.value
                        ]
                    }
                    break
                
                default:
                    break
            }
        }
        
        return serializedContact
    }
    
    private func buildPluginResult(resultType : pluginResultType) -> CDVPluginResult {
        var tempPluginResult: CDVPluginResult
        
        switch resultType {
            case .error:
                tempPluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: pluginResponseMessages["PLUGIN_ERROR"])
            case .ok:
                tempPluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: pluginResponseMessages["PLUGIN_OK"])
            case .error_fetch_request:
                tempPluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: pluginResponseMessages["PLUGIN_PARSE_ERROR"])
            case .error_response:
                tempPluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: pluginResponseMessages["PLUGIN_RESPONSE_ERROR"])
            case .error_id:
                tempPluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: pluginResponseMessages["PLUGIN_ERROR_ID"])
        }
        return tempPluginResult
    }
  
    private func getPredicate(forPredicate: Any?) -> NSPredicate {
        let _predicate = forPredicate as! [Any]
        var predicate: NSPredicate!
        
        switch _predicate[0] as! Int {
            case predicateType.contactsInContainerWithIdentifier.hashValue:
                predicate = CNContact.predicateForContactsInContainer(withIdentifier: _predicate[1] as! String)
            case predicateType.contactsInGroupWithIdentifier.hashValue:
                predicate = CNContact.predicateForContactsInGroup(withIdentifier: _predicate[1] as! String)
            case predicateType.contactsMatchingName.hashValue:
                predicate = CNContact.predicateForContacts(matchingName: _predicate[1] as! String)
            case predicateType.contactsWithIdentifiers.hashValue:
                predicate = CNContact.predicateForContacts(withIdentifiers: _predicate[1] as! [String])
            default:
                predicate = nil
            
        }
        return predicate
    }
    
  @objc(start:)
  func start(command: CDVInvokedUrlCommand) {
    
    let store = CNContactStore()
    
    var respPluginResult: CDVPluginResult
    
    //default plugin result to err
    respPluginResult = buildPluginResult(resultType: .error)
    
    //grab the request ID
    guard let id = command.arguments[0] as? String else {
        
        respPluginResult = buildPluginResult(resultType: .error_id)
        
        self.commandDelegate!.send(
            respPluginResult,
            callbackId: command.callbackId
        )
        
        return
    }
    
    // grab the fetch reuqest keys
    guard let fetchRequest = command.arguments[1] as? NSObject else {
        
        respPluginResult = buildPluginResult(resultType: .error_fetch_request)
        
        self.commandDelegate!.send(
            respPluginResult,
            callbackId: command.callbackId
        )
        
        return
    }
    
    let request = CNContactFetchRequest(keysToFetch: fetchRequest.value(forKey: "keysToFetch") as! [CNKeyDescriptor])
//    request.predicate = getPredicate(forPredicate: fetchRequest.value(forKey: "predicate"))
    request.sortOrder = CNContactSortOrder(rawValue: fetchRequest.value(forKey: "sortOrder") as! Int)!
    
    contactsWorkItem = DispatchWorkItem {
        do {
            
            try store.enumerateContacts(with: request) {
                (contact, stop) in
                
                DispatchQueue.main.async {
                    
                    if((self.contactsWorkItem)!.isCancelled) {
                        stop.pointee = true
                    }
                    
                    respPluginResult = CDVPluginResult(
                        status: CDVCommandStatus_OK,
                        messageAs: self.getSerializedContact(contact: contact, req: fetchRequest)
                    )
                    respPluginResult.setKeepCallbackAs(true)
                    
                    self.commandDelegate!.send(
                        respPluginResult,
                        callbackId: command.callbackId
                    )
                    
                }
            }
        }
        catch {
            print("unable to fetch contacts")
        }
    }
    
    DispatchQueue.global(qos: .background).async(execute: contactsWorkItem!)

  }

  @objc(stop:)
  func stop(command: CDVInvokedUrlCommand) {
    DispatchQueue.main.async {
        self.contactsWorkItem?.cancel()
    }
    
  }
}
