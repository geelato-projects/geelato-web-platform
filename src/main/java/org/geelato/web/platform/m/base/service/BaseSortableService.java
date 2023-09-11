package org.geelato.web.platform.m.base.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class BaseSortableService extends BaseService {
    private static final String DEFAULT_ORDER_BY = "seq_no ASC";

    /**
     * 全量查询
     *
     * @param entity 查询实体
     * @param params 条件参数
     * @param <T>
     * @return
     */
    @Override
    public <T> List<T> queryModel(Class<T> entity, Map<String, Object> params, String orderBy) {
        dao.SetDefaultFilter(true, filterGroup);
        orderBy = Strings.isNotBlank(orderBy) ? orderBy : BaseSortableService.DEFAULT_ORDER_BY;
        return dao.queryList(entity, params, orderBy);
    }

    @Override
    public <T> List<T> queryModel(Class<T> entity, Map<String, Object> params) {
        return queryModel(entity, params, BaseSortableService.DEFAULT_ORDER_BY);
    }

    /**
     * 全量查询
     *
     * @param entity 查询实体
     * @param filter 条件参数
     * @param <T>
     * @return
     */
    @Override
    public <T> List<T> queryModel(Class<T> entity, FilterGroup filter, String orderBy) {
        dao.SetDefaultFilter(true, filterGroup);
        orderBy = Strings.isNotBlank(orderBy) ? orderBy : BaseSortableService.DEFAULT_ORDER_BY;
        return dao.queryList(entity, filter, orderBy);
    }

    @Override
    public <T> List<T> queryModel(Class<T> entity, FilterGroup filter) {
        return queryModel(entity, filter, BaseSortableService.DEFAULT_ORDER_BY);
    }

    /**
     * 创建一条数据
     *
     * @param model 实体数据
     * @param <T>
     * @return
     */
    public <T extends BaseSortableEntity> Map createModel(T model) {
        model.setSeqNo(model.getSeqNo() > 0 ? model.getSeqNo() : ColumnDefault.SEQ_NO_VALUE);
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        if (Strings.isBlank(model.getTenantCode())) {
            model.setTenantCode(getSessionTenantCode());
        }
        return dao.save(model);
    }

    /**
     * 更新一条数据
     *
     * @param model 实体数据
     * @param <T>
     * @return
     */
    public <T extends BaseSortableEntity> Map updateModel(T model) {
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        return dao.save(model);
    }

    /**
     * 逻辑删除
     *
     * @param model
     * @param <T>
     */
    public <T extends BaseSortableEntity> void isDeleteModel(T model) {
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        model.setSeqNo(ColumnDefault.SEQ_NO_DELETE);
        dao.save(model);
    }
}
