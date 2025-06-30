package simplerag;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.schema.model.WeaviateClass;

public class ChunkService {

    private final WeaviateClient client;

    public ChunkService() {
        Config config = new Config("http", "localhost:8080");  // Replace with your Weaviate endpoint
        client = new WeaviateClient(config);

        Result<Boolean> chunkClassExistsRes = client.schema().exists().withClassName("Chunk").run();
        if (!chunkClassExistsRes.getResult()){

            WeaviateClass chunkClass = Chunk.makeSchema();
            Result<Boolean> run = client.schema().classCreator().withClass(chunkClass).run();
            if (!run.getResult()){
                throw new RuntimeException("create chunk class failed");
            }
        }
    }






}
