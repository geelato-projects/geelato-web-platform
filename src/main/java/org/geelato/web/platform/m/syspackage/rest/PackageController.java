package org.geelato.web.platform.m.syspackage.rest;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.geelato.core.Ctx;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.gql.GqlManager;
import org.geelato.core.gql.execute.BoundSql;
import org.geelato.core.gql.parser.JsonTextSaveParser;
import org.geelato.core.gql.parser.SaveCommand;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.EntityMeta;
import org.geelato.core.meta.model.field.FieldMeta;
import org.geelato.core.orm.TransactionHelper;
import org.geelato.core.sql.SqlManager;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.syspackage.entity.AppMeta;
import org.geelato.web.platform.m.syspackage.entity.AppPackage;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    private MetaManager metaManager = MetaManager.singleInstance();
    private GqlManager gqlManager = GqlManager.singleInstance();
    private SqlManager sqlManager = SqlManager.singleInstance();
    private JsonTextSaveParser jsonTextSaveParser = new JsonTextSaveParser();
    private String basePath = "D://geelato-project//app_package_temp/";
    private String baseUploadPath = basePath + "upload_temp/";

    /*
    打包应用
     */
    @RequestMapping(value = {"/packet/{appId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public Object packetApp(@PathVariable("appId") String appId) {
        Map<String, String> appMetaMap = appMetaMap(appId);
        AppPackage appPackage = new AppPackage();
        appPackage.setSourceAppId(appId);
        List<AppMeta> appMetaList = new ArrayList<>();
        for (String key : appMetaMap.keySet()) {
            String metaName = key;
            String value = appMetaMap.get(key);
            List<Map<String, Object>> metaData = dao.getJdbcTemplate().queryForList(value);
            if (metaName.equals("platform_app") && metaData.size() > 0) {
                appPackage.setAppCode(metaData.get(0).get("code").toString());
            }
            AppMeta appMeta = new AppMeta(metaName, metaData);
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
    public ApiResult packetProcess(@PathVariable("versionId") String appId) {
        return null;
    }

    /*
    下载版本包
     */
    @RequestMapping(value = {"/downloadPackage/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult downloadPackage(@PathVariable("versionId") String appId) {
        return null;
    }

    /*
    上传版本包
     */
    @RequestMapping(value = {"/uploadPackage"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public void uploadPackage(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String targetPath = baseUploadPath + file.getOriginalFilename();
        Files.write(Path.of(targetPath), bytes);
    }

    /*
    部署版本包
     */
    @RequestMapping(value = {"/deploy/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult deployPackage(@PathVariable("versionId") String versionId) {
        String appPackageData = readPackageData(versionId);
        AppPackage appPackage = resolveAppPackageData(appPackageData);
        deployAppPackageData(appPackage);
        return null;
    }

    private String readPackageData(String versionId) {
        String filePath = basePath + "/geelatoApp.gdp";
        String data = "";
        try {
            data = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;

    }

    private String readPackageData() {
        String filePath = basePath + "/geelatoApp.zgdp";
        String packageData = "";
        try (ZipFile zipFile = new ZipFile(filePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                String suffix = entryName.substring(entryName.lastIndexOf("."));
                if (".gdp".equals(suffix)) {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader br = new BufferedReader(reader);
                        String con = "";
                        while (br.readLine() != null) {
                            con += con;
                        }
                        packageData = con;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packageData;
    }

    private AppPackage resolveAppPackageData(String appPackageData) {
        AppPackage appPackage = JSONObject.parseObject(appPackageData, AppPackage.class);
        return appPackage;
    }

    private void deployAppPackageData(AppPackage appPackage) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(dao2.getJdbcTemplate().getDataSource());
        TransactionStatus transactionStatus = TransactionHelper.beginTransaction(dataSourceTransactionManager);
        for (AppMeta appMeta : appPackage.getAppMetaList()) {
            Map<String, Object> metaData = new HashMap<>();
            ArrayList<Map<String, Object>> metaDataArrary = new ArrayList<>();
            String appMetaName = appMeta.getMetaName();
            Object appMetaData = appMeta.getMetaData();
            EntityMeta entityMeta = metaManager.getByEntityName(appMetaName);
            JSONArray jsonArray = JSONArray.parseArray(JSONObject.toJSONString(appMetaData));
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jo = jsonArray.getJSONObject(i);
                Map<String, Object> columnMap = new HashMap<>();
                for (String key : jo.keySet()) {
                    FieldMeta fieldMeta = entityMeta.getFieldMetaByColumn(key);
                    if (!"id".equals(key)) {
                        columnMap.put(fieldMeta.getFieldName(), jo.get(key));
                    }
                }
                metaDataArrary.add(columnMap);
            }
            metaData.put(appMeta.getMetaName(), metaDataArrary);
            List<SaveCommand> saveCommandList = jsonTextSaveParser.parseBatch(JSONObject.toJSONString(metaData), new Ctx());
            for (SaveCommand saveCommand : saveCommandList) {
                BoundSql boundSql = sqlManager.generateSaveSql(saveCommand);
                String pkValue = dao.save(boundSql);
            }
        }
        TransactionHelper.commitTransaction(dataSourceTransactionManager, transactionStatus);
    }


    private Object parseValueExp(SaveCommand currentCommand, String valueExp, int times) {
        String valueExpTrim = valueExp.trim();
        // 检查是否存在变量$parent
        if (valueExpTrim.startsWith("$parent")) {
            return parseValueExp((SaveCommand) currentCommand.getParentCommand(), valueExpTrim.substring("$parent".length() + 1), times + 1);
        } else {
            if (times == 0) {
                //如果是第一次且无VARS_PARENT关键字，则直接返回值
                return valueExp;
            } else {
                if (currentCommand.getParentCommand() != null) {
                    SaveCommand parentSaveCommand = (SaveCommand) currentCommand.getParentCommand();
                    return parentSaveCommand.getValueMap().get(valueExpTrim);
                } else {
                    return currentCommand.getValueMap().get(valueExpTrim);
                }
            }
        }
    }

    /*
    获取部署进度
     */
    @RequestMapping(value = {"/deploy/progress/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult deployProcess(@PathVariable("versionId") String appId) {
        return null;
    }

    /*
    获取指定应用的版本信息
    */
    @RequestMapping(value = {"/queryVersion/{appId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult queryVersion(@PathVariable("appId") String appId) {
        return null;
    }

    /*
    删除版本
    */
    @RequestMapping(value = {"/delelteVersion/{versionId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult deleteVersion(@PathVariable("versionId") String appId) {
        return null;
    }


    private Map<String, String> appMetaMap(String appId) {
        Map<String, String> map = new HashMap<>();
        map.put("platform_app", String.format("select * from platform_app where id='%s'", appId)); // 应用表
        map.put("platform_app_page", String.format("select * from platform_app_page where app_id='%s'", appId)); // 应用页面配置
        map.put("platform_tree_node", String.format("select * from platform_tree_node where tree_id='%s'", appId)); // 应用菜单
        map.put("platform_dev_db_connect", "select * from platform_dev_db_connect"); //
        map.put("platform_dev_table", "select * from platform_dev_table"); //
        map.put("platform_dev_table_foreign", "select * from platform_dev_table_foreign"); //
        map.put("platform_dev_view", "select * from platform_dev_view"); //
        map.put("platform_dev_column", "select * from platform_dev_column"); //
        map.put("platform_dict", String.format("select * from platform_dict where app_id='%s'", appId)); // 字典
        map.put("platform_dict_item", String.format("select * from platform_dict_item where app_id='%s'", appId)); // 字典编码
        map.put("platform_permission", String.format("select * from platform_permission where app_id='%s'", appId));// 应用权限
        map.put("platform_role", String.format("select * from platform_permission where app_id='%s'", appId)); // 应用角色
        map.put("platform_encoding", String.format("select * from platform_permission where app_id='%s'", appId)); // 应用级流水编码
        map.put("platform_sys_config", String.format("select * from platform_permission where app_id='%s'", appId)); // 系统配置
        map.put("platform_resources", String.format("select * from platform_permission where app_id='%s'", appId)); // 资源信息
        map.put("platform_role_r_app", String.format("select * from platform_role_r_app where app_id='%s'", appId)); // 角色APP关系表
        map.put("platform_role_r_permission", String.format("select * from platform_role_r_permission where app_id='%s'", appId)); // 角色权限关系表
        map.put("platform_role_r_tree_node", String.format("select * from platform_role_r_tree_node where app_id='%s'", appId)); // 角色菜单关系表
        return map;
    }

    private Map<String, String> appResourceMetaMap(String appId) {
        Map<String, String> map = new HashMap<>();
//        map.put("platform_resources",String.format("select * from platform_permission where app_id='%s'",appId));   //表需要加app_id
        return map;
    }

    private void writePackageData(AppPackage appPackage) {
        String jsonStr = JSONObject.toJSONString(appPackage);
        String packageSuffix = ".gdp";
        String dataFileName = appPackage.getAppCode() == "" ? appPackage.getAppCode() : "geelatoApp";
        File file = new File(basePath + dataFileName + packageSuffix);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(jsonStr);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writePackageResourceData(AppPackage appPackage) {
        String jsonStr = JSONObject.toJSONString(appPackage);
        String packageSuffix = ".gdp";
        String dataFileName = appPackage.getAppCode() == "" ? appPackage.getAppCode() : "geelatoApp";
        File file = new File(basePath + dataFileName + packageSuffix);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(jsonStr);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void compressAppPackage(AppPackage appPackage) {
        String packageSuffix = ".zgdp";
        String appPackageName = appPackage.getAppCode() == "" ? appPackage.getAppCode() : "geelatoApp";
        String targetZipPath = basePath + appPackageName + packageSuffix;
        String sourceAppPackageFolderPath = "";
        compressDirectory(sourceAppPackageFolderPath, targetZipPath);
    }

    private void deCompressAppPackage() {
        String packageFilePath = "";
        String targetDirectoryPath = "";
        File packageFIle = new File(packageFilePath);
        File targetDirectory = new File(targetDirectoryPath);
        DecompressPackage(packageFIle, targetDirectory);
    }

    private void compressDirectory(String sourceFolder, String zipFilePath) {
        try {
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zos = new ZipOutputStream(fos);
            File sourceFile = new File(sourceFolder);
            addDirectoryToZip(zos, sourceFile, "");
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void DecompressPackage(File zipFile, File destDir) {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                File file = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addDirectoryToZip(ZipOutputStream zos, File sourceFile, String parentDirectoryName) throws IOException {
        File[] files = sourceFile.listFiles();
        byte[] buffer = new byte[1024];
        int bytesRead;
        for (File file : files) {
            if (file.isDirectory()) {
                String directoryName = file.getName();
                addDirectoryToZip(zos, file, parentDirectoryName + "/" + directoryName);
                continue;
            }
            FileInputStream fis = new FileInputStream(file);
            String entryName = parentDirectoryName + "/" + file.getName();
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);
            while ((bytesRead = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
            fis.close();
            zos.closeEntry();
        }
    }
    //todo
    //讨论归属于应用的静态文件
    //相关表需添加app_id字段
}
