# simplerag

## 搭建embedding model

ollama pull dengcao/Qwen3-Embedding-0.6B:F16
ollama serve

测试
```bash
curl http://localhost:11434/api/embed -d '{
"model": "dengcao/Qwen3-Embedding-0.6B:F16",
"input": "Why is the sky blue?"
}'
```


## HuggingFaceTokenizer.newInstance无法下载

java版本下载会失败，使用
```bash
python -c "from transformers import AutoTokenizer; AutoTokenizer.from_pretrained('deepseek-ai/DeepSeek-R1-0528')"
```
下载成功


## 观察weaviate

使用httpie 这种restful api的客户端，配合
https://weaviate.io/developers/weaviate/api/rest
来查看，更改数据库

localhost:8080/v1/schema 查看schema

localhost:8080/v1/objects 查看所有的objects

localhost:8080/v1/graphql 

- 查看chunk数
```graphql
{
  Aggregate {
    Chunk {
      meta {
        count
      }
    }
  }
}
```

- 检查vector
```graphql
{
  Get {
    Chunk (limit: 1){
      body
      _additional {
        vector  
      }
    }
  }
}
```

- 检查关键词搜索
```graphql
{
  Get {
    Chunk (
      bm25: {
        query: "张震",
        properties: ["body"]
      }){
      body
      _additional {
        score
      }
    }
  }
}
```

- 检查向量搜索
```graphql
{
  Get {
    Chunk (
       nearText: {
        concepts: ["坐骑有效期规则是什么"],
       }){
      body
      _additional {
        distance
      }
    }
  }
}
```