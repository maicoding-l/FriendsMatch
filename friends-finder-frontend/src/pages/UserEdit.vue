<template>
  <div class="user-edit-page">
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="editUser.currentValue"
          :name="editUser.editKey"
          :label="editUser.editName"
          :placeholder= "`请输入${ editUser.editName }`"
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

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ref } from 'vue'
import { getCurrentUser } from '../api/user'
import { showToast } from 'vant'
import myAxios from '../request'
import router from '../router'
const route = useRoute()
const editUser = ref({
  editName: route.query.editName,
  editKey: route.query.editKey,
  currentValue: route.query.currentValue,
})
// const onSubmit = async () => {
//   const res = await getCurrentUser();
//   if(!res){
//     showToast("获取当前用户信息失败。")
//   }
//   const currentUser = res.data.data;
//   const res1 = await myAxios.post('/user/update',{
//     'id' : currentUser.id,
//     [String(editUser.value.editKey)]: editUser.value.currentValue,
//   })
//   if(res1.data.code===0&&res1.data>0){
//     showToast("更新用户信息成功");
//     router.back();
    
//   }else{
//     showToast("更新用户信息失败");
//   }
// }

const onSubmit = async () => {
  try {
    const currentUserRes = await getCurrentUser();
    if (!currentUserRes?.data?.data) {
      showToast("获取当前用户信息失败，无法更新。");
      return; // 获取失败，直接返回
    }
    const currentUser = currentUserRes.data.data;

    const updateRes = await myAxios.post('/user/update', {
      'id': currentUser.id,
      [String(editUser.value.editKey)]: editUser.value.currentValue,
    });

    // 关键修改：判断 updateRes.data.data 而不是 updateRes.data
    if (updateRes.data.code === 0 && updateRes.data.data > 0) {
      showToast("更新用户信息成功");
      router.back();
    } else {
      // 给出更详细的失败原因
      showToast(updateRes.data.message || "更新用户信息失败");
    }
  } catch (error) {
    console.error("更新失败:", error);
    showToast("更新请求异常");
  }
};
</script>

<style scoped></style>
