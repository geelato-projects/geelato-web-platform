package org.geelato.web.platform.m.base.service;

import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.model.view.TableView;
import org.geelato.utils.StringUtils;
import org.geelato.web.platform.m.base.entity.App;
import org.geelato.web.platform.m.base.entity.AppViewMap;
import org.geelato.web.platform.m.model.service.DevViewService;
import org.geelato.web.platform.m.security.entity.Permission;
import org.geelato.web.platform.m.security.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author diabl
 */
@Component
public class AppViewMapService extends BaseService {
    @Lazy
    @Autowired
    private AppService appService;
    @Lazy
    @Autowired
    private DevViewService devViewService;
    @Lazy
    @Autowired
    private PermissionService permissionService;

    public void after(AppViewMap form) {
        if (StringUtils.isNotBlank(form.getAppId())) {
            App app = appService.getModel(App.class, form.getAppId());
            form.setAppName(app.getName());
        }
        if (StringUtils.isNotBlank(form.getViewId())) {
            TableView tableView = devViewService.getModel(TableView.class, form.getViewId());
            form.setTableName(tableView.getEntityName());
            form.setViewName(tableView.getViewName());
            form.setViewTitle(tableView.getTitle());
            if (StringUtils.isBlank(form.getViewAppId())) {
                form.setViewAppId(tableView.getAppId());
            }
        }
        if (StringUtils.isNotBlank(form.getPermissionId())) {
            FilterGroup filter = new FilterGroup();
            filter.addFilter("id", FilterGroup.Operator.in, form.getPermissionId());
            List<Permission> list = permissionService.queryModel(Permission.class, filter);
            List<String> names = new ArrayList<>();
            for (Permission permission : list) {
                if (StringUtils.isNotBlank(permission.getDescription())) {
                    names.add(String.format("%s（%s）", permission.getName(), permission.getDescription()));
                } else {
                    names.add(String.format("%s（%s）", permission.getName(), permission.getCode()));
                }
            }
            form.setPermissionName(String.join("\n\t", names));
        }
    }
}
