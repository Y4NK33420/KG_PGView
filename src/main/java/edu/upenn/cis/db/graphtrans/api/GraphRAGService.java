package edu.upenn.cis.db.graphtrans.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.catalog.Schema;
import edu.upenn.cis.db.graphtrans.catalog.SchemaEdge;
import edu.upenn.cis.db.graphtrans.catalog.SchemaNode;
import okhttp3.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graph RAG (Retrieval Augmented Generation) Service
 * Enables natural language querying of the knowledge graph using Gemini LLM
 */
public class GraphRAGService {
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final int MAX_NODES = 100;
    private static final int MAX_EDGES = 200;
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    
    /**
     * Process a natural language query about the graph
     */
    public static Map<String, Object> processNaturalLanguageQuery(String naturalLanguageQuery) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                result.put("success", false);
                result.put("error", "GEMINI_API_KEY environment variable not set");
                return result;
            }
            
            // Step 1: Get schema and view information
            String graphContext = buildGraphContext();
            
            // Step 2: Generate GQL query using Gemini
            String gqlQuery = generateGQLQuery(naturalLanguageQuery, graphContext, apiKey);
            
            if (gqlQuery == null || gqlQuery.isEmpty()) {
                result.put("success", false);
                result.put("error", "Failed to generate GQL query from natural language");
                return result;
            }
            
            result.put("generated_gql", gqlQuery);
            
            // Step 3: Execute the GQL query
            Map<String, Object> queryResult = executeGQLQuery(gqlQuery);
            
            if (!(boolean) queryResult.get("success")) {
                result.put("success", false);
                result.put("error", "Query execution failed: " + queryResult.get("error"));
                result.put("gql_query", gqlQuery);
                return result;
            }
            
            // Step 4: Format and limit results
            String formattedResults = formatQueryResults(queryResult);
            
            // Step 5: Get natural language response from Gemini
            String naturalLanguageResponse = generateNaturalLanguageResponse(
                naturalLanguageQuery, 
                gqlQuery, 
                formattedResults, 
                apiKey
            );
            
            result.put("success", true);
            result.put("question", naturalLanguageQuery);
            result.put("gql_query", gqlQuery);
            result.put("raw_results", formattedResults);
            result.put("answer", naturalLanguageResponse);
            result.put("result_count", queryResult.get("result_count"));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Graph RAG error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Build context about the graph schema and views
     */
    private static String buildGraphContext() {
        StringBuilder context = new StringBuilder();
        
        context.append("## Graph Schema\n\n");
        
        // Node types
        context.append("### Node Types:\n");
        for (SchemaNode node : Schema.getSchemaNodes()) {
            context.append("- ").append(node.getLabel()).append("\n");
        }
        context.append("\n");
        
        // Edge types
        context.append("### Edge Types:\n");
        for (SchemaEdge edge : Schema.getSchemaEdges()) {
            context.append("- ").append(edge.getLabel())
                   .append(" (").append(edge.getFrom()).append(" -> ").append(edge.getTo()).append(")\n");
        }
        context.append("\n");
        
        // Views
        context.append("### Available Views:\n");
        int numViews = GraphTransServer.getNumTransRuleList();
        if (numViews > 0) {
            for (int i = 0; i < numViews; i++) {
                TransRuleList view = GraphTransServer.getTransRuleList(i);
                context.append("- ").append(view.getViewName())
                       .append(" (").append(view.getViewType()).append(" view)\n");
            }
        } else {
            context.append("- No views currently defined\n");
        }
        context.append("\n");
        
        return context.toString();
    }
    
    /**
     * Generate GQL query from natural language using Gemini
     */
    private static String generateGQLQuery(String naturalLanguageQuery, String graphContext, String apiKey) throws IOException {
        String systemPrompt = buildGQLGenerationPrompt(graphContext);
        
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        
        JsonObject message = new JsonObject();
        JsonArray parts = new JsonArray();
        
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", systemPrompt);
        parts.add(systemPart);
        
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", "User question: " + naturalLanguageQuery + "\n\nGenerate ONLY the GQL query, nothing else.");
        parts.add(userPart);
        
        message.add("parts", parts);
        contents.add(message);
        requestBody.add("contents", contents);
        
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.1);
        generationConfig.addProperty("maxOutputTokens", 1000);
        requestBody.add("generationConfig", generationConfig);
        
        String responseText = callGeminiAPI(requestBody, apiKey);
        
        // Extract GQL query from response
        return extractGQLQuery(responseText);
    }
    
    /**
     * Generate natural language response from query results
     */
    private static String generateNaturalLanguageResponse(String question, String gqlQuery, String results, String apiKey) throws IOException {
        String prompt = String.format(
            "You are a helpful assistant explaining graph database query results.\n\n" +
            "User's Question: %s\n\n" +
            "GQL Query Executed: %s\n\n" +
            "Query Results:\n%s\n\n" +
            "Please provide a clear, concise answer to the user's question based on these results. " +
            "If the results are empty, say so. If there are many results, summarize the key findings.",
            question, gqlQuery, results
        );
        
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        
        JsonObject message = new JsonObject();
        JsonArray parts = new JsonArray();
        
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        
        message.add("parts", parts);
        contents.add(message);
        requestBody.add("contents", contents);
        
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("maxOutputTokens", 2000);
        requestBody.add("generationConfig", generationConfig);
        
        return callGeminiAPI(requestBody, apiKey);
    }
    
    /**
     * Call Gemini API
     */
    private static String callGeminiAPI(JsonObject requestBody, String apiKey) throws IOException {
        String url = GEMINI_API_URL + "?key=" + apiKey;
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API call failed: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            // Extract text from response
            if (jsonResponse.has("candidates")) {
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    JsonObject content = candidate.getAsJsonObject("content");
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts.size() > 0) {
                        return parts.get(0).getAsJsonObject().get("text").getAsString();
                    }
                }
            }
            
            throw new IOException("Unexpected Gemini API response format");
        }
    }
    
    /**
     * Build prompt for GQL query generation
     */
    private static String buildGQLGenerationPrompt(String graphContext) {
        return "You are an expert in GQL (Graph Query Language) for querying property graphs.\n\n" +
               graphContext +
               "\n## GQL Syntax:\n\n" +
               "### Basic Query Pattern:\n" +
               "```\n" +
               "MATCH (variable:NodeLabel)-[edge:EdgeLabel]->(variable2:NodeLabel2)\n" +
               "FROM g\n" +
               "WHERE condition\n" +
               "RETURN (variable), (edge), (variable2)\n" +
               "```\n\n" +
               "### Querying from a View:\n" +
               "```\n" +
               "MATCH (variable:NodeLabel)\n" +
               "FROM ViewName\n" +
               "RETURN (variable)\n" +
               "```\n\n" +
               "### Important Rules:\n" +
               "- Always specify node labels (e.g., `(p:Person)` not just `(p)`)\n" +
               "- Use `FROM g` for the base graph or `FROM ViewName` for views\n" +
               "- Property comparisons: `WHERE variable.property = \"value\"` or `WHERE variable.property > 100`\n" +
               "- String values must be in double quotes\n" +
               "- Numeric values don't need quotes\n" +
               "- Variables in RETURN must be wrapped in parentheses\n" +
               "- End query with semicolon\n\n" +
               "### Examples:\n" +
               "1. Find all people: `MATCH (p:Person) FROM g RETURN (p);`\n" +
               "2. Find relationships: `MATCH (p:Person)-[k:knows]->(p2:Person) FROM g RETURN (p),(k),(p2);`\n" +
               "3. With conditions: `MATCH (p:Person)-[w:works_at]->(c:Company) FROM g WHERE w.salary > 100000 RETURN (p),(c);`\n" +
               "4. Query a view: `MATCH (p:Person) FROM HighPay RETURN (p);`\n\n" +
               "Generate ONLY the GQL query, no explanations or markdown formatting.";
    }
    
    /**
     * Extract GQL query from LLM response
     */
    private static String extractGQLQuery(String response) {
        // Remove markdown code blocks if present
        response = response.replaceAll("```gql\\s*", "").replaceAll("```\\s*", "").trim();
        
        // Find the query (should start with MATCH or CREATE)
        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("MATCH") || line.startsWith("CREATE")) {
                return line;
            }
        }
        
        return response.trim();
    }
    
    /**
     * Execute GQL query and capture results
     */
    private static Map<String, Object> executeGQLQuery(String gqlQuery) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Capture System.out to get query results
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(baos));
            
            // Execute the query
            CommandExecutor.run(gqlQuery);
            
            // Restore System.out
            System.out.flush();
            System.setOut(originalOut);
            
            String output = baos.toString();
            
            result.put("success", true);
            result.put("output", output);
            result.put("result_count", countResults(output));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Format query results for LLM consumption
     */
    private static String formatQueryResults(Map<String, Object> queryResult) {
        String output = (String) queryResult.get("output");
        
        if (output == null || output.isEmpty()) {
            return "No results found.";
        }
        
        // Parse and limit results
        String[] lines = output.split("\n");
        StringBuilder formatted = new StringBuilder();
        int nodeCount = 0;
        int edgeCount = 0;
        boolean inResults = false;
        
        for (String line : lines) {
            if (line.contains("row(s)") && line.contains("col(s)")) {
                formatted.append(line).append("\n");
                break;
            }
            
            if (line.contains("===")) {
                inResults = !inResults;
                formatted.append(line).append("\n");
                continue;
            }
            
            if (inResults) {
                // Limit results
                if (nodeCount < MAX_NODES && edgeCount < MAX_EDGES) {
                    formatted.append(line).append("\n");
                    if (!line.trim().isEmpty() && !line.contains("===")) {
                        if (line.contains("\t")) {
                            nodeCount++;
                        }
                    }
                } else if (nodeCount == MAX_NODES) {
                    formatted.append("... (results truncated at ").append(MAX_NODES).append(" nodes / ").append(MAX_EDGES).append(" edges)\n");
                    nodeCount++; // Prevent multiple truncation messages
                }
            } else {
                formatted.append(line).append("\n");
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * Count results in query output
     */
    private static int countResults(String output) {
        // Strip ANSI color codes first
        output = output.replaceAll("\u001B\\[[;\\d]*m", "");
        
        // Pattern 1: Look for "query result #: X"
        if (output.contains("query result #:")) {
            try {
                String[] parts = output.split("query result #:");
                if (parts.length > 1) {
                    String numPart = parts[1].trim().split("\\s+")[0];
                    return Integer.parseInt(numPart);
                }
            } catch (NumberFormatException e) {
                // Continue to next pattern
            }
        }
        
        // Pattern 2: Look for "(X row(s) Y col(s))"
        if (output.contains("row(s)")) {
            try {
                // Find the last occurrence of row(s) pattern
                int idx = output.lastIndexOf("(");
                if (idx >= 0) {
                    String part = output.substring(idx + 1);
                    if (part.contains("row(s)")) {
                        String[] nums = part.split("\\s+");
                        return Integer.parseInt(nums[0]);
                    }
                }
            } catch (NumberFormatException e) {
                // Return 0 if parsing fails
            }
        }
        
        return 0;
    }
}

