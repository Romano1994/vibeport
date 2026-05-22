import { Tokens } from "../enum/Tokens.ts";
import client from '../api/client';
import axios from 'axios';

// access 토큰명
const accessName = Tokens.ACCESS;

export const getExpTime = () => {
    const accessToken = sessionStorage.getItem(accessName);

    if(!accessToken) {
        return;
    }

    const payloadBase64 = accessToken.split('.')[1];

    if(!payloadBase64) {
        return;
    }

    try {
        const payload = JSON.parse(atob(payloadBase64));

        if(payload.exp) {
            const expTime = payload.exp * 1000;
            return expTime;
        }
    } catch(err) {
        return null;
    }
}

const reissue = async() => {
    try {
        const res = await client.post('/user/reissue');
        const access = res.headers['authorization'];
        sessionStorage.setItem(Tokens.ACCESS, access);
        axios.defaults.headers.common['authorization'] = access;

        // 토큰 재발급 등록
        scheduleReissue();

    } catch (err) {
        console.error('토큰 재발급 실패:', err);
        
        // 재발급 실패 시 로그아웃 (동적 import로 순환 참조 방지)
        try {
            const { default: store } = await import('../store/store.js');
            const { logoutUser } = await import('../store/userSlice.js');
            await store.dispatch(logoutUser()).unwrap();
        } catch (logoutErr) {
            console.error('로그아웃 처리 실패:', logoutErr);
        }

        delete axios.defaults.headers.common['authorization'];
        sessionStorage.removeItem(Tokens.ACCESS);
    }
}

export const scheduleReissue = () => {
    const accessToken = sessionStorage.getItem(Tokens.ACCESS);
    const exptime = Number(getExpTime());
    const oneMinBefore = exptime - (Date.now()) - 60000;

    if(accessToken) {
        setTimeout(reissue, oneMinBefore > 0 ? oneMinBefore : 0);
    }
}

