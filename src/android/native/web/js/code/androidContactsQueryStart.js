// @flow

type CallbackNext = (result: any) => void;
type CallbackError = (err: Error) => void;
type CallbackComplete = () => void;

const pluginName = 'AndroidContentResolverQuery';
const pluginFnName = 'start';

const MESSAGE_TYPE_NULL = 5;

export default (
  cordova: any,
  id: string,
  uri: string,
  projection: string[],
  selection: string,
  selectionArgs: string[],
  sortOrder: string,
  cbNext?: ?CallbackNext,
  cbError?: ?CallbackError,
  cbComplete?: ?CallbackComplete,
) => {
  const onResult = (result: ?any) => {
    if (result && result != MESSAGE_TYPE_NULL) {
      if (cbNext) cbNext(result);
    } else if (result == MESSAGE_TYPE_NULL) {
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
    [id, uri, projection, selection, selectionArgs, sortOrder],
  );
};
