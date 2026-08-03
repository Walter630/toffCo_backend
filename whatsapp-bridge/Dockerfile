FROM node:20-alpine

WORKDIR /app

# Instala dependências primeiro (cache do Docker)
COPY package.json ./
RUN npm install --omit=dev

# Copia o código
COPY src/ ./src/

# Porta padrão do bridge
EXPOSE 3100

# A pasta de auth é montada como volume pra persistir entre restarts
VOLUME ["/app/auth_data"]

CMD ["node", "src/index.js"]
