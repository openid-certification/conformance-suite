FROM node:9
WORKDIR /server
COPY package*.json ./
RUN npm install
COPY authorizationServer.js ./
ENTRYPOINT ["node", "./authorizationServer.js"]
