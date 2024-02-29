package org.geelato.web.platform.m.syspackage.rest;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.itextpdf.text.Meta;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import netscape.javascript.JSObject;
import okio.FileMetadata;
import org.bouncycastle.cms.PasswordRecipientId;
import org.geelato.core.Ctx;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.gql.GqlManager;
import org.geelato.core.gql.execute.BoundPageSql;
import org.geelato.core.gql.execute.BoundSql;
import org.geelato.core.gql.parser.JsonTextSaveParser;
import org.geelato.core.gql.parser.QueryCommand;
import org.geelato.core.gql.parser.SaveCommand;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.EntityMeta;
import org.geelato.core.meta.model.field.FieldMeta;
import org.geelato.core.orm.DaoException;
import org.geelato.core.orm.TransactionHelper;
import org.geelato.core.sql.SqlManager;
import org.geelato.core.util.StringUtils;
import org.geelato.utils.ZipUtils;
import org.geelato.web.platform.m.base.entity.AppPage;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.rest.CacheController;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.DownloadService;
import org.geelato.web.platform.m.security.service.SecurityHelper;
import org.geelato.web.platform.m.syspackage.PackageConfigurationProperties;
import org.geelato.web.platform.m.syspackage.entity.AppMeta;
import org.geelato.web.platform.m.syspackage.entity.AppPackage;
import org.geelato.web.platform.m.syspackage.entity.AppVersion;
import org.geelato.web.platform.m.syspackage.service.AppVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.stylesheets.LinkStyle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping(value = "/package")
public class PackageController extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(PackageController.class);
    private String defaultPackageName="geelatoApp";
    @Resource
    private PackageConfigurationProperties packageConfigurationProperties;
    @Resource
    private AttachService attachService;
    @Resource
    private DownloadService downloadService;

    private MetaManager metaManager= MetaManager.singleInstance();
    private SqlManager sqlManager = SqlManager.singleInstance();
    private JsonTextSaveParser jsonTextSaveParser = new JsonTextSaveParser();

    @Autowired
    AppVersionService appVersionService;
    /*
    打包应用
     */
    @RequestMapping(value = {"/packet/{appId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult packetApp(@PathVariable("appId") String appId,String version,String description){
        ApiResult apiResult=new ApiResult();
        Map<String,String> appMetaMap=appMetaMap(appId,"package");
        AppPackage appPackage=new AppPackage();
        appPackage.setSourceAppId(appId);
        List<AppMeta> appMetaList=new ArrayList<>();
        for (String key : appMetaMap.keySet()) {
            String value = appMetaMap.get(key);
            List<Map<String, Object>> metaData= dao.getJdbcTemplate().queryForList(value);
            if(key.equals("platform_app")&& !metaData.isEmpty()) {
                appPackage.setAppCode(metaData.get(0).get("code").toString());
            }else{
                AppMeta appMeta=new AppMeta(key,metaData);
                appMetaList.add(appMeta);
            }
        }
        appPackage.setAppMetaList(appMetaList);
        String filePath=writePackageData(appPackage);
        AppVersion av=new AppVersion();
        av.setAppId(appId);
        av.setVersion(version);
        av.setDescription(description);
        av.setPackageSource("current packet");
        av.setStatus("release");
        av.setPacketTime(new Date());
        av.setPackagePath(filePath);
        apiResult.setData(appVersionService.createModel(av));
        return apiResult;
    }

    /*
    下载版本包
     */
    @RequestMapping(value = {"/downloadPackage/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public void downloadPackage(@PathVariable("versionId") String versionId) throws IOException {
        AppVersion appVersion = appVersionService.getModel(AppVersion.class, versionId);
        String filePath = appVersion.getPackagePath();
        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        response.setContentType("application/octet-stream");
        OutputStream outputStream = response.getOutputStream();
        int bytesRead;
        byte[] buffer = new byte[4096];
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        fileInputStream.close();
        outputStream.close();
    }

    /*
    上传版本包
     */
    @RequestMapping(value = {"/uploadPackage/{appId}"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult uploadPackage(@RequestParam("file") MultipartFile file,@PathVariable("appId") String appId) throws IOException {
        ApiResult apiResult=new ApiResult();
        byte[] bytes = file.getBytes();
        String targetPath=packageConfigurationProperties.getUploadPath()+file.getOriginalFilename();
        Files.write(Path.of(targetPath), bytes);
        AppVersion av=new AppVersion();
        av.setAppId(appId);
        av.setPacketTime(new Date());
        av.setPackagePath(targetPath);
        apiResult.setData( appVersionService.createModel(av));
        return apiResult;

    }

    /*
    部署版本包
     */
    @RequestMapping(value = {"/deploy/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public void deployPackage(@PathVariable("versionId") String versionId) throws DaoException {
        AppVersion appVersion= appVersionService.getModel(AppVersion.class,versionId);
        String appPackageData;
        if(appVersion!=null&&!StringUtils.isEmpty(appVersion.getPackagePath())) {
            if(appVersion.getPackagePath().contains(".zgdp")){
                appPackageData = ZipUtils.readPackageData(appVersion.getPackagePath(), ".gdp");
            }else{
                Attach attach = attachService.getModel(Attach.class, appVersion.getPackagePath());
                File file = downloadService.downloadFile(attach.getName(), attach.getPath());
                appPackageData = ZipUtils.readPackageData(file, ".gdp");
            }
            AppPackage appPackage = resolveAppPackageData(appPackageData);
            if (appPackage != null && !appPackage.getAppMetaList().isEmpty()) {
                deleteCurrentVersion(appVersion.getAppId());
                deployAppPackageData(appPackage);
            }else{
                logger.info("deploy error：无法读取到应用包数据，请检查应用包");
            }
        }
    }

    private void deleteCurrentVersion(String appId) {
        Map<String,String> appMetaMap= appMetaMap(appId,"remove");
        for (String key : appMetaMap.keySet()) {
            String value = appMetaMap.get(key);
             logger.info(String.format("remove sql：%s ",value));
             dao.getJdbcTemplate().execute(value);
        }
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
    @RequestMapping(value = {"/deleteVersion/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult deleteVersion(@PathVariable("versionId") String appId){
        return null;
    }


    private Map<String,String> appMetaMap(String appId,String type){
        Map<String,String> map=new HashMap<>();
        String preOperateSql="";
        switch (type){
            case "package":
                preOperateSql="select * from ";
                map.put("platform_app",String.format("%s platform_app where id='%s'",preOperateSql,appId));
                break;
            case "remove":
                preOperateSql="delete from  ";
                break;
            default:
                break;
        }
        map.put("platform_app_page",String.format("%s  platform_app_page where app_id='%s'",preOperateSql,appId));
        map.put("platform_tree_node",String.format("%s  platform_tree_node where tree_id='%s'",preOperateSql,appId));
        map.put("platform_dev_db_connect",String.format("%s platform_dev_db_connect where app_id='%s'",preOperateSql,appId));
        map.put("platform_dev_table",String.format("%s  platform_dev_table where app_id='%s' ",preOperateSql,appId));
        map.put("platform_dev_column",String.format("%s  platform_dev_column where app_id='%s'",preOperateSql,appId));
        map.put("platform_dev_table_foreign",String.format("%s  platform_dev_table_foreign where app_id='%s'",preOperateSql,appId));
        map.put("platform_dev_view",String.format("%s  platform_dev_view where app_id='%s'",preOperateSql,appId));
        map.put("platform_dict",String.format("%s  platform_dict where app_id='%s'",preOperateSql,appId));
        map.put("platform_dict_item",String.format("%s  platform_dict_item where app_id='%s'",preOperateSql,appId));
        map.put("platform_permission",String.format("%s platform_permission where app_id='%s'",preOperateSql,appId));
        map.put("platform_role",String.format("%s  platform_role where app_id='%s'",preOperateSql,appId));
        map.put("platform_role_r_permission",String.format("%s  platform_role_r_permission where app_id='%s'",preOperateSql,appId));
        map.put("platform_role_r_tree_node",String.format("%s  platform_role_r_tree_node where app_id='%s'",preOperateSql,appId));
        map.put("platform_role_r_app",String.format("%s  platform_role_r_app where app_id='%s'",preOperateSql,appId));
        map.put("platform_sys_config",String.format("%s  platform_sys_config where app_id='%s'",preOperateSql,appId));

        return map;
    }



    private Map<String,String> appResourceMetaMap(String appId){
        Map<String,String> map=new HashMap<>();
//        map.put("platform_resources",String.format("select * from platform_permission where app_id='%s'",appId));   //表需要加app_id
        return map;
    }
    private String writePackageData(AppPackage appPackage){
        String jsonStr= JSONObject.toJSONString(appPackage);
        String packageSuffix=".gdp";
        String dataFileName=StringUtils.isEmpty(appPackage.getAppCode())?
                defaultPackageName:appPackage.getAppCode();
        String fileName=dataFileName+packageSuffix;
        String tempFolderPath=dataFileName+"/";
        File file = new File(packageConfigurationProperties.getPath()+tempFolderPath+fileName);
        if(!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(jsonStr);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writePackageResourceData(appPackage);
        return compressAppPackage( packageConfigurationProperties.getPath()+tempFolderPath,appPackage);
    }

    private void writePackageResourceData(AppPackage appPackage){


    }
    private String  compressAppPackage(String sourcePackageFolder,AppPackage appPackage ){
        String packageSuffix=".zgdp";
        String appPackageName = StringUtils.isEmpty(appPackage.getAppCode())?
                defaultPackageName:appPackage.getAppCode();
        String appPackageFullName=appPackageName+packageSuffix;
        String targetZipPath=packageConfigurationProperties.getPath()+appPackageFullName;
        ZipUtils.compressDirectory(sourcePackageFolder,targetZipPath);
        return targetZipPath;
    }

    private AppPackage resolveAppPackageData(String appPackageData) {
        AppPackage appPackage=null;
        if(!StringUtils.isEmpty(appPackageData)){
            appPackage=JSONObject.parseObject(appPackageData,AppPackage.class);
        }
        return appPackage;
    }
    private void deployAppPackageData(AppPackage appPackage) throws DaoException {
        logger.info("----------------------deploy start--------------------");
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(dao.getJdbcTemplate().getDataSource());
        TransactionStatus transactionStatus = TransactionHelper.beginTransaction(dataSourceTransactionManager);
        for (AppMeta appMeta : appPackage.getAppMetaList()) {
            logger.info(String.format("开始处理元数据：%s",appMeta.getMetaName()));
            Map<String, Object> metaData = new HashMap<>();
            ArrayList<Map<String, Object>> metaDataArray=new ArrayList<>();
            String appMetaName = appMeta.getMetaName();
            Object appMetaData = appMeta.getMetaData();
            EntityMeta entityMeta = metaManager.getByEntityName(appMetaName);
            JSONArray jsonArray = JSONArray.parseArray(JSONObject.toJSONString(appMetaData));
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jo = jsonArray.getJSONObject(i);
                Map<String, Object> columnMap = new HashMap<>();
                for (String key : jo.keySet()) {
                    FieldMeta fieldMeta = entityMeta.getFieldMetaByColumn(key);
                    if("id".equals(key)){
                        columnMap.put("forceId", jo.get(key));
                    }else{
                        columnMap.put(fieldMeta.getFieldName(), jo.get(key));
                    }
                }
                metaDataArray.add(columnMap);
            }
            metaData.put(appMeta.getMetaName(), metaDataArray);
            List<SaveCommand> saveCommandList = jsonTextSaveParser.parseBatch(JSONObject.toJSONString(metaData), new Ctx());
            for (SaveCommand saveCommand : saveCommandList) {

                BoundSql boundSql = sqlManager.generateSaveSql(saveCommand);
                String pkValue = dao.save(boundSql);
            }
            logger.info(String.format("结束处理元数据：%s",appMeta.getMetaName()));
        }
        TransactionHelper.commitTransaction(dataSourceTransactionManager,transactionStatus);
        logger.info("----------------------deploy end--------------------");
    }
}
