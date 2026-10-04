import {defineConfig, loadEnv} from 'vite'
import react from '@vitejs/plugin-react-swc'
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    server: {
      open: true,
      port: 3000,
      host: true
    },
    preview: {
      open: true,
      host: true
    },
    define: {
      global: 'window'
    },
    resolve: {
      alias: [
        // { find: '', replacement: path.resolve(__dirname, 'src') },
        // {
        //   find: /^~(.+)/,
        //   replacement: path.join(process.cwd(), 'node_modules/$1')
        // },
        // {
        //   find: /^src(.+)/,
        //   replacement: path.join(process.cwd(), 'src/$1')
        // }
        // {
        //   find: 'assets',
        //   replacement: path.join(process.cwd(), 'src/assets')
        // },
      ]
    },
    base: '/modelix/dashboard/',
    plugins: [react(), tsconfigPaths()]
  };
});