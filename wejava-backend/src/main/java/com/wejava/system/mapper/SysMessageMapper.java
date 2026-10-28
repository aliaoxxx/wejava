package com.wejava.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wejava.system.model.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {

    /** 删除没有接收人的消息（收件箱记录已被清理） */
    int deleteOrphanMessages(@Param("before") java.time.LocalDateTime before);
}
