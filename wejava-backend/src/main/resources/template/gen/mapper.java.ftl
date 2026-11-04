package ${table.packageName}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ${table.packageName}.model.entity.${table.className};
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ${table.className}Mapper extends BaseMapper<${table.className}> {}
