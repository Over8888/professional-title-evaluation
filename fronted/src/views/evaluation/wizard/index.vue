<template>
  <div class="app-container wizard-page">
    <el-page-header title="返回活动管理" content="职称评审活动配置" @back="router.push('/system/activity')" />

    <el-steps :active="activeStep" finish-status="success" class="wizard-steps">
      <el-step title="活动与规则" />
      <el-step title="候选人" />
      <el-step title="评委" />
      <el-step title="范围确认" />
      <el-step title="发布" />
    </el-steps>

    <section v-show="activeStep === 0" class="panel">
      <el-form ref="baseRef" :model="baseForm" :rules="baseRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="活动名称" prop="name">
              <el-input v-model="baseForm.name" placeholder="请输入活动名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动类型">
              <el-input value="职称评审" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="baseForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="baseForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最高通过比例" prop="passRatio">
              <el-input-number v-model="ruleForm.passRatio" :min="0" :max="100" :precision="2" controls-position="right" />
              <span class="unit">%</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低淘汰比例" prop="rejectRatio">
              <el-input-number v-model="ruleForm.rejectRatio" :min="0" :max="100" :precision="2" controls-position="right" />
              <span class="unit">%</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="投票规则">
          <el-tag>通过 / 淘汰</el-tag>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="baseForm.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="activityId"
        :title="`当前活动已创建，编号 ${activityId}。再次保存只更新当前活动与规则。`"
        type="success"
        show-icon
        :closable="false"
      />
      <div class="actions">
        <el-button type="primary" :loading="savingBase" :disabled="savingBase" @click="createBase">
          {{ activityId ? '保存并进入候选人' : '创建活动并进入候选人' }}
        </el-button>
      </div>
    </section>

    <section v-show="activeStep === 1" class="panel">
      <div class="toolbar">
        <el-radio-group v-model="candidateMode">
          <el-radio-button label="pool">从资料库选择</el-radio-button>
          <el-radio-button label="import">导入当前活动</el-radio-button>
          <el-radio-button label="copy">复制历史活动</el-radio-button>
        </el-radio-group>
        <el-button type="primary" plain @click="refreshCandidates">刷新列表</el-button>
      </div>

      <div v-if="candidateMode === 'pool'" class="source-block">
        <el-table v-loading="poolCandidateLoading" :data="poolCandidates" @selection-change="candidateSelection = $event">
          <el-table-column type="selection" width="48" />
          <el-table-column label="姓名" prop="name" />
          <el-table-column label="身份证号" prop="idCard" width="180">
            <template #default="scope">{{ maskIdCard(scope.row.idCard) }}</template>
          </el-table-column>
          <el-table-column label="组别/专业" prop="department" />
          <el-table-column label="申报等级" prop="appliedLevel" width="110" />
          <el-table-column label="序号" prop="importSeq" width="90" />
        </el-table>
        <div class="actions">
          <el-checkbox v-model="replaceCandidates">替换当前活动候选人</el-checkbox>
          <el-button type="primary" @click="selectCandidates">加入当前活动</el-button>
        </div>
      </div>

      <div v-if="candidateMode === 'import'" class="source-block">
        <el-button type="primary" icon="Upload" :disabled="!activityId" @click="candidateImportRef.open()">导入当前活动候选人</el-button>
        <el-button plain icon="Upload" @click="candidatePoolImportRef.open()">导入候选人资料库</el-button>
      </div>

      <div v-if="candidateMode === 'copy'" class="source-block inline-copy">
        <el-select v-model="candidateSourceActivityId" placeholder="选择历史活动" filterable clearable>
          <el-option v-for="item in activityOptions" :key="item.id" :label="item.name + ' #' + item.id" :value="item.id" />
        </el-select>
        <el-checkbox v-model="replaceCandidates">替换当前活动候选人</el-checkbox>
        <el-button type="primary" @click="copyCandidates">复制候选人</el-button>
      </div>

      <el-divider />
      <el-table v-loading="candidateLoading" :data="activityCandidates">
        <el-table-column label="姓名" prop="name" />
        <el-table-column label="身份证号" prop="idCard" width="180">
          <template #default="scope">{{ maskIdCard(scope.row.idCard) }}</template>
        </el-table-column>
        <el-table-column label="组别/专业" prop="department" />
        <el-table-column label="申报等级" prop="appliedLevel" width="110" />
        <el-table-column label="序号" prop="importSeq" width="90" />
      </el-table>
      <div class="actions">
        <el-button @click="activeStep = 0">上一步</el-button>
        <el-button type="primary" @click="goVoters">下一步</el-button>
      </div>
    </section>

    <section v-show="activeStep === 2" class="panel">
      <div class="toolbar">
        <el-radio-group v-model="voterMode">
          <el-radio-button label="pool">从资料库选择</el-radio-button>
          <el-radio-button label="import">导入当前活动</el-radio-button>
          <el-radio-button label="copy">复制历史活动</el-radio-button>
        </el-radio-group>
        <el-button type="primary" plain @click="refreshVoters">刷新列表</el-button>
      </div>

      <div v-if="voterMode === 'pool'" class="source-block">
        <el-table v-loading="poolVoterLoading" :data="poolVoters" @selection-change="voterSelection = $event">
          <el-table-column type="selection" width="48" />
          <el-table-column label="姓名" prop="name" />
          <el-table-column label="工号" prop="employeeId" />
          <el-table-column label="部门" prop="department" />
        </el-table>
        <div class="actions">
          <el-checkbox v-model="replaceVoters">替换当前活动评委</el-checkbox>
          <el-button type="primary" @click="selectVoters">加入当前活动</el-button>
        </div>
      </div>

      <div v-if="voterMode === 'import'" class="source-block">
        <el-button type="primary" icon="Upload" :disabled="!activityId" @click="voterImportRef.open()">导入当前活动评委</el-button>
        <el-button plain icon="Upload" @click="voterPoolImportRef.open()">导入评委资料库</el-button>
      </div>

      <div v-if="voterMode === 'copy'" class="source-block inline-copy">
        <el-select v-model="voterSourceActivityId" placeholder="选择历史活动" filterable clearable>
          <el-option v-for="item in activityOptions" :key="item.id" :label="item.name + ' #' + item.id" :value="item.id" />
        </el-select>
        <el-checkbox v-model="replaceVoters">替换当前活动评委</el-checkbox>
        <el-button type="primary" @click="copyVoters">复制评委</el-button>
      </div>

      <el-divider />
      <el-table v-loading="voterLoading" :data="activityVoters">
        <el-table-column label="姓名" prop="name" />
        <el-table-column label="工号" prop="employeeId" />
        <el-table-column label="部门" prop="department" />
      </el-table>
      <div class="actions">
        <el-button @click="activeStep = 1">上一步</el-button>
        <el-button type="primary" @click="goPreview">下一步</el-button>
      </div>
    </section>

    <section v-show="activeStep === 3" class="panel">
      <div class="toolbar">
        <el-alert title="按组别和申报等级统计；排序前段固定通过，末尾固定不通过，中间候选人进入投票范围。" type="info" show-icon :closable="false" />
        <el-button type="primary" plain @click="loadPreview">刷新预览</el-button>
      </div>
      <el-table v-loading="previewLoading" :data="previewList" show-summary :summary-method="getPreviewSummaries">
        <el-table-column label="申报等级" prop="appliedLevel" width="120" align="center" />
        <el-table-column label="组别/专业" prop="department" min-width="140" align="center" />
        <el-table-column label="申报人数" prop="candidateCount" width="100" align="center" />
        <el-table-column label="最高通过比例" prop="passRatio" width="120" align="center">
          <template #default="{ row }">{{ formatPercent(row.passRatio) }}</template>
        </el-table-column>
        <el-table-column label="最多通过人数" prop="maxPassCount" width="120" align="center" />
        <el-table-column label="锁定通过人数" prop="lockedPassCount" width="150" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.lockedPassCount"
              :min="0"
              :max="Number(row.maxPassCount || 0)"
              :controls="false"
              size="small"
              class="count-input"
              @change="recalculatePreviewRow(row, 'lockedPassCount')"
            />
          </template>
        </el-table-column>
        <el-table-column label="确定通过序号范围" prop="confirmedPassRange" width="160" align="center" />
        <el-table-column label="需要投票范围" prop="voteRange" width="140" align="center" />
        <el-table-column label="投票不推荐人数" prop="minVoteRejectCount" width="150" align="center" />
        <el-table-column label="锁定不通过人数" prop="lockedRejectCount" width="160" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.lockedRejectCount"
              :min="0"
              :max="Math.max(0, Number(row.candidateCount || 0) - Number(row.lockedPassCount || 0) - 1)"
              :controls="false"
              size="small"
              class="count-input"
              @change="recalculatePreviewRow(row, 'lockedRejectCount')"
            />
          </template>
        </el-table-column>
        <el-table-column label="确定不通过序号范围" prop="confirmedRejectRange" width="170" align="center" />
        <el-table-column label="计划评委数" prop="plannedVoterCount" width="110" align="center" />
      </el-table>
      <div class="actions">
        <el-button @click="activeStep = 2">上一步</el-button>
        <el-button type="primary" :disabled="!previewList.length" @click="applyRanges">确认并应用范围</el-button>
      </div>
    </section>

    <section v-show="activeStep === 4" class="panel publish-panel">
      <el-result icon="success" title="配置完成" sub-title="确认候选人、评委和规则范围后可以发布活动。">
        <template #extra>
          <el-button @click="activeStep = 3">上一步</el-button>
          <el-button type="primary" :loading="publishing" @click="publish">发布活动</el-button>
        </template>
      </el-result>
    </section>

    <el-dialog
      v-model="voteEntryOpen"
      title="投票入口已生成"
      width="640px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-alert
        title="请先复制投票链接。离开后配置向导将重新开始一个活动设置，不再保留当前发布页面。"
        type="warning"
        show-icon
        :closable="false"
      />
      <div class="vote-entry-dialog-body">
        <div class="vote-entry-label">所有评委共用同一个投票入口，进入后通过姓名和工号确认身份。</div>
        <div class="vote-entry-qr-wrap">
          <img v-if="voteQrCodeDataUrl" :src="voteQrCodeDataUrl" alt="投票入口二维码" class="vote-entry-qr" />
          <el-alert v-else title="二维码暂无法生成，请使用下方投票链接。" type="warning" show-icon :closable="false" />
        </div>
        <div class="vote-entry-qr-actions">
          <el-button :disabled="!voteQrCodeDataUrl" @click="copyVoteQrCode">复制二维码</el-button>
        </div>
        <el-input ref="voteEntryInputRef" v-model="sharedVoteUrl" readonly class="vote-entry-url">
          <template #append>
            <el-button :disabled="!sharedVoteUrl" @click="copyVoteEntry">复制链接</el-button>
          </template>
        </el-input>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="!voteQrCodeDataUrl" @click="copyVoteQrCode">复制二维码</el-button>
          <el-button :disabled="!sharedVoteUrl" @click="copyVoteEntry">复制链接</el-button>
          <el-button type="primary" :disabled="!voteEntryCopied" @click="goNoticeAfterPublish">去通知公告</el-button>
        </div>
      </template>
    </el-dialog>

    <excel-import-dialog ref="candidateImportRef" title="导入当前活动候选人" :action="candidateImportAction" template-action="/evaluation/candidate/importTemplate" template-file-name="candidate_template" update-support-label="替换当前活动候选人" @success="handleCandidateImportSuccess" />
    <excel-import-dialog ref="candidatePoolImportRef" title="导入候选人资料库" action="/evaluation/candidate/pool/importData" template-action="/evaluation/candidate/importTemplate" template-file-name="candidate_pool_template" update-support-label="替换候选人资料库" @success="loadPoolCandidates" />
    <excel-import-dialog ref="voterImportRef" title="导入当前活动评委" :action="voterImportAction" template-action="/evaluation/voter/importTemplate" template-file-name="voter_template" update-support-label="替换当前活动评委" @success="refreshVoters" />
    <excel-import-dialog ref="voterPoolImportRef" title="导入评委资料库" action="/evaluation/voter/pool/importData" template-action="/evaluation/voter/importTemplate" template-file-name="voter_pool_template" update-support-label="替换评委资料库" @success="loadPoolVoters" />
  </div>
