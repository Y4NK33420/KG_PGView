# Graph RAG - Natural Language Querying

## Overview

The Graph RAG (Retrieval Augmented Generation) system enables natural language querying of your knowledge graph using Google's Gemini AI. Ask questions in plain English and get answers backed by actual graph data.

## Features

- 🧠 **Natural Language Understanding**: Ask questions in plain English
- 🔍 **Automatic GQL Generation**: LLM converts your question to proper GQL syntax
- 📊 **Smart Result Limiting**: Automatically caps results at 100 nodes and 200 edges for performance
- 🎯 **Context-Aware**: LLM has knowledge of your graph schema, views, and GQL syntax
- 💬 **Natural Language Responses**: Get human-readable answers, not just raw query results

## Prerequisites

1. **Gemini API Key**: Set the `GEMINI_API_KEY` environment variable
   ```bash
   export GEMINI_API_KEY="your-gemini-api-key-here"
   ```

2. **Running System**: API server must be running and connected to a graph
   ```bash
   # Start the API server
   mvn exec:java@api
   
   # In another terminal, connect to a graph
   curl -X POST http://localhost:7070/connect \
       -H "Content-Type: application/json" \
       -d '{"platform":"pg"}'
   
   curl -X POST http://localhost:7070/graph/use \
       -H "Content-Type: application/json" \
       -d '{"name":"dummygraph"}'
   ```

## API Endpoint

**POST** `/rag/ask`

### Request Body

```json
{
  "question": "Your natural language question here"
}
```

### Response

```json
{
  "success": true,
  "question": "Who are the people in the IT department?",
  "gql_query": "MATCH (p:Person)-[w:works_at]->(c:Company) FROM g WHERE w.department = \"IT\" RETURN (p),(c);",
  "raw_results": "... query results ...",
  "answer": "The IT department has 31 people...",
  "result_count": 31
}
```

## Example Queries

### 1. Find People by Attribute
```bash
curl -X POST http://localhost:7070/rag/ask \
    -H "Content-Type: application/json" \
    -d '{"question":"Who are the people in the IT department?"}'
```

**What happens:**
1. LLM receives context about your graph (Person, Company nodes; works_at edge)
2. Generates: `MATCH (p:Person)-[w:works_at]->(c:Company) FROM g WHERE w.department = "IT" RETURN (p),(c);`
3. Executes the query
4. Returns natural language answer: "The IT department has 31 people including..."

### 2. Query Views
```bash
curl -X POST http://localhost:7070/rag/ask \
    -H "Content-Type: application/json" \
    -d '{"question":"Show me the high earners from the HighPay view"}'
```

**Generated GQL:**
```gql
MATCH (p:Person) FROM HighPay RETURN (p);
```

### 3. Complex Queries
```bash
curl -X POST http://localhost:7070/rag/ask \
    -H "Content-Type: application/json" \
    -d '{"question":"Which people know each other and both work at companies?"}'
```

**Generated GQL:**
```gql
MATCH (p1:Person)-[k:knows]->(p2:Person), 
      (p1)-[w1:works_at]->(c1:Company),
      (p2)-[w2:works_at]->(c2:Company)
FROM g
RETURN (p1),(p2),(c1),(c2);
```

### 4. Aggregation Questions
```bash
curl -X POST http://localhost:7070/rag/ask \
    -H "Content-Type: application/json" \
    -d '{"question":"How many people work at companies?"}'
```

## How It Works

```
┌─────────────────┐
│  User Question  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  Graph RAG Service          │
│  1. Get graph schema        │
│  2. Get available views     │
│  3. Build context prompt    │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│  Gemini LLM (2.5-flash)     │
│  - Understands graph schema │
│  - Knows GQL syntax         │
│  - Generates valid query    │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│  Query Execution            │
│  - Execute GQL query        │
│  - Capture results          │
│  - Limit to 100N/200E       │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│  Natural Language Response  │
│  - Format results           │
│  - Send to Gemini           │
│  - Get human-readable answer│
└─────────────────────────────┘
```

## Result Limiting

To ensure performance and reasonable API costs:
- **Nodes**: Limited to 100 maximum
- **Edges**: Limited to 200 maximum

When results exceed these limits, the system:
1. Shows first N results
2. Adds "(results truncated at 100 nodes / 200 edges)" message
3. Informs the LLM about truncation for accurate response

## GQL Context Provided to LLM

The LLM receives:

