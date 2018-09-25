// @flow

type Callback = (err: ?Error) => void;

const pluginName = 'AppleCNContactStoreEnumerateContacts';
const pluginFnName = 'stop';

export default (cordova: any, id: string, cb?: Callback) => (
  cordova.exec(cb, cb, pluginName, pluginFnName, [id])
);
