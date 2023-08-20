package  org.geelato.web.platform.aop;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.geelato.core.env.EnvManager;

import org.geelato.core.gql.GqlManager;
import org.geelato.core.gql.parser.CommandType;
import org.geelato.core.gql.parser.SaveCommand;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.EntityMeta;

import org.geelato.core.mvc.Ctx;
import org.geelato.core.orm.Dao;
import org.geelato.utils.UIDGenerator;
import org.geelato.web.platform.aop.annotation.OpLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Aspect
@Component
public class OpLogAOPConfig {

    @Autowired
    @Qualifier("primaryDao")
    private Dao dao;

    private GqlManager gqlManager = GqlManager.singleInstance();

    @Around(value = "@annotation( org.geelato.web.platform.aop.annotation.OpLog)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint){
        MethodSignature methodSignature = (MethodSignature)proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        OpLog opLog=method.getAnnotation(OpLog.class);
        Object ret= null;
        try {
            ret = proceedingJoinPoint.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        switch(opLog.type()){
            case "save":
                resolveSaveOpRecord(proceedingJoinPoint,ret);
            default:
                break;
        }
            return ret;
    }



    private void resolveSaveOpRecord(ProceedingJoinPoint proceedingJoinPoint, Object ret){
        String gql=(String) proceedingJoinPoint.getArgs()[1];
        SaveCommand saveCommand = gqlManager.generateSaveSql(gql, new Ctx());
        EntityMeta entityMeta= MetaManager.singleInstance().get(saveCommand.getEntityName());
        String opUser= EnvManager.singleInstance().getCurrentUser().getUserName();
        String opDataId="";
        String opType="";
        String opRecord="";
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        if(ret!=null) {
            opDataId=ret.toString();
            if (saveCommand.getCommandType() == CommandType.Update) {
                opType = "u";
                for (Map.Entry<String, Object> entry : saveCommand.getValueMap().entrySet()) {
                    String filedKey = entry.getKey();
                    String filedName = entityMeta.getFieldMeta(filedKey).getTitle();
                    String filedValue = entry.getValue().toString();
                    //todo 如需改为“原值”修改为“目标值”,需多一次数据库，暂定不实现。
                    String filedChangeRecod = String.format("%s修改为%s", filedName, filedValue);
                    opRecord = opRecord + "," + filedChangeRecod;
                }
            } else if (saveCommand.getCommandType() == CommandType.Insert) {
                opType = "c";
                opRecord = "新增记录";
            }
            String baseSql = "insert into platform_oprecord (id,op_data_id,op_type,op_time,op_user,op_description) values ('%s','%s','%s','%s','%s','%s')";
            String saveSql = String.format(baseSql,
                    UIDGenerator.generate(0),
                    opDataId,
                    opType,
                    formatter.format(date),
                    opUser,
                    opRecord
            );

            dao.getJdbcTemplate().update(saveSql);
        }

    }

}