package simplerag.data;

import org.junit.jupiter.api.Test;
import simplerag.utils.TokenCounter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SplitterTest {
    @Test
    void splitMarkdown() {
        Splitter splitter = new Splitter(TokenCounter.getDeepSeekR10528(), new Splitter.SplitterConf(
                80, 20, 20, 30));

        String result = """
                -----token:21 (50)-----
                # 介绍
                这是一个关于Java的介绍。
                commonmark-java是一个很有用的库。
                aabb
                
                -----token:53 (171)-----
                ## 核心功能
                - **解析**: 将Markdown文本转换为AST。
                - **渲染**: 将AST转换为HTML或其他格式。
                
                代码示例：
                
                ```java
                Parser parser = Parser.builder().build();
                Node document = parser.parse(markdownText);
                ```
                
                -----token:21 (64)-----
                # 安装指南
                
                ## Maven
                请在pom.xml中添加依赖。
                ## Gradle
                请在build.gradle中添加依赖。
                
                """;
        List<Splitter.SplitterChunk> chunks = splitter.splitMarkdown(SegmentSplitterTest.markdown, "commonmark");
        assertEquals(result, Splitter.SplitterChunk.dump(chunks));
    }


    @Test
    void splitMarkdown_cut() {
        Splitter splitter = new Splitter(TokenCounter.getDeepSeekR10528(), new Splitter.SplitterConf(
                40, 20, 20, 30));

        String result = """
                -----token:21 (50)-----
                # 介绍
                这是一个关于Java的介绍。
                commonmark-java是一个很有用的库。
                aabb
                
                -----token:31 (77)-----
                ## 核心功能
                - **解析**: 将Markdown文本转换为AST。## 核心功能
                - **渲染**: 将AST转换为HTML或其他格式。
                代码示例：
                -----token:27 (112)-----
                ## 核心功能
                ```java
                Parser parser = Parser.builder().build();## 核心功能
                Node document = parser.parse(markdownText);
                ```
                -----token:21 (64)-----
                # 安装指南
                
                ## Maven
                请在pom.xml中添加依赖。
                ## Gradle
                请在build.gradle中添加依赖。
                
                """;
        List<Splitter.SplitterChunk> chunks = splitter.splitMarkdown(SegmentSplitterTest.markdown, "commonmark");
        // System.out.println(SplitChunk.dump(chunks));
        assertEquals(result, Splitter.SplitterChunk.dump(chunks));
    }

    @Test
    void findBestSplit() {
        int[] tokens = new int[]{100, 100, 200, 200};
        int[] levels = new int[]{0, 1, 2, 2};
        {
            int[] bestSplit = Splitter.findBestSplit(tokens, levels, 100, 120);
            assertArrayEquals(new int[]{1, 2, 3}, bestSplit);
            // System.out.println(Arrays.stream(bestSplit).boxed().toList());
        }

        {
            int[] bestSplit = Splitter.findBestSplit(tokens, levels, 180, 220);
            assertArrayEquals(new int[]{2, 3}, bestSplit);
        }

        {
            int[] bestSplit = Splitter.findBestSplit(tokens, levels, 200, 400);
            assertArrayEquals(new int[]{2}, bestSplit);
        }

    }
}