</template>

<script setup name="EvaluationWizard">
import ExcelImportDialog from '@/components/ExcelImportDialog'
import { createActivityWithRule, listActivity, publishActivity, updateActivity } from '@/api/evaluation/activity'
import { listCandidate, selectCandidateFromPool, copyCandidateFromActivity } from '@/api/evaluation/candidate'
import { listVoter, selectVoterFromPool, copyVoterFromActivity } from '@/api/evaluation/voter'
import { previewRule, applyConfirmedRule, saveRuleConfig } from '@/api/evaluation/rule'
import { copyQrCodeImage, createQrCodeDataUrl } from '@/utils/qrcode'

const router = useRouter()
const { proxy } = getCurrentInstance()

const activeStep = ref(0)
const activityId = ref(undefined)
const savingBase = ref(false)
const publishing = ref(false)
const sharedVoteUrl = ref('')
const voteQrCodeDataUrl = ref('')
const voteEntryOpen = ref(false)
const voteEntryCopied = ref(false)
const voteEntryInputRef = ref()
let initialized = false
let skipNextActivatedReset = false

const baseRef = ref()
const baseForm = reactive({
  name: '',
  type: 'TITLE_REVIEW',
  startTime: '',
  endTime: '',
  remark: ''
})
const ruleForm = reactive({
  passRatio: 0,
  rejectRatio: 0,
  voteType: 'PASS_REJECT'
})

