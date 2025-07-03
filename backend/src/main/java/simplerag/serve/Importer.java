package simplerag.serve;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplerag.data.Doc;
import simplerag.data.SplitChunk;
import simplerag.data.Splitter;
import simplerag.data.TokenCounter;
import simplerag.service.ChunkService;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class Importer {

    private static final Logger logger = LoggerFactory.getLogger(Importer.class.getName());

    public record DocPath(String docId,
                          Path path,
                          Charset encoding,
                          String project,
                          String title,
                          String url) {
    }


    public static void resolveDir(String dirType, Path dir, Map<String, String> projectMap, Charset encoding,
                                  Map<String, DocPath> result) throws IOException {
        List<Path> dateList = new ArrayList<>(16);
        try (Stream<Path> dirStream = Files.list(dir)) {
            dirStream.forEach(dateList::add);
        }
        dateList.sort(Comparator.comparing(a -> a.getFileName().toString()));
        for (Path dateDir : dateList) {
            String date = dateDir.getFileName().toString();

            List<Path> projectList = new ArrayList<>(8);
            try (Stream<Path> dirStream = Files.list(dateDir)) {
                dirStream.forEach(projectList::add);
            }
            for (Path projectDir : projectList) {
                String project = projectDir.getFileName().toString();
                if (projectMap != null) {
                    String realProject = projectMap.get(project);
                    if (realProject != null) {
                        project = realProject;
                    } else {
                        continue;
                    }
                }

                String realProject = project;
                String prefix = dirType + "/" + realProject + "/";
                if (Files.isDirectory(projectDir)) {
                    try (Stream<Path> stream = Files.walk(projectDir)) {
                        stream.filter(Files::isRegularFile).forEach(f -> {
                            String fn = f.getFileName().toString();
                            if (fn.endsWith(".md") || fn.endsWith(".MD")) {
                                String title = fn.substring(0, fn.length() - 3);
                                String relativizeF = projectDir.relativize(f).toString().replace("\\", "/");
                                String docId = prefix + relativizeF.substring(0, relativizeF.length() - 3);
                                DocPath old = result.put(docId, new DocPath(docId, f, encoding, realProject, title, ""));
                                if (old != null) {
                                    logger.info("updateDoc {} at {}", docId, date);
                                }
                            }
                        });
                    }
                }
            }
        }
    }


    private static void testOneFile() throws IOException {
        Path mdFile = Path.of("doc/dify_svn_woa/dify_svn_woa/2025-05-22_17_14/诛仙/策划文档/7功能设计/【公测版本】坐骑系统.md");
        String md = Files.readString(mdFile);
        Splitter splitter = new Splitter(TokenCounter.getDeepSeekR10528(), new Splitter.SplitterConf(
                2000, 1200, 750, 1250));
        List<SplitChunk> chunks = splitter.splitMarkdown(md, "坐骑系统");


        System.out.println(chunks.size());
        Doc doc = new Doc("svn/诛仙/策划文档/7功能设计/【公测版本】坐骑系统.md",
                "【公测版本】坐骑系统",
                md,
                "诛仙",
                "http://helloworld.com/123",
                List.of("xxx"),
                LocalDateTime.now(),
                LocalDateTime.now());

        ChunkService chunkService = new ChunkService();
        chunkService.importChunk(chunks, doc);
    }


    public static void importFolder(ChunkService chunkService) throws IOException {
        Map<String, String> idToProject = Map.of(
                "gmpwrd", "完美横版",
                "gmpwrd1", "完美S",
                "WL", "武林",
                "ZX1", "诛仙",
                "ZX2", "诛仙2");

        Map<String, DocPath> result = new LinkedHashMap<>();
        resolveDir("conf", Path.of("doc/dify_confluence/dify_confluence"), idToProject,
                Charset.forName("GBK"), result);
        resolveDir("svn", Path.of("doc/dify_svn_woa/dify_svn_woa"), null,
                StandardCharsets.UTF_8, result);
        resolveDir("woa", Path.of("doc/dify_woa/dify_woa"), null,
                StandardCharsets.UTF_8, result);
        logger.info("start import {} docs", result.size());

        TokenCounter tokenCounter = TokenCounter.getDeepSeekR10528();
        Splitter.SplitterConf splitterConf = new Splitter.SplitterConf(
                2000, 1200, 750, 1250);

        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            for (DocPath dp : result.values()) {
                executor.execute(() -> {
                    String md;
                    try {
                        md = Files.readString(dp.path(), dp.encoding());
                    } catch (IOException e) {
                        logger.error("read file {} failed", dp.docId(), e);
                        return;
                    }
                    Splitter splitter = new Splitter(tokenCounter, splitterConf);
                    List<SplitChunk> chunks = splitter.splitMarkdown(md, dp.title);
                    Doc doc = new Doc(dp.docId, dp.title, md, dp.project, dp.url,
                            List.of("xxx"),
                            LocalDateTime.now(),
                            LocalDateTime.now());

                    chunkService.importChunk(chunks, doc);
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void importFolderUse4B() throws IOException {
        ChunkService chunkService = new ChunkService(new WeaviateClient(new Config("http", "localhost:8080")),
                "Chunk4B",
                Map.of("apiEndpoint", "http://10.5.9.169:11434",
                        "model", "Qwen3-Embedding-4B"));

        importFolder(chunkService);
        logger.info("end {}", LocalDateTime.now());
    }

    public static void main(String[] args) throws IOException {
        importFolderUse4B();
    }
}
