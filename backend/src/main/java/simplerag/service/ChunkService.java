package simplerag.service;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import simplerag.data.Doc;
import simplerag.data.SplitChunk;
import simplerag.data.Splitter;
import simplerag.data.TokenCounter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ChunkService {

    private final WeaviateClient client;

    public ChunkService() {
        Config config = new Config("http", "localhost:8080");
        client = new WeaviateClient(config);

        Result<Boolean> chunkClassExistsRes = client.schema().exists().withClassName("Chunk").run();
        if (!chunkClassExistsRes.getResult()) {

            WeaviateClass chunkClass = Chunk.makeSchema();
            Result<Boolean> run = client.schema().classCreator().withClass(chunkClass).run();
            if (!run.getResult()) {
                throw new RuntimeException("create Chunk class failed");
            }
            System.out.println("create Chunk class ok");
        }
    }


    public void importChunk(List<SplitChunk> chunks, Doc doc) {
        try (ObjectsBatcher batcher = client.batch().objectsBatcher()) {

            for (SplitChunk chunk : chunks) {
                batcher.withObject(new Chunk(Chunk.genChunkUuid(), chunk.markdown(), doc).
                        toWeaviateObject());
            }
            Result<ObjectGetResponse[]> result = batcher.run();
            if (result.hasErrors()) {
                System.out.println(result.getError());
            }
            ObjectGetResponse[] res = result.getResult();
            if (res == null || res.length == 0) {
                System.out.println("import chunk failed");
            }else{
                System.out.println("import chunk ok, size: " + res.length);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(Chunk.genChunkUuid());

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


}