const validateTime = (rule, value, callback) => {
  if (baseForm.startTime && baseForm.endTime && baseForm.endTime < baseForm.startTime) {
    callback(new Error('结束时间不能早于开始时间'))
  } else if (baseForm.endTime && new Date(baseForm.endTime.replace(/-/g, '/')).getTime() <= Date.now()) {
    callback(new Error('结束时间必须晚于当前时间'))
  } else {
    callback()
  }
}

const baseRules = {
  name: [{ required: true, message: '活动名称不能为空', trigger: 'blur' }],
  startTime: [{ validator: validateTime, trigger: 'change' }],
  endTime: [{ validator: validateTime, trigger: 'change' }]
}

const candidateMode = ref('pool')
const voterMode = ref('pool')
const replaceCandidates = ref(false)
const replaceVoters = ref(false)
const candidateSelection = ref([])
const voterSelection = ref([])
const poolCandidates = ref([])
const activityCandidates = ref([])
const poolVoters = ref([])
const activityVoters = ref([])
const activityOptions = ref([])
const candidateSourceActivityId = ref()
const voterSourceActivityId = ref()

const poolCandidateLoading = ref(false)
const candidateLoading = ref(false)
const poolVoterLoading = ref(false)
const voterLoading = ref(false)
const previewLoading = ref(false)
const previewList = ref([])

