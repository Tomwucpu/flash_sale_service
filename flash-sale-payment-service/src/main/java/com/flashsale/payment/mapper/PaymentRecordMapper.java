package com.flashsale.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.payment.domain.PaymentRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecordEntity> {

    @Select("""
            select *
            from payment_record
            where order_no = #{orderNo}
              and is_deleted = 0
            order by id desc
            limit 1
            """)
    PaymentRecordEntity findLatestByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
            select *
            from payment_record
            where transaction_no = #{transactionNo}
              and is_deleted = 0
            limit 1
            """)
    PaymentRecordEntity findByTransactionNo(@Param("transactionNo") String transactionNo);
}
