import Contacts

enum PluginResultType {
    case typeError
    case typeOk
    case typeErrorFetchRequest
    case typeErrorResponse
    case typeErrorId
}

enum PredicateType {
    case contactsInContainerWithIdentifier
    case contactsInGroupWithIdentifier
    case contactsMatchingName
    case contactsWithIdentifiers
}

var pluginResponseMessages: [String: String] = [
    "PLUGIN_ERROR": "Unable to get contacts",
    "PLUGIN_ERROR_ID": "No request id found",
    "PLUGIN_OK": "OK",
    "PLUGIN_PARSE_ERROR": "Unable to parse arguments",
    "PLUGIN_RESPONSE_ERROR": "Invalid reponse format"
]

@objc(AppleCNContactStoreEnumerateContacts)
class AppleCNContactStoreEnumerateContacts: CDVPlugin {
    var ids: [String] = []

    var contactsWorkItem: DispatchWorkItem?

    // swiftlint:disable:next cyclomatic_complexity function_body_length
    private func getSerializedContact(contact: CNContact, req: NSObject) -> [String: Any] {
        var serializedContact: [String: Any] = [:]
        let keysToFetch = req.value(forKey: "keysToFetch") as? [String]
        // build the contact here
        for key in keysToFetch! {

            switch key {
            case CNContactFamilyNameKey:
                serializedContact[CNContactFamilyNameKey] = contact.familyName
            case CNContactGivenNameKey:
                serializedContact[CNContactGivenNameKey] = contact.givenName
            case CNContactEmailAddressesKey:
                serializedContact[CNContactEmailAddressesKey] =
                    contact.emailAddresses.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": labeledValue.value
                        ]}
            case CNContactPhoneNumbersKey:
                serializedContact[CNContactPhoneNumbersKey] =
                    contact.phoneNumbers.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": labeledValue.value.stringValue
                        ]}
            case CNContactThumbnailImageDataKey:
                serializedContact[CNContactThumbnailImageDataKey] =
                    contact.thumbnailImageData?.base64EncodedString()
            case CNContactBirthdayKey:
                serializedContact[CNContactBirthdayKey] =
                    contact.birthday.flatMap({ (date) -> String? in
                        return "\(date.month ?? 1)/\(date.day ?? 1)/\(date.year ?? 1970)"
                    })
            case CNContactPostalAddressesKey:
                serializedContact[CNContactPostalAddressesKey] =
                    contact.postalAddresses.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": [
                            "city": labeledValue.value.city,
                            "country": labeledValue.value.country,
                            "isoCountryCode": labeledValue.value.isoCountryCode,
                            "postalCode": labeledValue.value.postalCode,
                            "state": labeledValue.value.state,
                            "street": labeledValue.value.street,
                            "subAdministrativeArea": labeledValue.value.subAdministrativeArea,
                            "subLocality": labeledValue.value.subLocality
                        ]
                        ]}
            case CNContactDepartmentNameKey:
                serializedContact[CNContactDepartmentNameKey] = contact.departmentName
            case CNContactJobTitleKey:
                serializedContact[CNContactJobTitleKey] = contact.jobTitle
            case CNContactMiddleNameKey:
                serializedContact[CNContactMiddleNameKey] = contact.middleName
            case CNContactNamePrefixKey:
                serializedContact[CNContactNamePrefixKey] = contact.namePrefix
            case CNContactNameSuffixKey:
                serializedContact[CNContactNameSuffixKey] = contact.nameSuffix
            case CNContactNicknameKey:
                serializedContact[CNContactNicknameKey] = contact.nickname
            case CNContactNonGregorianBirthdayKey:
                serializedContact[CNContactNonGregorianBirthdayKey] = contact.nonGregorianBirthday
            case CNContactNoteKey:
                serializedContact[CNContactNoteKey] = contact.note
            case CNContactOrganizationNameKey:
                serializedContact[CNContactOrganizationNameKey] = contact.organizationName
            case CNContactPhoneticGivenNameKey:
                serializedContact[CNContactPhoneticGivenNameKey] = contact.phoneticGivenName
            case CNContactPhoneticFamilyNameKey:
                serializedContact[CNContactPhoneticFamilyNameKey] = contact.phoneticFamilyName
            case CNContactPhoneticMiddleNameKey:
                serializedContact[CNContactPhoneticMiddleNameKey] = contact.phoneticMiddleName
            case CNContactPreviousFamilyNameKey:
                serializedContact[CNContactPreviousFamilyNameKey] = contact.previousFamilyName
            case CNContactSocialProfilesKey:
                serializedContact[CNContactSocialProfilesKey] =
                    contact.socialProfiles.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": [
                            "service": labeledValue.value.service,
                            "urlString": labeledValue.value.urlString,
                            "userIdentifier": labeledValue.value.userIdentifier,
                            "username": labeledValue.value.username
                        ]
                        ]}
            case CNContactUrlAddressesKey:
                serializedContact[CNContactUrlAddressesKey] =
                    contact.urlAddresses.map { labeledValue in [
                        "identifier": labeledValue.identifier,
                        "label": labeledValue.label!,
                        "value": labeledValue.value
                        ]
                }
            default:
                break
            }
        }

        return serializedContact
    }

    private func buildPluginResult(resultType: PluginResultType) -> CDVPluginResult {
        var tempPluginResult: CDVPluginResult

        switch resultType {
        case .typeError:
            tempPluginResult = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: pluginResponseMessages["PLUGIN_ERROR"]
            )
        case .typeOk:
            tempPluginResult = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: pluginResponseMessages["PLUGIN_OK"]
            )
        case .typeErrorFetchRequest:
            tempPluginResult = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: pluginResponseMessages["PLUGIN_PARSE_ERROR"]
            )
        case .typeErrorResponse:
            tempPluginResult = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: pluginResponseMessages["PLUGIN_RESPONSE_ERROR"]
            )
        case .typeErrorId:
            tempPluginResult = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: pluginResponseMessages["PLUGIN_ERROR_ID"]
            )
        }
        return tempPluginResult
    }

    private func getPredicate(forPredicate: Any?) -> NSPredicate {
        let tempPredicate = forPredicate as? [Any]
        var predicate: NSPredicate!

        switch tempPredicate![0] as? Int {
        case PredicateType.contactsInContainerWithIdentifier.hashValue:
            predicate = CNContact.predicateForContactsInContainer(withIdentifier: (tempPredicate![1] as? String)!)
        case PredicateType.contactsInGroupWithIdentifier.hashValue:
            predicate = CNContact.predicateForContactsInGroup(withIdentifier: (tempPredicate![1] as? String)!)
        case PredicateType.contactsMatchingName.hashValue:
            predicate = CNContact.predicateForContacts(matchingName: (tempPredicate![1] as? String)!)
        case PredicateType.contactsWithIdentifiers.hashValue:
            predicate = CNContact.predicateForContacts(withIdentifiers: (tempPredicate![1] as? [String])!)
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
        respPluginResult = buildPluginResult(resultType: .typeError)

        //grab the request ID
        guard let callbackId = command.arguments[0] as? String else {

            respPluginResult = buildPluginResult(resultType: .typeErrorId)

            self.commandDelegate!.send(
                respPluginResult,
                callbackId: command.callbackId
            )

            return
        }

        // grab the fetch reuqest keys
        guard let fetchRequest = command.arguments[1] as? NSObject else {

            respPluginResult = buildPluginResult(resultType: .typeErrorFetchRequest)

            self.commandDelegate!.send(
                respPluginResult,
                callbackId: command.callbackId
            )

            return
        }

        let request = CNContactFetchRequest(
            keysToFetch:
                (fetchRequest.value(forKey: "keysToFetch") as? [CNKeyDescriptor])!
            )
        //    request.predicate = getPredicate(forPredicate: fetchRequest.value(forKey: "predicate"))
        request.sortOrder = CNContactSortOrder(
            rawValue: (fetchRequest.value(forKey: "sortOrder") as? Int)!
            )!

        contactsWorkItem = DispatchWorkItem {
            do {
                try store.enumerateContacts(with: request) { (contact, stop) in
                    DispatchQueue.main.async {
                        if (self.contactsWorkItem)!.isCancelled {
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
            } catch {
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