### 1. Graph Schema
```
Node Types:
- Person
- Company
- Product

Edge Types:
- works_at (Person -> Company)
- knows (Person -> Person)
- bought (Person -> Product)
- produces (Company -> Product)
```

### 2. Available Views
```
- Friends (materialized view)
- HighPay (materialized view)
- Veterans (materialized view)
- Shoppers (materialized view)
```

### 3. GQL Syntax Rules
- Basic query patterns
- How to query views
- Property conditions
- String/numeric value formatting
- Return clause requirements

## Error Handling

### Missing API Key
```json
{
  "success": false,
  "error": "GEMINI_API_KEY environment variable not set. Please set it to use Graph RAG."
}
```

### Not Connected to Graph
```json
{
  "success": false,
  "error": "Please select a graph first (use /graph/use endpoint)"
}
```

### Query Generation Failed
```json
{
  "success": false,
  "error": "Failed to generate GQL query from natural language",
  "generated_gql": "..."
}
```

### Query Execution Error
```json
{
  "success": false,
  "error": "Query execution failed: ...",
  "gql_query": "MATCH ..."
}
```

## Tips for Best Results

### Good Questions
✅ "Who are the people in the IT department?"
✅ "Show me high earners from the HighPay view"
✅ "Which people work at companies in Finance?"
✅ "Find all products bought by people in Dallas"

### Questions to Avoid
❌ "Tell me about the data" (too vague)
❌ "Show me everything" (would exceed limits)
❌ Questions about data not in your schema

### Query View-Specific Data
When asking about filtered data, mention the view name:
- "Show me people from the Veterans view"
- "Query the Friends view for connections"

## Python Example

```python
import requests
import json

# Configure
API_URL = "http://localhost:7070"

# Connect and use graph
requests.post(f"{API_URL}/connect", json={"platform": "pg"})
requests.post(f"{API_URL}/graph/use", json={"name": "dummygraph"})

# Ask a question
question = "Who are the high earners?"
response = requests.post(
    f"{API_URL}/rag/ask",
    json={"question": question}
)

result = response.json()

if result["success"]:
    print(f"Question: {result['question']}")
    print(f"Generated GQL: {result['gql_query']}")
    print(f"Answer: {result['answer']}")
    print(f"Found {result['result_count']} results")
else:
    print(f"Error: {result['error']}")
```

## Integration with Web UI

You can add a "Natural Language Query" section to the web UI that calls this endpoint. The frontend would:

1. Show a text input for questions
2. Call `/rag/ask` on submit
3. Display:
   - The generated GQL query (for transparency)
   - The natural language answer
   - Option to visualize results (if applicable)

## Performance Considerations

- **First Query**: May take 3-5 seconds (LLM call + query execution + response generation)
- **Subsequent Queries**: Similar timing (each requires LLM calls)
- **Cost**: Uses Gemini API (check Google's pricing)
- **Rate Limits**: Respect Gemini API rate limits

## Troubleshooting

### "Cannot find symbol" compilation errors
Make sure all dependencies are properly installed:
```bash
mvn clean compile
```

### No response from LLM
- Check GEMINI_API_KEY is set correctly
- Verify API key is valid
- Check internet connection

### Incorrect GQL generated
- The LLM is trained but may occasionally make mistakes
- Check the generated GQL in the response
- You can refine your question and try again
- The system prompt can be adjusted in `GraphRAGService.java`

## Architecture

**Files Added/Modified:**
- `pom.xml`: Added OkHttp dependency
- `GraphRAGService.java`: Core RAG logic
- `GraphViewAPI.java`: `/rag/ask` endpoint

**No interference** with existing functionality:
- All existing endpoints work as before
- Graph RAG is completely optional
- Can be disabled by not setting GEMINI_API_KEY

## Future Enhancements

Potential improvements:
- [ ] Caching for repeated questions
- [ ] Query history and suggestions
- [ ] Multi-turn conversations
- [ ] Automatic query optimization
- [ ] Support for other LLMs (Claude, GPT-4, etc.)
- [ ] Visualization of RAG-generated results

## License

Same as the main project.

## Support

For issues or questions:
1. Check API logs: `/tmp/pgview_api.log`
2. Verify GEMINI_API_KEY is set
3. Test with simple questions first
4. Check that your graph has data

---

**Ready to use!** Set your GEMINI_API_KEY and start asking questions about your knowledge graph! 🚀

