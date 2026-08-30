<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-mask">
      <div class="modal-box">
        <div class="modal-header">
          <span class="modal-title">{{ isEdit ? '编辑目标' : (isSubGoal ? '新增子目标' : '新增父目标') }}</span>
          <button class="close-btn" @click="close">✕</button>
        </div>

        <div v-if="parentTitle" class="parent-hint">父目标：{{ parentTitle }}</div>

        <!-- 从已有目标复制（仅在新建父目标时显示） -->
        <div v-if="!isEdit && !parentId" class="copy-section">
          <div class="copy-header">📋 从已有目标复制</div>
          <div class="copy-row">
            <select v-model="selectedCopyGoalId" class="form-control" @change="onCopyGoalChange">
              <option :value="null">— 不复制，手动输入 —</option>
              <!-- 列表含其他老师共享的模板，必须显示作者名：
                   不同老师很可能有同名目标（如都叫「期中复习」），只显示标题无法区分 -->
              <option v-for="g in copyableGoals" :key="g.id" :value="g.id">
                {{ g.title }}（{{ copyGoalAuthor(g) }}）
              </option>
            </select>
            <div v-if="selectedCopyGoal" class="copy-info">
              已复制：标题、描述、子目标结构、{{ selectedCopyGoal.assignmentIds?.length || 0 }} 个作业<br>学生与班级需自行指定（不从模板继承）
            </div>
          </div>
        </div>

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

          <!-- 关联班级 -->
          <div class="field-section">关联班级</div>
          <div class="field">
            <select v-model="form.classGroupId" class="form-control">
              <option :value="null">不指定班级</option>
              <option v-for="cg in classGroups" :key="cg.id" :value="cg.id">{{ cg.name }}</option>
            </select>
          </div>

          <!-- 关联作业 -->
          <div class="field-section">关联作业</div>
          <div class="field">
            <div class="assignment-checkbox-list">
              <div v-if="assignments.length === 0" class="assign-empty-sm">暂无已发布的作业</div>
              <label
                v-for="a in assignments"
                :key="a.id"
                class="assignment-checkbox-item"
                :class="{ checked: form.assignmentIds.includes(a.id) }"
              >
                <input
                  type="checkbox"
                  :value="a.id"
                  :checked="form.assignmentIds.includes(a.id)"
                  @change="toggleAssignment(a.id)"
                />
                <span>{{ a.title }}</span>
                <span v-if="a.classGroupName" class="assignment-class-tag">{{ a.classGroupName }}</span>
              </label>
            </div>
            <div v-if="assignments.length > 0" class="assignment-actions">
              <button type="button" class="btn-link" @click="selectAllAssignments">全选</button>
              <span class="sep">|</span>
              <button type="button" class="btn-link" @click="deselectAllAssignments">全不选</button>
              <span class="assignment-count">已选 {{ form.assignmentIds.length }} 个</span>
            </div>
          </div>

          <div class="field-section">分配学生</div>
          <div class="field">
            <div class="assign-students">
              <div class="selected-students" v-if="form.assigneeIds && form.assigneeIds.length">
                <span
                  v-for="sid in form.assigneeIds"
                  :key="sid"
                  class="student-chip"
                >
                  {{ studentNameOf(sid) }}
                  <button type="button" class="chip-remove" @click="removeStudent(sid)">✕</button>
                </span>
              </div>
              <div v-else class="assign-empty">暂未分配学生</div>
              <div class="assign-actions">
                <input
                  v-model="studentSearch"
                  type="text"
                  class="search-input"
                  placeholder="搜索学生姓名 / 用户名…"
                  @input="onStudentSearch"
                />
                <div v-if="filteredStudents.length" class="student-list">
                  <div
                    v-for="s in filteredStudents"
                    :key="s.id"
                    class="student-item"
                    :class="{ picked: form.assigneeIds.includes(s.id) }"
                    @click="toggleStudent(s.id)"
                  >
                    <span class="student-name">{{ s.realName || s.username }}</span>
                  </div>
                </div>
                <div v-else-if="studentSearch" class="assign-empty-sm">没有匹配的学生</div>
              </div>
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
import { ref, watch, computed, onMounted } from 'vue'
import axios from 'axios'
import { goalApi } from '@/api/goalApi'

// 用于复制操作的 store（仅调用 copy API）
import { useGoalStore } from '@/stores/goalStore'
import { useAuthStore } from '@/store/auth'

const props = defineProps({
  visible: Boolean,
  goalData: { type: Object, default: null },
  parentId: { type: Number, default: null },
  parentTitle: { type: String, default: '' },
})

const emit = defineEmits(['close', 'saved'])

// 用于在可复制列表里把自己创建的模板标成「我」
const authStore = useAuthStore()

const saving = ref(false)

const classGroups = ref([])
const assignments = ref([])
const copyableGoals = ref([])
const selectedCopyGoalId = ref(null)
const selectedCopyGoal = ref(null)

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
  assigneeIds: [],
  classGroupId: null,
  assignmentIds: [],
})

