package com.wejava.biz.service;

import com.wejava.biz.model.form.BizContractForm;
import com.wejava.biz.model.query.BizContractQuery;
import com.wejava.biz.model.vo.BizContractVO;
import com.wejava.common.core.PageResult;
import java.util.List;

/** 合同管理 */
public interface BizContractService {

    PageResult<BizContractVO> page(BizContractQuery query);

    BizContractVO getById(Long contractId);

    Long create(BizContractForm form);

    void update(BizContractForm form);

    void remove(List<Long> ids);
}
