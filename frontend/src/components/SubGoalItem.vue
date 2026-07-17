<template>
  <div class="sub-node">
    <div class="sub-item">
      <div class="connector">
        <div class="v-line top"></div>
        <div class="node-dot"></div>
        <div class="v-line bottom" :class="{ invisible: isLast }"></div>
      </div>
      <div class="h-line"></div>

      <div class="sub-header" @click="toggleExpanded">
        <span class="sub-toggle" v-if="goal.subGoals?.length > 0">{{ isExpanded ? '▾' : '▸' }}</span>

        <div class="sub-body">
          <div class="sub-title-row">
            <span class="sub-title">{{ goal.title }}</span>
            <StatusBadge :status="goal.status" small />
            <span class="sub-depth">层级 {{ goal.depth || 1 }}</span>
          </div>
          <div v-if="goal.description" class="sub-desc">{{ goal.description }}</div>
          <div class="sub-meta">
            <span>预计：{{ goal.plannedStart }} ~ {{ goal.plannedEnd }}</span>
            <span v-if="goal.actualStart">实际开始：{{ goal.actualStart }}</span>
            <span v-if="goal.actualEnd">实际完成：{{ goal.actualEnd }}</span>
            <span>实施者：{{ goal.owners || '—' }}</span>
            <span v-if="goal.classGroupName" class="class-badge">{{ goal.classGroupName }}</span>
          </div>

          <!-- 关联作业（老师和学生都可见） -->
          <div v-if="goal.assignmentIds?.length" class="sub-assignments-row" style="margin-top:6px">
            <span class="sec-label">📝 关联作业：</span>
            <span v-for="(aid, i) in goal.assignmentIds" :key="aid" class="assignment-link">
              <a :href="isTeacher ? `/assignments/${aid}` : `/assignments/${aid}/do`" target="_blank" @click.stop>
                {{ goal.assignmentTitles?.[i] || '作业#' + aid }}
              </a>
              <span v-if="i < goal.assignmentIds.length - 1">、</span>
            </span>
          </div>

          <div class="progress-row" style="margin-top:6px">
            <div class="bar-bg" style="height:3px">
              <div class="bar-fill" :class="`fill-${goal.status.toLowerCase()}`"
                   :style="{ width: progressPercent + '%' }" style="height:100%"></div>
            </div>
            <span class="pct" style="font-size:10px">{{ progressPercent }}%</span>
          </div>

          <!-- 学生扩展区域 -->
          <div v-if="!isTeacher && goal.studentProgress !== undefined" class="sub-student-section" @click.stop>
            <div class="sub-my-progress">
              <div class="my-progress-row" style="margin-top:4px">
                <span class="my-progress-label">我的进度：</span>
                <input type="range" min="0" max="100" step="5"
                  :value="goal.studentProgress" @input="onMyProgressChange($event.target.value)"
                  class="my-progress-slider" />
                <span class="pct" style="font-size:10px">{{ goal.studentProgress }}%</span>
              </div>
              <div class="my-dates-row">
                <label class="date-label">开始：</label>
                <input type="date" :value="goal.myActualStart" @change="onActualStartChange" class="date-input" />
                <label class="date-label">完成：</label>
                <input type="date" :value="goal.myActualEnd" @change="onActualEndChange" class="date-input" />
              </div>
            </div>

          </div>

          <!-- 公开评论区（老师和学生共用） -->
          <div class="sub-comments-section" @click.stop>
            <button class="btn-comment-toggle" @click="commentsExpanded = !commentsExpanded">
              💬 公开讨论（{{ localComments.length }}）{{ commentsExpanded ? '▾' : '▸' }}
            </button>
            <Transition name="slide">
              <div v-if="commentsExpanded" class="comments-body">
                <div v-if="localComments.length" class="comment-list">
                  <div v-for="c in localComments" :key="c.id" class="comment-item">
                    <div class="comment-meta">
                      <span class="comment-author">
                        <span :class="['author-role', c.authorRole === 'TEACHER' ? 'teacher' : 'student']">
                          {{ c.authorRole === 'TEACHER' ? '老师' : '学生' }}
                        </span>
                        {{ c.authorName || c.studentName }}
                      </span>
                      <span>{{ fmtTime(c.createdAt) }}</span>
                      <button v-if="c.own" class="comment-del" @click="deleteComment(c.id)">删除</button>
                    </div>
                    <div class="comment-content" v-html="renderLatex(c.content)"></div>
                    <div v-if="c.imageUrls?.length" class="comment-attachments">
                      <a v-for="(file, j) in getAttachments(c.imageUrls)" :key="j"
                         :href="file.url" class="attachment-link" download target="_blank">📎 {{ file.name }}</a>
                    </div>
                  </div>
                </div>
                <div v-else class="comment-empty">暂无讨论</div>
                <div v-if="canWriteComment" class="comment-input-area">
                  <textarea v-model="newComment" placeholder="输入评论…" rows="2"></textarea>
                  <div v-if="uploadedFiles.length" class="file-preview-list">
                    <div v-for="(f, i) in uploadedFiles" :key="i" class="file-preview-item">
                      <span class="file-preview-icon">📎</span>
                      <span class="file-preview-name">{{ f.name }}</span>
                      <span class="file-preview-size">{{ formatFileSize(f.size) }}</span>
                      <button class="file-preview-remove" @click="removeFile(i)">✕</button>
                    </div>
                  </div>
                  <div class="comment-actions">
                    <button class="btn-upload" @click="triggerUpload">📎 附件</button>
                    <input ref="fileInput" type="file" multiple style="display:none" @change="onUploadFiles" />
                    <button class="btn-send" :disabled="!newComment.trim() && !uploadedFiles.length" @click="submitComment">发送</button>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <!-- 私密评论区（仅老师或相关学生可见） -->
          <div class="sub-private-comments-section" @click.stop>
            <button class="btn-private-toggle" @click="togglePrivateComments">
              🔒 私密讨论（{{ localPrivateComments.length }}）{{ privateCommentsExpanded ? '▾' : '▸' }}
            </button>
            <Transition name="slide">
              <div v-if="privateCommentsExpanded" class="comments-body">
                <div v-if="localPrivateComments.length" class="comment-list">
                  <div v-for="c in localPrivateComments" :key="c.id" class="comment-item">
                    <div class="comment-meta">
                      <span class="comment-author">
                        <span :class="['author-role', c.authorRole === 'TEACHER' ? 'teacher' : 'student']">
                          {{ c.authorRole === 'TEACHER' ? '老师' : '学生' }}
                        </span>
                        {{ c.authorName || c.studentName }}
                        <span v-if="c.targetStudentId && c.targetStudentName" class="target-student">
                          → {{ c.targetStudentName }}
                        </span>
                      </span>
                      <span>{{ fmtTime(c.createdAt) }}</span>
                      <button v-if="c.own" class="comment-del" @click="deletePrivateComment(c.id)">删除</button>
                    </div>
                    <div class="comment-content" v-html="renderLatex(c.content)"></div>
                    <div v-if="c.imageUrls?.length" class="comment-attachments">
                      <a v-for="(file, j) in getAttachments(c.imageUrls)" :key="j"
                         :href="file.url" class="attachment-link" download target="_blank">📎 {{ file.name }}</a>
                    </div>
                  </div>
                </div>
                <div v-else class="comment-empty">暂无私密讨论</div>
                <!-- 老师：选择学生发送私密评论 -->
                <div v-if="isTeacher && goal.canComment" class="comment-input-area">
                  <div class="private-target-select">
                    <label>发送给：</label>
                    <select v-model="privateTargetStudentId">
                      <option :value="null" disabled>选择学生…</option>
                      <option v-for="(sid, idx) in goal.assigneeIds || []" :key="sid" :value="sid">
                        {{ goal.assigneeNames?.[idx] || '学生#' + sid }}
                      </option>
                    </select>
                  </div>
                  <textarea v-model="newPrivateComment" placeholder="输入私密评论（仅目标学生可见）…" rows="2"></textarea>
                  <div v-if="privateUploadedFiles.length" class="file-preview-list">
                    <div v-for="(f, i) in privateUploadedFiles" :key="i" class="file-preview-item">
                      <span class="file-preview-icon">📎</span>
                      <span class="file-preview-name">{{ f.name }}</span>
                      <span class="file-preview-size">{{ formatFileSize(f.size) }}</span>
                      <button class="file-preview-remove" @click="removePrivateFile(i)">✕</button>
                    </div>
                  </div>
                  <div class="comment-actions">
                    <button class="btn-upload" @click="triggerPrivateUpload">📎 附件</button>
                    <input ref="privateFileInput" type="file" multiple style="display:none" @change="onPrivateUploadFiles" />
                    <button class="btn-send" :disabled="!newPrivateComment.trim() || !privateTargetStudentId" @click="submitPrivateComment">发送</button>
                  </div>
                </div>
                <!-- 学生：回复私密评论 -->
                <div v-else-if="!isTeacher && goal.studentProgress !== undefined" class="comment-input-area">
                  <textarea v-model="newPrivateComment" placeholder="回复老师（私密）…" rows="2"></textarea>
                  <div v-if="privateUploadedFiles.length" class="file-preview-list">
                    <div v-for="(f, i) in privateUploadedFiles" :key="i" class="file-preview-item">
                      <span class="file-preview-icon">📎</span>
                      <span class="file-preview-name">{{ f.name }}</span>
                      <span class="file-preview-size">{{ formatFileSize(f.size) }}</span>
                      <button class="file-preview-remove" @click="removePrivateFile(i)">✕</button>
                    </div>
                  </div>
                  <div class="comment-actions">
                    <button class="btn-upload" @click="triggerPrivateUpload">📎 附件</button>
                    <input ref="privateFileInput" type="file" multiple style="display:none" @change="onPrivateUploadFiles" />
                    <button class="btn-send" :disabled="!newPrivateComment.trim()" @click="submitPrivateComment">发送</button>
                  </div>
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <div v-if="isTeacher" class="sub-actions" @click.stop>
          <button class="btn-icon btn-add-sub" @click="$emit('addSub', goal)">+ 子</button>
          <button class="btn-icon" @click="$emit('editSub', { parentId: parentId, sub: goal })">编辑</button>
          <button class="btn-icon" @click="$emit('deleteSub', { parentId: parentId, subId: goal.id })">删除</button>
        </div>
      </div>
    </div>

    <Transition name="slide">
      <div v-if="isExpanded && goal.subGoals?.length > 0" class="nested-sub-list">
        <SubGoalItem
          v-for="(subGoal, idx) in goal.subGoals"
          :key="subGoal.id"
          :goal="subGoal"
          :parent-id="goal.id"
          :is-last="idx === goal.subGoals.length - 1"
          :is-teacher="isTeacher"
          @add-sub="$emit('addSub', $event)"
          @edit-sub="$emit('editSub', $event)"
          @delete-sub="$emit('deleteSub', $event)"
          @update-my-progress="$emit('updateMyProgress', $event)"
        />
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import StatusBadge from './StatusBadge.vue'
import SubGoalItem from './SubGoalItem.vue'
import { useGoalStore } from '@/stores/goalStore'

