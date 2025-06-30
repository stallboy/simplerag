package simplerag.importer;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

import java.util.ArrayList;
import java.util.List;

public class MarkdownParser {

    private final TextContentRenderer textRenderer = TextContentRenderer.builder().build();
    private final Parser parser = Parser.builder().build();

    public List<Segment> removeLinkAndSplitByHeadings(String markdownText, String title) {
        Node document = parser.parse(markdownText);
        removeLink(document);
        List<Segment> segments = new ArrayList<>();

        Segment curSegment = new Segment(title, 0);
        segments.add(curSegment);

        Node node = document.getFirstChild();
        while (node != null) {

            if (node instanceof Heading heading) {
                String headingText = textRenderer.render(heading).trim();
                curSegment = new Segment(headingText, heading.getLevel());
                segments.add(curSegment);

            } else {
                curSegment.addBodyPart(node);
            }
            node = node.getNext();
        }

        for (Segment segment : segments) {
            segment.endBodyPart();
        }

        return segments;
    }


    public void removeLink(Node node) {
        if (node instanceof Link || node instanceof Image) {
            node.unlink();
            return;
        }

        Node c = node.getFirstChild();
        while (c != null) {
            Node nextChild = c.getNext();
            removeLink(c);
            c = nextChild;
        }
    }


    public static void main(String[] args) {
        String markdown = """
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


        List<Segment> result = new MarkdownParser().removeLinkAndSplitByHeadings(markdown, "介绍");

        System.out.println("成功分割成 " + result.size() + " 个Segment：\n");

        for (int i = 0; i < result.size(); i++) {
            Segment segment = result.get(i);
            System.out.println("--- Segment " + (i + 1) + " ---");
            System.out.println("Level     : " + segment.getLevel());
            System.out.println("Header    : " + segment.getHeader());
            System.out.printf("Body(%4d):\n%s\n\n", segment.getBody().trim().length(), segment.getBody().trim());
        }
    }

}
