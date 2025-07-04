package simplerag.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenCounterTest {

    @Test
    void testCountTokens() {
        try (TokenCounter tk = TokenCounter.getDeepSeekR10528()) {
            {
                int tokenCount = tk.countTokens( "你好，世界！");
                assertEquals(4, tokenCount);
            }
            {
                int tokenCount = tk.countTokens(  "Hello world, this is a test for DeepSeek tokenizer.");
                assertEquals(14, tokenCount);
            }
        }
    }
}