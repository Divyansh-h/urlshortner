# Stage 1: Build the React/Vite application
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package.json package-lock.json ./
RUN npm install

# Copy source code and build
COPY . .
RUN npm run build

# Stage 2: Serve the built application using Nginx
FROM nginx:alpine

# Copy the compiled static assets from the build stage to Nginx's default public directory
COPY --from=build /app/dist /usr/share/nginx/html

# Copy the custom Nginx configuration for React SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
