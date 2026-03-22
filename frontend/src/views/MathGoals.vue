<template>
  <div class="math-goals">
    <div class="header">
      <h1>📚 数学学习目标</h1>
      <button @click="showAddForm = !showAddForm" class="btn btn-add">
        {{ showAddForm ? '取消' : '+ 添加目标' }}
      </button>
    </div>

    <!-- 添加/编辑目标表单 -->
    <div v-if="showAddForm" class="form-card">
      <h2>{{ editingId ? '编辑目标' : '添加新目标' }}</h2>
      <form @submit.prevent="submitGoal">
        <div class="form-row">
          <div class="form-group">
            <label for="title">目标名称 *</label>
            <input 
              v-model="newGoal.title" 
              type="text" 
              id="title"
              placeholder="例如：掌握微积分"
              required
            >
          </div>
          <div class="form-group">
            <label for="category">分类 *</label>
            <select v-model="newGoal.category" id="category" required>
              <option value="">请选择分类</option>
              <option value="代数">代数</option>
              <option value="几何">几何</option>
              <option value="微积分">微积分</option>
              <option value="概率">概率</option>
              <option value="数论">数论</option>
              <option value="其他">其他</option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label for="description">描述</label>
          <textarea 
            v-model="newGoal.description" 
            id="description"
            placeholder="详细描述目标和学习内容"
            rows="3"
          ></textarea>
        </div>
        <div class="form-group">
          <label>子目标（将在目标页面表格中展示）</label>
          <div class="subgoal-form-inline">
            <input v-model="subgoalForm.name" placeholder="子目标名称" />
            <input type="date" v-model="subgoalForm.startDate" />
            <input type="date" v-model="subgoalForm.endDate" />
            <button type="button" class="btn btn-small btn-success" @click="addSubgoal(newGoal)">添加子目标</button>
          </div>
          <div class="subgoal-badge-wrap" v-if="newGoal.subGoals && newGoal.subGoals.length">
            <span class="subgoal-badge" v-for="(sub, idx) in newGoal.subGoals" :key="idx">
              {{ sub.name }} ({{ sub.startDate || '开始' }} ~ {{ sub.endDate || '结束' }})
              <button class="btn btn-mini" @click="newGoal.subGoals.splice(idx,1)">x</button>
            </span>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label for="targetDate">目标完成日期</label>
            <input 
              v-model="newGoal.targetDate" 
              type="date" 
              id="targetDate"
            >
          </div>
          <div class="form-group">
            <label for="progress">进度 (%)</label>
            <input 
              v-model.number="newGoal.progress" 
              type="number" 
              id="progress"
              min="0"
              max="100"
              placeholder="0-100"
            >
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary">
            {{ editingId ? '保存修改' : '创建目标' }}
          </button>
          <button v-if="editingId" type="button" @click="cancelEdit" class="btn btn-cancel">取消编辑</button>
        </div>
      </form>
    </div>

    <!-- 提示信息 -->
    <div v-if="message" :class="['message', message.type]">
      {{ message.text }}
    </div>

    <!-- 统计信息 -->
    <div class="stats">
      <div class="stat-card">
        <div class="stat-number">{{ goalsInProgress.length }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-card completed">
        <div class="stat-number">{{ completedGoals.length }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ abandonedGoals.length }}</div>
        <div class="stat-label">已放弃</div>
      </div>
    </div>

    <!-- 目标列表 -->
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="goals.length === 0" class="empty">
      <p>暂无学习目标，添加一个开始学习吧！</p>
    </div>
    <div v-else class="goals-container">
      <!-- 进行中的目标 -->
      <div v-if="goalsInProgress.length > 0" class="goals-section">
        <h2>🎯 进行中</h2>
        <div class="goals-list">
          <div 
            v-for="goal in goalsInProgress" 
            :key="goal.id" 
            class="goal-card progress"
          >
            <div class="goal-header">
              <h3>{{ goal.title }}</h3>
              <div class="goal-actions">
                <button @click="editGoal(goal)" class="btn-icon" title="编辑">✏️</button>
                <button @click="deleteGoal(goal.id)" class="btn-icon" title="删除">🗑️</button>
              </div>
            </div>
            <div class="goal-badge" :style="{background: getCategoryColor(goal.category)}">
              {{ goal.category }}
            </div>
            <p v-if="goal.description" class="goal-description">{{ goal.description }}</p>
            <div class="subgoals-section">
              <div class="subgoals-header">
                <h4>子目标列表</h4>
                <button class="btn btn-small" @click="openSubgoalForm(goal)">+ 添加子目标</button>
              </div>
              <table class="subgoals-table">
                <thead>
                  <tr>
                    <th>子目标名</th>
                    <th>开始日</th>
                    <th>结束日</th>
                    <th>详细链接</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!goal.subGoals || goal.subGoals.length === 0">
                    <td colspan="5" class="empty-row">暂无子目标</td>
                  </tr>
                  <tr v-for="(sub, idx) in goal.subGoals" :key="sub.id || idx">
                    <td>{{ sub.name }}</td>
                    <td>{{ sub.startDate || '-' }}</td>
                    <td>{{ sub.endDate || '-' }}</td>
                    <td><a href="javascript:void(0)" @click.prevent="viewSubgoalDetail(goal, sub)">查看详情</a></td>
                    <td><button class="btn btn-small btn-cancel" @click="deleteSubgoal(goal, idx)">删除</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="activeSubgoalGoalId === goal.id" class="subgoal-add-card">
                <div class="subgoal-row">
                  <input v-model="subgoalForm.name" placeholder="子目标名称" />
                  <input type="date" v-model="subgoalForm.startDate" />
                  <input type="date" v-model="subgoalForm.endDate" />
                  <button class="btn btn-success btn-small" @click="addSubgoal(goal)">保存子目标</button>
                  <button class="btn btn-cancel btn-small" @click="activeSubgoalGoalId = null">取消</button>
                </div>
              </div>
            </div>
            <div class="goal-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{width: goal.progress + '%'}"></div>
              </div>
              <span class="progress-text">{{ goal.progress }}%</span>
            </div>
            <div class="goal-footer">
              <span v-if="goal.targetDate" class="goal-date">
                📅 {{ formatDate(goal.targetDate) }}
              </span>
              <button @click="markComplete(goal)" class="btn btn-small btn-success">
                标记完成
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 已完成的目标 -->
      <div v-if="completedGoals.length > 0" class="goals-section">
        <h2>✅ 已完成</h2>
        <div class="goals-list">
          <div 
            v-for="goal in completedGoals" 
            :key="goal.id" 
            class="goal-card completed"
          >
            <div class="goal-header">
              <h3>{{ goal.title }}</h3>
              <div class="goal-actions">
                <button @click="deleteGoal(goal.id)" class="btn-icon" title="删除">🗑️</button>
              </div>
            </div>
            <div class="goal-badge" :style="{background: getCategoryColor(goal.category)}">
              {{ goal.category }}
            </div>
            <p v-if="goal.description" class="goal-description">{{ goal.description }}</p>
            <div class="goal-footer">
              <span v-if="goal.targetDate" class="goal-date">
                📅 {{ formatDate(goal.targetDate) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 已放弃的目标 -->
      <div v-if="abandonedGoals.length > 0" class="goals-section">
        <h2>❌ 已放弃</h2>
        <div class="goals-list">
          <div 
            v-for="goal in abandonedGoals" 
            :key="goal.id" 
            class="goal-card abandoned"
          >
            <div class="goal-header">
              <h3>{{ goal.title }}</h3>
              <div class="goal-actions">
                <button @click="deleteGoal(goal.id)" class="btn-icon" title="删除">🗑️</button>
              </div>
            </div>
            <div class="goal-badge" :style="{background: getCategoryColor(goal.category)}">
              {{ goal.category }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MathGoals',
  data() {
    return {
      goals: [],
      newGoal: {
        title: '',
        description: '',
        category: '',
        targetDate: '',
        progress: 0,
        status: '进行中',
        subGoals: []
      },
      subgoalForm: {
        name: '',
        startDate: '',
        endDate: ''
      },
      activeSubgoalGoalId: null,
      showAddForm: false,
      editingId: null,
      loading: false,
      message: null
    }
  },
  computed: {
    goalsInProgress() {
      return this.goals.filter(g => g.status === '进行中');
    },
    completedGoals() {
      return this.goals.filter(g => g.status === '已完成');
    },
    abandonedGoals() {
      return this.goals.filter(g => g.status === '已放弃');
    }
  },
  methods: {
    async fetchGoals() {
      this.loading = true;
      try {
        const response = await fetch('http://localhost:8080/api/math-goals');
        if (response.ok) {
          const data = await response.json();
          this.goals = data.map((goal, goalIndex) => ({
            ...goal,
            subGoals: (goal.subGoals || []).map((sub, subIndex) => {
              if (typeof sub === 'string') {
                const parts = sub.split('|');
                return {
                  id: `${goal.id || goalIndex}-${subIndex}`,
                  name: parts[0] || sub,
                  startDate: parts[1] || '',
                  endDate: parts[2] || ''
                };
              }
              return {
                id: sub.id || `${goal.id || goalIndex}-${subIndex}`,
                name: sub.name || '',
                startDate: sub.startDate || '',
                endDate: sub.endDate || ''
              };
            })
          }));
        } else {
          this.showMessage('获取目标失败', 'error');
        }
      } catch (error) {
        console.error('Error fetching goals:', error);
        this.showMessage('获取目标失败：' + error.message, 'error');
      } finally {
        this.loading = false;
      }
    },
    async submitGoal() {
      try {
        const subGoalsForApi = (this.newGoal.subGoals || []).map(sub => {
          // 保存为 name|start|end 格式，后端仅保存字符串列表
          return `${sub.name || ''}|${sub.startDate || ''}|${sub.endDate || ''}`;
        });

        const payload = {
          ...this.newGoal,
          subGoals: subGoalsForApi
        };

        if (payload.subGoals.length > 0) {
          this.newGoal.progress = this.newGoal.progress || 0;
          this.newGoal.status = this.newGoal.status || '进行中';
        }

        const url = this.editingId 
          ? `http://localhost:8080/api/math-goals/${this.editingId}`
          : 'http://localhost:8080/api/math-goals';
        const method = this.editingId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
          method,
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (response.ok) {
          this.showMessage(
            this.editingId ? '目标已更新' : '目标已创建',
            'success'
          );
          this.resetForm();
          this.fetchGoals();
        } else {
          this.showMessage('提交失败', 'error');
        }
      } catch (error) {
        console.error('Error submitting goal:', error);
        this.showMessage('提交失败：' + error.message, 'error');
      }
    },
    editGoal(goal) {
      this.newGoal = {
        ...goal,
        subGoals: (goal.subGoals || []).map((sub, idx) => ({
          id: sub.id || idx,
          name: sub.name || sub,
          startDate: sub.startDate || '',
          endDate: sub.endDate || ''
        }))
      };
      this.editingId = goal.id;
      this.showAddForm = true;
    },
    cancelEdit() {
      this.resetForm();
    },
    async deleteGoal(id) {
      if (confirm('确认删除此目标吗？')) {
        try {
          const response = await fetch(`http://localhost:8080/api/math-goals/${id}`, {
            method: 'DELETE'
          });

          if (response.ok) {
            this.showMessage('目标已删除', 'success');
            this.fetchGoals();
          } else {
            this.showMessage('删除失败', 'error');
          }
        } catch (error) {
          console.error('Error deleting goal:', error);
          this.showMessage('删除失败：' + error.message, 'error');
        }
      }
    },
    openSubgoalForm(goal) {
      this.activeSubgoalGoalId = goal.id;
      this.subgoalForm = { name: '', startDate: '', endDate: '' };
    },
    addSubgoal(goal) {
      if (!this.subgoalForm.name) {
        this.showMessage('请输入子目标名称', 'error');
        return;
      }
      goal.subGoals = goal.subGoals || [];
      goal.subGoals.push({
        id: Date.now() + Math.random(),
        name: this.subgoalForm.name,
        startDate: this.subgoalForm.startDate,
        endDate: this.subgoalForm.endDate
      });
      this.activeSubgoalGoalId = null;
      this.subgoalForm = { name: '', startDate: '', endDate: '' };
      this.showMessage('子目标已添加', 'success');
    },
    deleteSubgoal(goal, index) {
      goal.subGoals.splice(index, 1);
      this.showMessage('子目标已删除', 'success');
    },
    viewSubgoalDetail(goal, sub) {
      this.showMessage(`子目标详情：${goal.title} / ${sub.name}`, 'success');
      // 可以改为 router 跳转到子目标详情页
      // this.$router.push(`/math-goals/${goal.id}/subgoal/${sub.id}`);
    },
    async markComplete(goal) {
      goal.status = '已完成';
      goal.progress = 100;
      try {
        const response = await fetch(`http://localhost:8080/api/math-goals/${goal.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(goal)
        });

        if (response.ok) {
          this.showMessage('🎉 恭喜！目标已完成', 'success');
          this.fetchGoals();
        } else {
          this.showMessage('更新失败', 'error');
        }
      } catch (error) {
        console.error('Error marking complete:', error);
        this.showMessage('更新失败：' + error.message, 'error');
      }
    },
    resetForm() {
      this.newGoal = {
        title: '',
        description: '',
        category: '',
        targetDate: '',
        progress: 0,
        status: '进行中',
        subGoals: [],
        subGoalsText: ''
      };
      this.editingId = null;
      this.showAddForm = false;
    },
    showMessage(text, type) {
      this.message = { text, type };
      setTimeout(() => {
        this.message = null;
      }, 3000);
    },
    getCategoryColor(category) {
      const colors = {
        '代数': '#FF6B6B',
        '几何': '#4ECDC4',
        '微积分': '#45B7D1',
        '概率': '#FFA07A',
        '数论': '#98D8C8',
        '其他': '#A8A8A8'
      };
      return colors[category] || '#A8A8A8';
    },
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      });
    }
  },
  mounted() {
    this.fetchGoals();
  }
}
</script>

<style scoped>
.math-goals {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h1 {
  font-size: 32px;
  color: #333;
  margin: 0;
}

/* 按钮样式 */
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.btn-add {
  background: #4ECDC4;
  color: white;
}

.btn-add:hover {
  background: #3ab9b0;
  transform: translateY(-2px);
}

.btn-primary {
  background: #5E72E4;
  color: white;
  padding: 12px 24px;
}

.btn-primary:hover {
  background: #4c63d2;
}

.btn-cancel {
  background: #ccc;
  color: #333;
  padding: 10px 20px;
  margin-left: 10px;
}

.btn-cancel:hover {
  background: #aaa;
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
}

.btn-success {
  background: #51cf66;
  color: white;
}

.btn-success:hover {
  background: #40c057;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 0 5px;
  transition: transform 0.2s;
}

.btn-icon:hover {
  transform: scale(1.2);
}

/* 表单样式 */
.form-card {
  background: white;
  padding: 25px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 30px;
}

.form-card h2 {
  margin-top: 0;
  color: #333;
  font-size: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
}

.form-group label {
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
  font-size: 14px;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #5E72E4;
  box-shadow: 0 0 0 2px rgba(94, 114, 228, 0.1);
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

/* 消息提示 */
.message {
  padding: 15px 20px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-weight: 500;
}

.message.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.message.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

/* 统计卡片 */
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-card.completed {
  border-left: 4px solid #51cf66;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #5E72E4;
  margin-bottom: 8px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

/* 加载状态 */
.loading {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 16px;
}

.empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

/* 目标容器 */
.goals-container {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.goals-section h2 {
  color: #333;
  margin: 0 0 20px 0;
  font-size: 18px;
}

.goals-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

/* 目标卡片 */
.goal-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  border-left: 4px solid #5E72E4;
}

.goal-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.goal-card.completed {
  border-left-color: #51cf66;
  opacity: 0.8;
}

.goal-card.abandoned {
  border-left-color: #ccc;
  opacity: 0.6;
}

.goal-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  margin-bottom: 12px;
}

.goal-header h3 {
  margin: 0;
  color: #333;
  flex: 1;
  font-size: 18px;
}

.goal-actions {
  display: flex;
  gap: 5px;
}

.goal-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 12px;
}

.goal-description {
  color: #666;
  font-size: 14px;
  margin: 12px 0;
  line-height: 1.4;
}

.subgoals-list {
  margin: 0 0 10px;
  padding-left: 18px;
  color: #444;
}

.subgoals-list li {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.subgoals-list li.done {
  color: #777;
  text-decoration: line-through;
}

.subcheck {
  display: inline-block;
  width: 20px;
}

/* 进度条 */
.goal-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 15px 0;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #45B7D1, #4ECDC4);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  font-weight: 600;
  color: #5E72E4;
  min-width: 35px;
  text-align: right;
}

.goal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.goal-date {
  font-size: 12px;
  color: #999;
}
.subgoals-section {
  margin-top: 10px;
  border-top: 1px dashed #ddd;
  padding-top: 10px;
}
.subgoals-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.subgoals-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 10px;
}
.subgoals-table th,
.subgoals-table td {
  border: 1px solid #e1e7ef;
  padding: 8px;
  font-size: 13px;
  text-align: left;
}
.subgoals-table th {
  background: #f9fbff;
  color: #333;
}
.empty-row {
  text-align: center;
  color: #888;
  font-size: 13px;
}
.subgoal-add-card {
  background: #f9faff;
  border: 1px solid #e8ecf8;
  border-radius: 6px;
  padding: 8px;
}
.subgoal-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.subgoal-row input {
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
  min-width: 120px;
}
.subgoal-form-inline {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.subgoal-form-inline input {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 8px;
  min-width: 120px;
}
.subgoal-badge-wrap {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.subgoal-badge {
  background: #edf4ff;
  border: 1px solid #d6e4ff;
  border-radius: 15px;
  padding: 4px 8px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.btn-mini {
  background: #f0f0f0;
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 2px 6px;
  cursor: pointer;
}
/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .goals-list {
    grid-template-columns: 1fr;
  }

  .goal-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .stats {
    grid-template-columns: 1fr;
  }
}
</style>