const candidateImportRef = ref()
const candidatePoolImportRef = ref()
const voterImportRef = ref()
const voterPoolImportRef = ref()

const candidateImportAction = computed(() => `/evaluation/candidate/importData?activityId=${activityId.value || ''}`)
const voterImportAction = computed(() => `/evaluation/voter/importData?activityId=${activityId.value || ''}`)

function maskIdCard(idCard) {
  if (!idCard) return '-'
  const value = String(idCard)
  if (value.length < 10) return value
  return value.slice(0, 6) + '********' + value.slice(-4)
}

function resetWizardState() {
  activeStep.value = 0
  activityId.value = undefined
  savingBase.value = false
  publishing.value = false
  sharedVoteUrl.value = ''
  voteQrCodeDataUrl.value = ''
  voteEntryOpen.value = false
  voteEntryCopied.value = false
  Object.assign(baseForm, {
    name: '',
    type: 'TITLE_REVIEW',
    startTime: '',
    endTime: '',
    remark: ''
  })
  Object.assign(ruleForm, {
    passRatio: 0,
    rejectRatio: 0,
    voteType: 'PASS_REJECT'
  })
  candidateMode.value = 'pool'
  voterMode.value = 'pool'
  replaceCandidates.value = false
  replaceVoters.value = false
  candidateSelection.value = []
  voterSelection.value = []
  poolCandidates.value = []
  activityCandidates.value = []
  activityOptions.value = []
  poolVoters.value = []
  activityVoters.value = []
  candidateSourceActivityId.value = undefined
  voterSourceActivityId.value = undefined
  poolCandidateLoading.value = false
  candidateLoading.value = false
  poolVoterLoading.value = false
  voterLoading.value = false
  previewLoading.value = false
  previewList.value = []
  nextTick(() => {
    baseRef.value?.clearValidate?.()
  })
}

