const emitter = new EventTarget();

export const openAlert = (message, confirmCallBack) => {
     emitter.dispatchEvent(
        new CustomEvent('vp-alert', {
            detail: {type: 'alert', message, confirmCallBack},
        })
    );
};

export const openConfirm = (message, confirmCallBack, cancelCallBack) => {
    emitter.dispatchEvent(
        new CustomEvent('vp-alert', { // 'vp-confirm' → 'vp-alert'로 변경
            detail: { type: 'confirm', message, confirmCallBack, cancelCallBack},
        })
    );
};

export const subscribeAlert = (handler) => {
    const listener = (e) => handler(e.detail);
    emitter.addEventListener('vp-alert', listener);
    return () => emitter.removeEventListener('vp-alert', listener);
};

export default {
    openAlert,
    openConfirm,
    subscribeAlert,
}