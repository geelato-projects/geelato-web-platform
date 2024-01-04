package org.geelato.web.platform.m.excel.service;

import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author diabl
 * @description: TODO
 * @date 2024/1/4 20:05
 */
public class ExcelXSSFUtils {

    public static XSSFWorkbook copySheet(XSSFSheet sourceSheet) {
        XSSFWorkbook targetWorkbook = new XSSFWorkbook();
        XSSFSheet targetSheet = targetWorkbook.createSheet(sourceSheet.getSheetName());
        copySheet(sourceSheet, targetSheet);

        return targetWorkbook;
    }

    public static void copySheet(XSSFSheet sourceSheet, XSSFSheet targetSheet) {
        for (int i = 0; i < sourceSheet.getPhysicalNumberOfRows(); i++) {
            XSSFRow sourceRow = sourceSheet.getRow(i);
            XSSFRow targetRow = targetSheet.createRow(i);
            if (sourceRow != null) {
                copyRow(sourceRow, targetRow);
            }
        }
    }

    public static void copyRow(XSSFRow sourceRow, XSSFRow targetRow) {
        for (int i = 0; i < sourceRow.getPhysicalNumberOfCells(); i++) {
            XSSFCell sourceCell = sourceRow.getCell(i);
            XSSFCell targetCell = targetRow.createCell(i);
            if (sourceCell != null) {
                targetCell.copyCellFrom(sourceCell, new CellCopyPolicy());
            }
        }
    }

    public static void reserveSheet(XSSFWorkbook workbook, int reserveIndex) {
        workbook.setActiveSheet(reserveIndex);
        int maxSheets = workbook.getNumberOfSheets();
        for (int i = maxSheets - 1; i > reserveIndex; i--) {
            workbook.removeSheetAt(i);
        }
        for (int i = 0; i < reserveIndex; i++) {
            workbook.removeSheetAt(0);
        }
    }
}
