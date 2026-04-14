package com.flashsale.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.user.domain.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
