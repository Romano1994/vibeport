import { configureStore } from '@reduxjs/toolkit';
import userReducer from './userSlice';
import { combineReducers } from 'redux';
import persistReducer from 'redux-persist/es/persistReducer';
import storage from 'redux-persist/lib/storage';
import { PURGE } from 'redux-persist';

const persistConfig = {
  key: 'root',
  storage: storage,
  whitelist: ['user'],
};

const reducers = combineReducers({
  user: userReducer,
});

const persistedReducer = persistReducer(persistConfig, reducers);

const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) => getDefaultMiddleware({
    serializableCheck: {
      ignoreActions: [PURGE]
    }
  }),
});

// const store = configureStore({
//   reducer: {
//     user: userReducer,
//   },
// });

export default store;