package org.geelato.web.platform.m.base.service;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.J2Cache;
import org.apache.commons.collections.map.HashedMap;
import org.geelato.core.orm.TransactionHelper;
import  org.geelato.web.platform.aop.annotation.OpLog;
import org.geelato.core.api.*;
import org.geelato.core.biz.rules.BizManagerFactory;
import org.geelato.core.biz.rules.common.EntityValidateRule;
import org.geelato.core.constants.ApiResultCode;
import org.geelato.core.gql.GqlManager;
import org.geelato.core.gql.execute.BoundPageSql;
import org.geelato.core.gql.execute.BoundSql;
import org.geelato.core.gql.parser.DeleteCommand;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.gql.parser.QueryCommand;
import org.geelato.core.gql.parser.SaveCommand;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.EntityMeta;
import org.geelato.core.Ctx;
import org.geelato.core.orm.Dao;
import org.geelato.core.script.rule.BizMvelRuleManager;
import org.geelato.core.sql.SqlManager;
import org.geelato.web.platform.m.security.service.SecurityHelper;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import java.util.*;

/**
 * @author geelato
 */
@Component
public class RuleService {

    @Autowired
    @Qualifier("primaryDao")
    private Dao dao;
    private GqlManager gqlManager = GqlManager.singleInstance();
    private SqlManager sqlManager = SqlManager.singleInstance();
    private MetaManager metaManager = MetaManager.singleInstance();
    private BizMvelRuleManager bizMvelRuleManager = BizManagerFactory.getBizMvelRuleManager("mvelRule");
    private RulesEngine rulesEngine = new DefaultRulesEngine();
    private final static String VARS_PARENT = "$parent";
    private CacheChannel cache = J2Cache.getChannel();

    /**
     * <p>注意: 在使用之前，需先设置dao
     *
     * @see #setDao
     */
    public RuleService() {
    }

    /**
     * @param dao 设置dao，如primaryDao
     */
    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Map<String, Object> queryForMap(String gql) throws DataAccessException {
        QueryCommand command = gqlManager.generateQuerySql(gql, getSessionCtx());
        BoundSql boundSql = sqlManager.generateQuerySql(command);
        return dao.queryForMap(boundSql);
    }

    public <T> T queryForObject(String gql, Class<T> requiredType) throws DataAccessException {
        QueryCommand command = gqlManager.generateQuerySql(gql, getSessionCtx());
        BoundSql boundSql = sqlManager.generateQuerySql(command);
        return dao.queryForObject(boundSql, requiredType);
    }

    public ApiPagedResult queryForMapList(String gql, boolean withMeta) {
        QueryCommand command = gqlManager.generateQuerySql(gql, getSessionCtx());
        BoundPageSql boundPageSql = sqlManager.generatePageQuerySqlMulti(command);
        return dao.queryForMapList(boundPageSql, withMeta);
    }

    /**
     * @param entity 与platform_tree_node 关联的业务实体带有tree_node_id字段
     * @param treeId
     * @return
     */
    public ApiResult<List<Map>> queryForTreeNodeList(String entity, Long treeId) {
        ApiResult<List<Map>> result = new ApiResult<List<Map>>();
        if (!metaManager.containsEntity(entity)) {
            result.setCode(ApiResultCode.ERROR);
            result.setMsg("不存在该实体");
            return result;
        }
        Map params = new HashedMap(2);
        EntityMeta entityMeta = metaManager.getByEntityName(entity);
        params.put("tableName", entityMeta.getTableName());
        params.put("treeId", treeId);
        result.setData(dao.queryForMapList("select_tree_node_left_join", params));
        return result;
    }

    /**
     * @param entity 与platform_tree_node 关联的业务实体带有tree_node_id字段
     * @param treeId
     * @return
     */
    public ApiResult queryForTree(String entity, long treeId, String childrenKey) {
        ApiResult<List<Map>> result = queryForTreeNodeList(entity, treeId);
        if (!result.isSuccess()) {
            return result;
        }
        return new ApiResult().setData(toTree(result.getData(), treeId, childrenKey));
    }

