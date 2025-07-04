package simplerag.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.experimental.Where;
import io.weaviate.client.v1.filters.WhereFilter;
import io.weaviate.client.v1.graphql.model.GraphQLGetBaseObject;
import io.weaviate.client.v1.graphql.model.GraphQLTypedResponse;
import io.weaviate.client.v1.graphql.query.Get;
import io.weaviate.client.v1.graphql.query.argument.HybridArgument;
import io.weaviate.client.v1.graphql.query.argument.NearTextArgument;
import io.weaviate.client.v1.graphql.query.argument.WhereArgument;
import io.weaviate.client.v1.graphql.query.fields.Field;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplerag.data.Doc;
import simplerag.data.SplitChunk;

import java.util.List;
import java.util.Map;

public class ChunkService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkService.class.getName());
    private final WeaviateClient client;
    private final String className;
    private final Object ollamaCfg;

    public ChunkService() {
        this(new WeaviateClient(new Config("http", "localhost:8080")),
                "Chunk",
                Map.of("apiEndpoint", "http://host.docker.internal:11434",
                        "model", "dengcao/Qwen3-Embedding-0.6B:F16"));
    }

    public ChunkService(WeaviateClient client, String className, Object ollamaCfg) {
        this.client = client;
        this.className = className;
        this.ollamaCfg = ollamaCfg;

        Result<Boolean> chunkClassExistsRes = client.schema().exists().withClassName(className).run();
        if (!chunkClassExistsRes.getResult()) {
            WeaviateClass chunkClass = Chunk.toWeaviateClass(className, ollamaCfg);
            Result<Boolean> run = client.schema().classCreator().withClass(chunkClass).run();
            if (run.hasErrors()) {
                logger.error("create {} class failed: {}", className, run.getError());
            }
            if (!run.getResult()) {
                throw new RuntimeException("create " + className + " class failed");
            }
            logger.info("create {} class ok", className);
        }
    }

    public void importChunk(List<SplitChunk> chunks, Doc doc) {
        try (ObjectsBatcher batcher = client.batch().objectsBatcher()) {

            for (SplitChunk chunk : chunks) {
                batcher.withObject(new Chunk(Chunk.genChunkUuid(), chunk.markdown(), doc).
                        toWeaviateObject(className));
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
        WeaviateClass chunkClass = Chunk.toWeaviateClass(className, ollamaCfg);
        Result<Boolean> run = client.schema().classUpdater().withClass(chunkClass).run();
        if (run.hasErrors()) {
            logger.error("update Chunk class failed: {}", run.getError());
        }
        if (!run.getResult()) {
            throw new RuntimeException("update Chunk class failed");
        }
        logger.info("update Chunk class ok");
    }

    private static final String Qwen3EmbeddingQuestionInstruct =
            "Instruct: Given a Chinese search query, retrieve relevant passages that answer the question. Query: ";


    public List<RetrieveChunk> retrieve(@NotNull String query, String projectName) {
        NearTextArgument nearText = NearTextArgument.builder()
                .concepts(new String[]{Qwen3EmbeddingQuestionInstruct + query})
                .build();

        HybridArgument.Searches searches = HybridArgument.Searches.builder()
                .nearText(nearText)
                .build();

        HybridArgument hybridArgument = HybridArgument.builder()
                .query(query)
                .searches(searches)
                .alpha(0.75f) // 默认就是0.75，含义是nearText占0.75
                .build();

        Get get = client.graphQL().get()
                .withClassName(className)
                .withFields(Field.builder().name("body").build(),
                        Field.builder().name("docId").build(),
                        Field.builder().name("docProject").build(),
                        Field.builder().name("docTitle").build(),
                        Field.builder().name("docUrl").build(),
                        Field.builder().name("_additional").fields(new Field[]{
                                Field.builder().name("id").build(),
                                Field.builder().name("distance").build()
                        }).build())
                .withHybrid(hybridArgument)
                .withAutocut(2);

        if (projectName != null) {
            WhereArgument whereArgument = WhereArgument.builder()
                    .filter(WhereFilter.builder()
                            .operator(Where.Operator.EQUAL.toString())
                            .path("docProject")
                            .valueText(projectName)
                            .build())
                    .build();
            get.withWhere(whereArgument);
        }

        Result<GraphQLTypedResponse<JsonObject>> run = get.run(JsonObject.class);
        if (run.hasErrors()) {
            logger.error("query {} failed: {}", query, run.getError());
        }

        List<RetrieveChunk> result = null;
        JsonObject wrapper = run.getResult().getData().getObjects();
        if (wrapper != null) {
            try {
                JsonArray chunkArray = wrapper.getAsJsonArray(className);
                if (chunkArray != null && !chunkArray.isEmpty()) {
                    Gson gson = new Gson();
                    TypeToken<List<RetrieveChunk>> token = new TypeToken<>() {
                    };
                    result = gson.fromJson(chunkArray, token);
                }
            } catch (JsonSyntaxException e) {
                logger.error("failed to convert JSON to chunks", e);
            }
        }

        if (result != null) {
            return result;
        } else {
            logger.error("query {} result is null", query);
            return List.of();
        }
    }

    public static class RetrieveChunk extends GraphQLGetBaseObject {
        public String body;
        public String docId;
        public String docProject;
        public String docTitle;
        public String docUrl;
    }

}
