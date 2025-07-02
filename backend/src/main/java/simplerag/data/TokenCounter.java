package simplerag.data;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;

import java.util.Map;

public class TokenCounter implements AutoCloseable {

    public static TokenCounter getDeepSeekR10528() {
        return new TokenCounter("deepseek-ai/DeepSeek-R1-0528",
                Map.of("modelMaxLength", "128000",
                        "maxLength", "100000"));
    }

    private final HuggingFaceTokenizer tokenizer;

    public TokenCounter(String modelName, Map<String, String> options) {
        tokenizer = HuggingFaceTokenizer.newInstance(modelName, options);
    }

    public int countTokens(String text) {
        return tokenizer.encode(text).getTokens().length;
    }

    @Override
    public void close() {
        tokenizer.close();
    }

}
