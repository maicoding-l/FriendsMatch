<template>
    <van-form @submit="onSubmit">
  <van-cell-group inset>
    <van-field
      v-model="userAccount"
      name="username"
      label="用户名"
      placeholder="用户名"
      :rules="[{ required: true, message: '请填写用户名' }]"
    />
    <van-field
      v-model="userPassword"
      type="password"
      name="password"
      label="密码"
      placeholder="密码"
      :rules="[{ required: true, message: '请填写密码' }]"
    />
  </van-cell-group>
  <div style="margin: 16px;">
    <van-button round block type="primary" native-type="submit">
      提交
    </van-button>
  </div>
</van-form>

</template>

<script setup lang="ts">
import { ref } from 'vue';
import router from '../router';
import myAxios from '../request';
import { showToast } from 'vant';
const userAccount = ref('');
const userPassword = ref('');
const onSubmit = async (values: { userAccount: string; userPassword: string }) => {
  const res = await myAxios.post('/user/login',{
        userAccount : userAccount.value,
        userPassword : userPassword.value 
  })
   .then((response:any)=>{
    console.log('/user/login success',response);
    return response.data?.data;
    showToast('请求成功');
  })
  .catch((error : any)=>{
    console.log('/user/login fail',error);
    showToast('请求失败');
  })
  if(res){
    router.replace('/');
  }else{
    showToast('用户名或密码不正确');
  }
};

</script>

<style scoped>

</style>