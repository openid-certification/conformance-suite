FROM node:10
ENV TINI_VERSION v0.18.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini
ENTRYPOINT ["/tini", "--"]
WORKDIR /server
COPY package*.json ./
RUN npm install
COPY authorizationServer.js ./authorizationServer.js
CMD ["node", "./authorizationServer.js"]
