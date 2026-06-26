<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="68px">
      <el-form-item label="活动名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入活动名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="活动类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择活动类型" clearable style="width: 180px">
          <el-option label="职称评审" value="TITLE_REVIEW" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" style="width: 308px">
        <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['evaluation:activity:add']">配置向导</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['evaluation:activity:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['evaluation:activity:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['evaluation:activity:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="activityList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="活动编号" prop="id" width="100" align="center" />
      <el-table-column label="活动名称" prop="name" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="活动类型" prop="type" width="120" align="center">
        <template #default="scope">{{ formatType(scope.row.type) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="120" align="center">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ formatStatus(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" prop="startTime" width="160" align="center" />
      <el-table-column label="结束时间" prop="endTime" width="160" align="center" />
      <el-table-column label="创建人" prop="createdBy" width="120" align="center" />
      <el-table-column label="创建时间" prop="createdAt" width="160" align="center" />
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['evaluation:activity:edit']" />
          </el-tooltip>
          <el-tooltip content="发布" placement="top">
            <el-button link type="primary" icon="Promotion" @click="handlePublish(scope.row)" v-hasPermi="['evaluation:activity:publish']" />
          </el-tooltip>
          <el-tooltip content="投票入口" placement="top">
            <el-button link type="primary" icon="Link" @click="openVoteLinks(scope.row)" v-hasPermi="['evaluation:voter:list']" />
          </el-tooltip>
          <el-tooltip content="投票监控" placement="top">
            <el-button v-if="canMonitorRow(scope.row)" link type="primary" icon="DataLine" @click="openProgress(scope.row)" v-hasPermi="['evaluation:voter:list']" />
          </el-tooltip>
          <el-tooltip content="计算结果" placement="top">
            <el-button v-if="canCalculateRow(scope.row)" link type="primary" icon="DataAnalysis" @click="handleRowCalculate(scope.row)" v-hasPermi="['evaluation:result:calculate']" />
          </el-tooltip>
          <el-tooltip content="查看结果" placement="top">
            <el-button v-if="canViewRowResult(scope.row)" link type="primary" icon="View" @click="goResult(scope.row)" v-hasPermi="['evaluation:result:list']" />
          </el-tooltip>
          <el-tooltip content="导出结果" placement="top">
            <el-button v-if="canViewRowResult(scope.row)" link type="primary" icon="Download" @click="handleRowResultExport(scope.row)" v-hasPermi="['evaluation:result:export']" />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['evaluation:activity:remove']" />
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="activityRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入活动名称" maxlength="100" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="活动类型" prop="type">
              <el-input value="职称评审" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-input :model-value="formatStatus(form.status)" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="活动投票入口" v-model="voteLinksOpen" width="720px" append-to-body>
      <div v-loading="voteLinksLoading">
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
              <el-button v-copyText="sharedVoteUrl" :disabled="!sharedVoteUrl">复制</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="progressOpen" title="投票监控" size="520px">
      <div v-loading="progressLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动">{{ progressActivity?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成率">{{ progress.rate || 0 }}%</el-descriptions-item>
          <el-descriptions-item label="总评委">{{ progress.total || 0 }}</el-descriptions-item>
          <el-descriptions-item label="已投">{{ progress.done || 0 }}</el-descriptions-item>
          <el-descriptions-item label="未投">{{ progress.pending || 0 }}</el-descriptions-item>
          <el-descriptions-item label="活动状态">{{ formatStatus(progressActivity?.status) }}</el-descriptions-item>
          <el-descriptions-item label="结果状态">{{ progressResultText() }}</el-descriptions-item>
        </el-descriptions>
        <div class="drawer-actions">
          <el-button icon="Refresh" @click="loadProgress(progressActivity)">刷新</el-button>
          <el-button v-if="canCalculateProgressActivity()" type="success" icon="DataAnalysis" :loading="calculating" @click="handleProgressCalculate" v-hasPermi="['evaluation:result:calculate']">计算结果</el-button>
          <el-button v-if="canViewProgressResult()" type="primary" icon="View" @click="goResult(progressActivity)" v-hasPermi="['evaluation:result:list']">查看结果</el-button>
          <el-button v-if="canViewProgressResult()" type="warning" icon="Download" :loading="exportingResult" @click="handleProgressExport" v-hasPermi="['evaluation:result:export']">导出结果</el-button>
        </div>
        <template v-if="!progress.allCompleted">
          <h3>未投评委</h3>
          <el-table :data="progress.pendingVoters || []" max-height="360">
            <el-table-column label="姓名" prop="name" width="100" />
            <el-table-column label="工号" prop="employeeId" width="120" />
            <el-table-column label="部门" prop="department" />
          </el-table>
        </template>
        <el-alert v-else-if="canCalculateProgressActivity()" title="全员已完成投票，活动已结束，可以计算结果。" type="success" show-icon :closable="false" />
        <el-alert v-else-if="canViewProgressResult()" title="结果已计算，可以查看统计页或导出结果。" type="success" show-icon :closable="false" />
        <el-alert v-else title="全员已完成投票，系统正在同步活动结束状态，请刷新。" type="info" show-icon :closable="false" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="EvaluationActivity">
import { delActivity, getActivity, listActivity, publishActivity, updateActivity } from '@/api/evaluation/activity'
import { calculate as calculateResult, exportResult } from '@/api/evaluation/result'
import { getVoterProgress } from '@/api/evaluation/voter'
import { copyQrCodeImage, createQrCodeDataUrl } from '@/utils/qrcode'

const router = useRouter()
const { proxy } = getCurrentInstance()

const activityList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const dateRange = ref([])
const voteLinksOpen = ref(false)
const voteLinksLoading = ref(false)
const voteEntry = ref({})
const sharedVoteUrl = ref('')
const voteQrCodeDataUrl = ref('')
const progressOpen = ref(false)
const progressLoading = ref(false)
const progress = ref({})
const progressActivity = ref()
const calculating = ref(false)
const exportingResult = ref(false)

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已配置', value: 'CONFIGURED' },
  { label: '未开始', value: 'PUBLISHED' },
  { label: '投票中', value: 'VOTING' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已计算', value: 'CALCULATED' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已导出', value: 'EXPORTED' },
  { label: '已归档', value: 'ARCHIVED' }
]

function dateValue(value) {
  if (!value) return undefined
  if (value instanceof Date) return value.getTime()
  return Date.parse(String(value).replace(/-/g, '/'))
}

const validateTime = (rule, value, callback) => {
  const start = dateValue(data.form.startTime)
  const end = dateValue(data.form.endTime)
  if (Number.isFinite(start) && Number.isFinite(end) && end < start) {
    callback(new Error('结束时间不能早于开始时间'))
    return
  }
  if (Number.isFinite(end) && end <= Date.now()) {
    callback(new Error('结束时间必须晚于当前时间'))
    return
  }
  callback()
}

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: 'TITLE_REVIEW',
    status: undefined
  },
  rules: {
    name: [{ required: true, message: '活动名称不能为空', trigger: 'blur' }],
    type: [{ required: true, message: '活动类型不能为空', trigger: 'change' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
    startTime: [{ validator: validateTime, trigger: 'change' }],
    endTime: [{ validator: validateTime, trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function formatStatus(status) {
  return statusOptions.find(item => item.value === status)?.label || status || '-'
}

function statusTagType(status) {
  const map = {
    DRAFT: 'info',
    CONFIGURED: '',
    PUBLISHED: 'warning',
    VOTING: 'success',
    CLOSED: 'danger',
    CALCULATED: '',
    CONFIRMED: 'success',
    EXPORTED: 'success',
    ARCHIVED: 'info'
  }
  return map[status] || 'info'
}

function isVotingStatus(status) {
  return ['PUBLISHED', 'VOTING'].includes(status)
}

function isClosedStatus(status) {
  return status === 'CLOSED'
}

function canMonitorRow(row) {
  return isVotingStatus(row?.status)
}

function canCalculateRow(row) {
  return isClosedStatus(row?.status)
}

function canViewRowResult(row) {
  return isCalculatedStatus(row?.status)
}

function formatType(type) {
  const map = {
    TITLE_REVIEW: '职称评审',
    TALENT_SELECTION: '人才评选'
  }
  return map[type] || type || '-'
}

function getList() {
  loading.value = true
  listActivity(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    activityList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = {
    id: undefined,
    name: undefined,
    type: 'TITLE_REVIEW',
    status: 'DRAFT',
    startTime: undefined,
    endTime: undefined,
    remark: undefined
  }
  proxy.resetForm('activityRef')
}

function handleAdd() {
  router.push('/system/activity/wizard')
}

function handleUpdate(row) {
  reset()
  const activityId = row.id || ids.value
  getActivity(activityId).then(response => {
    form.value = response.data || {}
    open.value = true
    title.value = '修改活动'
  })
}

function submitForm() {
  proxy.$refs.activityRef.validate(valid => {
    if (!valid) return
    if (!form.value.id) {
      proxy.$modal.msgWarning('新增活动请使用配置向导')
      router.push('/system/activity/wizard')
      return
    }
    updateActivity(form.value).then(() => {
      proxy.$modal.msgSuccess('修改成功')
      open.value = false
      getList()
    })
  })
}

function cancel() {
  open.value = false
  reset()
}

function handleDelete(row) {
  const activityIds = row.id || ids.value
  proxy.$modal.confirm('是否确认删除活动编号为 "' + activityIds + '" 的数据项？').then(() => {
    return delActivity(activityIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handlePublish(row) {
  proxy.$modal.confirm('是否确认发布活动 "' + row.name + '"？').then(() => {
    return publishActivity(row.id)
  }).then((response) => {
    getList()
    proxy.$modal.msgSuccess('发布成功')
    openVoteLinks({ ...row, ...(response.data || {}), id: row.id })
  }).catch(() => {})
}

function handleExport() {
  proxy.download('evaluation/activity/export', { ...queryParams.value }, `activity_${new Date().getTime()}.xlsx`)
}

function openVoteLinks(row) {
  voteLinksOpen.value = true
  voteLinksLoading.value = true
  const activityId = row.id || row.activityId
  const request = row.voteEntryKey ? Promise.resolve({ data: row }) : getActivity(activityId)
  request.then(response => {
    const activity = response.data || row || {}
    voteEntry.value = {
      activityId: activity.id || activity.activityId || activityId,
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
    voteLinksLoading.value = false
  })
}

function buildSharedVoteUrl(activity) {
  if (activity.voteUrl) {
    return activity.voteUrl
  }
  if (!activity.voteEntryKey) {
    return ''
  }
  const origin = window.location.origin
  return `${origin}/vote/activity/${activity.voteEntryKey}`
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
}

function loadProgress(row) {
  if (!row) return
  progressActivity.value = row
  progressLoading.value = true
  getVoterProgress(row.id || row.activityId).then(response => {
    progress.value = response.data || {}
    const status = progress.value.activityStatus || progress.value.status
    if (status) {
      progressActivity.value = {
        ...progressActivity.value,
        status,
        endTime: progress.value.endTime || progressActivity.value.endTime
      }
    }
  }).finally(() => {
    progressLoading.value = false
  })
}

function isCalculatedStatus(status) {
  return ['CALCULATED', 'CONFIRMED', 'EXPORTED'].includes(status)
}

function canCalculateProgressActivity() {
  return progressActivity.value?.status === 'CLOSED'
}

function canViewProgressResult() {
  return isCalculatedStatus(progressActivity.value?.status)
}

function progressResultText() {
  if (!progress.value.allCompleted && !canCalculateProgressActivity()) return '等待全员投票'
  if (canCalculateProgressActivity()) return '已结束，可计算结果'
  if (canViewProgressResult()) return '已计算，可查看或导出'
  return '-'
}

function handleProgressCalculate() {
  const row = progressActivity.value
  if (!row) return
  doCalculate(row)
}

function handleRowCalculate(row) {
  doCalculate(row)
}

function doCalculate(row) {
  proxy.$modal.confirm('是否确认计算活动 "' + row.name + '" 的评审结果？').then(() => {
    calculating.value = true
    return calculateResult(row.id || row.activityId)
  }).then(() => {
    if (progressActivity.value && String(progressActivity.value.id || progressActivity.value.activityId) === String(row.id || row.activityId)) {
      progressActivity.value = { ...progressActivity.value, status: 'CALCULATED' }
    }
    getList()
    proxy.$modal.msgSuccess('计算完成')
  }).catch(() => {
  }).finally(() => {
    calculating.value = false
  })
}

function goResult(row) {
  const activityId = row?.id || row?.activityId
  if (!activityId) return
  router.push({ name: 'EvaluationResult', query: { activityId } })
}

function handleProgressExport() {
  const row = progressActivity.value
  handleRowResultExport(row)
}

function handleRowResultExport(row) {
  const activityId = row?.id || row?.activityId
  if (!activityId) return
  exportingResult.value = true
  exportResult(
    { activityId, exportType: 'FINAL_DECISION' },
    `evaluation_result_${activityId}_${new Date().getTime()}.xlsx`
  ).finally(() => {
    exportingResult.value = false
  })
}

getList()
</script>

<style scoped>
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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
