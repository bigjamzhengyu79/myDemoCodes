<template>
  <div class="parent-card">
    <div class="parent-header" @click="toggleExpanded">
      <span class="toggle" v-if="goal.subGoals?.length > 0">{{ isExpanded ? '▾' : '▸' }}</span>

      <div class="parent-info">
        <div class="title-row">
          <span class="title">{{ goal.title }}</span>
          <StatusBadge :status="goal.status" />
          <span class="sub-count" :class="{ 'has-subs': goal.subGoals?.length > 0 }">
            {{ goal.subGoals?.length || 0 }} 个子目标
          </span>
          <span class="depth-badge">层级 {{ goal.depth || 1 }}</span>
        </div>

        <div class="meta-row">
          <span>预计：{{ goal.plannedStart }} ~ {{ goal.plannedEnd }}</span>
          <span v-if="goal.actualStart">实际开始：{{ goal.actualStart }}</span>
          <span v-if="goal.actualEnd">实际完成：{{ goal.actualEnd }}</span>
          <span>实施者：{{ goal.owners || '—' }}</span>
          <span v-if="goal.classGroupName" class="class-badge">{{ goal.classGroupName }}</span>
        </div>

        <div class="progress-row">
          <div class="bar-bg">
            <div class="bar-fill" :class="`fill-${goal.status.toLowerCase()}`"
                 :style="{ width: progressPercent + '%' }"></div>
          </div>
          <span class="pct">{{ progressPercent }}%</span>
        </div>

        <!-- 学生个人进度（仅学生可见） -->
        <div v-if="!isTeacher && goal.studentProgress !== undefined" class="student-section">
          <div class="my-progress-row">
            <span class="my-progress-label">我的进度：</span>
            <input
              type="range" min="0" max="100" step="5"
              :value="goal.studentProgress"
              @input="onMyProgressChange($event.target.value)"
              class="my-progress-slider"
            />
            <span class="pct">{{ goal.studentProgress }}%</span>
          </div>

          <div class="my-dates-row">
            <div class="date-field">
              <label>实际开始</label>
              <input type="date" :value="goal.myActualStart" @change="onActualStartChange" />
            </div>
            <div class="date-field">
              <label>实际完成</label>
              <input type="date" :value="goal.myActualEnd" @change="onActualEndChange" />
            </div>
          </div>

          <div v-if="goal.assignmentIds?.length" class="my-assignments-row">
            <span class="section-label">📝 关联作业：</span>
            <span v-for="(aid, i) in goal.assignmentIds" :key="aid" class="assignment-link">
              <a :href="`/assignments/${aid}/do`" target="_blank" @click.stop>
                {{ goal.assignmentTitles?.[i] || '作业#' + aid }}
              </a>
              <span v-if="i < goal.assignmentIds.length - 1">、</span>
            </span>
          </div>
        </div>

        <!-- 公开评论区（老师和学生共用） -->
        <div class="public-comments-section" @click.stop>
          <div class="comments-header" @click="toggleComments">
            <span>💬 公开讨论（{{ localComments.length }}）</span>
            <span class="toggle-icon">{{ commentsExpanded ? '▾' : '▸' }}</span>
          </div>
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
                    <a
                      v-for="(file, j) in getAttachments(c.imageUrls)"
                      :key="j"
                      :href="file.url"
                      class="attachment-link"
                      download
                      target="_blank"
                    >📎 {{ file.name }}</a>
                  </div>
                </div>
              </div>
              <div v-else class="comment-empty">暂无讨论</div>
              <!-- 评论输入（学生：被分配时可评论；老师：自己创建的目标时可评论） -->
              <div v-if="canWriteComment" class="comment-input-area">
                <textarea v-model="newComment" placeholder="输入评论（支持 LaTeX）…" rows="2"></textarea>
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
              <div v-else-if="!isTeacher && goal.studentProgress === undefined" class="comment-empty-sm">
                仅被分配的学生可评论
              </div>
            </div>
          </Transition>
        </div>
      </div>

      <div v-if="isTeacher" class="actions" @click.stop>
        <button class="btn-sub" @click="$emit('addSub', goal)">+ 子目标</button>
        <button class="btn-icon" @click="$emit('edit', goal)">编辑</button>
        <button class="btn-icon" @click="$emit('delete', goal.id)">删除</button>
      </div>
    </div>

    <!-- 子目标列表 -->
    <Transition name="slide">
      <div v-if="isExpanded && goal.subGoals?.length > 0" class="sub-list">
        <SubGoalItem
          v-for="(sub, idx) in goal.subGoals"
          :key="sub.id"
          :goal="sub"
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

    <Transition name="slide">
      <div v-if="isExpanded" class="expanded-section">
        <div v-if="!goal.subGoals?.length" class="sub-empty">
          <div class="empty-icon">📋</div>
          <div class="empty-text">暂无子目标</div>
          <div class="empty-action" v-if="isTeacher">点击上方的「+ 子目标」按钮开始分解任务</div>
        </div>
        <div v-else class="expand-hint">
          子目标已显示在上面。如需添加更多子目标，请点击「+ 子目标」按钮。
        </div>
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
  isTeacher: { type: Boolean, default: true },
})

