// @flow

import type { AppleCNContactSortOrder } from './AppleCNContactSortOrder';
import type { AppleCNKeyDescriptor } from './AppleCNKeyDescriptor';

export type AppleCNContactFetchRequest = {
  keysToFetch: AppleCNKeyDescriptor[],
  mutableObject?: boolean,
  sortOrder?: AppleCNContactSortOrder,
  unifyResults?: boolean,
};
