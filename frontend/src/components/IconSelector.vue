<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择图标"
    width="800px"
    @close="handleClose"
  >
    <!-- 搜索框 -->
    <el-input
      v-model="searchKeyword"
      placeholder="搜索图标名称..."
      clearable
      prefix-icon="Search"
      class="mb-4"
    />
    
    <el-tabs v-model="activeTab" type="card" class="icon-tabs">
      <el-tab-pane v-for="category in iconCategories" :key="category.name" :label="category.name" :name="category.name">
        <div class="icon-grid">
          <div 
            v-for="icon in getFilteredIcons(category.icons)" 
            :key="icon.name"
            class="icon-item"
            :class="{ active: selectedIcon === icon.name }"
            @click="selectIcon(icon)"
          >
            <el-tooltip :content="icon.chineseName" placement="top" :effect="'light'">
              <div class="icon-wrapper">
                <el-icon :size="24" :color="selectedIcon === icon.name ? '#409eff' : '#606266'">
                  <component :is="icon.name" />
                </el-icon>
                <span class="icon-name">{{ icon.name }}</span>
              </div>
            </el-tooltip>
          </div>
          <div v-if="getFilteredIcons(category.icons).length === 0" class="no-results">
            没有找到匹配的图标
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

// 导入Element Plus图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 导入图标中文映射
import iconChineseNames from '../utils/iconI18n.js'

