# PG-View API - Quick Start Guide

## Overview

This guide will help you get started with the PG-View REST API for accessing the knowledge graph system from Python or any HTTP client.

## Setup

### 1. Start the API Server

```bash
cd /home/yankee/src/pg-view

# Start the REST API server (runs on port 7070)
mvn exec:java@api -Dexec.args="conf/graphview.conf"
```

The server will output:
```
PG-View REST API Server started on port 7070
```

### 2. Install Python Client

```bash
cd python-client
pip install -r requirements.txt
```

The Python client (`pgview_client.py`) is ready to use!

## Quick Test

### Test 1: Health Check

```bash
curl http://localhost:7070/health
```

Expected output:
```json
{
  "status": "ok",
  "platform": "pg",
  "version": "1.0.0"
}
```

### Test 2: Run Python Example

```bash
cd python-client
python3 example_knowledge_graph.py
```

This will:
1. Connect to PostgreSQL
2. Create a knowledge graph
3. Define schema (Entity, Concept, Document)
4. Insert sample data
5. Create multiple views
6. Execute queries

### Test 3: Manual API Call

```python
import requests

# Connect to PostgreSQL
response = requests.post(
    "http://localhost:7070/connect",
    json={"platform": "pg"}
)
print(response.json())

# Create a graph
response = requests.post(
    "http://localhost:7070/graph/create",
    json={"name": "TestGraph"}
)
print(response.json())
```

## Common Workflows

### Workflow 1: Build Knowledge Graph

```python
from pgview_client import PGViewClient

client = PGViewClient()

# 1. Setup
client.setup_graph("MyKG", platform="pg")

# 2. Define Schema
client.add_node_schema("Person")
client.add_edge_schema("Knows", "Person", "Person")

# 3. Insert Data
client.insert_node(1, "Person")
client.insert_node(2, "Person")
client.insert_edge(10, 1, 2, "Knows")

# 4. Query
result = client.query(
    "MATCH (a:Person)-[x:Knows]->(b:Person) FROM g RETURN (a),(b),(x)"
)
print(result)
```

### Workflow 2: Create and Query Views

```python
# Create a view
view_def = """CREATE virtual VIEW FriendsView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
)"""
client.create_view(view_def)

# Query the view
result = client.query(
    "MATCH (a:Person)-[x:Knows]->(b:Person) FROM FriendsView RETURN (a),(b),(x)"
)
```

### Workflow 3: Batch Operations

```python
commands = [
    "create node Company",
    "create edge WorksFor(Person -> Company)",
    "insert N(100, \"Company\")",
    "insert E(20, 1, 100, \"WorksFor\")"
]

result = client.execute_batch(commands)
```

## API Endpoints Reference

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/health` | Check server status |
| POST | `/connect` | Connect to backend (pg/sd/lb/n4) |
| POST | `/graph/create` | Create new graph |
| POST | `/graph/use` | Switch to graph |
| POST | `/schema/node` | Add node type |
| POST | `/schema/edge` | Add edge type |
| POST | `/data/insert` | Insert node/edge/property |
| POST | `/view/create` | Create view |
| POST | `/query` | Execute query |
| GET | `/views` | List all views |
| GET | `/schema` | Get schema |

## Native GQL Syntax Quick Reference

### Connect & Setup
```gql
connect pg;
create graph MyGraph;
use MyGraph;
```

### Schema
```gql
create node Person;
create edge Knows(Person -> Person);
```

### Data
```gql
insert N(1, "Person");
insert E(10, 1, 2, "Knows");
insert NP(1, "name", "Alice");
```

### Query
```gql
MATCH (a:Person)-[x:Knows]->(b:Person) FROM g RETURN (a),(b),(x);
```

### View
```gql
CREATE virtual VIEW MyView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
);
```

## Configuration

### Backend Selection

- **PostgreSQL** (`pg`): Production use, scales well
- **SimpleDatalog** (`sd`): Development/testing, in-memory
- **LogicBlox** (`lb`): Advanced analytics (requires LogicBlox)
- **Neo4j** (`n4`): Native graph DB (requires Neo4j)

### View Types

- **virtual**: Computed on-the-fly (default)
- **materialized**: Pre-computed and stored
- **hybrid**: Mix of virtual and materialized

## Troubleshooting

### Server won't start
- Check Java 11+ is installed: `java -version`
- Ensure port 7070 is free: `lsof -i :7070`
- Check Maven: `mvn --version`

### PostgreSQL connection failed
- Ensure PostgreSQL is running
- Check credentials in `conf/graphview.conf`
- Verify connection: `psql -U postgres -h 127.0.0.1`

### Python import error
- Install requests: `pip install requests`
- Check Python version: `python3 --version` (requires 3.6+)

## Next Steps

1. **Read full documentation**: `python-client/README.md`
2. **Try examples**: `python-client/example_knowledge_graph.py`
3. **Explore API**: Test endpoints with curl or Postman
4. **Integrate with AI agent**: Use the Python client in your application

## Support

- **Documentation**: See `python-client/README.md` for comprehensive docs
- **Examples**: Check `python-client/` directory for code examples
- **Issues**: Review `FIXES_APPLIED.md` for resolved issues
- **Grammar**: See `src/main/antlr4/.../GraphTransQuery.g4` for GQL syntax

---

**Server Running?** Test with: `curl http://localhost:7070/health`

**Ready to code?** See `example_knowledge_graph.py` for a complete example!

