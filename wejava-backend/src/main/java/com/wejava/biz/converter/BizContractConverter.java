package com.wejava.biz.converter;

import com.wejava.biz.model.entity.BizContract;
import com.wejava.biz.model.form.BizContractForm;
import com.wejava.biz.model.vo.BizContractVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BizContractConverter {

    BizContractVO toVO(BizContract entity);

    List<BizContractVO> toVOList(List<BizContract> entities);

    @Mapping(target = "contractId", ignore = true)
    BizContract toEntity(BizContractForm form);

    @Mapping(target = "contractId", ignore = true)
    void update(BizContractForm form, @MappingTarget BizContract entity);
}