export default {
  name: 'IconSelector',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'update:visible', 'confirm', 'cancel'],
  setup(props, { emit }) {
    const dialogVisible = ref(props.visible)
    const selectedIcon = ref(props.modelValue)
    const activeTab = ref('全部图标')
    const searchKeyword = ref('')
    

    // 图标中文映射已通过import导入
    
    // 获取图标中文名，无映射则使用英文名
    const getIconChineseName = (iconName) => {
      return iconChineseNames[iconName] || iconName
    }
    
    // 获取Element Plus所有可用图标
    const getAllIcons = () => {
      return Object.keys(ElementPlusIconsVue).map(name => ({
        name: name,
        chineseName: getIconChineseName(name)
      })).sort((a, b) => a.name.localeCompare(b.name))
    }
    
    // 图标分类
    const iconCategories = ref([
      {
        name: '全部图标',
        icons: getAllIcons()
      },
      {
        name: '常用图标',
        icons: [
          { name: 'User', chineseName: '用户' },
          { name: 'UserFilled', chineseName: '用户填充' },
          { name: 'Setting', chineseName: '设置' },
          { name: 'HomeFilled', chineseName: '首页填充' },
          { name: 'Menu', chineseName: '菜单' },
          { name: 'ArrowRight', chineseName: '右箭头' },
          { name: 'Search', chineseName: '搜索' },
          { name: 'Edit', chineseName: '编辑' },
          { name: 'Delete', chineseName: '删除' },
          { name: 'Plus', chineseName: '加号' },
          { name: 'Check', chineseName: '对勾' },
          { name: 'Close', chineseName: '关闭' },
          { name: 'Warning', chineseName: '警告' },
          { name: 'Success', chineseName: '成功' },
          { name: 'Info', chineseName: '信息' },
          { name: 'Error', chineseName: '错误' }
        ]
      },
      {
        name: '导航图标',
        icons: [
          { name: 'ArrowUp', chineseName: '上箭头' },
          { name: 'ArrowDown', chineseName: '下箭头' },
          { name: 'ArrowLeft', chineseName: '左箭头' },
          { name: 'ArrowRight', chineseName: '右箭头' },
          { name: 'DArrowRight', chineseName: '双右箭头' },
          { name: 'DArrowLeft', chineseName: '双左箭头' },
          { name: 'CaretLeft', chineseName: '左三角' },
          { name: 'CaretRight', chineseName: '右三角' },
          { name: 'CaretTop', chineseName: '上三角' },
          { name: 'CaretBottom', chineseName: '下三角' }
        ]
      },
      {
        name: '数据图标',
        icons: [
          { name: 'DataLine', chineseName: '数据折线' },
          { name: 'Histogram', chineseName: '柱状图' },
          { name: 'DataBoard', chineseName: '数据面板' },
          { name: 'LineChart', chineseName: '折线图' },
          { name: 'PieChart', chineseName: '饼图' },
          { name: 'TrendCharts', chineseName: '趋势图' },
          { name: 'Sort', chineseName: '排序' },
          { name: 'SortUp', chineseName: '升序' },
          { name: 'SortDown', chineseName: '降序' },
          { name: 'Rank', chineseName: '排名' }
        ]
      },
      {
        name: '文件图标',
        icons: [
          { name: 'Document', chineseName: '文档' },
          { name: 'DocumentAdd', chineseName: '添加文档' },
          { name: 'DocumentDelete', chineseName: '删除文档' },
          { name: 'DocumentChecked', chineseName: '确认文档' },
          { name: 'DocumentCopy', chineseName: '复制文档' },
          { name: 'Folder', chineseName: '文件夹' },
          { name: 'FolderAdd', chineseName: '添加文件夹' },
          { name: 'FolderDelete', chineseName: '删除文件夹' },
          { name: 'File', chineseName: '文件' },
          { name: 'Files', chineseName: '文件组' }
        ]
      },
      {
        name: '媒体图标',
        icons: [
          { name: 'Picture', chineseName: '图片' },
          { name: 'PictureRounded', chineseName: '圆角图片' },
          { name: 'VideoPlay', chineseName: '播放视频' },
          { name: 'VideoPause', chineseName: '暂停视频' },
          { name: 'Camera', chineseName: '相机' },
          { name: 'Microphone', chineseName: '麦克风' },
          { name: 'Headset', chineseName: '耳机' },
          { name: 'Monitor', chineseName: '显示器' }
        ]
      }
    ])
    
    // 移除重复定义的iconChineseNames常量，已合并到上面的扩展版本
    
    // 监听visible属性变化
    watch(() => props.visible, (newVal) => {
      dialogVisible.value = newVal
    })
    
    // 监听modelValue属性变化
    watch(() => props.modelValue, (newVal) => {
      selectedIcon.value = newVal
    })
    
    // 根据搜索关键词过滤图标
    const getFilteredIcons = (icons) => {
      if (!searchKeyword.value) return icons
      
      const keyword = searchKeyword.value.toLowerCase()
      return icons.filter(icon => 
        icon.name.toLowerCase().includes(keyword) || 
        icon.chineseName.toLowerCase().includes(keyword)
      )
    }
    
    // 选择图标
      const selectIcon = (icon) => {
        selectedIcon.value = icon.name
        emit('update:modelValue', icon.name)
      }
    
    // 确认选择
    const confirmSelection = () => {
      if (!selectedIcon.value) {
        ElMessage.warning('请选择一个图标')
        return
      }
      
      emit('update:modelValue', selectedIcon.value)
      emit('confirm', selectedIcon.value)
      dialogVisible.value = false
    }
    
    // 关闭对话框
    const handleClose = () => {
      dialogVisible.value = false
      emit('update:visible', false)
      emit('cancel')
    }
    
    return {
        dialogVisible,
        selectedIcon,
        activeTab,
        searchKeyword,
        iconCategories,
        selectIcon,
        handleConfirm: confirmSelection,
        handleCancel: handleClose,
        handleClose,
        getFilteredIcons
      }
  }
}
</script>

<style scoped>
.icon-tabs {
  margin-bottom: 20px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  max-height: 450px;
  overflow-y: auto;
  padding: 8px 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.no-results {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

.icon-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 4px;
  cursor: pointer;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.icon-item:hover {
  background-color: #ecf5ff;
  transform: translateY(-2px);
}

.icon-item.active {
  background-color: #ecf5ff;
  border: 2px solid #409eff;
}

.icon-name {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
  text-align: center;
  width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>