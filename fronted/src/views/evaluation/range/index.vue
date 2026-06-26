<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="80px">
      <el-form-item label="活动" prop="activityId">
        <el-select v-model="queryParams.activityId" placeholder="请选择活动" filterable clearable style="width: 260px" @change="handleQuery">
          <el-option v-for="item in activityOptions" :key="item.id" :label="formatActivityOption(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="组别" prop="department">
        <el-input v-model="queryParams.department" placeholder="请输入组别" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="申报等级" prop="level">
        <el-input v-model="queryParams.level" placeholder="请输入申报等级" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="View" @click="getList" v-hasPermi="['evaluation:range:view']">刷新预览</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['evaluation:candidate:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-tabs v-model="activeTab" @tab-change="getList">
      <el-tab-pane label="报名统计" name="stats">
        <el-table v-loading="loading" :data="statsList" show-summary :summary-method="getStatsSummaries">
          <el-table-column label="申报等级" prop="appliedLevel" width="120" align="center" />
          <el-table-column label="组别" prop="department" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="申报人数" prop="candidateCount" width="110" align="center" />
          <el-table-column label="最高通过比例" prop="passRatio" width="120" align="center">
            <template #default="{ row }">{{ formatPercent(row.passRatio) }}</template>
          </el-table-column>
          <el-table-column label="最多通过人数" prop="maxPassCount" width="120" align="center" />
          <el-table-column label="锁定通过人数" prop="lockedPassCount" width="130" align="center" />
          <el-table-column label="确定通过序号范围" prop="confirmedPassRange" width="160" align="center" />
          <el-table-column label="需要投票范围" prop="voteRange" width="140" align="center" />
          <el-table-column label="投票不推荐人数" prop="minVoteRejectCount" width="150" align="center" />
          <el-table-column label="锁定不通过人数" prop="lockedRejectCount" width="140" align="center" />
          <el-table-column label="确定不通过序号范围" prop="confirmedRejectRange" width="170" align="center" />
          <el-table-column label="计划评委数" prop="plannedVoterCount" width="120" align="center" />
          <el-table-column label="状态" prop="status" width="120" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="投票范围预览" name="range">
        <el-table v-loading="loading" :data="rangeList" show-summary :summary-method="getRangeSummaries">
          <el-table-column label="申报等级" prop="appliedLevel" width="120" align="center" />
          <el-table-column label="组别" prop="department" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="申报人数" prop="candidateCount" width="110" align="center" />
          <el-table-column label="确定通过序号范围" prop="confirmedPassRange" width="160" align="center" />
          <el-table-column label="需要投票范围" prop="voteRange" width="150" align="center" />
          <el-table-column label="确定不通过序号范围" prop="confirmedRejectRange" width="170" align="center" />
          <el-table-column label="状态" prop="status" width="120" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="EvaluationRange">
import { listActivity } from '@/api/evaluation/activity'
import { listRangePreview, listRegistrationStats } from '@/api/evaluation/rule'

const { proxy } = getCurrentInstance()

const activeTab = ref('stats')
const loading = ref(false)
const showSearch = ref(true)
const statsList = ref([])
const rangeList = ref([])
const total = ref(0)
const activityOptions = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    activityId: undefined,
    department: undefined,
    level: undefined
  }
})

const { queryParams } = toRefs(data)

function loadActivityOptions() {
  listActivity({ pageNum: 1, pageSize: 100, type: 'TITLE_REVIEW' }).then(response => {
    activityOptions.value = response.rows || []
  })
}

function formatActivityOption(activity) {
  if (!activity) return ''
  return `${activity.name || '未命名活动'} #${activity.id}`
}

function normalizeRangeRows(rows) {
  return rows.map(row => ({
    ...row,
    confirmedPassRange: row.confirmedPassRange ?? row.fixedPassRange ?? '-',
    confirmedRejectRange: row.confirmedRejectRange ?? row.fixedRejectRange ?? '-'
  }))
}

function getList() {
  if (!queryParams.value.activityId) {
    proxy.$modal.msgWarning('请先选择活动')
    return
  }
  loading.value = true
  const request = activeTab.value === 'stats' ? listRegistrationStats(queryParams.value) : listRangePreview(queryParams.value)
  request.then(response => {
    const rows = normalizeRangeRows(response.rows || response.data || [])
    if (activeTab.value === 'stats') {
      statsList.value = rows
    } else {
      rangeList.value = rows
    }
    total.value = response.total || rows.length
  }).finally(() => {
    loading.value = false
  })
}

function formatPercent(value) {
  if (value === undefined || value === null || value === '') return '-'
  const number = Number(value)
  if (!Number.isFinite(number)) return `${value}%`
  return `${Number.isInteger(number) ? number : number.toFixed(2).replace(/\.?0+$/, '')}%`
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.pageNum = 1
  statsList.value = []
  rangeList.value = []
  total.value = 0
}

function getStatsSummaries({ columns, data }) {
  const sumProps = new Set([
    'candidateCount',
    'maxPassCount',
    'lockedPassCount',
    'lockedRejectCount',
    'voteCount',
    'minVoteRejectCount',
    'plannedVoterCount'
  ])
  return columns.map((column, index) => {
    if (index === 0) return '合计'
    if (!sumProps.has(column.property)) return ''
    if (column.property === 'plannedVoterCount') {
      return sumPlannedVoterCountByDepartment(data)
    }
    return data.reduce((total, row) => total + Number(row[column.property] || 0), 0)
  })
}

function sumPlannedVoterCountByDepartment(data) {
  const counted = new Set()
  return data.reduce((total, row) => {
    const department = row.department || ''
    if (counted.has(department)) {
      return total
    }
    counted.add(department)
    return total + Number(row.plannedVoterCount || 0)
  }, 0)
}

function getRangeSummaries({ columns, data }) {
  return columns.map((column, index) => {
    if (index === 0) return '合计'
    if (column.property === 'candidateCount') {
      return data.reduce((total, row) => total + Number(row.candidateCount || 0), 0)
    }
    return ''
  })
}

function handleExport() {
  proxy.download('evaluation/rule/rangeExport', { ...queryParams.value, previewType: activeTab.value }, `range_${new Date().getTime()}.xlsx`)
}

loadActivityOptions()
</script>
