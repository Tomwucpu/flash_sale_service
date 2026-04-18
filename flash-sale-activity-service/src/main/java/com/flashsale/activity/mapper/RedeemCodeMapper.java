package com.flashsale.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.activity.domain.RedeemCodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RedeemCodeMapper extends BaseMapper<RedeemCodeEntity> {

    @Select("""
            select count(1)
            from redeem_code
            where activity_id = #{activityId}
              and status = 'AVAILABLE'
              and is_deleted = 0
            """)
    long countAvailableCodes(@Param("activityId") Long activityId);
}
