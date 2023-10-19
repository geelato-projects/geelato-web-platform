package org.geelato.web.platform.m.syspackage.rest;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.Ctx;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.gql.GqlManager;
import org.geelato.core.gql.execute.BoundPageSql;
import org.geelato.core.gql.parser.QueryCommand;
import org.geelato.core.sql.SqlManager;
import org.geelato.web.platform.m.base.entity.AppPage;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.syspackage.entity.AppMeta;
import org.geelato.web.platform.m.syspackage.entity.AppPackage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.stylesheets.LinkStyle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/package")
public class PackageController extends BaseController {

    private GqlManager gqlManager = GqlManager.singleInstance();
    private SqlManager sqlManager = SqlManager.singleInstance();
    private String basePath="D://geelato-project//app_package_temp/";
    private String baseUploadPath=basePath+"upload_temp/";
    /*
    打包应用
     */
    @RequestMapping(value = {"/packet/{appId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public Object packetApp(@PathVariable("appId") String appId){
        Map<String,String> appMetaMap=appMetaMap(appId);
        AppPackage appPackage=new AppPackage();
        appPackage.setSourceAppId(appId);
        List<AppMeta> appMetaList=new ArrayList<>();
        for (String key : appMetaMap.keySet()) {
            String metaName=key;
            String value = appMetaMap.get(key);
            List<Map<String, Object>> metaData= dao.getJdbcTemplate().queryForList(value);
            if(metaName.equals("platform_app")&&metaData.size()>0) {
                appPackage.setAppCode(metaData.get(0).get("code").toString());
            }
            AppMeta appMeta=new AppMeta(metaName,metaData);
            appMetaList.add(appMeta);
        }
        appPackage.setAppMetaList(appMetaList);

        writePackageData(appPackage);
        return appPackage;
    }

    /*
        获取打包进度
     */
    @RequestMapping(value = {"/packet/progress/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult packetProcess(@PathVariable("versionId") String appId){
        return null;
    }
    /*
    下载版本包
     */
    @RequestMapping(value = {"/downloadPackage/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult downloadPackage(@PathVariable("versionId") String appId){
        return null;
    }

    /*
    上传版本包
     */
    @RequestMapping(value = {"/uploadPackage"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public void uploadPackage(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String targetPath=baseUploadPath+file.getOriginalFilename();
        Files.write(Path.of(targetPath), bytes);
    }

    /*
    部署版本包
     */
    @RequestMapping(value = {"/deploy/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult deployPackage(HttpServletRequest req){
        return null;
    }

    /*
    获取部署进度
     */
    @RequestMapping(value = {"/deploy/progress/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult deployProcess(@PathVariable("versionId") String appId){
        return null;
    }
    /*
    获取指定应用的版本信息
    */
    @RequestMapping(value = {"/queryVersion/{appId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult queryVersion(@PathVariable("appId") String appId){
        return null;
    }

    /*
    删除版本
    */
    @RequestMapping(value = {"/delelteVersion/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult deleteVersion(@PathVariable("versionId") String appId){
        return null;
    }


    private Map<String,String> appMetaMap(String appId){
        Map<String,String> map=new HashMap<>();
        map.put("platform_app",String.format("select * from platform_app where id='%s'",appId));
        map.put("platform_app_page",String.format("select * from platform_app_page where app_id='%s'",appId));
        map.put("platform_tree_node",String.format("select * from platform_tree_node where tree_id='%s'",appId));
        map.put("platform_dev_db_connect","select * from platform_dev_db_connect");
        map.put("platform_dev_table","select * from platform_dev_table");
        map.put("platform_dev_table_foreign","select * from platform_dev_table_foreign");
        map.put("platform_dev_view","select * from platform_dev_view");
        map.put("platform_dev_column","select * from platform_dev_column");
        return map;
    }

    private void writePackageData(AppPackage appPackage){
        String jsonStr= JSONObject.toJSONString(appPackage);

        String packageSuffix=".gdp";
        String dataFileName=appPackage.getAppCode()==""?appPackage.getAppCode():"geelatoApp";
        File file = new File(basePath+dataFileName+packageSuffix);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(jsonStr);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
