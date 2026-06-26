<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="80px">
      <el-form-item label="姓名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入姓名" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="单位" prop="company">
        <el-select v-model="queryParams.company" placeholder="请选择单位" filterable clearable style="width: 220px" @change="handleQuery">
          <el-option v-for="item in optionData.companies" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="二级部门" prop="department">
        <el-select v-model="queryParams.department" placeholder="请选择二级部门" filterable clearable style="width: 200px" @change="handleQuery">
          <el-option v-for="item in optionData.departments" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="三级部门" prop="thirdLevelDepartment">
        <el-select v-model="queryParams.thirdLevelDepartment" placeholder="请选择三级部门" filterable clearable style="width: 220px" @change="handleQuery">
          <el-option v-for="item in optionData.thirdLevelDepartments" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="岗位" prop="position">
        <el-select v-model="queryParams.position" placeholder="请选择岗位" filterable clearable style="width: 200px" @change="handleQuery">
          <el-option v-for="item in optionData.positions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="当前等级" prop="currentLevel">
        <el-select v-model="queryParams.currentLevel" placeholder="请选择当前等级" filterable clearable style="width: 160px" @change="handleQuery">
          <el-option v-for="item in optionData.currentLevels" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="申报等级" prop="appliedLevel">
        <el-select v-model="queryParams.appliedLevel" placeholder="请选择申报等级" filterable clearable style="width: 160px" @change="handleQuery">
          <el-option v-for="item in optionData.appliedLevels" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Upload" @click="handleImport" v-hasPermi="['evaluation:candidate:import']">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['evaluation:candidate:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['evaluation:candidate:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['evaluation:candidate:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="candidateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" prop="importSeq" width="80" align="center" />
      <el-table-column label="姓名" prop="name" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="单位" prop="company" min-width="170" :show-overflow-tooltip="true" />
      <el-table-column label="二级部门" prop="department" min-width="130" :show-overflow-tooltip="true" />
      <el-table-column label="三级部门" prop="thirdLevelDepartment" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="岗位" prop="position" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="当前等级" prop="currentLevel" width="110" align="center" />
      <el-table-column label="申报等级" prop="appliedLevel" width="110" align="center" />
      <el-table-column label="身份证号" prop="idCard" width="180" align="center">
        <template #default="scope">{{ maskIdCard(scope.row.idCard) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="110" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['evaluation:candidate:edit']" />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['evaluation:candidate:remove']" />
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="candidateRef" :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="序号" prop="importSeq">
              <el-input v-model="form.importSeq" disabled placeholder="系统自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="单位" prop="company">
          <el-input v-model="form.company" placeholder="请输入单位" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="二级部门" prop="department">
              <el-input v-model="form.department" placeholder="请输入二级部门" @change="syncThirdLevelDepartment" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="三级部门" prop="thirdLevelDepartment">
              <el-input v-model="form.thirdLevelDepartment" placeholder="自动生成，可手动调整" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="岗位" prop="position">
          <el-input v-model="form.position" placeholder="请输入岗位" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="当前等级" prop="currentLevel">
              <el-input v-model="form.currentLevel" placeholder="请输入当前等级" @change="syncThirdLevelDepartment" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申报等级" prop="appliedLevel">
              <el-input v-model="form.appliedLevel" placeholder="请输入申报等级" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <excel-import-dialog
      ref="importDialogRef"
      title="候选人资料导入"
      action="/evaluation/candidate/pool/importData"
      template-action="/evaluation/candidate/importTemplate"
      template-file-name="candidate_template"
      update-support-label="覆盖已有候选人资料库"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup name="EvaluationCandidate">
import ExcelImportDialog from '@/components/ExcelImportDialog/index.vue'
import { delCandidate, getCandidate, listCandidate, listCandidateOptions, updateCandidate } from '@/api/evaluation/candidate'

const { proxy } = getCurrentInstance()

const candidateList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const importDialogRef = ref(null)
const optionData = reactive({
  companies: [],
  departments: [],
  thirdLevelDepartments: [],
  positions: [],
  currentLevels: [],
  appliedLevels: []
})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    pool: true,
    name: undefined,
    company: undefined,
    department: undefined,
    thirdLevelDepartment: undefined,
    position: undefined,
    currentLevel: undefined,
    appliedLevel: undefined
  }
})

const { queryParams, form } = toRefs(data)

function maskIdCard(idCard) {
  if (!idCard) return '-'
  const value = String(idCard)
  if (value.length < 10) return value
  return value.slice(0, 6) + '********' + value.slice(-4)
}

function loadOptions() {
  listCandidateOptions().then(response => {
    const data = response.data || {}
    optionData.companies = data.companies || []
    optionData.departments = data.departments || []
    optionData.thirdLevelDepartments = data.thirdLevelDepartments || []
    optionData.positions = data.positions || []
    optionData.currentLevels = data.currentLevels || []
    optionData.appliedLevels = data.appliedLevels || []
  })
}

function getList() {
  loading.value = true
  listCandidate(queryParams.value).then(response => {
    candidateList.value = response.rows || []
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
  queryParams.value.pool = true
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleImport() {
  importDialogRef.value?.open()
}

function handleImportSuccess() {
  loadOptions()
  getList()
}

function handleUpdate(row) {
  const candidateId = row.id || ids.value
  getCandidate(candidateId).then(response => {
    form.value = response.data || {}
    open.value = true
    title.value = '修改候选人'
  })
}

function buildThirdLevelDepartment() {
  const department = form.value.department || ''
  const currentLevel = form.value.currentLevel || ''
  if (!department && !currentLevel) return undefined
  return `${department}(${currentLevel})`
}

function syncThirdLevelDepartment() {
  form.value.thirdLevelDepartment = buildThirdLevelDepartment()
}

function submitForm() {
  form.value.thirdLevelDepartment = buildThirdLevelDepartment()
  updateCandidate(form.value).then(() => {
    proxy.$modal.msgSuccess('修改成功')
    open.value = false
    loadOptions()
    getList()
  })
}

function cancel() {
  open.value = false
  form.value = {}
}

function handleDelete(row) {
  const candidateIds = row.id || ids.value
  proxy.$modal.confirm('是否确认删除候选人编号为 "' + candidateIds + '" 的数据项？').then(() => {
    return delCandidate(candidateIds)
  }).then(() => {
    getList()
    loadOptions()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('evaluation/candidate/export', { ...queryParams.value }, `candidate_${new Date().getTime()}.xlsx`)
}

loadOptions()
getList()
</script>
