import type { CrudSchema } from '@/types/crud'
import {
  create${table.businessCap},
  get${table.businessCap},
  list${table.businessCap},
  remove${table.businessCap},
  update${table.businessCap}
} from '@/api/${table.moduleName}/${table.businessName}'

/** ${table.functionName} */
export const ${table.businessName}Schema: CrudSchema = {
  key: '${table.functionName}',
  rowKey: '${table.pkColumn.javaField}',
  permissionPrefix: '${table.permissionPrefix}',
  pagination: { pageSize: 10 },

  api: {
    list: list${table.businessCap},
    detail: get${table.businessCap},
    create: create${table.businessCap},
    update: update${table.businessCap},
    remove: remove${table.businessCap}
  },
<#if queryColumns?size gt 0>

  search: [
<#list queryColumns as col>
<#if col.queryType == "BETWEEN">
    { prop: 'begin${col.javaFieldCap}', label: '${col.columnComment}起', type: 'date' },
    { prop: 'end${col.javaFieldCap}', label: '${col.columnComment}止', type: 'date' },
<#elseif col.htmlType == "switch">
    { prop: '${col.javaField}', label: '${col.columnComment}', type: 'select'<#if col.dictType?? && col.dictType != "">, dict: '${col.dictType}'</#if> },
<#else>
    { prop: '${col.javaField}', label: '${col.columnComment}', type: '${col.htmlType}' },
</#if>
</#list>
  ],
</#if>

  columns: [
<#list listColumns as col>
    {
      prop: '${col.javaField}',
      label: '${col.columnComment}'<#if col.javaType == "String">,
      minWidth: 120<#else>,
      width: 100</#if><#if col.dictType?? && col.dictType != "">,
      dict: '${col.dictType}'</#if><#if col.javaType == "LocalDateTime">,
      dateFormat: 'YYYY-MM-DD HH:mm:ss'</#if><#if col.javaType == "String">,
      showOverflowTooltip: true</#if>
    }<#if col_has_next>,</#if>
</#list>
  ],
<#if insertColumns?size gt 0>

  form: [
<#list insertColumns as col>
    {
      prop: '${col.javaField}',
      label: '${col.columnComment}',
      type: '${col.htmlType}'<#if col.required>,
      required: true</#if><#if col.dictType?? && col.dictType != "">,
      dict: '${col.dictType}'</#if><#if col.javaField == "remark">,
      span: 24<#else>,
      span: 12</#if>
    }<#if col_has_next>,</#if>
</#list>
  ]
</#if>
}
