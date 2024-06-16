package org.geelato.web.platform.graal.service;

import org.geelato.core.env.entity.User;
import org.geelato.core.graal.GraalService;
import org.geelato.web.platform.PlatformContext;
import org.geelato.web.platform.Tenant;

@GraalService(name = "ctx",built = "true")
public class ContextService {
    public Tenant getCurrentTenant(){
        return PlatformContext.getCurrentTenant();
    }

    public User getCurrentUser(){
        return PlatformContext.getCurrentUser();
    }
}
