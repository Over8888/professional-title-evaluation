<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="评审活动" prop="activityId">
        <el-select
          v-model="queryParams.activityId"
          placeholder="请选择活动"
          filterable
          clearable
          style="width: 280px"
          @change="handleActivityChange"
        >
          <el-option v-for="item in activityOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="导出类型" prop="exportType">
        <el-select v-model="exportType" placeholder="请选择导出类型" style="width: 180px">
          <el-option v-for="item in exportTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" :disabled="!queryParams.activityId" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" :disabled="!queryParams.activityId" @click="reloadActivitiesAndResult">刷新</el-button>
        <el-button type="success" icon="DataAnalysis" :disabled="!queryParams.activityId" :loading="calculating" @click="handleCalculate" v-hasPermi="['evaluation:result:calculate']">计算</el-button>
        <el-button type="warning" icon="Download" :disabled="!queryParams.activityId" :loading="exporting" @click="handleExport" v-hasPermi="['evaluation:result:export']">导出</el-button>
      </el-form-item>
    </el-form>

    <el-empty v-if="!queryParams.activityId" description="请选择活动查看结果统计" />

    <template v-else>
      <div v-loading="summaryLoading" class="summary-grid">
        <div v-for="item in summaryItems" :key="item.label" class="summary-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="groupSummaryList"
        class="result-table"
        border
        show-summary
        :summary-method="getGroupSummaries"
      >
        <el-table-column label="序号" type="index" width="70" align="center" />
        <el-table-column label="二级部门" prop="department" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column label="三级部门" prop="thirdLevelDepartment" min-width="220" :show-overflow-tooltip="true" />
        <el-table-column label="申报等级" prop="appliedLevel" width="110" align="center" />
        <el-table-column label="申报人数" prop="candidateCount" width="110" align="center" />
        <el-table-column label="通过人数" prop="passCount" width="110" align="center" />
        <el-table-column label="淘汰人数" prop="rejectCount" width="110" align="center" />
        <el-table-column label="通过率" prop="passRate" width="110" align="center">
          <template #default="{ row }">{{ formatRate(row.passRate) }}</template>
        </el-table-column>
        <el-table-column label="淘汰率" prop="rejectRate" width="110" align="center">
          <template #default="{ row }">{{ formatRate(row.rejectRate) }}</template>
        </el-table-column>
        <el-table-column label="计算时间" prop="calculatedAt" width="170" align="center" />
      </el-table>

      <div class="section-header">
        <h3>导出记录</h3>
        <el-button link type="primary" icon="Refresh" @click="getExportJobs">刷新</el-button>
      </div>
      <el-table v-loading="jobLoading" :data="exportJobList" border>
        <el-table-column label="导出类型" prop="exportType" width="150" align="center">
          <template #default="{ row }">{{ formatExportType(row.exportType) }}</template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="jobStatusTag(row.status)">{{ formatJobStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件名" prop="fileName" min-width="220" :show-overflow-tooltip="true">
          <template #default="{ row }">
            <el-link v-if="row.fileUrl" type="primary" :href="row.fileUrl" target="_blank">{{ row.fileName || row.fileUrl }}</el-link>
            <span v-else>{{ row.fileName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="生成人" prop="generatedBy" width="120" align="center" />
        <el-table-column label="生成时间" prop="generatedAt" width="170" align="center" />
        <el-table-column label="错误信息" prop="errorMessage" min-width="220" :show-overflow-tooltip="true" />
      </el-table>
    </template>
  </div>
</template>

<script setup name="EvaluationResult">
import { calculate, exportResult, list as listResult, summary as getResultSummary } from '@/api/evaluation/result'
import { listExportJob } from '@/api/evaluation/exportJob'
import { listActivity } from '@/api/evaluation/activity'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const activityOptions = ref([])
const rawResultList = ref([])
const exportJobList = ref([])
const summaryData = ref({})
const loading = ref(false)
const summaryLoading = ref(false)
const jobLoading = ref(false)
const calculating = ref(false)
const exporting = ref(false)
const exportType = ref('FINAL_DECISION')
let skipNextActivatedRefresh = false

const exportTypeOptions = [
  { label: '最终总榜单', value: 'FINAL_DECISION' },
  { label: '统计结果', value: 'STAT_RESULT' },
  { label: '投票情况', value: 'VOTE_SUMMARY' },
  { label: '通过表决', value: 'PASS_DECISION' }
]

const data = reactive({
  queryParams: {
    activityId: undefined
  }
})

const { queryParams } = toRefs(data)

const selectedActivity = computed(() => {
  return activityOptions.value.find(item => String(item.id) === String(queryParams.value.activityId))
})

const groupSummaryList = computed(() => rawResultList.value || [])

const summaryItems = computed(() => {
  const summary = summaryData.value || {}
  return [
    { label: '活动名称', value: summary.activityName || selectedActivity.value?.name || '-' },
    { label: '活动状态', value: formatStatus(summary.activityStatus || summary.status || selectedActivity.value?.status) },
    { label: '候选人数', value: pickValue(summary, ['candidateCount', 'totalCandidates', 'total'], 0) },
    { label: '评委人数', value: pickValue(summary, ['voterCount', 'totalVoters'], 0) },
    { label: '已投评委', value: pickValue(summary, ['doneVoters', 'done'], 0) },
    { label: '未投评委', value: pickValue(summary, ['pendingVoters', 'pending'], 0) },
    { label: '汇总分组', value: groupSummaryList.value.length },
    { label: '计算时间', value: summary.calculatedAt || '-' }
  ]
})

function pickValue(source, keys, fallback) {
  if (!source) return fallback
  for (const key of keys) {
    if (source[key] !== undefined && source[key] !== null && source[key] !== '') {
      return source[key]
    }
  }
  return fallback
}

function loadActivities(refreshResult = false) {
  const currentActivityId = route.query.activityId || queryParams.value.activityId
  return listActivity({ pageNum: 1, pageSize: 1000, type: 'TITLE_REVIEW' }).then(response => {
    activityOptions.value = response.rows || []
    if (currentActivityId) {
      queryParams.value.activityId = Number(currentActivityId)
      if (refreshResult) {
        refreshAll()
      }
    }
  })
}

function reloadActivitiesAndResult() {
  loadActivities(true)
}

function loadInitialActivities() {
  loadActivities(true)
}

function refreshActiveActivities() {
  const currentActivityId = route.query.activityId || queryParams.value.activityId
  loadActivities(Boolean(currentActivityId))
}

onMounted(() => {
  skipNextActivatedRefresh = true
  loadInitialActivities()
})

onActivated(() => {
  if (skipNextActivatedRefresh) {
    skipNextActivatedRefresh = false
    return
  }
  refreshActiveActivities()
})

watch(() => route.query.activityId, value => {
  if (value && String(value) !== String(queryParams.value.activityId || '')) {
    queryParams.value.activityId = Number(value)
    refreshAll()
  }
  if (!value && queryParams.value.activityId) {
    queryParams.value.activityId = undefined
    rawResultList.value = []
    exportJobList.value = []
    summaryData.value = {}
  }
})

function handleActivityChange(value) {
  router.replace({ path: route.path, query: value ? { activityId: value } : {} })
  if (value) {
    refreshAll()
  } else {
    rawResultList.value = []
    exportJobList.value = []
    summaryData.value = {}
  }
}

function handleQuery() {
  refreshAll()
}

function refreshAll() {
  if (!queryParams.value.activityId) return
  getSummary()
  getResultList()
  getExportJobs()
}

function getSummary() {
  summaryLoading.value = true
  getResultSummary(queryParams.value.activityId).then(response => {
    summaryData.value = response.data || {}
  }).finally(() => {
    summaryLoading.value = false
  })
}

function getResultList() {
  if (!queryParams.value.activityId) return
  loading.value = true
  listResult({ activityId: queryParams.value.activityId }).then(response => {
    rawResultList.value = normalizeRows(response)
  }).finally(() => {
    loading.value = false
  })
}

function getExportJobs() {
  if (!queryParams.value.activityId) return
  jobLoading.value = true
  listExportJob({ pageNum: 1, pageSize: 20, activityId: queryParams.value.activityId, jobType: 'EXPORT' }).then(response => {
    exportJobList.value = response.rows || response.data?.rows || response.data || []
  }).finally(() => {
    jobLoading.value = false
  })
}

function handleCalculate() {
  if (!queryParams.value.activityId) return
  proxy.$modal.confirm('是否确认重新计算当前活动结果？').then(() => {
    calculating.value = true
    return calculate(queryParams.value.activityId)
  }).then(() => {
    proxy.$modal.msgSuccess('计算完成')
    refreshAll()
  }).catch(() => {
  }).finally(() => {
    calculating.value = false
  })
}

function handleExport() {
  if (!queryParams.value.activityId) return
  exporting.value = true
  exportResult(
    { activityId: queryParams.value.activityId, exportType: exportType.value },
    resultExportFileName(queryParams.value.activityId, exportType.value)
  ).then(() => {
    getExportJobs()
  }).finally(() => {
    exporting.value = false
  })
}

function resultExportFileName(activityId, type) {
  const extension = type === 'STAT_RESULT' ? 'zip' : 'xlsx'
  return `evaluation_result_${activityId}_${new Date().getTime()}.${extension}`
}

function normalizeRows(response) {
  const rows = response?.rows || response?.data?.rows || response?.data || []
  return Array.isArray(rows) ? rows : []
}

function getGroupSummaries({ columns, data }) {
  const sums = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (['candidateCount', 'passCount', 'rejectCount'].includes(column.property)) {
      sums[index] = data.reduce((total, row) => total + (Number(row[column.property]) || 0), 0)
      return
    }
    if (column.property === 'passRate') {
      sums[index] = formatRate(summaryRate(data, 'passCount'))
      return
    }
    if (column.property === 'rejectRate') {
      sums[index] = formatRate(summaryRate(data, 'rejectCount'))
      return
    }
    sums[index] = ''
  })
  return sums
}