const props = defineProps({
  goal: { type: Object, required: true },
  parentId: { type: Number, required: true },
  isLast: { type: Boolean, default: false },
  isTeacher: { type: Boolean, default: true },
})

const emit = defineEmits(['addSub', 'editSub', 'deleteSub', 'updateMyProgress'])

const goalStore = useGoalStore()
const isExpanded = computed(() => goalStore.expandedGoals.has(props.goal.id))
const commentsExpanded = ref(false)
const privateCommentsExpanded = ref(false)
const newComment = ref('')
const newPrivateComment = ref('')
const fileInput = ref(null)
const privateFileInput = ref(null)
const localComments = ref([])
const localPrivateComments = ref([])
const uploadedFiles = ref([])
const privateUploadedFiles = ref([])
const privateTargetStudentId = ref(null)

const canWriteComment = computed(() => {
  if (props.isTeacher) {
    return props.goal.canComment || false
  }
  return props.goal.studentProgress !== undefined
})

watch(() => props.goal.id, async () => {
  // 有条件加载评论：老师仅在有评论权限时（自己创建的目标），学生仅在被分配时
  if (props.isTeacher && !props.goal.canComment) return
  if (!props.isTeacher && props.goal.studentProgress === undefined) return
  await loadComments()

  // 有条件加载私密评论
  const canSeePrivate = props.isTeacher
    ? props.goal.canComment
    : props.goal.studentProgress !== undefined
  if (canSeePrivate) {
    await loadPrivateComments()
  }
}, { immediate: true })

