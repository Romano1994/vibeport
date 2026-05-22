# 1단계: 빌드 스테이지
FROM node:20-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .

# API Base URL (빌드 시 주입)
ARG REACT_APP_API_BASE_URL=/vibeport
ENV REACT_APP_API_BASE_URL=$REACT_APP_API_BASE_URL

# CRA 빌드 명령어를 실행합니다.
RUN npm run build

# 2단계: 실행 스테이지 (Nginx)
FROM nginx:stable-alpine

# [중요] CRA는 결과물이 /app/build 폴더에 생성됩니다.
COPY --from=build /app/build /usr/share/nginx/html

# 커스텀 nginx 설정 (API 프록시 포함)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]