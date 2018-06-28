// @flow

export type AppleCNContactSortOrderNone = 0;
export type AppleCNContactSortOrderUserDefault = 1;
export type AppleCNContactSortOrderGivenName = 2;
export type AppleCNContactSortOrderFamilyName = 3;

export type AppleCNContactSortOrder = (
  | AppleCNContactSortOrderNone
  | AppleCNContactSortOrderUserDefault
  | AppleCNContactSortOrderGivenName
  | AppleCNContactSortOrderFamilyName
);
