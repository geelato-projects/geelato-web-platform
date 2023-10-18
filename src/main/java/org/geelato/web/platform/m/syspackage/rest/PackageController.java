package org.geelato.web.platform.m.syspackage.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.web.platform.m.base.entity.AppPage;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/package")
public class PackageController extends BaseController {
    /*
    打包应用
     */
    @RequestMapping(value = {"/packet/{appId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult packetApp(@PathVariable("appId") String appId){
        return null;
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
    public ApiResult uploadPackage(HttpServletRequest req){
        return null;
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
}
