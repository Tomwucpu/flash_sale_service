package com.flashsale.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.order.domain.CompensationRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompensationRecordMapper extends BaseMapper<CompensationRecordEntity> {

    @Select("""
            select *
            from compensation_record
            where biz_key = #{bizKey}
              and source_event = #{sourceEvent}
              and is_deleted = 0
            order by id desc
            limit 1
            """)
    CompensationRecordEntity findLatestByBizKeyAndSourceEvent(
            @Param("bizKey") String bizKey,
            @Param("sourceEvent") String sourceEvent
    );
}
