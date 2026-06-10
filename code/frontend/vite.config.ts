import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ["vue", "vue-router", "pinia"],
      dts: "src/auto-imports.d.ts"
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: "src/components.d.ts"
    })
  ],
  resolve: {
    alias: {
      "@": resolve(__dirname, "src")
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ["vue", "vue-router", "pinia"],
          element: ["element-plus", "@element-plus/icons-vue"],
          echarts: ["echarts/core", "echarts/charts", "echarts/components", "echarts/renderers"]
        }
      }
    }
  },
  server: {
    port: 8848,
    host: "0.0.0.0",
    proxy: {
      "/logout": { target: "http://localhost:8080", changeOrigin: true },
      "/refresh-token": { target: "http://localhost:8080", changeOrigin: true },
      "/get-async-routes": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/user": { target: "http://localhost:8080", changeOrigin: true },
      "/list-all-role": { target: "http://localhost:8080", changeOrigin: true },
      "/list-role-ids": { target: "http://localhost:8080", changeOrigin: true },
      "/role": { target: "http://localhost:8080", changeOrigin: true },
      "/menu": { target: "http://localhost:8080", changeOrigin: true },
      "/dept": { target: "http://localhost:8080", changeOrigin: true },
      "/role-menu": { target: "http://localhost:8080", changeOrigin: true },
      "/role-menu-ids": { target: "http://localhost:8080", changeOrigin: true },
      "/mine": { target: "http://localhost:8080", changeOrigin: true },
      "/mine-logs": { target: "http://localhost:8080", changeOrigin: true },
      "/online-logs": { target: "http://localhost:8080", changeOrigin: true },
      "/login-logs": { target: "http://localhost:8080", changeOrigin: true },
      "/operation-logs": { target: "http://localhost:8080", changeOrigin: true },
      "/system-logs": { target: "http://localhost:8080", changeOrigin: true },
      "/api": { target: "http://localhost:8080", changeOrigin: true }
    }
  }
});
