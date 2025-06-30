package simplerag;

import io.weaviate.client.v1.misc.model.BM25Config;
import io.weaviate.client.v1.misc.model.InvertedIndexConfig;
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import simplerag.model.Doc;

import java.util.List;
import java.util.Map;


public record Chunk(
        String id, // generated
        String body,
        Doc doc) {

    public static WeaviateClass makeSchema() {
        Property bodyProperty = Property.builder()
                .name("body")
                .description("chunk body")
                .dataType(List.of(DataType.TEXT))
                .indexFilterable(false) // 长文本不要filter
                .indexSearchable(true)
                .build();

        Property docIdProperty = Property.builder()
                .name("docId")
                .description("path of doc")
                .dataType(List.of(DataType.TEXT))
                .indexFilterable(true)
                .indexSearchable(true)
                .build();

        Property docTitleProperty = Property.builder()
                .name("docTitle")
                .description("title of doc")
                .dataType(List.of(DataType.TEXT))
                .indexFilterable(true)
                .indexSearchable(true)
                .build();

        Property docProjectProperty = Property.builder()
                .name("docProject")
                .description("project of doc")
                .dataType(List.of(DataType.TEXT))
                .indexFilterable(true)
                .indexSearchable(false)
                .build();

        Property docUrlProperty = Property.builder()
                .name("docUrl")
                .description("url of doc")
                .dataType(List.of(DataType.TEXT))
                .indexFilterable(false)
                .indexSearchable(false)
                .moduleConfig(Map.of("text2vec-ollama", Map.of("skip", true)))
                .build();

        Property docWritersProperty = Property.builder()
                .name("docWriters")
                .description("writers of doc")
                .dataType(List.of(DataType.TEXT_ARRAY))
                .indexFilterable(true)
                .indexSearchable(false)
                .moduleConfig(Map.of("text2vec-ollama", Map.of("skip", true)))
                .build();

        Property docCreateTimeProperty = Property.builder()
                .name("docCreateTime")
                .description("create time of doc")
                .dataType(List.of(DataType.DATE))
                .indexFilterable(false)
                .indexSearchable(false)
                .indexRangeFilters(true)
                .build();


        Property docLastModifiedTimeProperty = Property.builder()
                .name("docLastModifiedTime")
                .description("last modified time of doc")
                .dataType(List.of(DataType.DATE))
                .indexFilterable(false)
                .indexSearchable(false)
                .indexRangeFilters(true)
                .build();

        Object tokenizeCfg = Map.of("text2vec-transformers", Map.of(
                "tokenization", "gse",
                "skip", true,  // 不生成向量，只用于分词
                "gseConfig", Map.of(
                        "mode", "accurate",
                        "stopPreset", "cn")));
                            // "userDictPath", "/dict/game_terms.txt"

        Object ollamaCfg = Map.of("apiEndpoint", "http://localhost:11434",
                "model", "dengcao/Qwen3-Embedding-0.6B:F16");

        Object moduleCfg = Map.of("text2vec-transformers", tokenizeCfg,
                "text2vec-ollama", ollamaCfg);

        // Configure BM25 settings
        BM25Config bm25Config = BM25Config.builder()
                .b(0.7f)
                .k1(1.25f)
                .build();

        // Configure inverted index with BM25 and other settings
        InvertedIndexConfig invertedIndexConfig = InvertedIndexConfig.builder()
                .bm25(bm25Config)
                .indexNullState(false)
                .indexPropertyLength(false)
                .indexTimestamps(false)
                .build();

        return WeaviateClass.builder()
                .className("Chunk")
                .description("document chunk")
                .properties(List.of(bodyProperty,
                        docIdProperty,
                        docTitleProperty,
                        docProjectProperty,
                        docUrlProperty,
                        docWritersProperty,
                        docCreateTimeProperty,
                        docLastModifiedTimeProperty))
                .invertedIndexConfig(invertedIndexConfig)
                .vectorizer("text2vec-ollama") // 但实际不生成类向量
                .moduleConfig(moduleCfg)
                .build();
    }


}
