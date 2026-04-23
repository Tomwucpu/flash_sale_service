package com.flashsale.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.order.domain.ActivityProductEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityProductMapper extends BaseMapper<ActivityProductEntity> {

    @Update("""
            update activity_product
            set available_stock = available_stock - 1,
                version = version + 1,
                updated_by = #{operatorId}
            where id = #{activityId}
              and is_deleted = 0
              and available_stock > 0
            """)
    int decreaseAvailableStock(@Param("activityId") Long activityId, @Param("operatorId") Long operatorId);

    @Update("""
            update activity_product
            set available_stock = available_stock + 1,
                version = version + 1,
                updated_by = #{operatorId}
            where id = #{activityId}
              and is_deleted = 0
              and available_stock < total_stock
            """)
    int increaseAvailableStock(@Param("activityId") Long activityId, @Param("operatorId") Long operatorId);
}
