<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
}

/* 确保下拉菜单能够完整显示内容 */
.el-select-dropdown {
  min-width: 200px;
}

/* 确保下拉菜单选项内容完整显示，不截断 */
.el-select-dropdown__item {
  white-space: nowrap;
  overflow: visible;
}

/* 确保下拉菜单能够根据内容自动调整宽度 */
.el-select-dropdown__wrap {
  max-height: 274px;
}

/* 确保下拉菜单选项文本完整显示 */
.el-select-dropdown__item span {
  display: inline-block;
  white-space: nowrap;
  overflow: visible;
}
</style>