const emit = defineEmits(['edit', 'delete', 'addSub', 'editSub', 'deleteSub', 'updateMyProgress'])

const goalStore = useGoalStore()
const isExpanded = computed(() => goalStore.expandedGoals.has(props.goal.id))
const commentsExpanded = ref(false)
const newComment = ref('')
const fileInput = ref(null)
const localComments = ref([])
const uploadedFiles = ref([])

// 判断当前用户是否有权评论
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
}, { immediate: true })

async function loadComments() {
  try {
    localComments.value = await goalStore.fetchComments(props.goal.id)
  } catch {
    localComments.value = []
  }
}

function toggleExpanded() {
  goalStore.toggleExpanded(props.goal.id)
}

function toggleComments() {
  commentsExpanded.value = !commentsExpanded.value
}

const progressPercent = computed(() => {
  if (!props.isTeacher && props.goal.studentProgress !== undefined) {
    return props.goal.studentProgress
  }
  return props.goal.progress || 0
})

function onMyProgressChange(val) {
  emit('updateMyProgress', {
    goalId: props.goal.id,
    progress: parseInt(val),
    status: null,
  })
}

function onActualStartChange(e) {
  emit('updateMyProgress', {
    goalId: props.goal.id,
    actualStart: e.target.value || null,
  })
}

function onActualEndChange(e) {
  emit('updateMyProgress', {
    goalId: props.goal.id,
    actualEnd: e.target.value || null,
  })
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
    alert('评论发送失败：' + (e.message || '未知错误'))
  }
}

