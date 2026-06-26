<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="活动" prop="activityId">
        <el-select v-model="queryParams.activityId" placeholder="请选择活动" filterable clearable style="width: 260px" @change="handleActivityChange" @clear="handleActivityClear">
          <el-option v-for="item in activityOptions" :key="item.id" :label="formatActivityOption(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="组别" prop="department">
        <el-input v-model="queryParams.department" placeholder="请输入组别" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="等级" prop="level">
        <el-input v-model="queryParams.level" placeholder="请输入等级" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">预览</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="rule-panel">
          <template #header>
            <span>规则配置</span>
          </template>
          <el-form ref="ruleRef" :model="form" :rules="rules" label-width="110px">
            <el-form-item label="活动" prop="activityId">
              <el-select v-model="form.activityId" placeholder="请选择活动" filterable style="width: 100%" @change="handleFormActivityChange">
                <el-option v-for="item in activityOptions" :key="item.id" :label="formatActivityOption(item)" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="通过比例" prop="passRatio">
              <el-input-number v-model="form.passRatio" :min="0" :max="100" :precision="2" controls-position="right" disabled />
              <span class="unit-text">%</span>
            </el-form-item>
            <el-form-item label="淘汰比例" prop="rejectRatio">
              <el-input-number v-model="form.rejectRatio" :min="0" :max="100" :precision="2" controls-position="right" disabled />
              <span class="unit-text">%</span>
            </el-form-item>
            <el-form-item label="投票规则">
              <el-tag>通过 / 淘汰</el-tag>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="暂无规则备注" maxlength="300" show-word-limit disabled />
            </el-form-item>
          </el-form>
          <div class="rule-actions">
            <el-button type="primary" icon="Edit" @click="goWizard" v-hasPermi="['evaluation:rule:edit']">进入配置向导</el-button>
            <el-button icon="View" @click="handlePreview" v-hasPermi="['evaluation:rule:preview']">规则预览</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Refresh" @click="handlePreview" v-hasPermi="['evaluation:rule:preview']">刷新预览</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="handlePreview" />
        </el-row>

        <el-table v-loading="loading" :data="previewList">
          <el-table-column label="组别" prop="department" min-width="120" align="center" :show-overflow-tooltip="true" />
          <el-table-column label="申报等级" prop="appliedLevel" width="110" align="center" />
          <el-table-column label="申报人数" prop="candidateCount" width="100" align="center" />
          <el-table-column label="最多通过" prop="maxPassCount" width="130" align="center">
            <template #default="scope">
              {{ scope.row.maxPassCount ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="确定通过序号范围" prop="confirmedPassRange" min-width="150" align="center" />
          <el-table-column label="需要投票范围" prop="voteRange" min-width="130" align="center" />
          <el-table-column label="确定不通过序号范围" prop="confirmedRejectRange" min-width="160" align="center" />
          <el-table-column label="计划评委数" prop="plannedVoterCount" width="110" align="center" />
          <el-table-column label="状态" prop="status" width="110" align="center">
            <template #default="scope">
              <el-tag :type="statusTag(scope.row.status)">{{ formatStatus(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="handlePreview" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="EvaluationRule">
import { listActivity } from '@/api/evaluation/activity'
import { getRuleConfig, previewRule } from '@/api/evaluation/rule'

const router = useRouter()
const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const previewList = ref([])
const total = ref(0)
const activityOptions = ref([])

const data = reactive({
  form: {
    id: undefined,
    activityId: undefined,
    passRatio: 70,
    rejectRatio: 30,
    voteType: 'PASS_REJECT',
    remark: undefined
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    activityId: undefined,
    department: undefined,
    level: undefined
  },
  rules: {
    activityId: [{ required: true, message: '请选择活动', trigger: 'change' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

function formatStatus(status) {
  const map = { PENDING_CONFIRM: '待确认', READY: '已确认' }
  return map[status] || '未确认'
}

function statusTag(status) {
  const map = { PENDING_CONFIRM: 'warning', READY: 'success' }
  return map[status] || 'info'
}

function handleQuery() {
  queryParams.value.pageNum = 1
  handlePreview()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  form.value.activityId = undefined
  previewList.value = []
  total.value = 0
}

function loadActivityOptions() {
  listActivity({ pageNum: 1, pageSize: 100, type: 'TITLE_REVIEW' }).then(response => {
    activityOptions.value = response.rows || []
  })
}

function formatActivityOption(activity) {
  if (!activity) return ''
  return `${activity.name || '未命名活动'} #${activity.id}`
}

function handleActivityChange(activityId) {
  form.value.activityId = activityId
  loadRule(activityId)
}

function handleFormActivityChange(activityId) {
  queryParams.value.activityId = activityId
  loadRule(activityId)
}

function handleActivityClear() {
  form.value.activityId = undefined
  previewList.value = []
  total.value = 0
}

function goWizard() {
  router.push('/system/activity/wizard')
}

function loadRule(activityId) {
  if (!activityId) return
  getRuleConfig(activityId).then(response => {
    form.value = {
      id: response.data?.id,
      activityId,
      passRatio: response.data?.passRatio ?? 70,
      rejectRatio: response.data?.rejectRatio ?? 30,
      voteType: 'PASS_REJECT',
      remark: response.data?.remark
    }
  })
}

function handlePreview() {
  const activityId = queryParams.value.activityId || form.value.activityId
  if (!activityId) {
    proxy.$modal.msgWarning('请先选择活动')
    return
  }
  queryParams.value.activityId = activityId
  form.value.activityId = activityId
  loading.value = true
  previewRule(activityId, queryParams.value).then(response => {
    previewList.value = (response.rows || response.data || []).map(row => ({
      ...row,
      confirmedPassRange: row.confirmedPassRange ?? row.fixedPassRange ?? '-',
      confirmedRejectRange: row.confirmedRejectRange ?? row.fixedRejectRange ?? '-'
    }))
    total.value = response.total || previewList.value.length
    loading.value = false
    loadRule(activityId)
  }).catch(() => {
    loading.value = false
  })
}

onMounted(() => {
  loadActivityOptions()
})
</script>

<style scoped>
.rule-panel {
  margin-bottom: 16px;
}

.unit-text {
  margin-left: 8px;
  color: #606266;
}

.rule-actions {
  padding-left: 110px;
}
</style>
