// @flow

export type AppleCNContactTypePerson = 0;
export type AppleCNContactTypeOrganization = 1;

export type AppleCNContactType = (
  | AppleCNContactTypePerson
  | AppleCNContactTypeOrganization
);