function createBase() {
  if (savingBase.value) return
  savingBase.value = true
  baseRef.value.validate(valid => {
    if (!valid) {
      savingBase.value = false
      return
    }
    if ((Number(ruleForm.passRatio) + Number(ruleForm.rejectRatio)) > 100) {
      proxy.$modal.msgWarning('最高通过比例和最低淘汰比例之和不能大于 100%')
      savingBase.value = false
      return
    }
    const request = activityId.value ? Promise.all([
      updateActivity({
        id: activityId.value,
        ...baseForm,
        type: 'TITLE_REVIEW'
      }),
      saveRuleConfig({
        activityId: activityId.value,
        ...ruleForm,
        voteType: 'PASS_REJECT'
      })
    ]) : createActivityWithRule({
      activity: { ...baseForm, type: 'TITLE_REVIEW' },
      ruleConfig: { ...ruleForm, voteType: 'PASS_REJECT' }
    })
    request.then(res => {
      if (!activityId.value) {
        activityId.value = res.data.id
        proxy.$modal.msgSuccess('当前活动已创建')
      } else {
        proxy.$modal.msgSuccess('当前活动与规则已保存')
      }
      activeStep.value = 1
      refreshActivityOptions()
      refreshCandidates()
      loadPoolCandidates()
    }).finally(() => {
      savingBase.value = false
    })
  })
}

function refreshActivityOptions() {
  listActivity({ pageNum: 1, pageSize: 100, type: 'TITLE_REVIEW' }).then(res => {
    activityOptions.value = (res.rows || []).filter(item => item.id !== activityId.value)
  })
}

function loadPoolCandidates() {
  poolCandidateLoading.value = true
  listCandidate({ pageNum: 1, pageSize: 200, pool: true }).then(res => {
    poolCandidates.value = res.rows || []
  }).finally(() => {
    poolCandidateLoading.value = false
  })
}

function refreshCandidates() {
  if (!activityId.value) return
  candidateLoading.value = true
  listCandidate({ pageNum: 1, pageSize: 500, activityId: activityId.value }).then(res => {
    activityCandidates.value = res.rows || []
  }).finally(() => {
    candidateLoading.value = false
  })
}

function handleCandidateImportSuccess(result) {
  if (result?.activityId) {
    activityId.value = result.activityId
  }
  refreshCandidates()
}

function selectCandidates() {
  const ids = candidateSelection.value.map(item => item.id)
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择候选人资料')
    return
  }
  selectCandidateFromPool(activityId.value, { ids, replaceExisting: replaceCandidates.value }).then(() => {
    proxy.$modal.msgSuccess('候选人已加入活动')
    refreshCandidates()
  })
}

function copyCandidates() {
  if (!candidateSourceActivityId.value) {
    proxy.$modal.msgWarning('请选择历史活动')
    return
  }
  copyCandidateFromActivity(activityId.value, candidateSourceActivityId.value, { replaceExisting: replaceCandidates.value }).then(() => {
    proxy.$modal.msgSuccess('候选人复制完成')
    refreshCandidates()
  })
}

function goVoters() {
  if (!activityCandidates.value.length) {
    proxy.$modal.msgWarning('请先选择或导入候选人')
    return
  }
  activeStep.value = 2
  refreshVoters()
  loadPoolVoters()
  refreshActivityOptions()
}

function loadPoolVoters() {
  poolVoterLoading.value = true
  listVoter({ pageNum: 1, pageSize: 200, pool: true }).then(res => {
    poolVoters.value = res.rows || []
  }).finally(() => {
    poolVoterLoading.value = false
  })
}

