package org.geelato.web.platform;

import org.geelato.core.env.entity.User;

public class PlatformContext {

    private static final ThreadLocal<User> threadLocalUser = new ThreadLocal<>();

    private static final ThreadLocal<Tenant> threadLocalTenant = new ThreadLocal<>();

    public static User getCurrentUser() {
        return threadLocalUser.get();
    }

    public static void setCurrentUser(User user) {
        threadLocalUser.set(user);
    }

    public static Tenant getCurrentTenant() {
        return threadLocalTenant.get();
    }

    public static void setCurrentTenant(Tenant tenant) {
        threadLocalTenant.set(tenant);
    }

}