    /**
     * @param itemList
     * @param parentId
     * @param childrenKey e.g. "items"、"children"
     * @return
     */
    private List<Map> toTree(List<Map> itemList, long parentId, String childrenKey) {
        List<Map> resultList = new ArrayList();
        List<Map> toParseList = new ArrayList();
        Iterator<Map> iterator = itemList.iterator();
        while (iterator.hasNext()) {
            Map item = iterator.next();
            long parent = Long.parseLong(item.get("tn_parent").toString());
            if (parentId == parent) {
                resultList.add(item);
            } else {
                toParseList.add(item);
            }
        }

        if (resultList.size() > 0) {
            for (Map item : resultList) {
                List<Map> items = toTree(toParseList, Long.parseLong(item.get("tn_id").toString()), childrenKey);
                if (items.size() > 0) {
                    item.put(childrenKey, items);
                }
            }
        }

        return resultList;
    }

    public ApiPagedResult queryTreeForMapList(String gql, boolean withMeta, String treeId) {
        QueryCommand command = gqlManager.generateQuerySql(gql, getSessionCtx());
        command.getWhere().addFilter("tn.tree_id", treeId);
        BoundPageSql boundPageSql = sqlManager.generatePageQuerySql(command);
        return dao.queryForMapList(boundPageSql, withMeta);
    }

    public ApiMultiPagedResult queryForMultiMapList(String gql, boolean withMeta) {
        Map<String, ApiMultiPagedResult.PageData> dataMap = new HashMap<String, ApiMultiPagedResult.PageData>();
        List<QueryCommand> commandList = gqlManager.generateMultiQuerySql(gql, getSessionCtx());
        for (QueryCommand command : commandList) {
            BoundPageSql boundPageSql = sqlManager.generatePageQuerySql(command);
            dataMap.put(command.getEntityName(), dao.queryForMapListToPageData(boundPageSql, withMeta));
        }
        ApiMultiPagedResult result = new ApiMultiPagedResult();
        result.setData(dataMap);
        return result;
    }

    public <T> List<T> queryForOneColumnList(String gql, Class<T> elementType) throws DataAccessException {
        QueryCommand command = gqlManager.generateQuerySql(gql, getSessionCtx());
        BoundSql boundSql = sqlManager.generateQuerySql(command);
        return dao.queryForOneColumnList(boundSql, elementType);
    }

    /**
     * 保存操作
     * <p>在保存之前，依据业务代码，从配置的业务规则库中读取规则，对command中的数据进行预处理，如更改相应的参数数据。</p>
     *
     * @param biz 业务代码
     * @param gql geelato query language
     * @return 第一个saveCommand执行的返回主健值（saveCommand内可能有子saveCommand）
     */
//    @OpLog(type="save")
    public String save(String biz, String gql) {
        SaveCommand command = gqlManager.generateSaveSql(gql, getSessionCtx());
        Facts facts = new Facts();
        facts.put("saveCommand", command);
        // TODO 通过biz获取业务规则，包括：内置的规则（实体检查），自定义规则（script脚本）
        Rules rules = new Rules();
        bizMvelRuleManager.getRule(biz);
        rules.register(new EntityValidateRule());
        rulesEngine.fire(rules, facts);
        // 存在子命令
        DataSourceTransactionManager dataSourceTransactionManager=new DataSourceTransactionManager(dao.getJdbcTemplate().getDataSource());
        TransactionStatus transactionStatus= TransactionHelper.beginTransaction(dataSourceTransactionManager);
        return recursiveSave(command,dataSourceTransactionManager,transactionStatus);
    }

    public Object batchSave(String gql,Boolean transaction) {
        List<String> returnPks=new ArrayList<>();
        List<SaveCommand> commandList = gqlManager.generateBatchSaveSql(gql, getSessionCtx());
        if(transaction){
            DataSourceTransactionManager dataSourceTransactionManager=new DataSourceTransactionManager(dao.getJdbcTemplate().getDataSource());
            TransactionStatus transactionStatus= TransactionHelper.beginTransaction(dataSourceTransactionManager);
            for (SaveCommand saveCommand : commandList){
                BoundSql boundSql = sqlManager.generateSaveSql(saveCommand);
                String pkValue = dao.save(boundSql);
                if(pkValue.equals("saveFail")){
                    TransactionHelper.rollbackTransaction(dataSourceTransactionManager,transactionStatus);
                    break;
                }else{
                    returnPks.add(pkValue);
                }
            }
            TransactionHelper.commitTransaction(dataSourceTransactionManager,transactionStatus);
        }else {
            for (SaveCommand saveCommand : commandList){
                BoundSql boundSql = sqlManager.generateSaveSql(saveCommand);
                String pkValue = dao.save(boundSql);
                if(pkValue.equals("saveFail")){
                    continue;
                }else{
                    returnPks.add(pkValue);
                }
            }
        }
        return  returnPks;
    }




