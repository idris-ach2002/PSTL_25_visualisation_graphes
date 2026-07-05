FROM emscripten/emsdk:3.1.64 AS wasm
WORKDIR /app
COPY wasm-engine ./wasm-engine
COPY frontend ./frontend
RUN make -C wasm-engine wasm

FROM node:22-bookworm AS frontend
WORKDIR /app/frontend
COPY --from=wasm /app/frontend ./
RUN npm ci --no-audit --no-fund --prefer-offline
RUN npm run build

FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend /app/frontend/dist /usr/share/nginx/html
EXPOSE 80
