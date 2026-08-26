<template>
  <div class="admin-mask pwd-mask" @click.self="$emit('close')">
    <div class="pwd-dialog">
      <header class="pwd-head">
        <h3>修改密码</h3>
        <button class="admin-drawer-close" @click="$emit('close')">×</button>
      </header>
      <form class="pwd-body" @submit.prevent="onSubmit">
        <div class="admin-field">
          <label class="admin-field-label">旧密码</label>
          <CxInput v-model="form.oldPassword" type="password" variant="admin" model-modifier="trim" autocomplete="current-password" />
        </div>
        <div class="admin-field">
          <label class="admin-field-label">新密码</label>
          <CxInput v-model="form.newPassword" type="password" variant="admin" model-modifier="trim" autocomplete="new-password" />
          <p class="admin-field-tip">至少 16 位，修改后立即生效</p>
        </div>
        <div class="admin-field">
          <label class="admin-field-label">确认新密码</label>
          <CxInput v-model="form.confirmPassword" type="password" variant="admin" model-modifier="trim" autocomplete="new-password" />
        </div>
        <p v-if="error" class="pwd-error">{{ error }}</p>
        <footer class="pwd-foot">
          <CxButton plain @click="$emit('close')">取消</CxButton>
          <CxButton native-type="submit" :disabled="saving">{{ saving ? '保存中…' : '确认修改' }}</CxButton>
        </footer>
      </form>
    </div>
  </div>
</template>

<script setup>
import { inject, reactive, ref } from 'vue'
import { changePassword } from '../../api/admin'
import CxButton from '../../components/cx/CxButton.vue'
import CxInput from '../../components/cx/CxInput.vue'

const emit = defineEmits(['close'])

const toast = inject('adminToast', () => {})
const unauthorized = inject('adminUnauthorized', () => {})

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const error = ref('')
const saving = ref(false)

async function onSubmit() {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    error.value = '请填写全部三项'
    return
  }
  if (form.newPassword.length < 16) {
    error.value = '新密码至少 16 位'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }
  if (form.newPassword === form.oldPassword) {
    error.value = '新密码不能与旧密码相同'
    return
  }
  saving.value = true
  error.value = ''
  try {
    await changePassword(form.oldPassword, form.newPassword)
    toast('密码已更新，下次登录请使用新密码')
    emit('close')
  } catch (err) {
    if (err && err.unauthorized) {
      unauthorized()
      return
    }
    error.value = (err && err.message) || '修改失败，请稍后再试'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.pwd-mask {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 150;
}

.pwd-dialog {
  width: min(400px, 92vw);
  background-color: var(--adm-card);
  border: 1px solid var(--adm-border-soft);
  border-radius: 20px;
  box-shadow: var(--adm-shadow-lg);
  overflow: hidden;
}

.pwd-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 22px;
  border-bottom: 1px solid var(--adm-border-soft);
}

.pwd-head h3 {
  margin: 0;
  font-size: 18.5px;
  color: var(--adm-text);
}

.pwd-body {
  padding: 18px 22px 20px;
}

.pwd-error {
  margin: 0 0 10px;
  font-size: 15px;
  color: var(--adm-danger);
}

.pwd-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

/* ---------- 移动端适配（≤900px，追加） ---------- */

@media (max-width: 900px) {
  .pwd-mask {
    padding: 16px;
  }

  .pwd-dialog {
    width: calc(100vw - 32px);
    max-width: 400px;
  }
}
</style>
