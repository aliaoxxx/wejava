package com.wejava.gen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wejava.gen.model.entity.GenTable;
import com.wejava.gen.model.vo.DbColumn;
import com.wejava.gen.model.vo.DbTableVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GenTableMapper extends BaseMapper<GenTable> {

    /** 当前库中可导入的业务表（排除生成器自身的表） */
    @Select(
            """
            SELECT table_name AS tableName, table_comment AS tableComment, table_rows AS rowCount
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_type = 'BASE TABLE'
              AND table_name NOT LIKE 'gen\\_%'
            ORDER BY table_name
            """)
    List<DbTableVO> selectDbTables();

    /** 指定表的列信息 */
    @Select(
            """
            SELECT column_name AS columnName, column_type AS columnType, column_comment AS columnComment,
                   is_nullable AS isNullable, column_key AS columnKey, extra AS extra,
                   column_default AS columnDefault
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = #{tableName}
            ORDER BY ordinal_position
            """)
    List<DbColumn> selectDbColumns(@Param("tableName") String tableName);
}
