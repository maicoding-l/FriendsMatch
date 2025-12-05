<template>
  <div class="team-add-page">
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="addTeamData.teamName"
          name="teamName"
          label="队伍名称"
          placeholder="队伍名称"
          :rules="[{ required: true, message: '请填写队伍名称' }]"
        />
        <van-field
          v-model="addTeamData.description"
          name="description"
          label="队伍描述"
          placeholder="队伍描述"
          :rules="[{ required: true, message: '请填写队伍描述' }]"
        />
        <!-- 步进器选择最大人数 -->
        <van-field name="stepper" label="最大人数">
          <template #input>
            <van-stepper v-model="addTeamData.maxNum" max="10" />
          </template>
        </van-field>
        <!-- 选择过期时间 -->
        <van-field
          v-model="addTeamData.expireTime"
          is-link
          readonly
          name="datePicker"
          label="时间选择"
          placeholder="点击选择时间"
          @click="showPicker = true"
        />
        <van-popup v-model:show="showPicker" destroy-on-close position="bottom">
          <van-date-picker
            :min-date="minDate"
            :model-value="pickerValue"
            @confirm="onConfirm"
            @cancel="showPicker = false"
          />
        </van-popup>
        <!-- 队伍状态 -->
        <van-field name="radio" label="单选框">
          <template #input>
            <van-radio-group
              v-model="addTeamData.status"
              direction="horizontal"
            >
              <van-radio name="0">公开</van-radio>
              <van-radio name="1">私有</van-radio>
              <van-radio name="2">加密</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <!-- 如果选择加密，则出现输入密码框 -->
        <van-field
          v-if="Number(addTeamData.status) === 2"
          v-model="addTeamData.password"
          type="password"
          name="password"
          label="密码"
          placeholder="密码"
          :rules="[{ required: true, message: '请填写队伍密码' }]"
        />
      </van-cell-group>
      <div style="margin: 16px">
        <van-button round block type="primary" native-type="submit">
          提交
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { showToast, showDialog } from 'vant'
import myAxios from '../request'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import moment from 'moment'

const router = useRouter()

//日期选择弹出
const showPicker = ref(false)
const pickerValue = ref([])
const minDate = new Date()

const onConfirm = ({ selectedValues }) => {
  addTeamData.value.expireTime = selectedValues.join('/')
  pickerValue.value = selectedValues
  showPicker.value = false
}

const initFormData = {
  teamName: '',
  description: '',
  maxNum: 5,
  expireTime: '',
  status: 0,
  password: '',
}
//需要用户填写的表单数据，对象扩展
const addTeamData = ref({ ...initFormData })

//提交数据
const onSubmit = async () => {
  try {
    // 检查日期是否已选择
    if (!addTeamData.value.expireTime) {
      showToast('请选择过期时间')
      return
    }

    // 正确解析日期格式 YYYY/MM/DD，转换为时间戳（毫秒）
    const expireTime = moment(addTeamData.value.expireTime, 'YYYY/MM/DD')
      .endOf('day')
      .valueOf()

    const postData = {
      ...addTeamData.value,
      status: Number(addTeamData.value.status),
      expireTime: expireTime,
    }

    console.log('发送请求数据:', postData)
    const res = await myAxios.post('team/add', postData)
    console.log('响应数据:', res)

    if (res?.data.code === 0) {
      showDialog({
        title: '操作成功',
        message: '成功创建队伍！',
        confirmButtonText: '确定',
      }).then(() => {
        router.push({
          path: '/team',
          replace: true,
        })
      })
    } else {
      showToast(res?.data?.description || res?.data?.message || '添加失败')
    }
  } catch (error) {
    console.error('提交错误:', error)
    showToast('提交出错，请检查控制台')
  }
}
</script>

<style scoped>
.team-add-page {
}
</style>
