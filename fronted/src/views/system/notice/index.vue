<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="活动名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入活动名称"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建人" prop="createdBy">
        <el-input
          v-model="queryParams.createdBy"
          placeholder="请输入创建人"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="activityList">
      <el-table-column label="活动编号" align="center" prop="id" width="100" />
      <el-table-column label="活动名称" prop="name" min-width="220" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <el-button link type="primary" @click="openProgress(row)">{{ row.name }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="活动类型" align="center" prop="type" width="120">
        <template #default="{ row }">{{ formatType(row.type) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ formatStatus(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="170">
        <template #default="{ row }">{{ parseTime(row.startTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="170">
        <template #default="{ row }">{{ parseTime(row.endTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="发布时间" align="center" prop="publishTime" width="170">
        <template #default="{ row }">{{ parseTime(row.publishTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="createdBy" width="120" />
      <el-table-column label="创建时间" align="center" prop="createdAt" width="170">
        <template #default="{ row }">{{ parseTime(row.createdAt) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Link" @click="openVoteEntry(row)">投票入口</el-button>
          <el-button v-if="canClose(row)" link type="primary" icon="SwitchButton" @click="handleClose(row)">提前结束</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="活动投票入口" v-model="voteEntryOpen" width="720px" append-to-body>
      <div v-loading="voteEntryLoading">
        <el-alert title="所有评委共用同一个投票入口，进入后通过姓名和工号确认身份。" type="info" show-icon :closable="false" />
        <div class="vote-entry-panel">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="活动">{{ voteEntry.activityName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="活动状态">{{ formatStatus(voteEntry.status) }}</el-descriptions-item>
          </el-descriptions>
          <div class="vote-entry-qr-wrap">
            <img v-if="voteQrCodeDataUrl" :src="voteQrCodeDataUrl" alt="投票入口二维码" class="vote-entry-qr" />
            <el-alert v-else title="二维码暂无法生成，请使用下方投票链接。" type="warning" show-icon :closable="false" />
          </div>
          <div class="vote-entry-qr-actions">
            <el-button :disabled="!voteQrCodeDataUrl" @click="copyVoteQrCode">复制二维码</el-button>
          </div>
          <el-input v-model="sharedVoteUrl" readonly class="vote-entry-url">
            <template #append>
              <el-button v-copyText="sharedVoteUrl" :disabled="!sharedVoteUrl">复制链接</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="progressOpen" title="投票进度" size="520px" @closed="stopProgressAutoRefresh">
      <div v-loading="progressLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动">{{ progressActivity?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成率">{{ progress.rate || 0 }}%</el-descriptions-item>
          <el-descriptions-item label="总评委">{{ progress.total || 0 }}</el-descriptions-item>
          <el-descriptions-item label="已投">{{ progress.done || 0 }}</el-descriptions-item>
          <el-descriptions-item label="未投">{{ progress.pending || 0 }}</el-descriptions-item>
          <el-descriptions-item label="结果状态">{{ progress.allCompleted ? '可查看结果' : '等待所有评委提交' }}</el-descriptions-item>
        </el-descriptions>
        <div class="drawer-actions">
          <el-button icon="Refresh" @click="loadProgress(progressActivity)">刷新</el-button>
        </div>
        <h3>未投评委</h3>
        <el-table :data="progress.pendingVoters || []" max-height="360">
          <el-table-column label="姓名" prop="name" width="100" />
          <el-table-column label="工号" prop="employeeId" width="120" />
          <el-table-column label="部门" prop="department" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="Notice">
import { closeActivity, delActivity, getActivity, listActivity } from '@/api/evaluation/activity'
import { getVoterProgress } from '@/api/evaluation/voter'
import { copyQrCodeImage, createQrCodeDataUrl } from '@/utils/qrcode'

const { proxy } = getCurrentInstance()

const activityList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const progressOpen = ref(false)
const progressLoading = ref(false)
const progressActivity = ref()
const progress = ref({})
const voteEntryOpen = ref(false)
const voteEntryLoading = ref(false)
const voteEntry = ref({})
const sharedVoteUrl = ref('')
const voteQrCodeDataUrl = ref('')
let progressTimer = null

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'CONFIGURED', label: '已配置' },
  { value: 'PUBLISHED', label: '未开始' },
  { value: 'VOTING', label: '投票中' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'CALCULATED', label: '已计算' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'EXPORTED', label: '已导出' },
  { value: 'ARCHIVED', label: '已归档' }
]

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    type: 'TITLE_REVIEW',
    name: undefined,
    status: undefined,
    createdBy: undefined
  }
})

