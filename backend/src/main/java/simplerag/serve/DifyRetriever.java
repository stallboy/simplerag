package simplerag.serve;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import net.minidev.json.JSONValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
            LogicalOperator logical_operator, // json可没有，默认为and
            List<Condition> conditions) {
    }

    public record Condition(
            List<String> name,
            ComparisonOperator comparison_operator, // 比如eq, gt, lt, in, not_in
            String value /*json可没有，empty, not empty, null, or not null */) {
    }

    public enum ComparisonOperator {
        CONTAINS,
        NOT_CONTAINS,
        START_WITH,
        END_WITH,
        IS,
        IS_NOT,
        EMPTY,
        NOT_EMPTY,
        EQ,
        NEQ,
        GT,
        LT,
        GTE,
        LTE,
        BEFORE,
        AFTER,
    }


    public enum LogicalOperator {
        AND,
        OR;
    }


    public record RetrieveResponse(
            List<RetrieveRecord> records) {
    }

    public record RetrieveRecord(
            String content,
            float score,
            String title,
            JSONValue metadata) {
    }


    @Override
    public void handle(@NotNull Context ctx) throws Exception {


    }


    public static void main(String[] args) {
        var app = Javalin.create(/*config*/)
                .get("/", ctx -> ctx.result("Hello World"))
                .post("/retrieval", new DifyRetriever())
                .start(7070);
    }


}
