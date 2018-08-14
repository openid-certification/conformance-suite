FROM node:10
WORKDIR /server
COPY package*.json ./
RUN npm install
COPY authorizationServer.js ./server.js
