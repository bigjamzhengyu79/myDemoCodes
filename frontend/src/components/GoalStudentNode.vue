<template>
  <div class="node-wrapper">
    <div class="node-header" @click="expanded = !expanded">
      <span class="toggle">{{ expanded ? '▾' : '▸' }}</span>
      <span class="node-title">{{ data.goalTitle }}</span>
      <span class="student-count">{{ data.studentProgresses?.length || 0 }} 名学生</span>
    </div>

    <Transition name="slide">
      <div v-if="expanded" class="node-body">
        <!-- 学生进度表格 -->
        <div v-if="data.studentProgresses?.length" class="progress-table">
          <div class="table-header">
            <span class="col-name">学生</span>
            <span class="col-progress">进度</span>
            <span class="col-status">状态</span>
            <span class="col-date">实际开始</span>
            <span class="col-date">实际结束</span>
            <span class="col-action">操作</span>
          </div>
          <div
            v-for="sp in data.studentProgresses"
            :key="sp.studentId"
            class="table-row"
          >
            <span class="col-name">{{ sp.studentName }}</span>
            <span class="col-progress">
              <div class="bar-bg">
                <div class="bar-fill" :style="{ width: sp.progress + '%' }"></div>
              </div>
              <span class="pct">{{ sp.progress }}%</span>
            </span>
            <span class="col-status">
              <span :class="['status-badge', statusClass(sp.status)]">{{ statusLabel(sp.status) }}</span>
            </span>
            <span class="col-date">{{ sp.actualStart || '—' }}</span>
            <span class="col-date">{{ sp.actualEnd || '—' }}</span>
            <span class="col-action">
              <button class="btn-private" @click.stop="togglePrivateComment(sp.studentId, sp.studentName)">
                💬 私密点评
              </button>
            </span>
          </div>
        </div>
        <div v-else class="no-students">暂无学生被分配到此目标</div>

        <!-- 私密点评面板 -->
        <div v-if="privateTarget" class="private-section">
          <div class="private-header">
            <span>🔒 私密点评 — {{ privateTarget.name }}</span>
            <button class="btn-close" @click="privateTarget = null">✕</button>
          </div>
          <div class="private-comments">
            <div v-if="privateComments.length" class="comment-list">
              <div v-for="c in privateComments" :key="c.id" class="comment-item">
                <div class="comment-meta">
                  <span class="comment-author">
                    <span :class="['author-role', c.authorRole === 'TEACHER' ? 'teacher' : 'student']">
                      {{ c.authorRole === 'TEACHER' ? '老师' : c.authorName }}
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
            <div v-else class="comment-empty">暂无私密对话</div>
            <div class="comment-input-area">
              <textarea v-model="privateNewComment" placeholder="输入私密点评…" rows="2"></textarea>
              <div v-if="privateUploadedFiles.length" class="file-preview-list">
                <div v-for="(f, i) in privateUploadedFiles" :key="i" class="file-preview-item">
                  <span class="file-preview-icon">📎</span>
                  <span class="file-preview-name">{{ f.name }}</span>
                  <span class="file-preview-size">{{ formatFileSize(f.size) }}</span>
                  <button class="file-preview-remove" @click="privateRemoveFile(i)">✕</button>
                </div>
              </div>
              <div class="comment-actions">
                <button class="btn-upload" @click="privateTriggerUpload">📎 附件</button>
                <input ref="privateFileInput" type="file" multiple style="display:none" @change="privateOnUploadFiles" />
                <button class="btn-send" :disabled="!privateNewComment.trim() && !privateUploadedFiles.length"
                        @click="submitPrivateComment">发送</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 公开评论汇总 -->
        <div v-if="data.studentComments?.length" class="comments-section">
          <div class="comments-title">📌 学生评论汇总</div>
          <div v-for="group in data.studentComments" :key="group.studentId" class="comment-group">
            <div class="comment-student" @click="toggleComments(group.studentId)">
              <span class="comment-name">{{ group.studentName }}</span>
              <span class="comment-count">（{{ group.comments?.length || 0 }} 条评论）</span>
            </div>
            <Transition name="slide">
              <div v-if="expandedComments.has(group.studentId)" class="comment-list">
                <div v-for="c in group.comments" :key="c.id" class="comment-item">
                  <div class="comment-meta">
                    <span class="comment-author">
                      <span :class="['author-role', c.authorRole === 'TEACHER' ? 'teacher' : 'student']">
                        {{ c.authorRole === 'TEACHER' ? '老师' : c.authorName }}
                      </span>
                    </span>
                    <span>{{ fmtTime(c.createdAt) }}</span>
                  </div>
                  <div class="comment-content" v-html="renderLatex(c.content)"></div>
                  <div v-if="c.imageUrls?.length" class="comment-attachments">
                    <a v-for="(file, j) in getAttachments(c.imageUrls)" :key="j"
                       :href="file.url" class="attachment-link" download target="_blank">📎 {{ file.name }}</a>
                  </div>
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <!-- 递归子目标 -->
        <div v-if="data.subGoals?.length" class="sub-nodes">
          <GoalStudentNode
            v-for="sub in data.subGoals"
            :key="sub.goalId"
            :data="sub"
            :depth="depth + 1"
          />
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { goalApi } from '@/api/goalApi'

