package org.geelato.web.platform.m.base.service;

import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.utils.StringUtils;
import org.geelato.web.platform.m.base.entity.App;
import org.geelato.web.platform.m.base.entity.AppTableMap;
import org.geelato.web.platform.m.model.service.DevTableService;
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
public class AppTableMapService extends BaseService {
    @Lazy
    @Autowired
    private AppService appService;
    @Lazy
    @Autowired
    private DevTableService devTableService;
    @Lazy
    @Autowired
    private PermissionService permissionService;

    public void after(AppTableMap form) {
        if (StringUtils.isNotBlank(form.getAppId())) {
            App app = appService.getModel(App.class, form.getAppId());
            form.setAppName(app.getName());
        }
        if (StringUtils.isNotBlank(form.getTableId())) {
            TableMeta tableMeta = devTableService.getModel(TableMeta.class, form.getTableId());
            form.setTableName(tableMeta.getEntityName());
            form.setTableTitle(tableMeta.getTitle());
            if (StringUtils.isBlank(form.getTableAppId())) {
                form.setTableAppId(tableMeta.getAppId());
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
