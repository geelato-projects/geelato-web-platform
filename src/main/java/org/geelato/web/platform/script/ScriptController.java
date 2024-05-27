package org.geelato.web.platform.script;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.graaljs.GraaljsTestObj;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.service.RuleService;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
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
@RequestMapping(value = "/api/script")
public class ScriptController extends BaseController {
    @Autowired
    InstanceProxy instanceProxy;
    @RequestMapping(value = "/exec/{scriptId}", method = RequestMethod.POST)
    @ResponseBody
    public ApiPagedResult exec(@PathVariable("scriptId") String scriptId, HttpServletRequest request){
        String gql=getGql(request);

        RuleService ruleService= instanceProxy.getRuleService();
        String scriptContent="(function(){" +
                "var result=ruleService.queryForMapList(gql,false);" +
                "return result;" +
                "});";
        Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true).build();
            context.getBindings("js").putMember("ruleService",ruleService);
            ApiPagedResult result = context.eval("js",scriptContent).execute(gql).as(ApiPagedResult.class);
            return result;


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