async function deleteComment(commentId) {
  if (!confirm('确认删除此评论？')) return
  try {
    await goalStore.deleteComment(props.goal.id, commentId)
    await loadComments()
  } catch (e) {
    alert('删除失败')
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

async function onUploadFiles(e) {
  const files = [...e.target.files]
  if (!files.length) return
  try {
    const { goalApi } = await import('@/api/goalApi')
    for (const file of files) {
      const result = await goalApi.uploadFile(file)
      if (result.url) {
        uploadedFiles.value.push({
          url: result.url,
          name: result.originalName || file.name,
          size: file.size,
        })
      }
    }
  } catch (err) {
    alert('文件上传失败')
  }
  e.target.value = ''
}

function removeFile(index) {
  uploadedFiles.value.splice(index, 1)
}

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
.parent-card {
  background: #fff; border: 0.5px solid #ddd;
  border-radius: 12px; overflow: hidden;
}
.parent-header {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px 16px; cursor: pointer; user-select: none;
}
.parent-header:hover { background: #f8f8f7; }
.toggle { font-size: 12px; color: #999; margin-top: 2px; flex-shrink: 0; }
.parent-info { flex: 1; min-width: 0; }
.title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 6px; }
.title { font-size: 14px; font-weight: 500; color: #111; }
.sub-count {
  font-size: 11px; color: #999; background: #f2f2f0;
  border-radius: 10px; padding: 1px 7px; margin-left: 2px;
}
.sub-count.has-subs { color: #1D9E75; background: #E1F5EE; font-weight: 500; }
.depth-badge {
  font-size: 10px; color: #666; background: #e8f4fd;
  border-radius: 8px; padding: 1px 6px;
}
.meta-row {
  display: flex; gap: 14px; font-size: 12px;
  color: #888; flex-wrap: wrap; margin-bottom: 8px;
}
.class-badge {
  background: #e8f4fd; color: #185FA5; border-radius: 8px;
  padding: 0 6px; font-size: 11px; font-weight: 500;
}
.progress-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.bar-bg { flex: 1; height: 5px; background: #f0f0ee; border-radius: 3px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 3px; transition: width .3s; }
.fill-todo { background: #1D9E75; }
.fill-in_progress { background: #378ADD; }
.fill-done { background: #639922; }
.fill-late { background: #E24B4A; }
.pct { font-size: 11px; color: #888; min-width: 28px; text-align: right; }

/* 学生区域 */
.student-section {
  margin-top: 8px; display: flex; flex-direction: column; gap: 6px;
}
.my-progress-row {
  display: flex; align-items: center; gap: 8px;
  background: #f0faf5; border-radius: 6px; padding: 4px 8px;
}
.my-progress-label { font-size: 11px; color: #0F6E56; font-weight: 500; white-space: nowrap; }
.my-progress-slider { flex: 1; height: 4px; cursor: pointer; }
.my-dates-row {
  display: flex; gap: 12px; align-items: center;
  background: #fafafa; border-radius: 6px; padding: 4px 8px;
}
.date-field { display: flex; align-items: center; gap: 4px; }
.date-field label { font-size: 11px; color: #888; white-space: nowrap; }
.date-field input {
  border: 0.5px solid #ccc; border-radius: 4px; padding: 2px 6px;
  font-size: 11px; width: 130px;
}
.my-assignments-row {
  display: flex; align-items: center; gap: 4px; flex-wrap: wrap;
  background: #fafafa; border-radius: 6px; padding: 4px 8px;
}
.section-label { font-size: 12px; color: #555; }
.assignment-link a { color: #185FA5; font-size: 12px; text-decoration: none; }
.assignment-link a:hover { text-decoration: underline; }

/* 公开讨论区 */
.public-comments-section {
  margin-top: 8px;
  border: 0.5px solid #e8e8e8; border-radius: 8px; overflow: hidden;
}
.comments-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 10px; cursor: pointer; background: #fafafa; font-size: 12px; color: #555;
}
.comments-header:hover { background: #f5f5f3; }
.toggle-icon { font-size: 10px; color: #999; }
.comments-body { padding: 8px 10px; border-top: 0.5px solid #eee; }
.comment-list { margin-bottom: 8px; }
.comment-item { margin-bottom: 8px; padding-bottom: 6px; border-bottom: 0.5px solid #f0f0ee; }
.comment-item:last-child { border-bottom: none; }
.comment-meta { font-size: 11px; color: #aaa; display: flex; justify-content: space-between; align-items: center; }
.comment-author { display: flex; align-items: center; gap: 4px; }
.author-role {
  font-size: 10px; padding: 1px 5px; border-radius: 4px; font-weight: 500;
}
.author-role.teacher { background: #e8f4fd; color: #185FA5; }
.author-role.student { background: #E1F5EE; color: #0F6E56; }
.comment-del { border: none; background: none; color: #e24b4a; cursor: pointer; font-size: 11px; }
.comment-del:hover { text-decoration: underline; }
.comment-content { font-size: 12px; color: #333; margin: 2px 0; line-height: 1.5; }
.comment-attachments { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 4px; }
.attachment-link {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 11px; color: #185FA5; text-decoration: none;
  background: #f0f6ff; padding: 2px 8px; border-radius: 4px;
}
.attachment-link:hover { background: #dce8f8; text-decoration: underline; }
.comment-empty { font-size: 11px; color: #aaa; text-align: center; padding: 8px 0; }
.comment-empty-sm { font-size: 10px; color: #bbb; text-align: center; padding: 4px 0; }
.comment-input-area { border-top: 0.5px solid #eee; padding-top: 6px; }
.comment-input-area textarea {
  width: 100%; border: 0.5px solid #ccc; border-radius: 6px; padding: 5px 8px;
  font-size: 12px; resize: vertical; box-sizing: border-box;
}
.file-preview-list {
  display: flex; flex-direction: column; gap: 4px;
  margin: 6px 0; max-height: 120px; overflow-y: auto;
}
.file-preview-item {
  display: flex; align-items: center; gap: 6px;
  background: #f5f7fa; border: 0.5px solid #e0e4e8; border-radius: 6px;
  padding: 4px 8px; font-size: 11px;
}
.file-preview-icon { font-size: 13px; }
.file-preview-name { flex: 1; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-preview-size { color: #999; font-size: 10px; white-space: nowrap; }
.file-preview-remove {
  border: none; background: none; color: #e24b4a; cursor: pointer; font-size: 12px; padding: 0 2px;
}
.file-preview-remove:hover { color: #c0392b; }
.comment-actions { display: flex; gap: 6px; justify-content: flex-end; margin-top: 4px; }
.btn-upload, .btn-send {
  border: 0.5px solid #ccc; border-radius: 6px; padding: 3px 10px;
  font-size: 11px; cursor: pointer; background: #fff; color: #666;
}
.btn-upload:hover { background: #f5f5f3; }
.btn-send { background: #1D9E75; color: #fff; border-color: #1D9E75; }
.btn-send:hover { background: #0F6E56; }
.btn-send:disabled { opacity: .5; cursor: not-allowed; }

.actions { display: flex; gap: 6px; flex-shrink: 0; margin-top: 2px; }
.btn-sub {
  border: 0.5px solid #1D9E75; color: #0F6E56; background: transparent;
  border-radius: 6px; padding: 3px 8px; font-size: 11px; cursor: pointer;
}
.btn-sub:hover { background: #E1F5EE; }
.btn-icon {
  border: 0.5px solid #e0e0e0; background: transparent;
  border-radius: 6px; padding: 3px 8px; font-size: 11px; cursor: pointer; color: #888;
}
.btn-icon:hover { background: #f5f5f3; color: #333; }
.sub-list { border-top: 0.5px solid #eee; }
.sub-empty { padding: 20px 16px; font-size: 12px; color: #aaa; text-align: center; }
.empty-icon { font-size: 24px; margin-bottom: 8px; }
.empty-text { font-weight: 500; margin-bottom: 4px; }
.empty-action { font-size: 11px; color: #888; }
.slide-enter-active, .slide-leave-active { transition: all .2s ease; overflow: hidden; }
.slide-enter-from, .slide-leave-to { max-height: 0; opacity: 0; }
.slide-enter-to, .slide-leave-from { max-height: 2000px; opacity: 1; }
.expanded-section { border-top: 0.5px solid #eee; padding: 10px 16px; background: #fafafa; }
.expand-hint { font-size: 12px; color: #666; text-align: center; }
</style>