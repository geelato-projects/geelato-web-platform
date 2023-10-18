package org.geelato.web.platform.m.excel.service;

import org.apache.logging.log4j.util.Strings;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlToken;
import org.geelato.utils.UIDGenerator;
import org.geelato.web.platform.m.excel.entity.PlaceholderMeta;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/9/20 10:24
 */
@Component
public class WordXWPFWriter {
    private static final Pattern paragraphPattern = Pattern.compile("\\$\\{[\\\u4e00-\\\u9fa5,\\w,\\.]+\\}");
    private static final Pattern tablePattern = Pattern.compile("\\$\\{rowMeta\\.[\\w,\\.,\\=]+\\}");
    private final Logger logger = LoggerFactory.getLogger(WordXWPFWriter.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static void insertPicture(XWPFDocument document, String filePath, CTInline inline, double imageWidth, double imageHeight, int format) throws FileNotFoundException, InvalidFormatException {
        document.addPictureData(new FileInputStream(filePath), XWPFDocument.PICTURE_TYPE_PNG);
        long id = UIDGenerator.generate();
        long width = (long) Math.floor(Units.toEMU(imageWidth) * 1000 / 35);
        long height = (long) Math.floor(Units.toEMU(imageHeight) * 1000 / 35);
        String blipId = document.addPictureData(new FileInputStream(filePath), format);
        String picXml = getPicXml(blipId, width, height);
        XmlToken xmlToken = null;
        try {
            xmlToken = XmlToken.Factory.parse(picXml);
        } catch (XmlException xe) {
            throw new RuntimeException(xe.getMessage());
        }
        inline.set(xmlToken);
        inline.setDistT(0);
        inline.setDistB(0);
        inline.setDistL(0);
        inline.setDistR(0);
        CTPositiveSize2D extent = inline.addNewExtent();
        extent.setCx(width);
        extent.setCy(height);
        CTNonVisualDrawingProps docPr = inline.addNewDocPr();
        docPr.setId(id);
        docPr.setName("IMG_" + id);
        docPr.setDescr("IMG_" + id);
    }

    private static String getPicXml(String blipId, long width, long height) {
        String picXml =
                "<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">" +
                        "   <a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                        "      <pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                        "         <pic:nvPicPr>" + "            <pic:cNvPr id=\"" + 0 +
                        "\" name=\"Generated\"/>" + "            <pic:cNvPicPr/>" +
                        "         </pic:nvPicPr>" + "         <pic:blipFill>" +
                        "            <a:blip r:embed=\"" + blipId +
                        "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"/>" +
                        "            <a:stretch>" + "               <a:fillRect/>" +
                        "            </a:stretch>" + "         </pic:blipFill>" +
                        "         <pic:spPr>" + "            <a:xfrm>" +
                        "               <a:off x=\"0\" y=\"0\"/>" +
                        "               <a:ext cx=\"" + width + "\" cy=\"" + height +
                        "\"/>" + "            </a:xfrm>" +
                        "            <a:prstGeom prst=\"rect\">" +
                        "               <a:avLst/>" + "            </a:prstGeom>" +
                        "         </pic:spPr>" + "      </pic:pic>" +
                        "   </a:graphicData>" + "</a:graphic>";
        return picXml;
    }

    /**
     * @param document
     * @param placeholderMetaMap
     * @param valueMapList
     * @param valueMap
     */
    public void writeDocument(XWPFDocument document, Map<String, PlaceholderMeta> placeholderMetaMap, List<Map> valueMapList, Map valueMap) {
        List<IBodyElement> bodyElements = document.getBodyElements();// 所有对象（段落+表格）
        int templateBodySize = bodyElements.size();// 标记模板文件（段落+表格）总个数
        int currentTable = 0;// 当前操作表格对象的索引
        int currentParagraph = 0;// 当前操作段落对象的索引
        for (int i = 0; i < templateBodySize; i++) {
            IBodyElement body = bodyElements.get(i);
            if (BodyElementType.PARAGRAPH.equals(body.getElementType())) {// 段落、图片
                XWPFParagraph ph = body.getBody().getParagraphArray(currentParagraph);
                if (ph != null) {
                    List<XWPFRun> runs = ph.getRuns();
                    if (runs != null) {
                        for (int r = 0; r < runs.size(); r++) {
                            String runText = runs.get(r).getText(0);
                            if (Strings.isNotBlank(runText)) {
                                Matcher phm = paragraphPattern.matcher(runText);
                                while (phm.find()) {
                                    System.out.println(phm.group());
                                    PlaceholderMeta meta = placeholderMetaMap.get(phm.group());
                                    boolean isReplace = false;
                                    if (meta != null) {
                                        String value = (String) valueMap.get(meta.getVar());
                                        if (meta.isImage()) {
                                            if (new File(value).exists()) {
                                                CTInline inline = runs.get(r).getCTR().addNewDrawing().addNewInline();
                                                try {
                                                    insertPicture(document, value, inline, meta.getImageWidth(), meta.getImageHeight(), XWPFDocument.PICTURE_TYPE_PNG);
                                                    document.createParagraph();
                                                } catch (Exception e) {
                                                    throw new RuntimeException("Image construction failure!", e);
                                                }
                                            }
                                        } else {
                                            runText = runText.replace(phm.group(), (String) valueMap.get(meta.getVar()));
                                        }
                                    }
                                    if (!isReplace) {
                                        runText = runText.replace(phm.group(), "");
                                    }
                                }
                                runs.get(r).setText(runText, 0);
                            }
                        }
                    }
                    currentParagraph++;
                }
            }
        }
    }

}
