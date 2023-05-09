package org.geelato.web.platform.m.security.service;

import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.security.entity.Org;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

public class OrgService {


    @Autowired
    @Qualifier("primaryDao")
    private Dao dao;


}
