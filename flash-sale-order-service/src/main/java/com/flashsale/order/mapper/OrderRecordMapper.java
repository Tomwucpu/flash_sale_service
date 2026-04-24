package com.flashsale.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.order.domain.OrderRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderRecordMapper extends BaseMapper<OrderRecordEntity> {

    @Select("""
            select *
            from order_record
            where purchase_unique_key = #{purchaseUniqueKey}
              and is_deleted = 0
            limit 1
            """)
    OrderRecordEntity findByPurchaseUniqueKey(@Param("purchaseUniqueKey") String purchaseUniqueKey);

    @Select("""
            select *
            from order_record
            where order_no = #{orderNo}
              and is_deleted = 0
            limit 1
            """)
    OrderRecordEntity findByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
            select *
            from order_record
            where activity_id = #{activityId}
              and user_id = #{userId}
              and is_deleted = 0
            order by updated_at desc, id desc
            """)
    List<OrderRecordEntity> findByActivityIdAndUserId(
            @Param("activityId") Long activityId,
            @Param("userId") Long userId
    );

    @Select("""
            select *
            from order_record
            where activity_id = #{activityId}
              and is_deleted = 0
            order by updated_at desc, id desc
            """)
    List<OrderRecordEntity> findByActivityId(@Param("activityId") Long activityId);

    @Select("""
            select *
            from order_record
            where user_id = #{userId}
              and is_deleted = 0
            order by updated_at desc, id desc
            """)
    List<OrderRecordEntity> findByUserId(@Param("userId") Long userId);

    @Select("""
            select *
            from order_record
            where order_status = 'INIT'
              and pay_status = 'WAIT_PAY'
              and created_at <= #{deadline}
              and is_deleted = 0
            order by created_at asc, id asc
            limit #{limit}
            """)
    List<OrderRecordEntity> findOverdueWaitPayOrders(
            @Param("deadline") LocalDateTime deadline,
            @Param("limit") int limit
    );

    @Update("""
            update order_record
            set order_status = 'CLOSED',
                pay_status = 'CLOSED',
                fail_reason = #{failReason},
                updated_by = #{operatorId},
                updated_at = CURRENT_TIMESTAMP
            where id = #{orderId}
              and order_status = 'INIT'
              and pay_status = 'WAIT_PAY'
              and is_deleted = 0
            """)
    int closeWaitPayOrder(
            @Param("orderId") Long orderId,
            @Param("failReason") String failReason,
            @Param("operatorId") Long operatorId
    );
}
