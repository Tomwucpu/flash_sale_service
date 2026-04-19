package com.flashsale.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.order.domain.OrderRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
