<template>
  <div class="page">
    <div class="flex-between mb-3">
      <div>
        <h1 style="font-size:20px;font-weight:600">题库管理</h1>
        <div class="text-muted text-sm" style="margin-top:2px">管理所有题目，支持按知识点筛选</div>
      </div>
      <button class="btn btn-primary" @click="$router.push('/questions/new')">+ 新建题目</button>
    </div>

    <!-- Filters -->
    <div class="card card-sm mb-3 flex gap-3" style="flex-wrap:wrap">
      <div>
        <label class="form-label">题型</label>
        <select v-model="filter.type" class="form-control" style="width:120px">
          <option value="">全部</option>
          <option value="SINGLE_CHOICE">单选</option>
          <option value="FILL_BLANK">填空</option>
          <option value="OPEN_ENDED">解答</option>
        </select>
      </div>
      <div>
        <label class="form-label">难度</label>
        <select v-model="filter.difficulty" class="form-control" style="width:100px">
          <option value="">全部</option>
          <option v-for="i in 5" :key="i" :value="i">{{ '★'.repeat(i) }}</option>
        </select>
      </div>
      <div>
        <label class="form-label">知识点</label>
        <select v-model="filter.tag" class="form-control" style="width:140px">
          <option value="">全部</option>
          <option v-for="t in tags" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
      </div>
      <div style="display:flex;align-items:flex-end">
        <button class="btn" @click="filter = { type:'', difficulty:'', tag:'' }">重置</button>
      </div>
    </div>

    <!-- Table -->
    <div v-if="loading" style="text-align:center;padding:60px"><div class="spinner" style="margin:auto"></div></div>
    <div v-else class="card" style="padding:0;overflow:hidden">
      <table class="table">
        <thead>
          <tr>
            <th style="width:50px">ID</th>
            <th>题目</th>
            <th style="width:80px">题型</th>
            <th style="width:80px">难度</th>
            <th style="width:60px">分值</th>
            <th>知识点</th>
            <th style="width:70px">可见性</th>
            <th style="width:80px">创建者</th>
            <th style="width:60px">来源</th>
            <th style="width:150px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="q in filtered" :key="q.id">
            <td class="text-muted">#{{ q.id }}</td>
            <td style="max-width:280px">
              <div v-if="q.title" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px;font-weight:500">
                {{ q.title }}
              </div>
              <div :class="q.title ? 'text-muted' : ''"
                   style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px">
                {{ stripLatex(q.contentLatex) }}
              </div>
            </td>
            <td><span :class="['badge', typeBadge(q.questionType)]">{{ typeLabel(q.questionType) }}</span></td>
            <td>
              <div class="diff-dots">
                <span v-for="i in 5" :key="i" :class="['diff-dot', i <= q.difficulty ? 'on' : '']"></span>
              </div>
            </td>
            <td class="text-muted">{{ q.totalScore }}分</td>
            <td>
              <div style="display:flex;gap:4px;flex-wrap:wrap">
                <span v-for="t in q.knowledgeTags" :key="t.id" class="badge badge-gray">{{ t.name }}</span>
              </div>
            </td>
            <td>
              <span v-if="q.visibility === 'PRIVATE'" class="badge badge-gray">私有</span>
              <span v-else-if="q.visibility === 'SHARED'" class="badge badge-blue">共享</span>
              <span v-else class="badge badge-green">公开</span>
            </td>
            <td class="text-muted text-sm">{{ q.createdByName || '-' }}</td>
            <td class="text-muted text-sm">{{ q.source || '-' }}</td>
            <td>
              <!-- 后端已强制"仅作成者可改"，这里只是不给出无效按钮 -->
              <div class="flex gap-2" style="flex-wrap:wrap">
                <template v-if="canModify(q)">
                  <button class="btn btn-sm" @click="$router.push(`/questions/${q.id}/edit`)">编辑</button>
                  <button class="btn btn-sm btn-danger" @click="confirmDelete(q)">删除</button>
                </template>
                <span v-else class="text-muted text-sm">他人共享</span>
                <!-- 指定共享教师是管理员专属操作 -->
                <button v-if="auth.isAdmin()" class="btn btn-sm" @click="openShare(q)">共享</button>
              </div>
            </td>
          </tr>
          <tr v-if="filtered.length === 0">
            <td colspan="10" style="text-align:center;padding:40px;color:var(--c-text3)">暂无题目</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Delete confirm -->
    <div v-if="deleteTarget" class="modal-mask" @click.self="deleteTarget = null">
      <div class="modal-box card" style="width:360px">
        <h3 style="margin-bottom:8px">确认删除</h3>
        <p class="text-muted" style="font-size:13px;margin-bottom:16px">
          确定要删除题目 #{{ deleteTarget.id }} 吗？此操作不可恢复。
        </p>
        <p v-if="deleteErr" style="font-size:13px;margin-bottom:16px;color:var(--c-danger)">{{ deleteErr }}</p>
        <div class="flex gap-2" style="justify-content:flex-end">
          <button class="btn" @click="deleteTarget = null">取消</button>
          <button class="btn btn-danger" @click="doDelete">删除</button>
        </div>
      </div>
    </div>

    <!-- 管理员：指定共享教师 -->
    <div v-if="shareTarget" class="modal-mask" @click.self="closeShare">
      <div class="modal-box card" style="width:420px">
        <h3 style="margin-bottom:4px">共享设置 · #{{ shareTarget.id }}</h3>
        <div class="text-muted text-sm" style="margin-bottom:12px">
          {{ shareTarget.title || stripLatex(shareTarget.contentLatex) }}
        </div>

        <div v-if="shareLoading" style="text-align:center;padding:24px">
          <div class="spinner" style="margin:auto"></div>
        </div>

        <template v-else>
          <div class="form-group">
            <label class="form-label">可见范围</label>
            <select v-model="shareForm.visibility" class="form-control">
              <option value="PUBLIC">公开（全体教师可用）</option>
              <option value="SHARED">指定教师</option>
              <option value="PRIVATE">仅作成者</option>
            </select>
          </div>

          <div v-if="shareForm.visibility === 'SHARED'">
            <label class="form-label">共享给</label>
            <div v-if="shareForm.sharedUserIds.length" class="share-chips">
              <span v-for="uid in shareForm.sharedUserIds" :key="uid" class="badge badge-blue share-chip">
                {{ teacherNameOf(uid) }}
                <button type="button" class="chip-x" @click="removeTeacher(uid)">✕</button>
              </span>
            </div>
            <div v-else class="text-muted text-sm" style="padding:4px 0">尚未共享给任何教师</div>

            <input v-model="teacherSearch" class="form-control" style="margin-top:6px"
                   placeholder="搜索教师姓名 / 用户名…" />
            <div class="share-list">
              <div v-for="t in filteredTeachers" :key="t.id"
                   :class="['share-item', shareForm.sharedUserIds.includes(t.id) ? 'picked' : '']"
                   @click="toggleTeacher(t.id)">
                <span>{{ t.realName || t.username }}</span>
                <span v-if="shareForm.sharedUserIds.includes(t.id)"
                      class="text-sm" style="color:var(--c-primary)">✓</span>
              </div>
              <div v-if="filteredTeachers.length === 0" class="text-muted text-sm" style="padding:8px">
                {{ teachers.length ? '没有匹配的教师' : '暂无可共享的教师' }}
              </div>
            </div>
          </div>

          <p v-if="shareErr" style="font-size:13px;margin-top:12px;color:var(--c-danger)">{{ shareErr }}</p>

          <div class="flex gap-2" style="justify-content:flex-end;margin-top:16px">
            <button class="btn" @click="closeShare">取消</button>
            <button class="btn btn-primary" :disabled="shareSaving" @click="doShare">
              {{ shareSaving ? '保存中...' : '保存' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { questionApi, userApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import { stripLatex, typeLabel, typeBadge } from '@/utils/question'

const auth = useAuthStore()
const questions = ref([])
const tags = ref([])
const loading = ref(true)
const filter = ref({ type: '', difficulty: '', tag: '' })
const deleteTarget = ref(null)
const deleteErr = ref('')

// 仅作成者（或管理员）可改；被共享的题目只能使用
function canModify(q) {
  return auth.isAdmin() || (q.createdById != null && q.createdById === auth.user?.id)
}

// ── 管理员：共享设置弹窗 ────────────────────────────────
const shareTarget = ref(null)      // 当前操作的题目（Summary 行）
const shareForm = ref({ visibility: 'SHARED', sharedUserIds: [] })
const shareOwnerId = ref(null)
const shareLoading = ref(false)
const shareSaving = ref(false)
const shareErr = ref('')
const teachers = ref([])
const teacherSearch = ref('')

const filteredTeachers = computed(() => {
  const kw = teacherSearch.value.trim().toLowerCase()
  // 作成者本人不在候选中（后端也会剔除）
  const base = teachers.value.filter(t => t.id !== shareOwnerId.value)
  const list = kw
    ? base.filter(t => (t.realName || '').toLowerCase().includes(kw) ||
                       (t.username || '').toLowerCase().includes(kw))
    : base
  return list.slice(0, 30)
})

function teacherNameOf(uid) {
  const t = teachers.value.find(x => x.id === uid)
  return t ? (t.realName || t.username) : `#${uid}`
}
function toggleTeacher(uid) {
  const arr = shareForm.value.sharedUserIds
  shareForm.value.sharedUserIds = arr.includes(uid) ? arr.filter(x => x !== uid) : [...arr, uid]
}
function removeTeacher(uid) {
  shareForm.value.sharedUserIds = shareForm.value.sharedUserIds.filter(x => x !== uid)
}

async function openShare(q) {
  shareTarget.value = q
  shareErr.value = ''
  shareLoading.value = true
  shareForm.value = { visibility: q.visibility || 'PUBLIC', sharedUserIds: [] }
  shareOwnerId.value = q.createdById ?? null
  try {
    // Summary 刻意不含 sharedUserIds（那是 ToMany，列表场景会引发 N+1），
    // 所以打开弹窗时单独取一次完整题目。教师列表跨弹窗缓存，只取一次。
    const [full, users] = await Promise.all([
      questionApi.get(q.id),
      teachers.value.length ? Promise.resolve(null) : userApi.listTeachers()
    ])
    if (users) teachers.value = users || []
    if (full?.success) {
      shareForm.value.visibility = full.data.visibility || 'PUBLIC'
      shareForm.value.sharedUserIds = full.data.sharedUserIds || []
      shareOwnerId.value = full.data.createdById ?? shareOwnerId.value
    }
  } catch (e) {
    shareErr.value = typeof e === 'string' ? e : '加载共享信息失败'
  } finally {
    shareLoading.value = false
  }
}

function closeShare() {
  shareTarget.value = null
  teacherSearch.value = ''
}

async function doShare() {
  shareErr.value = ''
  shareSaving.value = true
  try {
    const res = await questionApi.updateShares(shareTarget.value.id, {
      visibility: shareForm.value.visibility,
      sharedUserIds: shareForm.value.sharedUserIds
    })
    if (res && res.success === false) {
      shareErr.value = res.message || '保存失败'
      return
    }
    closeShare()
    await loadQuestions()   // 刷新行上的可见性徽章
  } catch (e) {
    shareErr.value = typeof e === 'string' ? e : '保存失败'
  } finally {
    shareSaving.value = false
  }
}

const filtered = computed(() => questions.value.filter(q => {
  if (filter.value.type && q.questionType !== filter.value.type) return false
  if (filter.value.difficulty && q.difficulty !== Number(filter.value.difficulty)) return false
  if (filter.value.tag && !q.knowledgeTags.some(t => t.id === Number(filter.value.tag))) return false
  return true
}))

function confirmDelete(q) { deleteTarget.value = q; deleteErr.value = '' }
async function doDelete() {
  deleteErr.value = ''
  try {
    const res = await questionApi.delete(deleteTarget.value.id)
    // 后端会以 {success:false, message:'无权删除此题目'} 的形式拒绝
    if (res && res.success === false) {
      deleteErr.value = res.message || '删除失败'
      return
    }
  } catch (e) {
    deleteErr.value = typeof e === 'string' ? e : '删除失败'
    return
  }
  deleteTarget.value = null
  await loadQuestions()
}

async function loadQuestions() {
  const res = await questionApi.listSummary()
  questions.value = res.data || []
}

onMounted(async () => {
  try {
    const [qRes, tRes] = await Promise.all([questionApi.listSummary(), questionApi.getTags()])
    questions.value = qRes.data || []
    tags.value = tRes.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal-box { max-height: 90vh; overflow-y: auto; }
/* ── 管理员共享设置：教师选择器 ── */
.share-chips { display: flex; flex-wrap: wrap; gap: 4px; padding: 4px 0; }
.share-chip { display: inline-flex; align-items: center; gap: 4px; }
.chip-x { border: none; background: none; cursor: pointer; color: inherit; padding: 0; font-size: 10px; line-height: 1; }
.share-list { max-height: 180px; overflow-y: auto; margin-top: 6px; border: 1px solid var(--c-border); border-radius: var(--radius-sm); }
.share-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 8px; font-size: 13px; cursor: pointer; border-bottom: 1px solid var(--c-border); }
.share-item:last-child { border-bottom: none; }
.share-item:hover { background: var(--c-surface2); }
.share-item.picked { background: var(--c-primary-bg); }
</style>
