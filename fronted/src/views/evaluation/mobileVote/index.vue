<template>
  <div class="vote-page">
    <div class="vote-shell">
      <div v-if="loading" class="state-box">
        <el-icon class="is-loading"><Loading /></el-icon>
        <p>正在加载投票信息...</p>
      </div>

      <el-result v-else-if="invalid" icon="warning" title="暂不可投票" :sub-title="invalidReason">
        <template #extra>
          <el-button @click="loadEntry">重新加载</el-button>
        </template>
      </el-result>

      <template v-else-if="submitted || alreadySubmitted">
        <el-result icon="success" title="投票已提交" :sub-title="submittedText">
          <template #extra>
            <el-button type="primary" :loading="resultLoading" @click="loadResult">刷新结果</el-button>
          </template>
        </el-result>
        <section class="panel result-panel">
          <el-alert v-if="!resultData.allCompleted" title="等待所有评委提交后查看结果" type="info" show-icon :closable="false" />
          <el-alert v-else-if="!isCalculatedResult()" title="全员已完成投票，等待 HR 统计结果" type="success" show-icon :closable="false" />
          <template v-else>
            <div class="vote-title">
              <h2>评审结果</h2>
            </div>
            <div v-for="item in resultRows()" :key="item.activityCandidateId || item.id" class="result-item">
              <div>
                <div class="candidate-name">{{ item.name }}</div>
                <div class="candidate-meta">
                  <span>排名：{{ item.rankNo || '-' }}</span>
                  <span>{{ item.department || '-' }}</span>
                  <span>{{ item.appliedLevel || '-' }}</span>
                </div>
              </div>
              <div class="result-counts">
                <el-tag :type="finalResultTag(item.finalResult)">{{ formatFinalResult(item.finalResult) }}</el-tag>
                <span>通过率 {{ formatRate(item.passRate) }}</span>
                <span>淘汰率 {{ formatRate(item.rejectRate) }}</span>
              </div>
            </div>
          </template>
        </section>
      </template>

      <template v-else>
        <header class="vote-header">
          <div class="activity-name">{{ entry.activity?.name }}</div>
          <div class="activity-meta">{{ entry.activity?.startTime || '-' }} 至 {{ entry.activity?.endTime || '-' }}</div>
        </header>

        <section v-if="!confirmed" class="panel">
          <h2>身份确认</h2>
          <div v-if="!entry.sharedEntry" class="identity-card">
            <div>
              <span>姓名</span>
              <strong>{{ entry.voter?.name || '-' }}</strong>
            </div>
            <div>
              <span>工号</span>
              <strong>{{ entry.voter?.employeeId || '-' }}</strong>
            </div>
            <div>
              <span>部门</span>
              <strong>{{ entry.voter?.department || '-' }}</strong>
            </div>
          </div>
          <el-form ref="confirmRef" :model="confirmForm" :rules="confirmRules" label-position="top">
            <el-form-item label="请输入姓名" prop="name">
              <el-input v-model="confirmForm.name" placeholder="与评委名单一致" />
            </el-form-item>
            <el-form-item label="请输入工号" prop="employeeId">
              <el-input v-model="confirmForm.employeeId" placeholder="与评委名单一致" />
            </el-form-item>
          </el-form>
          <el-button type="primary" size="large" class="wide-button" :loading="confirming" @click="confirmIdentity">确认并开始投票</el-button>
        </section>

        <section v-else class="panel">
          <div class="vote-title">
            <h2>候选人投票</h2>
            <span>{{ answeredCount }}/{{ candidates.length }}</span>
          </div>
          <div class="quick-actions">
            <el-button size="small" type="success" plain @click="fillAllVotes('PASS')">一键通过</el-button>
            <el-button size="small" type="danger" plain @click="fillAllVotes('REJECT')">一键淘汰</el-button>
            <el-button size="small" plain @click="clearVotes">清空</el-button>
          </div>
          <el-empty v-if="!candidates.length" description="当前活动没有需要投票的候选人" />
          <div v-for="candidate in candidates" :key="candidate.activityCandidateId" class="candidate-item">
            <div class="candidate-main">
              <div class="candidate-heading">
                <div class="candidate-name">{{ candidate.name }}</div>
                <el-tag class="applied-level-tag" type="primary" effect="plain">申请等级：{{ candidate.appliedLevel || '-' }}</el-tag>
              </div>
              <div class="candidate-meta">
                <span>{{ candidate.company || '-' }}</span>
                <span>{{ candidate.department || '-' }}</span>
                <span>{{ candidate.position || '-' }}</span>
                <span>当前等级：{{ candidate.currentLevel || '-' }}</span>
              </div>
            </div>
            <el-radio-group v-model="votes[candidate.activityCandidateId]" class="decision">
              <el-radio-button label="PASS">通过</el-radio-button>
              <el-radio-button label="REJECT">淘汰</el-radio-button>
            </el-radio-group>
          </div>
          <el-button
            type="primary"
            size="large"
            class="wide-button"
            :disabled="!canSubmit"
            :loading="submitting"
            @click="confirmSubmit"
          >
            提交投票
          </el-button>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup name="EvaluationMobileVote">