function summaryRate(data, countField) {
  const candidateCount = data.reduce((total, row) => total + (Number(row.candidateCount) || 0), 0)
  const count = data.reduce((total, row) => total + (Number(row[countField]) || 0), 0)
  return candidateCount > 0 ? count / candidateCount : 0
}

function formatStatus(status) {
  const map = {
    DRAFT: '草稿',
    CONFIGURED: '已配置',
    PUBLISHED: '未开始',
    VOTING: '投票中',
    CLOSED: '已关闭',
    CALCULATED: '已计算',
    CONFIRMED: '已确认',
    EXPORTED: '已导出',
    ARCHIVED: '已归档'
  }
  return map[status] || status || '-'
}

function formatRate(value) {
  if (value === undefined || value === null || value === '') return '-'
  const number = Number(value)
  if (!Number.isFinite(number)) return value
  const normalized = Math.abs(number) <= 1 ? number * 100 : number
  return `${normalized.toFixed(2)}%`
}

function formatExportType(type) {
  return exportTypeOptions.find(item => item.value === type)?.label || type || '-'
}

function formatJobStatus(status) {
  const map = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    SUCCESS: '成功',
    PARTIAL_SUCCESS: '部分成功',
    FAILED: '失败'
  }
  return map[status] || status || '-'
}

function jobStatusTag(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PARTIAL_SUCCESS') return 'warning'
  return 'info'
}
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 12px 0 16px;
}

.summary-item {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  padding: 12px 14px;
  min-height: 66px;
}

.summary-item span {
  display: block;
  color: #606266;
  font-size: 12px;
  line-height: 18px;
}

.summary-item strong {
  display: block;
  margin-top: 6px;
  color: #1f2937;
  font-size: 18px;
  line-height: 24px;
  font-weight: 600;
  word-break: break-all;
}

.result-table {
  margin-top: 8px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 8px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
