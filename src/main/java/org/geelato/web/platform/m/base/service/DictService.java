package org.geelato.web.platform.m.base.service;

import org.geelato.web.platform.m.base.entity.Dict;
import org.geelato.web.platform.m.base.entity.DictItem;
import org.geelato.core.enums.DeleteStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class DictService extends BaseSortableService {
    @Autowired
    private DictItemService dictItemService;

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(Dict model) {
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        dao.save(model);
        // 清理 字典项
        Map<String, Object> params = new HashMap<>();
        params.put("dictId", model.getId());
        List<DictItem> iList = dictItemService.queryModel(DictItem.class, params);
        if (iList != null) {
            for (DictItem iModel : iList) {
                iModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(iModel);
            }
        }
    }
}
