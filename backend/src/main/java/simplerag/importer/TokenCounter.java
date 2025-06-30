package simplerag.importer;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;

import java.io.IOException;
import java.util.List;
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

    public static void main(String[] args) throws IOException {
        try (TokenCounter tk = getDeepSeekR10528()) {
            List<String> inputs = List.of(
                    "你好，世界！",
                    "Hello world, this is a test for DeepSeek tokenizer.",
                    "A key feature of DeepSeek-V2 is its adoption of Mixture-of-Experts (MoE) architecture."
            );

            for (String input : inputs) {
                System.out.printf("(%5d) : %s:\n", tk.countTokens(input), input);
            }
        }
    }

}
