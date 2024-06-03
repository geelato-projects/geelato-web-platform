package org.geelato.web.platform.script;

import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.graal.GraalManager;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.service.RuleService;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping(value = "/api")
public class ScriptController extends BaseController {
    @Autowired
    InstanceProxy instanceProxy;

    public GraalManager graalManager= GraalManager.singleInstance();
    @RequestMapping(value = "/exec/{scriptId}", method = RequestMethod.POST)
    @ResponseBody
    public ApiPagedResult exec(@PathVariable("scriptId") String scriptId, HttpServletRequest request){
        String gql=getGql(request);
        String scriptContent=getScriptContent(scriptId);
        Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true).build();
        Map<String,Object> graalServiceMap= graalManager.getGraalServiceMap();
        Map<String,Object> graalVariableMap= graalManager.getGraalVariableMap();
        for(Map.Entry entry : graalServiceMap.entrySet()){
            context.getBindings("js").putMember(entry.getKey().toString(),entry.getValue());
        }
        for(Map.Entry entry : graalVariableMap.entrySet()) {
            context.getBindings("js").putMember(entry.getKey().toString(), entry.getValue());
        }
        ApiPagedResult result = context.eval("js",scriptContent).execute(gql).as(ApiPagedResult.class);
        return result;
    }

    private String getScriptContent(String scriptId) {
        StringBuilder sb=new StringBuilder();
        sb.append("(function(gql){");
        sb.append(defaultContent());
        sb.append(scriptContent());
        sb.append("})");
        return sb.toString();
    }

    private String scriptContent() {
        return "var result=$gl.dao.queryForMapList(gql,false);" +
                "return result;";
    }

    private String defaultContent() {
        String content="var $gl={};" +
                "$gl.dao=GqlService;" +
                "$gl.tenant=tenant;" +
                "$gl.user=user;";
        return content;
    }

    private String getGql(HttpServletRequest request) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = null;
        try {
            br = request.getReader();
        } catch (IOException e) {
        }
        String str;
        try {
            while ((str = br.readLine()) != null) {
                stringBuilder.append(str);
            }
        } catch (IOException e) {
        }
        return stringBuilder.toString();
    }

}
