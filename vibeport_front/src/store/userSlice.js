import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import client from '../api/client';
import axios from 'axios';
import { Tokens } from '../enum/Tokens.ts';
import { persistor } from '../index.js';
import { PURGE } from 'redux-persist';
import { scheduleReissue } from '../utils/AuthUtils.js';

export const loginUser = createAsyncThunk(
    'user/loginUser',
    async ({email, password}, {rejectWithValue}) => {
        try {
            const res = await client.post('/login', {email, password});

            const access = res.headers['authorization'];
            sessionStorage.setItem(Tokens.ACCESS, access);
            axios.defaults.headers.common['authorization'] = access;

            // refresh 토큰 재발급 등록
            scheduleReissue();

            return res.data.userVo;
        } catch(err) {
            const msg = '로그인 실패';
            return rejectWithValue(msg);
        }
    }
);

export const logoutUser = createAsyncThunk(
    'user/logoutUser',
    async() => {
        try {
            await client.post('/logout')
            .then(() => {
                persistor.purge();
            });
            
            delete axios.defaults.headers.common['authorization'];
            sessionStorage.removeItem(Tokens.ACCESS);
            

            return {success: true};
        } catch(err) {
            const msg = '로그아웃 실패';
            return msg;
        }
    }
);

const initialState = {
    isLoggedIn: false,
    userInfo: null,
    loading: false,
    error: null,
};

const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        logout: async(state) => {
            state.isLoggedIn = false;
            state.userInfo = null;
            state.loading = false;
            state.error = null;

            sessionStorage.removeItem(Tokens.ACCESS);
            delete axios.defaults.headers.common['authorization'];
        },
        updateUserInfo: (state, action) => {
            state.userInfo = {...state.userInfo, ...action.payload}
        },
    },
    extraReducers : (builder) => {
        builder
            .addCase(loginUser.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(loginUser.fulfilled, (state, action) => {
                state.isLoggedIn = true;
                state.userInfo = action.payload;
                state.loading = false;
                state.error = null;
            })
            .addCase(loginUser.rejected, (state, action) => {
                state.isLoggedIn = false;
                state.loading = false;
                state.userInfo = null;
                state.error = action.payload;
            })
            .addCase(logoutUser.pending, (state) => {
                state.isLoggedIn = true;
                state.loading = true;
            })
            .addCase(logoutUser.fulfilled, (state) => {
                state.isLoggedIn = false;
                state.loading = false;
                state.userInfo = null;
                state.error = null;
            })
            .addCase(PURGE, (state) => {
                state.isLoggedIn = false;
                state.loading = false;
                state.error = false;
            })
    }
});

export const { logout, updateUserInfo } = userSlice.actions;
export default userSlice.reducer;