package simplerag.service;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplerag.data.Doc;
import simplerag.data.SplitChunk;

import java.util.List;

public class ChunkService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkService.class.getName());
    private final WeaviateClient client;

    public ChunkService() {
        Config config = new Config("http", "localhost:8080");
        client = new WeaviateClient(config);

        Result<Boolean> chunkClassExistsRes = client.schema().exists().withClassName("Chunk").run();
        if (!chunkClassExistsRes.getResult()) {

            WeaviateClass chunkClass = Chunk.toWeaviateClass();
            Result<Boolean> run = client.schema().classCreator().withClass(chunkClass).run();
            if (run.hasErrors()) {
                logger.error("create Chunk class failed: {}", run.getError());
            }
            if (!run.getResult()) {
                throw new RuntimeException("create Chunk class failed");
            }
            logger.info("create Chunk class ok");
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
                logger.error("import {} failed: {}", doc.id(), result.getError());
            }
            ObjectGetResponse[] res = result.getResult();
            if (res != null && res.length > 0) {
                logger.info("import {} ok, size: {}", doc.id(), res.length);
            }
        }
    }


    public void updateChunkClass() {
        WeaviateClass chunkClass = Chunk.toWeaviateClass();
        Result<Boolean> run = client.schema().classUpdater().withClass(chunkClass).run();
        if (run.hasErrors()) {
            logger.error("update Chunk class failed: {}", run.getError());
        }
        if (!run.getResult()) {
            throw new RuntimeException("update Chunk class failed");
        }
        logger.info("update Chunk class ok");
    }

    public record RetrievedChunk(
            String content,
            float score,
            String title) {
    }

    public List<RetrievedChunk> retrieve(String query) {
        return List.of();
    }


}