function refreshVoters() {
  if (!activityId.value) return
  voterLoading.value = true
  listVoter({ pageNum: 1, pageSize: 500, activityId: activityId.value }).then(res => {
    activityVoters.value = res.rows || []
  }).finally(() => {
    voterLoading.value = false
  })
}

function selectVoters() {
  const ids = voterSelection.value.map(item => item.id)
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择评委资料')
    return
  }
  selectVoterFromPool(activityId.value, { ids, replaceExisting: replaceVoters.value }).then(() => {
    proxy.$modal.msgSuccess('评委已加入活动')
    refreshVoters()
  })
}

function copyVoters() {
  if (!voterSourceActivityId.value) {
    proxy.$modal.msgWarning('请选择历史活动')
    return
  }
  copyVoterFromActivity(activityId.value, voterSourceActivityId.value, { replaceExisting: replaceVoters.value }).then(() => {
    proxy.$modal.msgSuccess('评委复制完成')
    refreshVoters()
  })
}

function goPreview() {
  if (!activityVoters.value.length) {
    proxy.$modal.msgWarning('请先选择或导入评委')
    return
  }
  activeStep.value = 3
  loadPreview()
}

function loadPreview() {
  previewLoading.value = true
  previewRule(activityId.value, {}).then(res => {
    previewList.value = (res.rows || res.data || []).map(row => ({
      ...row,
      lockedPassCount: row.lockedPassCount ?? row.fixedPassCount ?? 0,
      lockedRejectCount: row.lockedRejectCount ?? row.fixedRejectCount ?? 0,
      fixedPassCount: row.fixedPassCount ?? row.lockedPassCount ?? 0,
      fixedRejectCount: row.fixedRejectCount ?? row.lockedRejectCount ?? 0,
      confirmedPassRange: row.confirmedPassRange ?? row.fixedPassRange ?? '-',
      confirmedRejectRange: row.confirmedRejectRange ?? row.fixedRejectRange ?? '-',
      minVoteRejectCount: row.minVoteRejectCount ?? 0,
      passRatio: row.passRatio ?? ruleForm.passRatio
    }))
    if (!previewList.value.length) {
      proxy.$modal.msgWarning('当前活动没有可预览的候选人')
    }
  }).finally(() => {
    previewLoading.value = false
  })
}

function recalculatePreviewRow(row, changedField) {
  const total = Number(row.candidateCount || 0)
  const maxPassCount = Math.max(0, Math.min(Number(row.maxPassCount || 0), total))
  let passCount = clampCount(row.lockedPassCount, 0, maxPassCount)
  let rejectCount = clampCount(row.lockedRejectCount, 0, Math.max(0, total - passCount - 1))
  if (passCount + rejectCount >= total) {
    if (changedField === 'lockedPassCount') {
      rejectCount = Math.max(0, total - passCount - 1)
    } else {
      passCount = Math.max(0, total - rejectCount - 1)
    }
  }
  const voteStart = passCount + 1
  const voteEnd = total - rejectCount
  const voteCount = Math.max(0, total - passCount - rejectCount)
  const minVoteRejectCount = Math.max(0, maxPassCount - passCount - rejectCount)
  row.maxPassCount = maxPassCount
  row.fixedPassCount = passCount
  row.lockedPassCount = passCount
  row.fixedRejectCount = rejectCount
  row.lockedRejectCount = rejectCount
  row.voteCount = voteCount
  row.minVoteRejectCount = minVoteRejectCount
  row.confirmedPassRange = formatSortRange(1, passCount)
  row.fixedPassRange = row.confirmedPassRange
  row.voteRange = formatSortRange(voteStart, voteEnd)
  row.confirmedRejectRange = formatSortRange(voteEnd + 1, total)
  row.fixedRejectRange = row.confirmedRejectRange
}

function clampCount(value, min, max) {
  const number = Number(value || 0)
  if (!Number.isFinite(number)) return min
  return Math.max(min, Math.min(Math.round(number), max))
}

