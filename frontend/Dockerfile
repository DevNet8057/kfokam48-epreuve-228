# VITE_API_URL est figée au build (variables Vite inlinées) : imposée par docker-compose.yml.
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build
EXPOSE 5173
CMD ["sh", "-c", "npm run preview -- --host 0.0.0.0 --port 5173"]
