import type { CrudSchema } from '@/types/crud'
import {
  createContract,
  getContract,
  listContract,
  removeContract,
  updateContract
} from '@/api/biz/contract'

/** 合同管理 */
export const contractSchema: CrudSchema = {
  key: '合同管理',
  rowKey: 'contractId',
  permissionPrefix: 'biz:contract',
  pagination: { pageSize: 10 },

  api: {
    list: listContract,
    detail: getContract,
    create: createContract,
    update: updateContract,
    remove: removeContract
  },

  search: [
    { prop: 'contractNo', label: '合同编号', type: 'input' },
    { prop: 'contractName', label: '合同名称', type: 'input' },
    { prop: 'contractType', label: '合同类型', type: 'select', dict: 'biz_contract_type' },
    { prop: 'beginSignDate', label: '签订日期起', type: 'date' },
    { prop: 'endSignDate', label: '签订日期止', type: 'date' },
    { prop: 'beginExpireTime', label: '到期时间起', type: 'datetime' },
    { prop: 'endExpireTime', label: '到期时间止', type: 'datetime' },
    { prop: 'status', label: '状态', type: 'select', dict: 'sys_normal_disable' },
  ],

  columns: [
    {
      prop: 'contractId',
      label: '合同ID',
      width: 100
    },
    {
      prop: 'contractNo',
      label: '合同编号',
      minWidth: 120,
      showOverflowTooltip: true
    },
    {
      prop: 'contractName',
      label: '合同名称',
      minWidth: 120,
      showOverflowTooltip: true
    },
    {
      prop: 'contractType',
      label: '合同类型',
      minWidth: 120,
      dict: 'biz_contract_type',
      showOverflowTooltip: true
    },
    {
      prop: 'amount',
      label: '合同金额',
      width: 100
    },
    {
      prop: 'signDate',
      label: '签订日期',
      width: 100
    },
    {
      prop: 'expireTime',
      label: '到期时间',
      width: 100,
      dateFormat: 'YYYY-MM-DD HH:mm:ss'
    },
    {
      prop: 'status',
      label: '状态',
      width: 100,
      dict: 'sys_normal_disable'
    },
    {
      prop: 'remark',
      label: '备注',
      minWidth: 120,
      showOverflowTooltip: true
    },
    {
      prop: 'createTime',
      label: 'Create Time',
      width: 100,
      dateFormat: 'YYYY-MM-DD HH:mm:ss'
    }
  ],

  form: [
    {
      prop: 'contractNo',
      label: '合同编号',
      type: 'input',
      required: true,
      span: 12
    },
    {
      prop: 'contractName',
      label: '合同名称',
      type: 'input',
      required: true,
      span: 12
    },
    {
      prop: 'contractType',
      label: '合同类型',
      type: 'select',
      required: true,
      dict: 'biz_contract_type',
      span: 12
    },
    {
      prop: 'amount',
      label: '合同金额',
      type: 'number',
      span: 12
    },
    {
      prop: 'signDate',
      label: '签订日期',
      type: 'date',
      span: 12
    },
    {
      prop: 'expireTime',
      label: '到期时间',
      type: 'datetime',
      span: 12
    },
    {
      prop: 'status',
      label: '状态',
      type: 'switch',
      dict: 'sys_normal_disable',
      span: 12
    },
    {
      prop: 'remark',
      label: '备注',
      type: 'textarea',
      span: 24
    }
  ]
}
