package com.flashsale.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.order.domain.OrderRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            where user_id = #{userId}
              and is_deleted = 0
            order by updated_at desc, id desc
            """)
    List<OrderRecordEntity> findByUserId(@Param("userId") Long userId);
}
