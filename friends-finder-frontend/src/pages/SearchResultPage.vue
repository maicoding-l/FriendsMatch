<template>
    <div class="search-result-page">
      <user-cards-list :users-list="usersList"></user-cards-list>
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
// onMounted(()=>{
//   myAxios.get(`user/search/tags?tagsNameList=${tags}`)
  // .then((response:any)=>{
  //   console.log('user/search/tags success',response);
  //   usersList.value = response.data.data;
  //   showToast('请求成功');
  // })
  // .catch((error : any)=>{
  //   console.log('user/search/tags fail',error);
  //   showToast('请求失败');
  // })
//   })
onMounted(async ()=>{
  const usersListData = await myAxios.get('user/search/tags',{
    params:{
      tagsNameList : tags,
    }
  })
    .then((response:any)=>{
    console.log('user/search/tags success',response);
    return response.data?.data;
    showToast('请求成功');
  })
  .catch((error : any)=>{
    console.log('user/search/tags fail',error);
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