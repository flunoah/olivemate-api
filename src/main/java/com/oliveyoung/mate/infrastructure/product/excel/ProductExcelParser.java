package com.oliveyoung.mate.infrastructure.product.excel;

import com.oliveyoung.mate.application.product.ProductUploadException;
import com.oliveyoung.mate.application.product.ProductUploadItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class ProductExcelParser {

    public List<ProductUploadItem> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProductUploadException("업로드된 파일이 비어 있습니다.");
        }

        List<ProductUploadItem> items = new ArrayList<>();
        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                try {
                    String goodsNo = cellToString(row.getCell(0));
                    String brand = cellToString(row.getCell(1));
                    String name = cellToString(row.getCell(2));
                    long regularPrice = (long) cellToNumeric(row.getCell(3));
                    long salePrice = (long) cellToNumeric(row.getCell(4));

                    if (goodsNo.isBlank() || name.isBlank()) {
                        log.warn("상품코드/상품명이 비어 있어 스킵: row={}", rowIdx + 1);
                        continue;
                    }
                    items.add(new ProductUploadItem(goodsNo, brand, name, regularPrice, salePrice));
                } catch (Exception e) {
                    log.warn("행 파싱 실패, 스킵: row={}", rowIdx + 1, e);
                }
            }
        } catch (IOException e) {
            throw new ProductUploadException("엑셀 파일을 열 수 없습니다. xlsx/xls 형식인지 확인해주세요.", e);
        }

        return items;
    }

    private String cellToString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.toString().trim();
    }

    private double cellToNumeric(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.STRING) {
            return Double.parseDouble(cell.getStringCellValue().replaceAll("[^0-9.]", ""));
        }
        return cell.getNumericCellValue();
    }
}
