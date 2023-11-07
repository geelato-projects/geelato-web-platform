package org.geelato.web.platform.m.excel.entity;

import org.apache.logging.log4j.util.Strings;
import org.geelato.web.platform.enums.ExcelColumnTypeRuleEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * @author diabl
 * @description: 解析规则
 * @date 2023/10/30 10:28
 */
public class BusinessTypeRuleData {
    // 类型
    private String type;
    // 规则，正则表达式；字典编码；表格:字段,字段...
    private String rule;
    // 目标字段；替换值
    private String goal;
    // 是否优先于 全局多值处理
    private boolean priority = false;
    // 执行次序
    private Integer order;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public boolean isPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public boolean isRuleTypeDeletes() {
        return ExcelColumnTypeRuleEnum.DELETES.name().equalsIgnoreCase(this.type);
    }

    public boolean isRuleTypeReplace() {
        return ExcelColumnTypeRuleEnum.REPLACE.name().equalsIgnoreCase(this.type);
    }

    public boolean isRuleTypeUpperCase() {
        return ExcelColumnTypeRuleEnum.UPPERCASE.name().equalsIgnoreCase(this.type);
    }

    public boolean isRuleTypeLowerCase() {
        return ExcelColumnTypeRuleEnum.LOWERCASE.name().equalsIgnoreCase(this.type);
    }

    public boolean isRuleTypeDictionary() {
        return ExcelColumnTypeRuleEnum.DICTIONARY.name().equalsIgnoreCase(this.type);
    }

    public boolean isRuleTypeQueryGoal() {
        return ExcelColumnTypeRuleEnum.QUERYGOAL.name().equalsIgnoreCase(this.type);
    }

    public boolean isRuleTypeQueryRule() {
        return ExcelColumnTypeRuleEnum.QUERYRULE.name().equalsIgnoreCase(this.type);
    }

    /**
     * 求取主键值所需，表格名称
     *
     * @return
     */
    public String getQueryRuleTable() {
        if (this.isRuleTypeQueryRule() || this.isRuleTypeQueryGoal()) {
            if (Strings.isNotBlank(this.rule)) {
                String[] keys = this.rule.split(":");
                if (keys != null && keys.length == 2 && Strings.isNotBlank(keys[0]) && Strings.isNotBlank(keys[1])) {
                    return keys[0];
                }
            }
        }
        return null;
    }

    /**
     * 求取主键值所需，字段名称
     *
     * @return
     */
    public List<String> getQueryRuleColumn() {
        List<String> columns = new ArrayList<>();
        if (this.isRuleTypeQueryRule() || this.isRuleTypeQueryGoal()) {
            if (Strings.isNotBlank(this.rule)) {
                String[] keys = this.rule.split(":");
                if (keys != null && keys.length == 2 && Strings.isNotBlank(keys[0]) && Strings.isNotBlank(keys[1])) {
                    String[] keys1 = keys[1].split(",");
                    if (keys1 != null) {
                        for (String key : keys1) {
                            if (Strings.isNotBlank(key) && !columns.contains(key)) {
                                columns.add(key);
                            }
                        }
                    }
                }
            }
        }
        return columns;
    }
}