const form = ref(defaultForm())

const isEdit = computed(() => !!props.goalData?.id)
const isSubGoal = computed(() => !!props.parentId)
const hasSubGoals = computed(() =>
  isEdit.value && props.goalData?.subGoals?.length > 0
)

const allStudents = ref([])
const studentSearch = ref('')
const isFilteredByClass = ref(false)

const filteredStudents = computed(() => {
  const kw = studentSearch.value.trim().toLowerCase()
  if (!kw) return allStudents.value.slice(0, 20)
  return allStudents.value.filter(s => {
    const name = (s.realName || '').toLowerCase()
    const uname = (s.username || '').toLowerCase()
    return name.includes(kw) || uname.includes(kw)
  }).slice(0, 30)
})

const studentNameOf = (sid) => {
  const s = allStudents.value.find(x => x.id === sid)
  return s ? (s.realName || s.username) : `#${sid}`
}

const toggleStudent = (sid) => {
  const arr = form.value.assigneeIds || []
  const idx = arr.indexOf(sid)
  if (idx >= 0) {
    form.value.assigneeIds = arr.filter(x => x !== sid)
  } else {
    form.value.assigneeIds = [...arr, sid]
  }
}

const removeStudent = (sid) => {
  form.value.assigneeIds = (form.value.assigneeIds || []).filter(x => x !== sid)
}

const onStudentSearch = () => { /* 触发 computed */ }

// ====== 作业选择（checkbox 模式） ======
function toggleAssignment(aid) {
  const arr = form.value.assignmentIds || []
  const idx = arr.indexOf(aid)
  if (idx >= 0) {
    form.value.assignmentIds = arr.filter(x => x !== aid)
  } else {
    form.value.assignmentIds = [...arr, aid]
  }
}

function selectAllAssignments() {
  form.value.assignmentIds = assignments.value.map(a => a.id)
}

function deselectAllAssignments() {
  form.value.assignmentIds = []
}

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const http = axios.create({
  baseURL: apiBase + '/api',
  timeout: 90000, // Render 冷启动需 90 秒以上，说明见 api/goalApi.js
})
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
http.interceptors.response.use(res => res.data)

async function fetchStudents() {
  try {
    const resp = await http.get('/users')
    allStudents.value = (resp || [])
      .filter(u => u.role === 'STUDENT')
      .map(u => ({
        id: u.id,
        realName: u.realName || u.real_name,
        username: u.username,
      }))
  } catch (e) {
    allStudents.value = []
  }
}

async function fetchStudentsByClass(classGroupId) {
  if (!classGroupId) {
    isFilteredByClass.value = false
    return fetchStudents()
  }
  try {
    const resp = await http.get(`/class-groups/${classGroupId}/students`)
    const students = resp || []
    allStudents.value = students.map(u => ({
      id: u.id,
      realName: u.realName || u.real_name,
      username: u.username,
    }))
    isFilteredByClass.value = true
  } catch (e) {
    allStudents.value = []
    isFilteredByClass.value = false
  }
}

async function fetchClassGroups() {
  try {
    const resp = await http.get('/class-groups')
    classGroups.value = resp || []
  } catch (e) {
    classGroups.value = []
  }
}

async function fetchAssignments() {
  try {
    const resp = await http.get('/assignments')
    const list = Array.isArray(resp) ? resp : (resp?.data || [])
    assignments.value = list.filter(a => a.status === 'PUBLISHED')
  } catch (e) {
    assignments.value = []
  }
}

async function fetchCopyableGoals() {
  try {
    copyableGoals.value = await goalApi.listCopyable()
  } catch (e) {
    copyableGoals.value = []
  }
}

/**
 * 下拉项里显示的作者名。
 * 列表包含其他老师共享的模板，自己的标成「我」更容易一眼分辨。
 * managerName 可能为空（历史数据里 manager 允许为 null），兜底显示「未知」。
 */
function copyGoalAuthor(g) {
  if (g.managerId && g.managerId === authStore.user?.id) return '我'
  return g.managerName || '未知'
}

onMounted(() => {
  fetchStudents()
  fetchClassGroups()
  fetchAssignments()
  fetchCopyableGoals()
})

// 监听班级选择变化，自动同步学生列表
watch(() => form.value.classGroupId, async (newVal) => {
  if (newVal) {
    await fetchStudentsByClass(newVal)
  } else {
    await fetchStudents()
    isFilteredByClass.value = false
  }
})

watch(() => props.visible, (val) => {
  if (val) {
    selectedCopyGoalId.value = null
    selectedCopyGoal.value = null
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
        assigneeIds: Array.isArray(props.goalData.assigneeIds)
          ? [...props.goalData.assigneeIds]
          : [],
        classGroupId: props.goalData.classGroupId || null,
        assignmentIds: Array.isArray(props.goalData.assignmentIds)
          ? [...props.goalData.assignmentIds]
          : [],
      }
    } else {
      form.value = defaultForm()
      fetchCopyableGoals()
    }
  }
})