import { Loading } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { confirmVoteIdentity, getVoteEntry, getVoteResult, listVoteCandidates, submitVote } from '@/api/evaluation/vote'

const route = useRoute()
const token = computed(() => route.params.token)
const activeToken = ref('')
const loading = ref(true)
const invalid = ref(false)
const invalidReason = ref('')
const submitted = ref(false)
const alreadySubmitted = ref(false)
const confirmed = ref(false)
const confirming = ref(false)
const submitting = ref(false)
const resultLoading = ref(false)
const entry = ref({})
const resultData = ref({})
const candidates = ref([])
const votes = reactive({})
const confirmRef = ref()
let resultTimer = null

const confirmForm = reactive({
  name: '',
  employeeId: ''
})

const confirmRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  employeeId: [{ required: true, message: '请输入工号', trigger: 'blur' }]
}

const answeredCount = computed(() => candidates.value.filter(item => votes[item.activityCandidateId]).length)
const canSubmit = computed(() => candidates.value.length > 0 && answeredCount.value === candidates.value.length)
const voteAccessToken = computed(() => activeToken.value || token.value)
const submittedText = computed(() => {
  const time = entry.value.submittedAt || entry.value.voter?.submittedAt
  return time ? `提交时间：${time}` : '提交后不可修改'
})

function loadEntry() {
  loading.value = true
  invalid.value = false
  invalidReason.value = ''
  activeToken.value = ''
  confirmed.value = false
  candidates.value = []
  resultData.value = {}
  stopResultAutoRefresh()
  resetVotes()
  getVoteEntry(token.value).then(response => {
    entry.value = response.data || {}
    submitted.value = !!entry.value.submitted
    alreadySubmitted.value = !!entry.value.submitted
    if (submitted.value && entry.value.voter?.voteToken) {
      activeToken.value = entry.value.voter.voteToken
      loadResult()
    }
    if (!entry.value.canVote && !entry.value.submitted) {
      invalid.value = true
      invalidReason.value = entry.value.unavailableReason || '活动当前不可投票'
    }
  }).catch(error => {
    invalid.value = true
    invalidReason.value = error?.message || '投票链接无效'
  }).finally(() => {
    loading.value = false
  })
}

function confirmIdentity() {
  confirmRef.value.validate(valid => {
    if (!valid) return
    confirming.value = true
    confirmVoteIdentity(token.value, confirmForm).then(response => {
      entry.value = response.data || entry.value
      activeToken.value = entry.value.voter?.voteToken || token.value
      submitted.value = !!entry.value.submitted
      alreadySubmitted.value = !!entry.value.submitted
      confirmed.value = true
      if (submitted.value) {
        loadResult()
      } else {
        loadCandidates()
      }
    }).finally(() => {
      confirming.value = false
    })
  })
}

function loadCandidates() {
  listVoteCandidates(voteAccessToken.value).then(response => {
    candidates.value = response.data || []
    candidates.value.forEach(item => {
      if (!Object.prototype.hasOwnProperty.call(votes, item.activityCandidateId)) {
        votes[item.activityCandidateId] = undefined
      }
    })
  })
}

function fillAllVotes(result) {
  candidates.value.forEach(item => {
    votes[item.activityCandidateId] = result
  })
}

