package com.wejava.common.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.StringUtils;

/**
 * 把 DataScopeContext 里的条件 AND 到 SELECT 的 WHERE 上。
 * <p>
 * 失败时**拒绝执行**而不是放行 —— 数据权限属于安全边界，宁可报错也不能泄露数据。
 */
@Slf4j
public class DataScopeInnerInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(
            Executor executor,
            MappedStatement ms,
            Object parameter,
            RowBounds rowBounds,
            ResultHandler resultHandler,
            BoundSql boundSql) {
        String fragment = DataScopeContext.get();
        if (!StringUtils.hasText(fragment)) {
            return;
        }
        String sql = boundSql.getSql();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select)) {
                return;
            }
            PlainSelect plainSelect = select.getPlainSelect();
            if (plainSelect == null) {
                return;
            }
            // 直接在片段外面套括号，交给解析器生成 Parenthesis（5.x 不能手工构造）
            Expression condition = CCJSqlParserUtil.parseCondExpression("(" + fragment + ")");
            Expression where = plainSelect.getWhere();
            plainSelect.setWhere(where == null ? condition : new AndExpression(where, condition));
            PluginUtils.mpBoundSql(boundSql).sql(select.toString());
        } catch (Exception e) {
            log.error("数据权限 SQL 改写失败，已拒绝执行: {}", sql, e);
            throw new IllegalStateException("数据权限处理失败，查询已中止", e);
        }
    }

    @Override
    public void setProperties(Properties properties) {
        // 无需配置
    }
}
