package com.mai.friendsFinder.service;

import java.util.List;

public interface RecommendService {

    public List<Long> personalRank(Long targetUserId);
}
