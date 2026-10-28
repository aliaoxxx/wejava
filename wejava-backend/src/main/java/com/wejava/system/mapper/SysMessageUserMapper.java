package com.wejava.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.system.model.entity.SysMessageUser;
import com.wejava.system.model.query.MessageQuery;
import com.wejava.system.model.vo.MessageVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysMessageUserMapper extends BaseMapper<SysMessageUser> {

    /** 收件箱分页：消息本体与已读状态联表，过滤条件在 SQL 里完成 */
    Page<MessageVO> selectInbox(Page<MessageVO> page, @Param("userId") Long userId, @Param("query") MessageQuery query);

    /** 批量投递 */
    int insertRecipients(@Param("messageId") Long messageId, @Param("userIds") List<Long> userIds);
}
