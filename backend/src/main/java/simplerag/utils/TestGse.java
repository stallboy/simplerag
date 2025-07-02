package simplerag.utils;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.Tokenization;
import io.weaviate.client.v1.schema.model.WeaviateClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestGse {

    private final WeaviateClient client;
    private final String className;
    private final boolean useGseForContentTokenization;
    private final boolean useModuleCfg;

    public TestGse(WeaviateClient client, String className, boolean useGseForContentTokenization, boolean useModuleCfg) {
        this.client = client;
        this.className = className;
        this.useGseForContentTokenization = useGseForContentTokenization;
        this.useModuleCfg = useModuleCfg;
    }

    public void run() {
        createClass();
        importData();
    }

    private void createClass() {
        Result<Boolean> checkExists = client.schema().exists().withClassName(className).run();
        if (checkExists.hasErrors()) {
            System.err.println("Error checking if collection exists: " + checkExists.getError());
            return;
        }
        if (checkExists.getResult()) {
            Result<Boolean> deleteResult = client.schema().classDeleter().withClassName(className).run();
            if (deleteResult.hasErrors()) {
                System.err.println("Error deleting existing collection: " + deleteResult.getError());
                return;
            }
            System.out.println("Deleted existing collection: " + className);
        }

        Property contentProperty = Property.builder()
                .name("content")
                .description("content")
                .dataType(List.of(DataType.TEXT))
                .tokenization(useGseForContentTokenization ? "gse" : Tokenization.WORD)
                .indexFilterable(true)
                .indexSearchable(true)
                .build();

        Property titleProperty = Property.builder()
                .name("title")
                .description("title")
                .dataType(List.of(DataType.TEXT))
                .tokenization(Tokenization.WORD)
                .indexFilterable(true)
                .indexSearchable(true)
                .build();

        WeaviateClass.WeaviateClassBuilder builder = WeaviateClass.builder();
        builder.className(className)
                .description("test class")
                .properties(List.of(contentProperty, titleProperty));

        if (useModuleCfg) {
            Object tokenizeCfg = Map.of("text2vec-transformers", Map.of(
                    "tokenization", "gse",
                    "skip", true,
                    "gseConfig", Map.of(
                            "mode", "accurate",
                            "stopPreset", "cn")));

            builder.moduleConfig(tokenizeCfg);
        }


        Result<Boolean> createResult = client.schema().classCreator()
                .withClass(builder.build())
                .run();

        if (createResult.hasErrors()) {
            System.err.println("Error creating collection: " + createResult.getError());
        } else {
            System.out.println("created " + className);
        }
    }

    private void importData() {
        importOne("人工智能技术正在快速发展。", "科技进步");
        importOne("人，工智能技术正在快速发展。", "科技进步");
        importOne("西雅图地标建筑, Seattle Space Needle, 西雅图太空针. Sky tree.", "sky");
        importOne("《复仇者联盟3：无限战争》是全片使用IMAX摄影机拍摄制作的的科幻片.", "复仇者");
    }

    private void importOne(String content, String title) {
        Map<String, Object> dataObject = new HashMap<>();
        dataObject.put("content", content);
        dataObject.put("title", title);

        Result<WeaviateObject> result1 = client.data().creator()
                .withClassName(className)
                .withProperties(dataObject)
                .withID(UUID.randomUUID().toString())
                .run();
        if (result1.hasErrors()) {
            System.err.println("Error inserting object : " + result1.getError());
        } else {
            System.out.println("Object inserted successfully. " + title);
        }
    }


    public static void main(String[] args) throws IOException {
        Config config = new Config("http", "localhost:8080");
        WeaviateClient client = new WeaviateClient(config);

        new TestGse(client, "Test_Default", false, false).run();
        new TestGse(client, "Test_Gse", true, false).run();
        new TestGse(client, "Test_Gse_ModuleCfg", true, true).run();

        // 结果：Test_Default 搜不到人工，搜不到imax；搜不到科技，可搜到 科技进步
        // Test_Gse 可搜到 人，人工，imax，人工智能 ；搜不到科技，可搜到 科技进步
        // Test_Gse_ModuleCfg 跟Test_Gse没区别

        // docker_compose.yml 配置如下，关键是ENABLE_TOKENIZER_GSE: 'true'
        //services:
        //  weaviate:
        //    command:
        //    - --host
        //    - 0.0.0.0
        //    - --port
        //    - '8080'
        //    - --scheme
        //    - http
        //    image: cr.weaviate.io/semitechnologies/weaviate:1.31.1
        //    ports:
        //    - 8080:8080
        //    - 50051:50051
        //    volumes:
        //    - weaviate_data:/var/lib/weaviate
        //    restart: on-failure:0
        //    environment:
        //      QUERY_DEFAULTS_LIMIT: 25
        //      AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED: 'true'
        //      PERSISTENCE_DATA_PATH: '/var/lib/weaviate'
        //      ENABLE_API_BASED_MODULES: 'true'
        //      ENABLE_MODULES: 'text2vec-ollama'
        //      ENABLE_TOKENIZER_GSE: 'true'
        //      CLUSTER_HOSTNAME: 'node1'
        //volumes:
        //  weaviate_data:
    }

}
