// @flow

import type { AppleCNContactRelation } from './AppleCNContactRelation';
import type { AppleCNContactType } from './AppleCNContactType';
import type { AppleCNInstantMessageAddress } from './AppleCNInstantMessageAddress';
import type { AppleCNLabeledValue } from './AppleCNLabeledValue';
import type { AppleCNPhoneNumber } from './AppleCNPhoneNumber';
import type { AppleCNPostalAddress } from './AppleCNPostalAddress';
import type { AppleCNSocialProfile } from './AppleCNSocialProfile';

export type AppleCNContact = {
  birthday?: string,
  contactRelations?: AppleCNLabeledValue<AppleCNContactRelation>[],
  contactType?: AppleCNContactType,
  dates?: AppleCNLabeledValue<string>[],
  departmentName?: string,
  emailAddresses?: AppleCNLabeledValue<string>[],
  familyName?: string,
  givenName?: string,
  identifier?: string,
  imageData?: string,
  imageDataAvailable?: boolean,
  instantMessageAddresses?: AppleCNLabeledValue<AppleCNInstantMessageAddress>[],
  jobTitle?: string,
  middleName?: string,
  namePrefix?: string,
  nameSuffix?: string,
  nickname?: string,
  nonGregorianBirthday?: string,
  note?: string,
  organizationName?: string,
  phoneNumbers?: AppleCNLabeledValue<AppleCNPhoneNumber>[],
  phoneticFamilyName?: string,
  phoneticGivenName?: string,
  phoneticMiddleName?: string,
  phoneticOrganizationName?: string,
  postalAddresses?: AppleCNLabeledValue<AppleCNPostalAddress>[],
  previousFamilyName?: string,
  socialProfiles?: AppleCNLabeledValue<AppleCNSocialProfile>[],
  thumbnailImageData?: string,
  urlAddresses?: AppleCNLabeledValue<string>[],
};
