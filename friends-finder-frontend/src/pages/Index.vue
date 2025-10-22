<template>
    <div class="search-result-page">
      <UserCardsList :users-list="usersList">

      </UserCardsList>
    </div>
    <van-empty v-if="usersList?.length===0" description="没有符合的结果" />
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import { ref,onMounted } from 'vue';
import type { UserType } from '../models/user';
import myAxios from '../request';
import { showToast } from 'vant';
import UserCardsList from '../components/UserCardsList.vue';
const route = useRoute();
const {tags} = route.query;
const usersList = ref<UserType[]>();


onMounted(async ()=>{
  const usersListData = await myAxios.get('user/recommend',{
    params:{
      pageNo:1,
      pageSize:10
    }
  })
    .then((response:any)=>{
    console.log('user/recommend success',response);
    showToast('请求成功');
    return response.data?.data?.records;
  })
  .catch((error : any)=>{
    console.log('user/recommend fail',error);
    showToast('请求失败');
  })
  if(usersListData){
    usersListData.forEach((user: { tags: string; }) =>{
      if(user.tags){
        user.tags = JSON.parse(user.tags);
      }
    })
  }
  usersList.value = usersListData;
})

</script>

<style scoped>

</style>
