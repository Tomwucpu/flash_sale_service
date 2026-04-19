package com.flashsale.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.order.domain.RedeemCodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RedeemCodeMapper extends BaseMapper<RedeemCodeEntity> {

    @Select("""
            select *
            from redeem_code
            where activity_id = #{activityId}
              and status = 'AVAILABLE'
              and is_deleted = 0
            order by id asc
            limit 1
            """)
    RedeemCodeEntity findFirstAvailableCode(@Param("activityId") Long activityId);

    @Update("""
            update redeem_code
            set status = 'ASSIGNED',
                assigned_user_id = #{userId},
                assigned_order_id = #{orderId},
                assigned_at = #{assignedAt},
                updated_at = #{assignedAt},
                updated_by = #{userId}
            where id = #{codeId}
              and status = 'AVAILABLE'
              and is_deleted = 0
            """)
    int claimImportedCode(
            @Param("codeId") Long codeId,
            @Param("userId") Long userId,
            @Param("orderId") Long orderId,
            @Param("assignedAt") LocalDateTime assignedAt
    );

    @Select("""
            select *
            from redeem_code
            where assigned_order_id = #{orderId}
              and is_deleted = 0
            limit 1
            """)
    RedeemCodeEntity findByAssignedOrderId(@Param("orderId") Long orderId);
}