const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listActivity(queryParams.value).then(response => {
    activityList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.type = 'TITLE_REVIEW'
  handleQuery()
}

function formatType(type) {
  return type === 'TITLE_REVIEW' ? '职称评审' : (type || '-')
}

function formatStatus(status) {
  const option = statusOptions.find(item => item.value === status)
  return option ? option.label : (status || '-')
}

function statusTagType(status) {
  if (status === 'VOTING') {
    return 'success'
  }
  if (status === 'PUBLISHED') {
    return 'warning'
  }
  if (status === 'CLOSED' || status === 'ARCHIVED') {
    return 'info'
  }
  if (status === 'CALCULATED' || status === 'CONFIRMED' || status === 'EXPORTED') {
    return 'warning'
  }
  return ''
}

function canClose(row) {
  return row && (row.status === 'PUBLISHED' || row.status === 'VOTING')
}

function handleClose(row) {
  proxy.$modal.confirm('是否确认提前结束活动 "' + row.name + '"？').then(() => {
    return closeActivity(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('活动已结束')
    getList()
    if (progressActivity.value?.id === row.id) {
      progressActivity.value.status = 'CLOSED'
    }
  }).catch(() => {})
}

function handleDelete(row) {
  const message = row.status === 'VOTING'
    ? '活动 "' + row.name + '" 正在投票中，请先提前结束后再删除。'
    : '是否确认删除活动 "' + row.name + '"？'
  if (row.status === 'VOTING') {
    proxy.$modal.msgWarning(message)
    return
  }
  proxy.$modal.confirm(message).then(() => {
    return delActivity(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
    if (progressActivity.value?.id === row.id) {
      progressOpen.value = false
    }
  }).catch(() => {})
}

function openVoteEntry(row) {
  voteEntryOpen.value = true
  voteEntryLoading.value = true
  sharedVoteUrl.value = ''
  voteQrCodeDataUrl.value = ''
  const request = row.voteEntryKey ? Promise.resolve({ data: row }) : getActivity(row.id)
  request.then(response => {
    const activity = response.data || row || {}
    voteEntry.value = {
      activityId: activity.id || row.id,
      activityName: activity.name || row.name,
      status: activity.status || row.status,
      voteEntryKey: activity.voteEntryKey
    }
    sharedVoteUrl.value = buildSharedVoteUrl(activity)
    voteQrCodeDataUrl.value = buildVoteQrCode(sharedVoteUrl.value)
    if (!sharedVoteUrl.value) {
      proxy.$modal.msgWarning('活动尚未生成投票入口，请先发布活动')
    }
  }).finally(() => {
    voteEntryLoading.value = false
  })
}

function buildSharedVoteUrl(activity) {
  if (activity.voteUrl) {
    return activity.voteUrl
  }
  if (!activity.voteEntryKey) {
    return ''
  }
  return `${window.location.origin}/vote/activity/${activity.voteEntryKey}`
}

function buildVoteQrCode(url) {
  if (!url) return ''
  try {
    return createQrCodeDataUrl(url)
  } catch (error) {
    console.warn('Failed to generate vote QR code', error)
    return ''
  }
}

function copyVoteQrCode() {
  copyQrCodeImage(voteQrCodeDataUrl.value).then(() => {
    proxy.$modal.msgSuccess('二维码已复制')
  }).catch(() => {
    proxy.$modal.msgWarning('二维码复制失败，请截图或复制链接发送')
  })
}

function openProgress(row) {
  progressOpen.value = true
  loadProgress(row)
  startProgressAutoRefresh()
}

function loadProgress(row) {
  if (!row) return
  progressActivity.value = row
  progressLoading.value = true
  getVoterProgress(row.id).then(response => {
    progress.value = response.data || {}
  }).finally(() => {
    progressLoading.value = false
  })
}

function startProgressAutoRefresh() {
  stopProgressAutoRefresh()
  progressTimer = window.setInterval(() => {
    if (progressOpen.value && progressActivity.value) {
      loadProgress(progressActivity.value)
    }
  }, 10000)
}

function stopProgressAutoRefresh() {
  if (progressTimer) {
    window.clearInterval(progressTimer)
    progressTimer = null
  }
}

onBeforeUnmount(() => {
  stopProgressAutoRefresh()
})

getList()
</script>

<style scoped>
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  margin: 14px 0;
}

.vote-entry-panel {
  margin-top: 12px;
}

.vote-entry-url {
  margin-top: 12px;
}

.vote-entry-qr-wrap {
  display: flex;
  justify-content: center;
  margin: 16px 0 8px;
}

.vote-entry-qr {
  width: 220px;
  height: 220px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.vote-entry-qr-actions {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}
</style>