async function loadComments() {
  try {
    localComments.value = await goalStore.fetchComments(props.goal.id)
  } catch {
    localComments.value = []
  }
}

async function loadPrivateComments() {
  try {
    localPrivateComments.value = await goalStore.fetchPrivateComments(props.goal.id)
  } catch {
    localPrivateComments.value = []
  }
}

function toggleExpanded() {
  goalStore.toggleExpanded(props.goal.id)
}

function togglePrivateComments() {
  privateCommentsExpanded.value = !privateCommentsExpanded.value
  if (privateCommentsExpanded.value && !localPrivateComments.value.length) {
    loadPrivateComments()
  }
}

const progressPercent = computed(() => {
  if (!props.isTeacher && props.goal.studentProgress !== undefined) {
    return props.goal.studentProgress
  }
  return props.goal.progress || 0
})

function onMyProgressChange(val) {
  emit('updateMyProgress', { goalId: props.goal.id, progress: parseInt(val), status: null })
}

function onActualStartChange(e) {
  emit('updateMyProgress', { goalId: props.goal.id, actualStart: e.target.value || null })
}

function onActualEndChange(e) {
  emit('updateMyProgress', { goalId: props.goal.id, actualEnd: e.target.value || null })
}

async function submitComment() {
  if (!newComment.value.trim() && !uploadedFiles.value.length) return
  try {
    const attachmentUrls = uploadedFiles.value.map(f => f.url)
    await goalStore.addComment(props.goal.id, {
      content: newComment.value,
      imageUrls: attachmentUrls,
      attachmentNames: uploadedFiles.value.map(f => f.name),
    })
    newComment.value = ''
    uploadedFiles.value = []
    await loadComments()
  } catch (e) {
    alert('评论发送失败')
  }
}

