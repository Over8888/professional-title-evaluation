<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="68px">
      <el-form-item label="评委姓名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入评委姓名" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工号" prop="employeeId">
        <el-input v-model="queryParams.employeeId" placeholder="请输入工号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="部门" prop="department">
        <el-select v-model="queryParams.department" placeholder="请选择部门" clearable filterable style="width: 180px">
          <el-option v-for="item in voterOptions.departments" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Upload" @click="handleImport" v-hasPermi="['evaluation:voter:import']">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['evaluation:voter:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['evaluation:voter:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['evaluation:voter:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="voterList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="评委编号" prop="importSeq" width="100" align="center" />
      <el-table-column label="姓名" prop="name" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="工号" prop="employeeId" width="120" align="center" />
      <el-table-column label="部门" prop="department" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['evaluation:voter:edit']" />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['evaluation:voter:remove']" />
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="voterRef" :model="form" label-width="90px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入评委姓名" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工号" prop="employeeId">
              <el-input v-model="form.employeeId" placeholder="请输入工号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资料库">
              <el-input value="评委资料库" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="部门" prop="department">
          <el-input v-model="form.department" placeholder="请输入部门" />
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
      ref="importRef"
      title="评委资料导入"
      action="/evaluation/voter/pool/importData"
      template-action="/evaluation/voter/importTemplate"
      template-file-name="voter_template"
      update-support-label="覆盖已有评委资料库"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup name="EvaluationVoter">
import ExcelImportDialog from '@/components/ExcelImportDialog/index.vue'
import { delVoter, getVoter, listVoter, listVoterOptions, updateVoter } from '@/api/evaluation/voter'

const { proxy } = getCurrentInstance()

const voterList = ref([])
const voterOptions = ref({
  departments: []
})
const importRef = ref(null)
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    pool: true,
    name: undefined,
    employeeId: undefined,
    department: undefined
  }
})

const { queryParams, form } = toRefs(data)

function getList() {
  loading.value = true
  listVoter(queryParams.value).then(response => {
    voterList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function getOptions() {
  listVoterOptions().then(response => {
    voterOptions.value = {
      departments: response.data?.departments || []
    }
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
  importRef.value?.open()
}

function handleImportSuccess() {
  getOptions()
  getList()
}

function handleUpdate(row) {
  const voterId = row.id || ids.value
  getVoter(voterId).then(response => {
    form.value = response.data || {}
    open.value = true
    title.value = '修改评委'
  })
}

function submitForm() {
  updateVoter(form.value).then(() => {
    proxy.$modal.msgSuccess('修改成功')
    open.value = false
    getList()
  })
}

function cancel() {
  open.value = false
  form.value = {}
}

function handleDelete(row) {
  const voterIds = row.id || ids.value
  proxy.$modal.confirm('是否确认删除评委编号为 "' + voterIds + '" 的数据项？').then(() => {
    return delVoter(voterIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('evaluation/voter/export', { ...queryParams.value }, `voter_${new Date().getTime()}.xlsx`)
}

getList()
getOptions()
</script>