    public Object multiSave(String gql) {
        List<SaveCommand> commandList = gqlManager.generateMultiSaveSql(gql, getSessionCtx());
        List<BoundSql> boundSqlList = sqlManager.generateBatchSaveSql(commandList);
        return  dao.multiSave(boundSqlList);
    }
    /**
     * 递归执行，存在需解析依赖变更的情况
     * 不执行业务规则检查
     *
     * @param command
     * @param dataSourceTransactionManager
     * @param transactionStatus
     * @return
     */
    public String recursiveSave(SaveCommand command, DataSourceTransactionManager dataSourceTransactionManager, TransactionStatus transactionStatus) {

        // 如果更新了个人配置，则去除缓存
        if ("platform_user_config".equals(command.getEntityName()) && SecurityHelper.getCurrentUser() != null) {
            cache.evict("config", SecurityHelper.getCurrentUser().id);
        }
        BoundSql boundSql = sqlManager.generateSaveSql(command);
        String pkValue = dao.save(boundSql);
        if(pkValue.equals("saveFail")){
            command.setExecution(false);
        }else{
            command.setExecution(true);
        }
        // 存在子command，需执行
        if (command.hasCommands()) {
            command.getCommands().forEach(subCommand -> {
                // 保存之前需先替换subCommand中的变量值，如依赖于父command执行的返回id：$parent.id
                subCommand.getValueMap().forEach((key, value) -> {
                    if (value != null) {
                        subCommand.getValueMap().put(key, parseValueExp(subCommand, value.toString(), 0));
                    }
                });
                recursiveSave(subCommand, dataSourceTransactionManager, transactionStatus);
            });
        }else{
            if(pkValue.equals("saveFail")){
                TransactionHelper.rollbackTransaction(dataSourceTransactionManager,transactionStatus);
            }else{
                TransactionHelper.commitTransaction(dataSourceTransactionManager,transactionStatus);
            }
        }
        return pkValue;
    }

    /**
     * 解析值表达式
     *
     * @param currentCommand
     * @param valueExp       e.g. $parent.name
     * @param times          递归调用的次数，在该方法外部调用时，传入0；之后该方法内部递归调用，自增该值
     * @return
     */
    private Object parseValueExp(SaveCommand currentCommand, String valueExp, int times) {
        String valueExpTrim = valueExp.trim();
        // 检查是否存在变量$parent
        if (valueExpTrim.startsWith(VARS_PARENT)) {
            return parseValueExp((SaveCommand) currentCommand.getParentCommand(), valueExpTrim.substring(VARS_PARENT.length() + 1), times + 1);
        } else {
            if (times == 0) {
                //如果是第一次且无VARS_PARENT关键字，则直接返回值
                return valueExp;
            } else {
                if(currentCommand.getParentCommand()!=null){
                    SaveCommand parentSaveCommand= (SaveCommand) currentCommand.getParentCommand();
                    return parentSaveCommand.getValueMap().get(valueExpTrim);
                }else{
                    return currentCommand.getValueMap().get(valueExpTrim);
                }
            }
        }
    }

    /**
     * 删除操作
     * <p>在删除之前，依据业务代码，从配置的业务规则库中读取规则，对command中的数据进行预处理，如更改相应的参数数据。</p>
     *
     * @param biz 业务代码
     * @return 主健值
     */
    public int delete(String biz, String id) {
        FilterGroup filterGroup;

        if(id.contains(",")){
            filterGroup = new FilterGroup().addFilter("id", FilterGroup.Operator.in,id);
        }else{
            filterGroup = new FilterGroup().addFilter("id",id);
        }

        BoundSql boundSql = sqlManager.generateDeleteSql(biz,filterGroup);
        return dao.delete(boundSql);
    }

    public int deleteByGql(String biz, String gql) {
        DeleteCommand command = gqlManager.generateDeleteSql(gql, getSessionCtx());
        BoundSql boundSql = sqlManager.generateDeleteSql(command);
        return dao.delete(boundSql);
    }

    /**
     * @return 当前会话信息
     */
    protected Ctx getSessionCtx() {
        return new Ctx();
    }


}
