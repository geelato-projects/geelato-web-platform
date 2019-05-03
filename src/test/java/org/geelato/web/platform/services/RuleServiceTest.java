package org.geelato.web.platform.services;

import org.geelato.core.gql.parser.JsonTextSaveParser;
import org.geelato.core.gql.parser.SaveCommand;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.DemoEntity;
import org.geelato.core.mvc.Ctx;
import org.geelato.web.platform.service.RuleService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author geemeta
 */
@RunWith(SpringRunner.class)
public class RuleServiceTest {

    private RuleService ruleService = new RuleService();

    @Test
    public void recursiveSave() {
        MetaManager.singleInstance().parseOne(DemoEntity.class);
        String json = getText("./gql/parser/saveJsonText3.json");
        SaveCommand saveCommand = new JsonTextSaveParser().parse(json, new Ctx());
        ruleService.recursiveSave(saveCommand);
        Assert.assertEquals("张三", saveCommand.getCommands().get(0).getValueMap().get("name"));
        Assert.assertEquals("code1234", saveCommand.getCommands().get(1).getCommands().get(0).getValueMap().get("code"));
        Assert.assertEquals("张三", saveCommand.getCommands().get(1).getCommands().get(0).getValueMap().get("name"));

    }

    public static String getText(String resUrl) {
        URL url = RuleServiceTest.class.getClassLoader().getResource(resUrl);
        List<String> list = null;
        try {
            list = Files.readAllLines(Paths.get(url.toURI()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        for (String line : list) {
            sb.append(line);
        }
        return sb.toString();
    }
}