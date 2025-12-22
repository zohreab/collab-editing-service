import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    // This defines 'global' as 'window' during the build process
    global: 'window',
  },
})