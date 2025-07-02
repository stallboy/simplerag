package simplerag.serve;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.config.Key;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import simplerag.service.ChunkService;
import simplerag.utils.StringOrListDeserializer;

import java.util.ArrayList;
import java.util.List;

import static simplerag.service.ChunkService.*;

/**
 *  <a href="https://docs.dify.ai/en/guides/knowledge-base/external-knowledge-api">接口说明</a>
 */
public class DifyRetriever implements Handler {

    public record RetrieveRequest(
            String knowledge_id,
            String query,
            RetrieveSetting retrieval_setting,
            RetrieveMetaDataCondition metadata_condition /* json可没有，默认null*/) {
    }

    public record RetrieveSetting(
            int top_k,
            float score_threshold) {
    }

    public record RetrieveMetaDataCondition(
            @JSONField(defaultValue = "and")
            LogicalOperator logical_operator, // json可没有，默认为and
            List<Condition> conditions) {
    }

    public record Condition(
            @JSONField(deserializeUsing = StringOrListDeserializer.class)
            List<String> name,
            ComparisonOperator comparison_operator,
            String value /*json可没有，empty, not empty, null, or not null */) {
    }

    public enum ComparisonOperator {
        // for string or array
        @JSONField(name = "contains")
        CONTAINS,
        @JSONField(name = "not contains")
        NOT_CONTAINS,
        @JSONField(name = "start with")
        START_WITH,
        @JSONField(name = "end with")
        END_WITH,
        @JSONField(name = "is")
        IS,
        @JSONField(name = "is not")
        IS_NOT,
        @JSONField(name = "empty")
        EMPTY,
        @JSONField(name = "not empty")
        NOT_EMPTY,
        @JSONField(name = "=")
        // for number
        EQ,
        @JSONField(name = "≠") // 从dify代码中复制出来的，我不知道怎么输入出来
        NEQ,
        @JSONField(name = ">")
        GT,
        @JSONField(name = "<")
        LT,
        @JSONField(name = "≥")
        GTE,
        @JSONField(name = "≤")
        LTE,
        // for time
        @JSONField(name = "before")
        BEFORE,
        @JSONField(name = "after")
        AFTER,
    }


    public enum LogicalOperator {
        @JSONField(name = "and")
        AND,
        @JSONField(name = "or")
        OR;
    }


    public record RetrieveResponse(
            List<RetrieveRecord> records) {
    }

    public record RetrieveRecord(
            String content,
            float score,
            String title,
            JSONObject metadata) {
    }

    public record RetrieveError(
            int error_code,
            String error_msg) {

        public static final int InvalidAuthorizationHeaderFormat = 1001;
        public static final int AuthorizationFailed = 1002;
        public static final int KnowledgeNotExist = 2001;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String body = ctx.body();
        RetrieveRequest req = JSON.parseObject(body, RetrieveRequest.class);

        ChunkService chunkService = ctx.appData(CHUNK_SERVICE_KEY);
        List<RetrievedChunk> result = chunkService.retrieve(req.query);
        List<RetrieveRecord> records = new ArrayList<>(result.size());
        for (RetrievedChunk chunk : result) {
            RetrieveRecord record = new RetrieveRecord(
                    chunk.content(),
                    chunk.score(),
                    chunk.title(),
                    null);
            records.add(record);
        }
        String jsonString = JSON.toJSONString(new RetrieveResponse(records));
        ctx.contentType(ContentType.JSON).result(jsonString);
    }

    public static final Key<ChunkService> CHUNK_SERVICE_KEY = new Key<>("ChunkService");

    private static void initConfig(JavalinConfig config) {
        config.appData(CHUNK_SERVICE_KEY, new ChunkService());
    }

    public static void main(String[] args) {
        var app = Javalin.create(DifyRetriever::initConfig)
                .get("/", ctx -> ctx.result("Hello World"))
                .post("/retrieval", new DifyRetriever());

        app.start();
    }


}