async function deleteComment(commentId) {
  if (!confirm('确认删除？')) return
  try {
    await goalStore.deleteComment(props.goal.id, commentId)
    await loadComments()
  } catch (e) {
    alert('删除失败')
  }
}

async function submitPrivateComment() {
  const content = newPrivateComment.value.trim()
  if (!content && !privateUploadedFiles.value.length) return

  if (props.isTeacher && !privateTargetStudentId.value) {
    alert('请选择要发送的学生')
    return
  }

  try {
    const attachmentUrls = privateUploadedFiles.value.map(f => f.url)
    const payload = {
      content,
      imageUrls: attachmentUrls,
      attachmentNames: privateUploadedFiles.value.map(f => f.name),
      visibility: 'PRIVATE_TO_STUDENT',
    }
    if (props.isTeacher && privateTargetStudentId.value) {
      payload.targetStudentId = privateTargetStudentId.value
    }
    await goalStore.addPrivateComment(props.goal.id, payload)
    newPrivateComment.value = ''
    privateUploadedFiles.value = []
    privateTargetStudentId.value = null
    await loadPrivateComments()
  } catch (e) {
    alert('私密评论发送失败')
  }
}

async function deletePrivateComment(commentId) {
  if (!confirm('确认删除此私密评论？')) return
  try {
    await goalStore.deletePrivateComment(props.goal.id, commentId)
    await loadPrivateComments()
  } catch (e) {
    alert('删除失败')
  }
}

function triggerPrivateUpload() { privateFileInput.value?.click() }

async function onPrivateUploadFiles(e) {
  const files = [...e.target.files]
  if (!files.length) return
  try {
    const { goalApi } = await import('@/api/goalApi')
    for (const file of files) {
      const result = await goalApi.uploadFile(file)
      if (result.url) {
        privateUploadedFiles.value.push({ url: result.url, name: result.originalName || file.name, size: file.size })
      }
    }
  } catch (err) { alert('文件上传失败') }
  e.target.value = ''
}

function removePrivateFile(index) { privateUploadedFiles.value.splice(index, 1) }

function triggerUpload() { fileInput.value?.click() }

async function onUploadFiles(e) {
  const files = [...e.target.files]
  if (!files.length) return
  try {
    const { goalApi } = await import('@/api/goalApi')
    for (const file of files) {
      const result = await goalApi.uploadFile(file)
      if (result.url) {
        uploadedFiles.value.push({ url: result.url, name: result.originalName || file.name, size: file.size })
      }
    }
  } catch (err) { alert('文件上传失败') }
  e.target.value = ''
}

function removeFile(index) { uploadedFiles.value.splice(index, 1) }

function getAttachments(imageUrls) {
  if (!imageUrls || !imageUrls.length) return []
  return imageUrls.map((item, idx) => {
    if (typeof item === 'string') {
      const sepIdx = item.lastIndexOf('::')
      if (sepIdx > 0 && sepIdx + 2 < item.length) {
        return { url: item.substring(0, sepIdx), name: item.substring(sepIdx + 2) }
      }
      return { url: item, name: extractFileName(item) || `附件 ${idx + 1}` }
    }
    return { url: item.url || item, name: item.name || extractFileName(item.url) || `附件 ${idx + 1}` }
  })
}

