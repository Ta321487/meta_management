const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  publicPath: '/metadata-system/',
  outputDir: 'dist',
  assetsDir: 'static',
  // 共享文件夹下 Webpack 默认 watch 也会失败，与 devServer.watchFiles 一并开启轮询
  configureWebpack: {
    watchOptions: {
      poll: 1000,
      ignored: /node_modules/,
    },
  },
  devServer: {
    port: 8081,
    // VMware/HGFS 等共享盘不支持原生 fs watch，需轮询否则 dev server 报错
    watchFiles: {
      paths: ['src/**/*', 'public/**/*'],
      options: {
        usePolling: true,
        interval: 1000,
      },
    },
    client: {
      overlay: {
        errors: true,
        warnings: false,
        runtimeErrors: (error) => {
          // 忽略 ResizeObserver 错误
          const message = error.message || ''
          if (message.includes('ResizeObserver loop') || 
              message.includes('ResizeObserver loop limit exceeded')) {
            return false
          }
          return true
        }
      }
    },
    proxy: {
      '/metadata-system/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})

