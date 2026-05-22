import { useEffect, useState } from "react";
import { subscribeAlert } from "../utils/AlertService";
import AlertPopup from "../popup/AlertPopup";

export default function AlertHost() {
    const [state, setState] = useState({
        open: false,
        type: 'alert',
        message: '',
        confirmCallBack: null,
        cancelCallBack: null,
    });

    useEffect(() => {
        const unsubscribe = subscribeAlert((detail) => {
            setState({
                open: true,
                type: detail.type || 'alert',
                message: detail.message || '',
                confirmCallBack: detail.confirmCallBack || null,
                cancelCallBack: detail.cancelCallBack || null,
            });
        });
        return unsubscribe;
    }, []);

    const handleClose = () => {
        if(typeof state.confirmCallBack === 'function') state.confirmCallBack();
        setState((s) => ({...s, open: false}));
    };

    const handleCancel = () => {
        if(typeof state.cancelCallBack === 'function') state.cancelCallBack();
        setState((s) => ({...s, open: false}));
    }

    return (
        <AlertPopup
            open = {state.open}
            message = {state.message}
            type = {state.type}
            onClose = {handleClose}
            onCancel = {handleCancel}
        />
    )
}