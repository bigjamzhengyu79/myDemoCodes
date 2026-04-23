<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-mask" @click.self="close">
      <div class="modal-box">
        <div class="modal-header">
          <span class="modal-title">{{ isEdit ? '编辑目标' : (isSubGoal ? '新增子目标' : '新增父目标') }}</span>
          <button class="close-btn" @click="close">✕</button>
        </div>

        <div v-if="parentTitle" class="parent-hint">父目标：{{ parentTitle }}</div>

        <div class="modal-body">
          <div class="field">
            <label>目标名称 <span class="required">*</span></label>
            <input v-model="form.title" type="text" placeholder="请输入目标名称" />
          </div>

          <div class="field">
            <label>目标描述</label>
            <textarea v-model="form.description" placeholder="描述具体学习内容…"></textarea>
          </div>

          <div class="field-row">
            <div class="field">
              <label>实施者</label>
              <input v-model="form.owners" type="text" placeholder="多人用逗号分隔" />
            </div>
            <div class="field">
              <label>状态</label>
              <select v-model="form.status">
                <option value="TODO">未开始</option>
                <option value="IN_PROGRESS">进行中</option>
                <option value="DONE">已完成</option>
                <option value="LATE">已延期</option>
              </select>
            </div>
          </div>

          <div class="field-section">预计期间</div>
          <div class="field-row">
            <div class="field">
              <label>预计开始日期</label>
              <input v-model="form.plannedStart" type="date" />
            </div>
            <div class="field">
              <label>预计完成日期</label>
              <input v-model="form.plannedEnd" type="date" />
            </div>
          </div>

          <div class="field-section">实际执行</div>
          <div class="field-row">
            <div class="field">
              <label>实际开始时间</label>
              <input v-model="form.actualStart" type="date" />
            </div>
            <div class="field">
              <label>实际完成时间</label>
              <input v-model="form.actualEnd" type="date" />
            </div>
          </div>

          <div class="field">
            <label>完成进度{{ hasSubGoals ? '（子目标将自动汇总）' : '' }}</label>
            <div class="progress-input">
              <input
                v-model.number="form.progress"
                type="range" min="0" max="100" step="5"
                :disabled="hasSubGoals"
              />
              <span>{{ form.progress }}%</span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="close">取消</button>
          <button class="btn-save" :disabled="saving" @click="submit">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  visible: Boolean,
  goalData: { type: Object, default: null },
  parentId: { type: Number, default: null },
  parentTitle: { type: String, default: '' },
})

const emit = defineEmits(['close', 'saved'])

const saving = ref(false)

const defaultForm = () => ({
  title: '',
  description: '',
  status: 'TODO',
  plannedStart: '',
  plannedEnd: '',
  actualStart: '',
  actualEnd: '',
  progress: 0,
  owners: '',
})

const form = ref(defaultForm())

const isEdit = computed(() => !!props.goalData?.id)
const isSubGoal = computed(() => !!props.parentId)
const hasSubGoals = computed(() =>
  isEdit.value && props.goalData?.subGoals?.length > 0
)

watch(() => props.visible, (val) => {
  if (val) {
    if (props.goalData) {
      form.value = {
        title: props.goalData.title || '',
        description: props.goalData.description || '',
        status: props.goalData.status || 'TODO',
        plannedStart: props.goalData.plannedStart || '',
        plannedEnd: props.goalData.plannedEnd || '',
        actualStart: props.goalData.actualStart || '',
        actualEnd: props.goalData.actualEnd || '',
        progress: props.goalData.progress || 0,
        owners: props.goalData.owners || '',
      }
    } else {
      form.value = defaultForm()
    }
  }
})

function close() {
  emit('close')
}

async function submit() {
  if (!form.value.title.trim()) {
    alert('请输入目标名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form.value,
      parentId: isSubGoal.value ? props.parentId : null,
    }
    emit('saved', {
      id: props.goalData?.id || null,
      payload,
    })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.modal-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,.35);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-box {
  background: #fff; border-radius: 12px; width: 520px; max-width: 95vw;
  max-height: 90vh; overflow-y: auto; border: 0.5px solid #ddd;
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 22px 0;
}
.modal-title { font-size: 15px; font-weight: 500; color: #111; }
.close-btn { border: none; background: none; font-size: 14px; cursor: pointer; color: #888; }
.close-btn:hover { color: #333; }
.parent-hint {
  margin: 8px 22px 0; background: #f5f5f3; border-radius: 8px;
  padding: 6px 10px; font-size: 12px; color: #666;
}
.modal-body { padding: 14px 22px; }
.modal-footer {
  display: flex; gap: 8px; justify-content: flex-end;
  padding: 12px 22px 18px; border-top: 0.5px solid #eee;
}
.field { margin-bottom: 12px; }
.field label { display: block; font-size: 12px; color: #666; margin-bottom: 4px; }
.field input, .field select, .field textarea {
  width: 100%; border: 0.5px solid #ccc; border-radius: 8px;
  padding: 7px 10px; font-size: 13px; color: #111; background: #fff;
}
.field textarea { height: 60px; resize: vertical; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.field-section {
  font-size: 11px; font-weight: 500; color: #999; text-transform: uppercase;
  letter-spacing: .07em; padding: 8px 0 4px; border-top: 0.5px solid #eee; margin-top: 4px;
}
.required { color: #e24b4a; }
.progress-input { display: flex; align-items: center; gap: 8px; }
.progress-input input[type=range] { flex: 1; }
.progress-input span { font-size: 12px; font-weight: 500; min-width: 32px; }
.btn-cancel {
  border: 0.5px solid #ccc; background: transparent; border-radius: 8px;
  padding: 6px 14px; font-size: 13px; cursor: pointer; color: #666;
}
.btn-save {
  background: #1D9E75; color: #fff; border: none; border-radius: 8px;
  padding: 6px 18px; font-size: 13px; font-weight: 500; cursor: pointer;
}
.btn-save:hover { background: #0F6E56; }
.btn-save:disabled { opacity: .6; cursor: not-allowed; }
</style>