const props = defineProps({
  data: { type: Object, required: true },
  depth: { type: Number, default: 0 },
})

const expanded = ref(props.depth === 0)
const expandedComments = ref(new Set())

// 私密点评
const privateTarget = ref(null) // { studentId, name }
const privateComments = ref([])
const privateNewComment = ref('')
const privateFileInput = ref(null)
const privateUploadedFiles = ref([])

function togglePrivateComment(studentId, studentName) {
  if (privateTarget.value?.studentId === studentId) {
    privateTarget.value = null
    return
  }
  privateTarget.value = { studentId, name: studentName }
  loadPrivateComments(studentId)
}

async function loadPrivateComments(studentId) {
  try {
    // 获取该目标下所有私密评论，前端按 studentId 过滤
    const allPrivate = await goalApi.getPrivateComments(props.data.goalId)
    privateComments.value = allPrivate.filter(c =>
      c.targetStudentId === studentId || c.authorId === studentId
    )
  } catch {
    privateComments.value = []
  }
}

async function submitPrivateComment() {
  if (!privateNewComment.value.trim() && !privateUploadedFiles.value.length) return
  try {
    const attachmentUrls = privateUploadedFiles.value.map(f => f.url)
    await goalApi.addComment(props.data.goalId, {
      content: privateNewComment.value,
      imageUrls: attachmentUrls,
      attachmentNames: privateUploadedFiles.value.map(f => f.name),
      visibility: 'PRIVATE_TO_STUDENT',
      targetStudentId: privateTarget.value.studentId,
    })
    privateNewComment.value = ''
    privateUploadedFiles.value = []
    await loadPrivateComments(privateTarget.value.studentId)
  } catch (e) {
    alert('私密点评发送失败：' + (e.message || '未知错误'))
  }
}

async function deletePrivateComment(commentId) {
  if (!confirm('确认删除此私密点评？')) return
  try {
    await goalApi.deleteComment(props.data.goalId, commentId)
    await loadPrivateComments(privateTarget.value.studentId)
  } catch (e) {
    alert('删除失败')
  }
}

function privateTriggerUpload() { privateFileInput.value?.click() }

async function privateOnUploadFiles(e) {
  const files = [...e.target.files]
  if (!files.length) return
  try {
    for (const file of files) {
      const result = await goalApi.uploadFile(file)
      if (result.url) {
        privateUploadedFiles.value.push({
          url: result.url,
          name: result.originalName || file.name,
          size: file.size,
        })
      }
    }
  } catch (err) { alert('文件上传失败') }
  e.target.value = ''
}

function privateRemoveFile(index) { privateUploadedFiles.value.splice(index, 1) }

function toggleComments(studentId) {
  if (expandedComments.value.has(studentId)) {
    expandedComments.value.delete(studentId)
  } else {
    expandedComments.value.add(studentId)
  }
}

