package ${table.packageName}.service;

import com.wejava.common.core.PageResult;
import ${table.packageName}.model.form.${table.className}Form;
import ${table.packageName}.model.query.${table.className}Query;
import ${table.packageName}.model.vo.${table.className}VO;
import java.util.List;

/** ${table.functionName} */
public interface ${table.className}Service {

    PageResult<${table.className}VO> page(${table.className}Query query);

    ${table.className}VO getById(${table.pkColumn.javaType} ${table.pkColumn.javaField});

    ${table.pkColumn.javaType} create(${table.className}Form form);

    void update(${table.className}Form form);

    void remove(List<${table.pkColumn.javaType}> ids);
}
