<template>
<div class="team-page">
    <van-button type="primary" @click="toTeamAddPage">新建队伍</van-button>
    <teamCardList :teams-list="teamsList"></teamCardList>
</div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router';
import teamCardList from '../components/TeamCardsList.vue'
import type { TeamType } from '../models/team';
import myAxios from '../request';
import { showToast } from 'vant';
import { ref,onMounted } from 'vue';
const router = useRouter();
const teamsList= ref<TeamType[]>();
const toTeamAddPage = () =>{
    router.push({
        path:'/team/add'
    })
}
onMounted(async ()=>{
    const teamsListData = await myAxios.get("/team/list").then((response : any)=>{
        console.log("/team/list success",response);
        showToast("请求成功");
        return response?.data?.data;
    }).catch((error :any)=>{
        console.log("/team/list error",error);
        showToast("请求失败");
    })
    teamsList.value = teamsListData;
})

</script>

<style scoped>

</style>