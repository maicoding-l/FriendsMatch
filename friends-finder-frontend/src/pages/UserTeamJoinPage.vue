<template>
<div class="user-team-create-page">
    <teamCardList :teams-list="teamsList"></teamCardList>
    <van-empty v-if="teamsList?.length===0" description="没有加入队伍" />
</div>
</template>

<script lang="ts" setup>
import teamCardList from '../components/TeamCardsList.vue'
import type { TeamType } from '../models/team';
import myAxios from '../request';
import { showToast } from 'vant';
import { ref,onMounted } from 'vue';
const teamsList= ref<TeamType[]>();


/**
 * 搜索队伍
 */
const listTeam = async (val:string) =>{
    const teamsListData = await myAxios.get("/team/list/my/join").then((response : any)=>{
        console.log("/team/list/my/join success",response);
        showToast("请求成功");
        return response?.data?.data;
    }).catch((error :any)=>{
        console.log("/team/list/my/join error",error);
        showToast("请求失败");
    })
    teamsList.value = teamsListData;
}


onMounted( async () =>{
    listTeam('')
}
)

</script>

<style scoped>

</style>