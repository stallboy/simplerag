package simplerag.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmentSplitterTest {

    static String markdown = """
                # 介绍
                这是一个关于Java的介绍。
                commonmark-java是一个很有用的库。
                aa![A mushroom-head robot drinking bubble tea](https://raw.githubusercontent.com/Codecademy/docs/main/media/codey.jpg)bb
                
                ## 核心功能
                - **解析**: 将Markdown文本转换为AST。
                - **渲染**: 将AST转换为HTML或其他格式。
                
                代码示例：
                ```java
                Parser parser = Parser.builder().build();
                Node document = parser.parse(markdownText);
                ```
                
                # 安装指南
                ## Maven
                请在pom.xml中添加依赖。
                
                ## Gradle
                请在build.gradle中添加依赖。""";

    @Test
    void removeLinkAndSplitByHeadings() {
        List<Segment> result = new SegmentSplitter().removeLinkAndSplitByHeadings(markdown, "title");
        assertEquals(6, result.size());
        assertEquals("title", result.getFirst().getHeader());

        assertEquals("介绍", result.get(1).getHeader());
        assertEquals("核心功能", result.get(2).getHeader());
        assertEquals("安装指南", result.get(3).getHeader());
        assertEquals("Maven", result.get(4).getHeader());
        assertEquals(2, result.get(4).getLevel());
        assertEquals("Gradle", result.get(5).getHeader());

        assertEquals("请在build.gradle中添加依赖。", result.get(5).getBody().trim());
    }

    @Test
    void removeImageForMarkItDownBug() {

        {
            String v = """
                    中![C:\\Users\\Administrator\\AppData\\Local\\Microsoft\\Windows\\INetCache\\Content.Word\\20220505165603.png](data:image/png;base64...)国""";
            String r = SegmentSplitter.removeImageForMarkItDownBug(v);
            assertEquals("中国", r);
        }

        {
            String v = """
                    中![C:][]](data:image/png;base64...)国""";
            String r = SegmentSplitter.removeImageForMarkItDownBug(v);
            assertEquals("中国", r);
        }

    }
}