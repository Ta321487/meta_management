const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  publicPath: '/metadata-system/',
  outputDir: 'dist',
  assetsDir: 'static',
  devServer: {
    port: 8081,
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

