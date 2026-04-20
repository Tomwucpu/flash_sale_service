package com.flashsale.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.order.domain.ExportTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTaskEntity> {

    @Select("""
            select *
            from export_task
            where id = #{taskId}
              and is_deleted = 0
            limit 1
            """)
    ExportTaskEntity findByIdActive(@Param("taskId") Long taskId);

    @Select("""
            select *
            from export_task
            where activity_id = #{activityId}
              and is_deleted = 0
            order by id desc
            """)
    List<ExportTaskEntity> findByActivityId(@Param("activityId") Long activityId);
}