function onCopyGoalChange() {
  if (!selectedCopyGoalId.value) {
    selectedCopyGoal.value = null
    return
  }
  selectedCopyGoal.value = copyableGoals.value.find(g => g.id === selectedCopyGoalId.value)
  if (selectedCopyGoal.value) {
    const g = selectedCopyGoal.value
    form.value = {
      title: g.title || '',
      description: g.description || '',
      status: 'TODO',
      plannedStart: '',
      plannedEnd: '',
      actualStart: '',
      actualEnd: '',
      progress: 0,
      owners: g.owners || '',
      // 不预填学生与班级：模板可能来自其他老师，其名单/班级与复制者无关。
      // 后端 copySubGoalsRecursive 同样不再复制这两项，两侧保持一致。
      assigneeIds: [],
      classGroupId: null,
      assignmentIds: Array.isArray(g.assignmentIds) ? [...g.assignmentIds] : [],
    }
  }
}

function close() {
  emit('close')
}

async function submit() {
  saving.value = true
  try {
    if (!form.value.title.trim()) {
      alert('请输入目标名称')
      return
    }
    const payload = {
      ...form.value,
      parentId: isSubGoal.value ? props.parentId : null,
    }
    // 如果选择了从已有目标复制（且非编辑模式，非子目标），
    // 将源目标 ID 传给后端，让后端先用表单数据创建父目标，
    // 然后递归复制源目标的子目标及其关联数据
    if (!isEdit.value && !props.parentId && selectedCopyGoalId.value) {
      payload.sourceGoalId = selectedCopyGoalId.value
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
.copy-section {
  margin: 8px 22px 0; padding: 8px 10px;
  background: #f0faf5; border: 0.5px solid #c8e6d9; border-radius: 8px;
}
.copy-header { font-size: 12px; font-weight: 500; color: #0F6E56; margin-bottom: 4px; }
.copy-row { display: flex; flex-direction: column; gap: 4px; }
.copy-info { font-size: 11px; color: #888; margin-top: 2px; }
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

.assign-students {
  background: #f9fafb; border: 1px solid #eef0f2; border-radius: 10px;
  padding: 10px;
}
.selected-students {
  display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px;
}
.student-chip {
  display: inline-flex; align-items: center; gap: 4px;
  background: #E1F5EE; color: #0F6E56; border-radius: 999px;
  padding: 3px 4px 3px 10px; font-size: 12px;
}
.chip-remove {
  border: none; background: transparent; color: #0F6E56;
  cursor: pointer; font-size: 12px; padding: 0 4px;
}
.chip-remove:hover { color: #c0392b; }
.assign-empty, .assign-empty-sm {
  font-size: 12px; color: #999; text-align: center; padding: 6px 0;
}
.assign-empty-sm { padding: 4px 0; text-align: left; }
.search-input {
  width: 100%; border: 0.5px solid #ccc; border-radius: 6px;
  padding: 5px 10px; font-size: 12px;
}
.student-list {
  max-height: 160px; overflow-y: auto; border: 0.5px solid #eee;
  border-radius: 6px; background: #fff; margin-top: 6px;
}
.student-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 10px; cursor: pointer; font-size: 12px;
  border-bottom: 0.5px solid #f5f5f5;
}
.student-item:last-child { border-bottom: none; }
.student-item:hover { background: #f0faf5; }
.student-item.picked { background: #E1F5EE; color: #0F6E56; }
.student-class { color: #888; font-size: 11px; }

/* 作业选择 checkbox 列表 */
.assignment-checkbox-list {
  background: #f9fafb; border: 1px solid #eef0f2; border-radius: 10px;
  padding: 8px; max-height: 180px; overflow-y: auto;
}
.assignment-checkbox-item {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 8px; cursor: pointer; border-radius: 6px;
  font-size: 12px; color: #333; transition: background .15s;
}
.assignment-checkbox-item:hover { background: #f0faf5; }
.assignment-checkbox-item.checked { background: #E1F5EE; color: #0F6E56; }
.assignment-checkbox-item input[type="checkbox"] {
  width: auto; margin: 0; cursor: pointer; accent-color: #1D9E75;
}
.assignment-class-tag {
  font-size: 10px; color: #185FA5; background: #e8f4fd;
  border-radius: 4px; padding: 0 5px; margin-left: auto; white-space: nowrap;
}
.assignment-actions {
  display: flex; align-items: center; gap: 6px; margin-top: 6px;
  font-size: 12px;
}
.btn-link {
  border: none; background: none; color: #185FA5; cursor: pointer;
  font-size: 12px; padding: 0; text-decoration: none;
}
.btn-link:hover { text-decoration: underline; }
.sep { color: #ccc; font-size: 11px; }
.assignment-count {
  margin-left: auto; font-size: 11px; color: #888;
}
</style>
