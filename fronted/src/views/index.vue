<template>
  <div class="app-container home">
    <section class="page-header">
      <h1>评审活动工作台</h1>
      <p>选择活动类型后进入配置流程，按排序名单、范围确认、评委投票和结果导出完成评审。</p>
    </section>

    <div class="entry-grid">
      <button class="entry-card title-review" @click="goTitleReview">
        <span class="entry-icon"><svg-icon icon-class="education" /></span>
        <span class="entry-content">
          <span class="entry-kicker">已开放</span>
          <strong>职称评审</strong>
          <span>导入排序名单，设置通过/淘汰比例，确认投票范围并发布活动。</span>
        </span>
      </button>
      <button class="entry-card talent" @click="showTalentNotice">
        <span class="entry-icon muted"><svg-icon icon-class="peoples" /></span>
        <span class="entry-content">
          <span class="entry-kicker muted">二期</span>
          <strong>人才评选</strong>
          <span>后续支持多选推荐、评分制、权重评委等人才评选能力。</span>
        </span>
      </button>
    </div>

    <el-card class="guide-card" shadow="never">
      <template #header>使用规则</template>
      <div class="guide-list">
        <div class="guide-item">
          <span class="guide-step">1</span>
          <div>
            <strong>先导入排序结果</strong>
            <p>按大组和申报职称读取排序表，排序列作为范围分段和兜底排名依据。</p>
          </div>
        </div>
        <div class="guide-item">
          <span class="guide-step">2</span>
          <div>
            <strong>再确认评审范围</strong>
            <p>前段固定通过，末尾固定不通过，中间候选人进入投票表决。</p>
          </div>
        </div>
        <div class="guide-item">
          <span class="guide-step">3</span>
          <div>
            <strong>最后发布和导出</strong>
            <p>投票端只展示待投票人员，结果导出汇总全部候选人的最终通过/不通过状态。</p>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
const router = useRouter()
const { proxy } = getCurrentInstance()

function goTitleReview() {
  router.push('/system/activity/wizard')
}

function showTalentNotice() {
  proxy.$modal.msgWarning('人才评选为二期能力，当前版本暂未开放。')
}
</script>

<style scoped lang="scss">
.home {
  min-height: calc(100vh - 84px);
  background: #f6f8fb;
}

.page-header {
  padding: 20px 0 14px;

  h1 {
    margin: 0 0 8px;
    color: #1f2f46;
    font-size: 28px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: #5d6b82;
    font-size: 14px;
  }
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin: 14px 0 18px;
}

.entry-card {
  display: flex;
  align-items: center;
  min-height: 174px;
  padding: 26px;
  text-align: left;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;

  .entry-icon {
    flex: 0 0 62px;
    width: 62px;
    height: 62px;
    margin-right: 22px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: #2f74d0;
    background: #edf5ff;
    border-radius: 6px;
    font-size: 32px;
  }

  .entry-icon.muted {
    color: #b7791f;
    background: #fff7e6;
  }

  .entry-content {
    display: block;
    min-width: 0;
  }

  strong {
    display: block;
    margin: 10px 0;
    color: #1f2f46;
    font-size: 26px;
    font-weight: 600;
  }

  .entry-content > span:last-child {
    display: block;
    color: #5d6b82;
    line-height: 1.7;
  }

  &:hover {
    border-color: #409eff;
    box-shadow: 0 8px 22px rgba(31, 47, 70, 0.08);
  }
}

.entry-kicker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  padding: 0 10px;
  background: #ecfdf3;
  border-radius: 13px;
  color: #067647;
  font-size: 13px;
  font-weight: 600;
}

.entry-kicker.muted {
  background: #fff7e6;
  color: #8a5a00;
}

.guide-card {
  border-radius: 6px;

  :deep(.el-card__header) {
    color: #1f2f46;
    font-weight: 600;
  }
}

.guide-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.guide-item {
  display: flex;
  min-height: 96px;
  padding: 18px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 6px;

  strong {
    display: block;
    margin: 2px 0 8px;
    color: #1f2f46;
    font-size: 15px;
  }

  p {
    margin: 0;
    color: #5d6b82;
    line-height: 1.7;
  }
}

.guide-step {
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  margin-right: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #409eff;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .page-header {
    padding-top: 32px;

    h1 {
      font-size: 28px;
    }
  }

  .entry-grid,
  .guide-list {
    grid-template-columns: 1fr;
  }

  .entry-card {
    min-height: 150px;
    padding: 22px;

    strong {
      font-size: 24px;
    }
  }
}
</style>
