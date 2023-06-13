package org.geelato.web.platform.m.model.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.TableTypeEnum;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.List;

/**
 * @author diabl
 */
@Component
public class DevTableColumnService extends BaseSortableService {

    public void createDefaultColumn(TableMeta tableMeta) {
        Assert.notNull(tableMeta, ApiErrorMsg.IS_NULL);
        if (Strings.isBlank(tableMeta.getId()) || Strings.isBlank(tableMeta.getEntityName())) {
            throw new RuntimeException(ApiErrorMsg.ID_IS_NULL);
        }
        if (TableTypeEnum.TABLE.getCode().equals(tableMeta.getTableType())) {
            List<ColumnMeta> metaList = MetaManager.singleInstance().getDefaultColumn();
            if (metaList != null && !metaList.isEmpty()) {
                // 排序
                metaList.sort(new Comparator<ColumnMeta>() {
                    @Override
                    public int compare(ColumnMeta o1, ColumnMeta o2) {
                        return o1.getOrdinalPosition() - o2.getOrdinalPosition();
                    }
                });
                // 创建
                for (ColumnMeta meta : metaList) {
                    meta.setTableId(tableMeta.getId().toString());
                    meta.setTableName(tableMeta.getEntityName());
                    meta.setTableCatalog(null);
                    meta.setTableSchema(null);
                    createModel(meta);
                }
            }
        }
    }
}
