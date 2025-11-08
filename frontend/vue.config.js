const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  publicPath: '/metadata-system/',
  outputDir: 'dist',
  assetsDir: 'static',
  devServer: {
    port: 8081,
    proxy: {
      '/metadata-system/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})

