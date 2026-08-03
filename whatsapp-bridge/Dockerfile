FROM node:20-alpine

WORKDIR /app

# Git é necessário porque o Baileys puxa dependências de repositórios git
RUN apk add --no-cache git

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
