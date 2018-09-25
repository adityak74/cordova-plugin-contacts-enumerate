import Contacts

enum PluginResultType {
    case typeError
    case typeOk
    case typeErrorFetchRequest
    case typeErrorResponse
    case typeErrorId
    case typeNull
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
    "PLUGIN_RESPONSE_ERROR": "Invalid reponse format",
    "PLUGIN_NULL": "0"
]

let contactID = "id"
let contactDisplayName = "displayName"
let contactName = "name"
let contactFormatted = "formatted"
let contactEmails = "emails"
let contactPhotos = "photos"
var errorCode = false
var enumerationIDs: [String] = []
// hold the callback instance for the web layer interface
var instanceCallbackID = ""

@objc(AppleCNContactStoreEnumerateContacts)
// swiftlint:disable:next type_body_length
class AppleCNContactStoreEnumerateContacts: CDVPlugin {
    var ids: [String] = []
    var contactsWorkItem: DispatchWorkItem?
    // swiftlint:disable:next cyclomatic_complexity function_body_length
    private func getSerializedContact(contact: CNContact, req: NSObject) -> [String: Any] {
        var serializedContact: [String: Any] = [:]
        let keysToFetch = req.value(forKey: "keysToFetch") as? [String]
        // build the contact here
        serializedContact[contactID] = contact.identifier
        serializedContact[contactName] = [:]
        for key in keysToFetch! {
            switch key {
            case CNContactFamilyNameKey:
                if var nameArr = serializedContact[contactName] as? [String: Any] {
                    nameArr[CNContactFamilyNameKey] = contact.familyName
                    serializedContact[contactName] = nameArr
                }
            case CNContactGivenNameKey:
                if var nameArr = serializedContact[contactName] as? [String: Any] {
                    nameArr[CNContactGivenNameKey] = contact.givenName
                    serializedContact[contactName] = nameArr
                }
            case CNContactEmailAddressesKey:
                serializedContact[contactEmails] =
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
            case CNContactImageDataKey:
                let contactImageData = contact.imageData
                if contactImageData == nil {
                    break
                }
                do {
                    let fileURL = try FileManager.default.url(for: .documentDirectory,
                                                               in: .userDomainMask,
                                                               appropriateFor: nil,
                                                               create: false)
                        .appendingPathComponent(
                            "contact_photo_\(UUID().uuidString)"
                    )
                    try contactImageData?.write(to: fileURL, options: .atomic)
                    serializedContact[contactPhotos] = [[
                        "pref": "false",
                        "type": "url",
                        "value": fileURL.path
                        ]]
                } catch {
                    print(error)
                }
            case CNContactBirthdayKey:
                serializedContact[CNContactBirthdayKey] =
                    contact.birthday.flatMap({ (date) -> String? in
                        return "\(date.month ?? 1)/\(date.day ?? 1)/\(date.year ?? 1970)"
                    })
            case CNContactPostalAddressesKey:
                if #available(iOS 10.3, *) {
                    serializedContact[CNContactPostalAddressesKey] =
                        contact.postalAddresses.map { labeledValue in
                            [
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
                            ]
                    }
                } else {
                    serializedContact[CNContactPostalAddressesKey] =
                        contact.postalAddresses.map { labeledValue in
                            [
                                "identifier": labeledValue.identifier,
                                "label": labeledValue.label!,
                                "value": [
                                    "city": labeledValue.value.city,
                                    "country": labeledValue.value.country,
                                    "isoCountryCode": labeledValue.value.isoCountryCode,
                                    "postalCode": labeledValue.value.postalCode,
                                    "state": labeledValue.value.state,
                                    "street": labeledValue.value.street
                                ]
                            ]
                    }
                }
            case CNContactDepartmentNameKey:
                serializedContact[CNContactDepartmentNameKey] = contact.departmentName
            case CNContactJobTitleKey:
                serializedContact[CNContactJobTitleKey] = contact.jobTitle
            case CNContactMiddleNameKey:
                if var nameArr = serializedContact[contactName] as? [String: Any] {
                    nameArr[CNContactMiddleNameKey] = contact.middleName
                    serializedContact[contactName] = nameArr
                }
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
        if var nameArr = serializedContact[contactName] as? [String: Any] {
            serializedContact[contactDisplayName] = "\(nameArr[CNContactFamilyNameKey] ?? NSNull.self) \(nameArr[CNContactGivenNameKey] ?? NSNull.self)"
            nameArr[contactFormatted] = "\(nameArr[CNContactGivenNameKey] ?? NSNull.self) \(nameArr[CNContactMiddleNameKey] ?? NSNull.self) \(nameArr[CNContactFamilyNameKey] ?? NSNull.self)"
            serializedContact[contactName] = nameArr
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
                status: CDVCommandStatus_OK,
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
        case .typeNull:
            tempPluginResult = CDVPluginResult(
                status: CDVCommandStatus_OK,
                messageAs: pluginResponseMessages["PLUGIN_NULL"]
            )
        }
        return tempPluginResult
    }

    private func getPredicate(forPredicate: Any?) -> NSPredicate? {
        let tempPredicate = forPredicate as? [Any]
        var predicate: NSPredicate!
        if forPredicate != nil {
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
        } else {
            predicate = nil
        }
        return predicate
    }

    private func getSortOrder(forSortOrder: Any?) -> CNContactSortOrder? {
        var sortOrder: CNContactSortOrder?
        if forSortOrder != nil {
            sortOrder = CNContactSortOrder(rawValue: (forSortOrder as? Int)!)
        } else {
            sortOrder = CNContactSortOrder.userDefault
        }
        return sortOrder
    }

    private func sendComplete() {
        DispatchQueue.main.async {
            let respPluginResult = self.buildPluginResult(resultType: .typeNull)
            self.commandDelegate!.send(
                respPluginResult,
                callbackId: instanceCallbackID
            )
        }
    }

    @objc(start:)
    // swiftlint:disable:next function_body_length
    func start(command: CDVInvokedUrlCommand) {
        instanceCallbackID = command.callbackId
        var respPluginResult: CDVPluginResult
        let store = CNContactStore()

        //default plugin result to null
        respPluginResult = buildPluginResult(resultType: .typeNull)

        //grab the request ID
        guard let callbackId = command.arguments[0] as? String else {
            respPluginResult = buildPluginResult(resultType: .typeErrorId)
            self.commandDelegate!.send(
                respPluginResult,
                callbackId: instanceCallbackID
            )
            return
        }
        enumerationIDs.append(callbackId)
        // grab the fetch reuqest keys
        guard let fetchRequest = command.arguments[1] as? NSObject else {
            respPluginResult = buildPluginResult(resultType: .typeErrorFetchRequest)
            self.commandDelegate!.send (
                respPluginResult,
                callbackId: instanceCallbackID
            )
            return
        }

        let request = CNContactFetchRequest(
            keysToFetch:
            (fetchRequest.value(forKey: "keysToFetch") as? [CNKeyDescriptor])!
        )
        request.predicate = getPredicate(forPredicate: fetchRequest.value(forKey: "predicate"))
        request.sortOrder = getSortOrder(forSortOrder: fetchRequest.value(forKey: "sortOrder"))!
        do {
            try store.enumerateContacts(with: request) { (contact, stop) in
                DispatchQueue.main.async {
                    if enumerationIDs.contains(callbackId) {
                        respPluginResult = CDVPluginResult(
                            status: CDVCommandStatus_OK,
                            messageAs: self.getSerializedContact(contact: contact, req: fetchRequest)
                        )
                        respPluginResult.setKeepCallbackAs(true)
                        self.commandDelegate!.send(
                            respPluginResult,
                            callbackId: instanceCallbackID
                        )
                    } else {
                        stop.pointee = true
                    }
                }
            }
            sendComplete()
        } catch {
            print("unable to fetch contacts")
            errorCode = true
        }
        // on any error while enumerate publish error
        if errorCode {
            let respPluginResult = self.buildPluginResult(resultType: .typeError)
            self.commandDelegate!.send(
                respPluginResult,
                callbackId: instanceCallbackID
            )
        }
    }

    @objc(stop:)
    func stop(command: CDVInvokedUrlCommand) {
        var respPluginResult: CDVPluginResult
        //grab the request ID and remove from the enumerationIDs
        guard let callbackId = command.arguments[0] as? String else {
            respPluginResult = buildPluginResult(resultType: .typeErrorId)
            self.commandDelegate!.send(
                respPluginResult,
                callbackId: instanceCallbackID
            )
            return
        }
        if enumerationIDs.contains(callbackId) {
            if enumerationIDs.remove(at: enumerationIDs.index(of: callbackId)!) == callbackId {
                sendComplete()
            }
        }
    }
}