function statusClass(s) {
  return { TODO: 'status-todo', IN_PROGRESS: 'status-progress', DONE: 'status-done', LATE: 'status-late' }[s] || ''
}

function statusLabel(s) {
  return { TODO: '未开始', IN_PROGRESS: '进行中', DONE: '已完成', LATE: '已延期' }[s] || s
}

function fmtTime(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function renderLatex(text) {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
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
</script>

<style scoped>
.node-wrapper {
  background: #fff; border: 0.5px solid #ddd; border-radius: 10px; overflow: hidden;
}
.node-header {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; cursor: pointer; user-select: none;
  background: #f8f8f7;
}
.node-header:hover { background: #f0f0ee; }
.toggle { font-size: 12px; color: #999; }
.node-title { font-size: 14px; font-weight: 500; color: #111; }
.student-count { font-size: 11px; color: #888; background: #e8f4fd; border-radius: 8px; padding: 1px 6px; margin-left: auto; }
.node-body { padding: 10px 14px; border-top: 0.5px solid #eee; }
.progress-table { margin-bottom: 12px; }
.table-header, .table-row {
  display: grid; grid-template-columns: 120px 1fr 90px 100px 100px 80px; gap: 8px;
  align-items: center; padding: 6px 0; font-size: 12px;
}
.table-header { color: #999; font-weight: 500; border-bottom: 0.5px solid #eee; }
.table-row { border-bottom: 0.5px solid #f5f5f5; }
.table-row:hover { background: #fafaf9; }
.col-name { font-weight: 500; color: #111; }
.col-progress { display: flex; align-items: center; gap: 6px; }
.bar-bg { flex: 1; height: 4px; background: #f0f0ee; border-radius: 2px; overflow: hidden; }
.bar-fill { height: 100%; background: #1D9E75; border-radius: 2px; transition: width .3s; }
.pct { font-size: 11px; color: #888; min-width: 28px; }
.status-badge { font-size: 11px; padding: 1px 6px; border-radius: 8px; }
.status-todo { background: #f0f0ee; color: #888; }
.status-progress { background: #e1f5ee; color: #0f6e56; }
.status-done { background: #e8f5e8; color: #3b6d11; }
.status-late { background: #fde8e8; color: #a32d2d; }
.col-action { text-align: center; }
.btn-private {
  border: 0.5px solid #185FA5; color: #185FA5; background: transparent;
  border-radius: 6px; padding: 2px 6px; font-size: 10px; cursor: pointer; white-space: nowrap;
}
.btn-private:hover { background: #e8f4fd; }
.no-students { font-size: 12px; color: #aaa; padding: 10px 0; text-align: center; }

/* 私密点评 */
.private-section {
  border: 0.5px solid #d0d8e8; border-radius: 8px; margin-bottom: 12px; overflow: hidden;
}
.private-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 10px; background: #e8f4fd; font-size: 12px; color: #185FA5; font-weight: 500;
}
.btn-close { border: none; background: none; color: #888; cursor: pointer; font-size: 14px; }
.btn-close:hover { color: #333; }
.private-comments { padding: 8px 10px; }
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

.comments-section { border-top: 0.5px solid #eee; padding-top: 10px; margin-top: 10px; }
.comments-title { font-size: 13px; font-weight: 500; color: #111; margin-bottom: 8px; }
.comment-group { margin-bottom: 8px; }
.comment-student { cursor: pointer; font-size: 12px; color: #185FA5; font-weight: 500; padding: 4px 0; }
.comment-student:hover { color: #0d3d6e; }
.comment-count { font-weight: 400; color: #888; }
.comment-list { padding: 4px 0 4px 12px; border-left: 2px solid #e8f4fd; margin: 4px 0; }
.sub-nodes { margin-top: 10px; display: flex; flex-direction: column; gap: 8px; padding-left: 8px; }
.slide-enter-active, .slide-leave-active { transition: all .2s ease; overflow: hidden; }
.slide-enter-from, .slide-leave-to { max-height: 0; opacity: 0; }
.slide-enter-to, .slide-leave-from { max-height: 2000px; opacity: 1; }
</style>