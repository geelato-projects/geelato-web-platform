package org.geelato.web.platform.m.model.service;

import org.geelato.core.constants.MetaDaoSql;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.core.meta.schema.SchemaTable;
import org.geelato.core.util.SchemaUtils;
import org.geelato.web.platform.m.base.rest.MetaDdlController;
import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class DevTableService extends BaseSortableService {
    private static Logger logger = LoggerFactory.getLogger(MetaDdlController.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Lazy
    @Autowired
    private DevTableColumnService devTableColumnService;
    @Lazy
    @Autowired
    private DevTableForeignService devTableForeignService;

    public void resetTableByDataBase(TableMeta tableMeta) throws ParseException {
        // database_table
        String tableSql = String.format(MetaDaoSql.INFORMATION_SCHEMA_TABLES, MetaDaoSql.TABLE_SCHEMA_METHOD, " AND TABLE_NAME='" + tableMeta.getEntityName() + "'");
        logger.info(tableSql);
        List<Map<String, Object>> tableList = dao.getJdbcTemplate().queryForList(tableSql);
        List<SchemaTable> schemaTables = SchemaUtils.buildData(SchemaTable.class, tableList);
        // handle
        if (schemaTables != null && schemaTables.size() > 0) {
            SchemaTable schemaTable = schemaTables.get(0);
            tableMeta.setTitle(schemaTable.getTableComment());
            tableMeta.setTableComment(schemaTable.getTableComment());
            tableMeta.setTableName(schemaTable.getTableName());
            tableMeta.setUpdateAt(sdf.parse(schemaTable.getCreateTime()));
            updateModel(tableMeta);
            devTableColumnService.resetTableColumnByDataBase(tableMeta, false);
            devTableForeignService.resetTableForeignByDataBase(tableMeta, false);
        } else {
            tableMeta.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
            isDeleteModel(tableMeta);
            devTableColumnService.resetTableColumnByDataBase(tableMeta, true);
            devTableForeignService.resetTableForeignByDataBase(tableMeta, false);
        }
    }
}
