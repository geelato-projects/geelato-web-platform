package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.api.ApiResult;
import org.geelato.web.platform.m.base.entity.TreeNode;
import org.geelato.web.platform.m.base.service.TreeNodeService;
import org.geelato.core.constants.ApiErrorMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/treeNode")
public class TreeNodeController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(DictController.class);
    @Autowired
    private TreeNodeService treeNodeService;

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<List<TreeNode>> query(HttpServletRequest req) {
        ApiResult<List<TreeNode>> result = new ApiResult<>();
        try {
            Map<String, Object> params = this.getQueryParameters(TreeNode.class, req);
            return result.setData(treeNodeService.queryModel(TreeNode.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult<TreeNode> isDelete(@PathVariable(required = true) long id) {
        ApiResult<TreeNode> result = new ApiResult<>();
        try {
            TreeNode mResult = treeNodeService.getModel(TreeNode.class, id);
            if (mResult != null) {
                treeNodeService.isDeleteModel(mResult);
                result.success();
            } else {
                result.error().setMsg(ApiErrorMsg.IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
        }

        return result;
    }
}
