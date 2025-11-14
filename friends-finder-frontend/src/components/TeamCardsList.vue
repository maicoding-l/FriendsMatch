<template>
    <van-card
    v-for="team in props.teamsList"
    :key="team.id"
    :desc="team.description"
    :title="team.teamName"
    thumb="https://i.pinimg.com/736x/70/b7/9d/70b79d93ab3f362738dd48984ea01564.jpg"
    >
        <template #tag>
            <van-tag plain type="danger" style="margin-right: 8px;margin-top: 8px;">
                {{ teamStatusEnum[team.status] }}
            </van-tag>
        </template>
        <template #bottom>
            <div>
                <p>队长：{{ team.createUser?.username }}</p>
            </div>
            <div>
                <p>最大人数： {{team.maxNum }}</p>
            </div>
            <div v-if="team.createTime">
                <p>创建时间：{{ team.createTime }}</p>
            </div>
            <div v-if="team.expireTime">
                <p>过期时间: {{ team.expireTime }}</p>
            </div>
        </template>
        <template #footer>
            <van-button size="mini" v-if="team.userId===currentUser?.id">更新队伍</van-button>
            <van-button size="mini">联系我</van-button>
        </template>
        
    </van-card>
</template>

<script setup lang="ts">
import type { TeamType } from '../models/team';
import { teamStatusEnum } from '../constants/team';
import { getCurrentUser } from '../api/user';
import { ref,onMounted } from 'vue';
interface TeamCardListProps{
    teamsList?: TeamType[];
}

const currentUser = ref();
const props = withDefaults(defineProps<TeamCardListProps>(),{
    teamsList: () => ([] as TeamType[])
})

onMounted(async()=>{
    currentUser.value = await getCurrentUser();
},
)

</script>

<style scoped>
  /* 标签颜色*/
  .van-tag--danger.van-tag--plain {
    color: #002fff;
  }
</style>