// @flow

import type { AppleCNContact } from '../types/flow/AppleCNContact';
import type { AppleCNContactFetchRequest } from '../types/flow/AppleCNContactFetchRequest';

type CallbackNext = (contact: AppleCNContact) => void;
type CallbackError = (err: Error) => void;
type CallbackComplete = () => void;

const pluginName = 'AppleCNContactStoreEnumerateContacts';
const pluginFnName = 'start';

export default (
  cordova: any,
  id: string,
  req: AppleCNContactFetchRequest,
  cbNext?: ?CallbackNext,
  cbError?: ?CallbackError,
  cbComplete?: ?CallbackComplete,
) => {
  const onResult = (contact: ?AppleCNContact) => {
    if (contact) {
      if (cbNext) cbNext(contact);
    } else {
      if (cbComplete) cbComplete();
    }
  };

  const onError = (err: Error) => {
    if (cbError) cbError(err);
    if (cbComplete) cbComplete();
  }

  cordova.exec(
    onResult,
    onError,
    pluginName,
    pluginFnName,
    [id, req],
  );
};
