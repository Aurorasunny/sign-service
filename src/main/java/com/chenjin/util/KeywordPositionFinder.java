package com.chenjin.util;

import com.chenjin.pojo.dto.PdfKeywordPos;
import lombok.Getter;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * pdf关键词坐标获取工具
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-19 17:42
 **/
@Getter
public class KeywordPositionFinder extends PDFTextStripper {

    private final String keyword;

    private final LinkedList<PdfKeywordPos> keywordPosList = new LinkedList<>();

    /**
     * 构造器
     *
     * @param keyword 关键词
     */
    public KeywordPositionFinder(String keyword) throws IOException {
        this.keyword = keyword;
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        // 拼接文本来查找关键词
        StringBuilder builder = new StringBuilder();
        for (TextPosition textPosition : textPositions) {
            builder.append(textPosition.getUnicode());
        }
        String fullText = builder.toString();

        // 检查是否包含关键词
        if (fullText.contains(keyword)) {
            // 获取关键词的位置和所在页面
            for (TextPosition textPosition : textPositions) {
                String character = textPosition.getUnicode();
                if (keyword.startsWith(character)) {
                    // 打印关键词所在页面及坐标 (左下角的 x, y 坐标)
                    PdfKeywordPos keywordPos = PdfKeywordPos.builder()
                            .keyword(keyword)
                            .pageNo(getCurrentPageNo())
                            .pageX(textPosition.getXDirAdj())
                            .pageY(textPosition.getYDirAdj())
                            .build();
                    keywordPosList.offer(keywordPos);
                    break;
                }
            }
        }
        super.writeString(text, textPositions);
    }
}