function extractFileName(url) {
  if (!url) return ''
  const parts = url.split('/')
  return parts[parts.length - 1] || ''
}

function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function fmtTime(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function renderLatex(text) {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
}
</script>

<style scoped>
.sub-node { width: 100%; }
.sub-item {
  display: flex; align-items: flex-start; gap: 0;
  padding: 10px 16px; border-top: 0.5px solid #f0f0ee;
}
.sub-item:hover { background: #fafaf9; }
.connector {
  display: flex; flex-direction: column; align-items: center;
  width: 26px; flex-shrink: 0; align-self: stretch; padding-top: 2px;
}
.v-line { width: 1px; background: #ddd; flex: 1; min-height: 6px; }
.v-line.invisible { background: transparent; }
.node-dot {
  width: 8px; height: 8px; border-radius: 50%;
  border: 1.5px solid #ccc; background: #fff; flex-shrink: 0;
}
.h-line { width: 12px; height: 1px; background: #ddd; flex-shrink: 0; margin-top: 12px; }
.sub-header {
  flex: 1; min-width: 0; display: flex; align-items: flex-start; gap: 0;
  cursor: pointer; user-select: none; padding: 2px 0;
}
.sub-header:hover { background: #f5f5f3; border-radius: 6px; }
.sub-toggle {
  font-size: 12px; color: #999; margin-top: 2px; flex-shrink: 0;
  margin-right: 8px;
}
.sub-body { flex: 1; min-width: 0; }
.sub-title-row { display: flex; align-items: center; gap: 7px; margin-bottom: 4px; flex-wrap: wrap; }
.sub-title { font-size: 13px; color: #111; }
.sub-depth { font-size: 10px; color: #666; background: #f0f8ff; border-radius: 6px; padding: 1px 5px; }
.sub-desc { font-size: 11px; color: #888; margin-bottom: 5px; line-height: 1.5; }
.sub-meta { display: flex; gap: 12px; font-size: 11px; color: #888; flex-wrap: wrap; }
.sub-meta .class-badge {
  background: #e8f4fd; color: #185FA5; border-radius: 6px;
  padding: 0 5px; font-size: 10px; font-weight: 500;
}
.sub-actions { display: flex; gap: 4px; flex-shrink: 0; margin-top: 2px; }
.btn-icon {
  border: 0.5px solid #e0e0e0; background: transparent;
  border-radius: 6px; padding: 3px 8px; font-size: 11px; cursor: pointer; color: #888;
}
.btn-icon:hover { background: #f5f5f3; color: #333; }
.btn-add-sub {
  border: 0.5px solid #1D9E75; color: #0F6E56; background: transparent;
  border-radius: 6px; padding: 2px 6px; font-size: 10px; cursor: pointer; font-weight: 500;
}
.btn-add-sub:hover { background: #E1F5EE; }
.nested-sub-list { margin-left: 26px; padding-left: 10px; border-left: 1px solid #eee; width: calc(100% - 26px); }
.slide-enter-active, .slide-leave-active { transition: max-height .2s ease, opacity .2s ease; overflow: hidden; }
.slide-enter-from, .slide-leave-to { max-height: 0; opacity: 0; }
.slide-enter-to, .slide-leave-from { max-height: 2000px; opacity: 1; }

/* 学生区域 */
.sub-student-section { margin-top: 6px; display: flex; flex-direction: column; gap: 4px; }
.my-progress-row {
  display: flex; align-items: center; gap: 8px;
  background: #f0faf5; border-radius: 6px; padding: 3px 8px;
}
.my-progress-label { font-size: 10px; color: #0F6E56; font-weight: 500; white-space: nowrap; }
.my-progress-slider { flex: 1; height: 3px; cursor: pointer; }
.my-dates-row { display: flex; align-items: center; gap: 4px; background: #fafafa; border-radius: 4px; padding: 2px 6px; }
.date-label { font-size: 10px; color: #888; }
.date-input { border: 0.5px solid #ccc; border-radius: 3px; padding: 1px 4px; font-size: 10px; width: 95px; }
.sub-assignments-row { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; background: #fafafa; border-radius: 4px; padding: 2px 6px; }
.sec-label { font-size: 11px; color: #555; }
.assignment-link a { color: #185FA5; font-size: 11px; text-decoration: none; }
.assignment-link a:hover { text-decoration: underline; }
.pct { font-size: 10px; color: #888; min-width: 24px; text-align: right; }

/* 公开讨论区 */
.sub-comments-section { margin-top: 2px; }
.btn-comment-toggle {
  border: none; background: #f5f5f3; border-radius: 4px;
  padding: 2px 8px; font-size: 10px; cursor: pointer; color: #555;
}
.btn-comment-toggle:hover { background: #e8e8e8; }
.comments-body { padding: 6px 8px; border: 0.5px solid #eee; border-radius: 6px; margin-top: 4px; background: #fafafa; }
.comment-list { margin-bottom: 6px; }
.comment-item { margin-bottom: 6px; padding-bottom: 4px; border-bottom: 0.5px solid #f0f0ee; }
.comment-item:last-child { border-bottom: none; }
.comment-meta { font-size: 10px; color: #aaa; display: flex; justify-content: space-between; align-items: center; }
.comment-author { display: flex; align-items: center; gap: 3px; }
.author-role { font-size: 9px; padding: 1px 4px; border-radius: 3px; font-weight: 500; }
.author-role.teacher { background: #e8f4fd; color: #185FA5; }
.author-role.student { background: #E1F5EE; color: #0F6E56; }
.comment-del { border: none; background: none; color: #e24b4a; cursor: pointer; font-size: 10px; }
.comment-content { font-size: 11px; color: #333; margin: 2px 0; line-height: 1.4; }
.comment-attachments { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 2px; }
.attachment-link {
  display: inline-flex; align-items: center; gap: 2px;
  font-size: 10px; color: #185FA5; text-decoration: none;
  background: #f0f6ff; padding: 1px 6px; border-radius: 3px;
}
.attachment-link:hover { background: #dce8f8; text-decoration: underline; }
.comment-empty { font-size: 10px; color: #aaa; text-align: center; padding: 4px 0; }
.comment-input-area { border-top: 0.5px solid #eee; padding-top: 4px; }
.comment-input-area textarea {
  width: 100%; border: 0.5px solid #ccc; border-radius: 4px; padding: 3px 6px;
  font-size: 11px; resize: vertical; box-sizing: border-box;
}
.file-preview-list {
  display: flex; flex-direction: column; gap: 3px;
  margin: 4px 0; max-height: 100px; overflow-y: auto;
}
.file-preview-item {
  display: flex; align-items: center; gap: 4px;
  background: #f5f7fa; border: 0.5px solid #e0e4e8; border-radius: 4px;
  padding: 2px 6px; font-size: 10px;
}
.file-preview-icon { font-size: 11px; }
.file-preview-name { flex: 1; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-preview-size { color: #999; font-size: 9px; white-space: nowrap; }
.file-preview-remove {
  border: none; background: none; color: #e24b4a; cursor: pointer; font-size: 11px; padding: 0 2px;
}
.file-preview-remove:hover { color: #c0392b; }
.comment-actions { display: flex; gap: 4px; justify-content: flex-end; margin-top: 3px; }
.btn-upload, .btn-send {
  border: 0.5px solid #ccc; border-radius: 4px; padding: 2px 8px;
  font-size: 10px; cursor: pointer; background: #fff; color: #666;
}
.btn-send { background: #1D9E75; color: #fff; border-color: #1D9E75; }
.btn-send:hover { background: #0F6E56; }
.btn-send:disabled { opacity: .5; cursor: not-allowed; }

/* 私密讨论区 */
.sub-private-comments-section { margin-top: 2px; }
.btn-private-toggle {
  border: none; background: #fff0f0; border-radius: 4px;
  padding: 2px 8px; font-size: 10px; cursor: pointer; color: #A32D2D;
}
.btn-private-toggle:hover { background: #ffe0e0; }
.private-target-select {
  display: flex; align-items: center; gap: 6px; margin-bottom: 4px;
}
.private-target-select label { font-size: 10px; color: #A32D2D; font-weight: 500; white-space: nowrap; }
.private-target-select select {
  border: 0.5px solid #e0b0b0; border-radius: 4px; padding: 2px 6px;
  font-size: 10px; flex: 1; background: #fffdf5; color: #333;
}
.target-student {
  font-size: 9px; color: #A32D2D; background: #fff0f0;
  border-radius: 3px; padding: 1px 4px; font-weight: 500;
}
</style>