function clearVotes() {
  candidates.value.forEach(item => {
    votes[item.activityCandidateId] = undefined
  })
}

function confirmSubmit() {
  if (!canSubmit.value) return
  ElMessageBox.confirm('提交后不可修改，是否确认提交？', '确认提交', {
    confirmButtonText: '确认提交',
    cancelButtonText: '继续检查',
    type: 'warning'
  }).then(() => {
    submitting.value = true
    const items = candidates.value.map(item => ({
      activityCandidateId: item.activityCandidateId,
      result: votes[item.activityCandidateId]
    }))
    return submitVote(voteAccessToken.value, { roundNo: 1, votes: items })
  }).then(response => {
    submitted.value = true
    alreadySubmitted.value = true
    entry.value = { ...entry.value, ...(response.data || {}) }
    loadResult()
  }).catch(() => {
  }).finally(() => {
    submitting.value = false
  })
}

function loadResult() {
  if (!voteAccessToken.value) return
  resultLoading.value = true
  getVoteResult(voteAccessToken.value).then(response => {
    resultData.value = response.data || {}
    if (isCalculatedResult()) {
      stopResultAutoRefresh()
    } else {
      startResultAutoRefresh()
    }
  }).finally(() => {
    resultLoading.value = false
  })
}

function startResultAutoRefresh() {
  if (resultTimer || !(submitted.value || alreadySubmitted.value)) return
  resultTimer = window.setInterval(() => {
    loadResult()
  }, 10000)
}

function stopResultAutoRefresh() {
  if (resultTimer) {
    window.clearInterval(resultTimer)
    resultTimer = null
  }
}

function resetVotes() {
  Object.keys(votes).forEach(key => {
    delete votes[key]
  })
}

function isCalculatedResult() {
  const status = resultData.value.activityStatus || resultData.value.status || resultData.value.activity?.status
  return resultData.value.calculated === true ||
    resultData.value.resultCalculated === true ||
    ['CALCULATED', 'CONFIRMED', 'EXPORTED'].includes(status)
}

function resultRows() {
  return resultData.value.resultRows || resultData.value.rows || []
}

function formatFinalResult(result) {
  const map = {
    PASS: '通过',
    REJECT: '不通过',
    PENDING: '待定'
  }
  return map[result] || result || '-'
}

function finalResultTag(result) {
  if (result === 'PASS') return 'success'
  if (result === 'REJECT') return 'danger'
  return 'info'
}

function formatRate(value) {
  if (value === undefined || value === null || value === '') return '-'
  const number = Number(value)
  if (!Number.isFinite(number)) return value
  const normalized = Math.abs(number) <= 1 ? number * 100 : number
  return `${normalized.toFixed(2)}%`
}

onBeforeUnmount(() => {
  stopResultAutoRefresh()
})

loadEntry()
</script>

<style scoped lang="scss">
.vote-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
}

.vote-shell {
  max-width: 720px;
  margin: 0 auto;
}

.state-box {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #606266;
  gap: 10px;
}

.vote-header {
  padding: 18px 4px 12px;
}

.activity-name {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
}

.activity-meta {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 18px;
}

.panel h2 {
  margin: 0 0 16px;
  font-size: 18px;
}

.identity-card {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.identity-card div {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 10px;
}

.identity-card span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.identity-card strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 15px;
}

.vote-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.vote-title h2 {
  margin: 0;
}

.vote-title span {
  color: #409eff;
  font-weight: 600;
}

.candidate-item {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 14px;
  margin-bottom: 12px;
}

.candidate-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.candidate-name {
  font-weight: 700;
  color: #111827;
  font-size: 16px;
}

.applied-level-tag {
  flex: none;
}

.candidate-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.decision {
  margin-top: 12px;
  width: 100%;
}

.decision :deep(.el-radio-button) {
  width: 50%;
}

.decision :deep(.el-radio-button__inner) {
  width: 100%;
}

.wide-button {
  width: 100%;
  margin-top: 8px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.result-panel {
  margin-top: 12px;
}

.result-item {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-counts {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 520px) {
  .vote-page {
    padding: 10px;
  }

  .identity-card {
    grid-template-columns: 1fr;
  }

  .panel {
    padding: 14px;
  }

  .result-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
