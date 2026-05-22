import axios from 'axios';
import { Tokens } from '../enum/Tokens.ts';

const client = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081/vibeport',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

client.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem(Tokens.ACCESS);

    if(token) {
      config.headers['authorization'] = token;
    }

    return config;
  },
  (err) => {
    return Promise.reject(err);
  }
)

export default client;
