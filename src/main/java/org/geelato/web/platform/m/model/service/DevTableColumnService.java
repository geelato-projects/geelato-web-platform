package org.geelato.web.platform.m.model.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.constants.MetaDaoSql;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.core.enums.TableTypeEnum;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.core.meta.schema.SchemaColumn;
import org.geelato.core.meta.schema.SchemaIndex;
import org.geelato.core.util.SchemaUtils;
import org.geelato.core.util.StringUtils;
import org.geelato.web.platform.m.base.rest.MetaDdlController;
import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author diabl
 */
@Component
public class DevTableColumnService extends BaseSortableService {
    private static Logger logger = LoggerFactory.getLogger(MetaDdlController.class);

    /**
     * 生成默认字段，表格类型：数据库表
     *
     * @param tableMeta
     */
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

    /**
     * 获取默认视图
     *
     * @param entityName
     * @return
     */
    public String getDefaultViewSql(String entityName) {
        if (Strings.isBlank(entityName)) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("tableName", entityName);
        List<ColumnMeta> columnMetaList = queryModel(ColumnMeta.class, params);
        if (columnMetaList != null && !columnMetaList.isEmpty()) {
            // 排序
            columnMetaList.sort(new Comparator<ColumnMeta>() {
                @Override
                public int compare(ColumnMeta o1, ColumnMeta o2) {
                    return o1.getOrdinalPosition() - o2.getOrdinalPosition();
                }
            });
            // 去重
            HashMap<String, ColumnMeta> columnMetaMap = new HashMap<String, ColumnMeta>();
            for (ColumnMeta columnMeta : columnMetaList) {
                if (Strings.isNotBlank(columnMeta.getName()) && !columnMetaMap.containsKey(columnMeta.getName())) {
                    columnMetaMap.put(columnMeta.getName(), columnMeta);
                }
            }
            // 拼接
            if (!columnMetaMap.isEmpty()) {
                List<String> asList = new ArrayList<>();
                for (Map.Entry<String, ColumnMeta> metaMap : columnMetaMap.entrySet()) {
                    ColumnMeta meta = metaMap.getValue();
                    asList.add(meta.getName());
                }
                return String.format(MetaDaoSql.SQL_TABLE_DEFAULT_VIEW, String.join(",", asList), entityName);
            }
        }
        String defaultViewSql = String.format(MetaDaoSql.SQL_TABLE_DEFAULT_VIEW, "*", entityName);

        return defaultViewSql;
    }

    /**
     * 依据表格情况，从数据库中更新至 dev_column 中
     *
     * @param tableMeta
     * @param deleteAll
     */
    public void resetTableColumnByDataBase(TableMeta tableMeta, boolean deleteAll) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("tableId", tableMeta.getId());
        queryParams.put("tableName", tableMeta.getEntityName());
        queryParams.put("tableSchema", null);
        // dev_table
        List<ColumnMeta> columnMetas = queryModel(ColumnMeta.class, queryParams);
        HashMap<String, ColumnMeta> columnMetaMap = new HashMap<>();
        if (columnMetas != null && columnMetas.size() > 0) {
            for (ColumnMeta meta : columnMetas) {
                if (deleteAll) {
                    meta.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
                    isDeleteModel(meta);
                    continue;
                }
                if (Strings.isNotBlank(meta.getName()) && !columnMetaMap.containsKey(meta.getName())) {
                    columnMetaMap.put(meta.getName(), meta);
                }
            }
        }
        if (!deleteAll) {
            // database_table_index
            String indexSql = String.format(MetaDaoSql.SQL_INDEXES_NO_PRIMARY, tableMeta.getEntityName());
            logger.info(indexSql);
            List<Map<String, Object>> indexList = dao.getJdbcTemplate().queryForList(indexSql);
            List<SchemaIndex> schemaIndices = SchemaUtils.buildData(SchemaIndex.class, indexList);
            List<String> uniques = new ArrayList<>();
            if (schemaIndices != null && schemaIndices.size() > 0) {
                for (SchemaIndex index : schemaIndices) {
                    boolean isUnique = Strings.isBlank(index.getNonUnique()) ? false : !Boolean.parseBoolean(index.getNonUnique());
                    if (Strings.isNotBlank(index.getColumnName()) && isUnique) {
                        uniques.add(index.getColumnName());
                    }
                }
            }
            // database_table_column
            String columnSql = String.format(MetaDaoSql.INFORMATION_SCHEMA_COLUMNS, MetaDaoSql.TABLE_SCHEMA_METHOD, " AND TABLE_NAME='" + tableMeta.getEntityName() + "'");
            logger.info(columnSql);
            List<Map<String, Object>> columnList = dao.getJdbcTemplate().queryForList(columnSql);
            List<SchemaColumn> schemaColumns = SchemaUtils.buildData(SchemaColumn.class, columnList);
            HashMap<String, SchemaColumn> schemaColumnMap = new HashMap<>();
            if (schemaColumns != null && schemaColumns.size() > 0) {
                for (SchemaColumn schema : schemaColumns) {
                    if (Strings.isNotBlank(schema.getColumnName()) && !schemaColumnMap.containsKey(schema.getColumnName())) {
                        schema.setUnique(uniques.contains(schema.getColumnName()));
                        schemaColumnMap.put(schema.getColumnName(), schema);
                    }
                }
            }
            compareHashMapKeys(tableMeta, columnMetaMap, schemaColumnMap);
        }
    }

    /**
     * 比较 dev_column 和 数据库中字段 创建、更新、删除
     *
     * @param metaMap
     * @param schemaMap
     */
    private void compareHashMapKeys(TableMeta tableMeta, HashMap<String, ColumnMeta> metaMap, Map<String, SchemaColumn> schemaMap) {
        // 遍历 metaMap 的键 不存在：删除
        for (String key : metaMap.keySet()) {
            if (!schemaMap.containsKey(key)) {
                ColumnMeta meta = metaMap.get(key);
                meta.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
                isDeleteModel(meta);
            }
        }
        // 遍历 schemaMap 的键 存在：更新；不存在：添加
        for (String key : schemaMap.keySet()) {
            SchemaColumn schema = schemaMap.get(key);
            ColumnMeta meta = schema.convertIntoMeta(metaMap.get(key));
            meta.setTableId(tableMeta.getId());
            meta.setTableName(tableMeta.getTableName());
            if (!metaMap.containsKey(key)) {
                meta.setName(schema.getColumnName().toLowerCase(Locale.ENGLISH));
                meta.setFieldName(StringUtils.toCamelCase(meta.getName()));
                createModel(meta);
            } else {
                updateModel(meta);
            }
        }
    }

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(ColumnMeta model) {
        // 常用
        model.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        model.setSeqNo(ColumnDefault.SEQ_NO_DELETE);
        // 去除 主键、必填、唯一约束
        model.setKey(false);
        model.setNullable(true);
        model.setUniqued(false);

        dao.save(model);

    }
}
