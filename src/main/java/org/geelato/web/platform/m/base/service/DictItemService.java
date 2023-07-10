package org.geelato.web.platform.m.base.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.web.platform.m.base.entity.DictItem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class DictItemService extends BaseSortableService {
    private static final String DEFAULT_ORDER_BY = "seq_no ASC";

    /**
     * 分页查询
     *
     * @param entity   查询实体
     * @param pageNum  页码
     * @param pageSize 分页数量
     * @param params   条件参数
     * @param <T>
     * @return
     */
    @Override
    public <T> List<T> pageQueryModel(Class<T> entity, int pageNum, int pageSize, Map<String, Object> params) {
        dao.SetDefaultFilter(true, filterGroup);
        return dao.queryList(entity, pageNum, pageSize, DictItemService.DEFAULT_ORDER_BY, params);
    }

    /**
     * 批量插入和更新
     *
     * @param dictId
     * @param forms
     */
    public void batchCreateOrUpdate(String dictId, List<DictItem> forms) {
        if (Strings.isBlank(dictId)) {
            throw new RuntimeException(ApiErrorMsg.UPDATE_FAIL);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("dictId", dictId);
        List<DictItem> itemList = queryModel(DictItem.class, params);
        // 删除
        if (itemList != null && !itemList.isEmpty()) {
            for (DictItem mItem : itemList) {
                boolean isExist = false;
                if (forms != null && !forms.isEmpty()) {
                    for (DictItem fItem : forms) {
                        if (mItem.getId().equals(fItem.getId())) {
                            isExist = true;
                            break;
                        }
                    }
                }
                if (!isExist) {
                    mItem.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
                    isDeleteModel(mItem);
                }
            }
        }
        // 保存、更新
        if (forms != null && !forms.isEmpty()) {
            for (int i = 0; i < forms.size(); i++) {
                DictItem item = forms.get(i);
                item.setSeqNo(i + 1);
                item.setDictId(Strings.isBlank(item.getDictId()) ? dictId : item.getDictId());
                item.setDelStatus(DeleteStatusEnum.NO.getCode());
                dao.save(item);
            }
        }
    }
}
