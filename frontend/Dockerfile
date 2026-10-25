# Frontend image: build the Vite app with Node, serve the static files with nginx.

# --- build stage -----------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first so they are cached between source-only changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Vite bakes VITE_* values into the bundle at build time, so the Google client id
# is a build argument: docker build --build-arg VITE_GOOGLE_CLIENT_ID=... frontend
ARG VITE_GOOGLE_CLIENT_ID=""
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID
RUN npm run build

# --- run stage -------------------------------------------------------------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
