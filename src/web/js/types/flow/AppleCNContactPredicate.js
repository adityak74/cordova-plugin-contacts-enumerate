// @flow

export type AppleCNContactPredicateForContactsInContainerWithIdentifier = [0, string];
export type AppleCNContactPredicateForContactsInGroupWithIdentifier = [1, string];
export type AppleCNContactPredicateForContactsMatchingName = [2, string];
export type AppleCNContactPredicateForContactsWithIdentifiers = [3, string[]];

export type AppleCNContactPredicate = (
  | AppleCNContactPredicateForContactsInContainerWithIdentifier
  | AppleCNContactPredicateForContactsInGroupWithIdentifier
  | AppleCNContactPredicateForContactsMatchingName
  | AppleCNContactPredicateForContactsWithIdentifiers
);