function getPreviewSummaries({ columns, data }) {
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

function formatSortRange(start, end) {
  if (end < start) return '-'
  return `${start}-${end}`
}

function formatPercent(value) {
  if (value === undefined || value === null || value === '') return '-'
  const number = Number(value)
  if (!Number.isFinite(number)) return `${value}%`
  return `${Number.isInteger(number) ? number : number.toFixed(2).replace(/\.?0+$/, '')}%`
}

function applyRanges() {
  const ranges = previewList.value.map(row => ({
    ...row,
    fixedPassCount: Number(row.lockedPassCount || 0),
    fixedRejectCount: Number(row.lockedRejectCount || 0),
    lockedPassCount: Number(row.lockedPassCount || 0),
    lockedRejectCount: Number(row.lockedRejectCount || 0),
    minVoteRejectCount: Number(row.minVoteRejectCount || 0)
  }))
  applyConfirmedRule(activityId.value, ranges).then(() => {
    proxy.$modal.msgSuccess('范围已应用')
    refreshCandidates()
    activeStep.value = 4
  })
}

function publish() {
  publishing.value = true
  publishActivity(activityId.value).then((response) => {
    proxy.$modal.msgSuccess('活动已发布')
    setSharedVoteEntry(response.data || {})
    voteEntryCopied.value = false
    voteEntryOpen.value = true
  }).finally(() => {
    publishing.value = false
  })
}

function setSharedVoteEntry(activity) {
  sharedVoteUrl.value = buildSharedVoteUrl(activity)
  voteQrCodeDataUrl.value = buildVoteQrCode(sharedVoteUrl.value)
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

function copyVoteEntry() {
  if (!sharedVoteUrl.value) {
    return
  }
  copyText(sharedVoteUrl.value).then(() => {
    voteEntryCopied.value = true
    proxy.$modal.msgSuccess('投票链接已复制')
  }).catch(() => {
    voteEntryCopied.value = false
    voteEntryInputRef.value?.select?.()
    proxy.$modal.msgWarning('自动复制失败，请手动选中链接复制')
  })
}

function copyVoteQrCode() {
  copyQrCodeImage(voteQrCodeDataUrl.value).then(() => {
    voteEntryCopied.value = true
    proxy.$modal.msgSuccess('二维码已复制')
  }).catch(() => {
    proxy.$modal.msgWarning('二维码复制失败，请截图或复制链接发送')
  })
}

function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(text)
  }
  return new Promise((resolve, reject) => {
    const element = document.createElement('textarea')
    element.value = text
    element.setAttribute('readonly', '')
    element.style.position = 'fixed'
    element.style.left = '-9999px'
    document.body.appendChild(element)
    element.select()
    try {
      const successful = document.execCommand('copy')
      document.body.removeChild(element)
      successful ? resolve() : reject(new Error('copy failed'))
    } catch (error) {
      document.body.removeChild(element)
      reject(error)
    }
  })
}

function goNoticeAfterPublish() {
  voteEntryOpen.value = false
  resetWizardState()
  proxy.$tab.closeOpenPage({ path: '/system/notice' })
}

onMounted(() => {
  if (!initialized) {
    resetWizardState()
    initialized = true
    skipNextActivatedReset = true
  }
  refreshActivityOptions()
  loadPoolCandidates()
})

onActivated(() => {
  if (skipNextActivatedReset) {
    skipNextActivatedReset = false
    return
  }
  if (initialized && !voteEntryOpen.value) {
    resetWizardState()
    refreshActivityOptions()
    loadPoolCandidates()
  }
})
</script>

<style scoped lang="scss">
.wizard-page {
  background: #f6f8fb;
  min-height: calc(100vh - 84px);
}

.wizard-steps {
  margin: 18px 0;
  padding: 18px 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.panel {
  padding: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.toolbar,
.actions,
.inline-copy {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.actions {
  justify-content: flex-end;
  margin-top: 16px;
}

.source-block {
  margin-top: 12px;
}

.unit {
  margin-left: 8px;
  color: #606266;
}

.count-input {
  width: 96px;

  :deep(.el-input__inner) {
    text-align: center;
  }
}

.publish-panel {
  min-height: 320px;
}

.vote-entry-url {
  margin-top: 12px;
}

.vote-entry-dialog-body {
  margin-top: 14px;
}

.vote-entry-label {
  color: #606266;
  font-size: 14px;
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
