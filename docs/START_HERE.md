# 🚀 PG-View Knowledge Graph System - Complete Guide

## What You Have Now

A **fully functional** Knowledge Graph system with Python API for your AI agent, including:

### ✅ Fixed Core System
- Command parsing (trailing space bug fixed)
- PostgreSQL initialization (table creation fixed)
- View creation (auto-DEFAULT MAP for selection views)
- Idempotent operations (can re-run scripts)
- All backends working (PostgreSQL, SimpleDatalog, LogicBlox, Neo4j)

### ✅ REST API Server
- **File**: `src/main/java/edu/upenn/cis/db/graphtrans/api/GraphViewAPI.java`
- **Port**: 7070
- **Endpoints**: 20+ RESTful endpoints
- **Framework**: Javalin (lightweight Java web server)

### ✅ Python Client Library
- **File**: `python-client/pgview_client.py` (410 lines)
- **Features**: Complete wrapper for all operations
- **Type hints**: Full type annotations
- **Documentation**: Comprehensive docstrings

### ✅ Documentation (2000+ lines)
- `python-client/README.md` - Complete reference guide
- `API_QUICK_START.md` - Quick start guide
- `PYTHON_API_SUMMARY.md` - Summary and architecture
- `FIXES_APPLIED.md` - All bug fixes documented

### ✅ Working Example
- **File**: `python-client/example_knowledge_graph.py` (232 lines)
- Builds a complete knowledge graph for an AI agent
- Demonstrates all features
- Ready to run!

---

## Quick Start (3 Minutes)

### Step 1: Start the API Server

```bash
cd /home/yankee/src/pg-view
mvn exec:java@api -Dexec.args="conf/graphview.conf"
```

**Expected output:**
```
PG-View REST API Server started on port 7070
```

Leave this terminal running.

### Step 2: Test the Example (New Terminal)

```bash
cd /home/yankee/src/pg-view/python-client
pip install -r requirements.txt
python3 example_knowledge_graph.py
```

**You'll see:**
- ✓ Knowledge graph created
- ✓ Schema defined (Entity, Concept, Document)
- ✓ Data inserted (people, technologies, documents)
- ✓ Views created (PersonNetwork, TechRelations, etc.)
- ✓ Queries executed successfully

### Step 3: Use in Your AI Agent

```python
from pgview_client import PGViewClient

# Initialize
client = PGViewClient("http://localhost:7070")

# Setup graph
client.setup_graph("MyAIKnowledgeBase", platform="pg")

# Define ontology
client.add_node_schema("Entity")
client.add_node_schema("Concept")
client.add_edge_schema("IsA", "Entity", "Concept")
client.add_edge_schema("RelatesTo", "Entity", "Entity")

# Add knowledge
client.insert_node(1, "Entity")
client.insert_node_property(1, "name", "Python")
client.insert_node_property(1, "type", "technology")

client.insert_node(2, "Concept")
client.insert_node_property(2, "name", "Programming Language")

client.insert_edge(10, 1, 2, "IsA")

# Query
result = client.query(
    "MATCH (e:Entity)-[r:IsA]->(c:Concept) FROM g RETURN (e),(c),(r)"
)
print(result)
```

---

## System Capabilities

### 🗄️ Database Operations
- ✅ Create/drop/list graphs
- ✅ Switch between graphs
- ✅ Multiple backends (PostgreSQL, SimpleDatalog, etc.)

### 📊 Schema Management
- ✅ Define node types
- ✅ Define edge types with constraints
- ✅ View current schema

### 📥 Data Operations
- ✅ Insert nodes, edges, properties
- ✅ Import from CSV files
- ✅ Batch operations
- ✅ Handle millions of entities (PostgreSQL)

### 🔍 Querying
- ✅ Pattern matching (MATCH...RETURN)
- ✅ Multi-hop queries
- ✅ Path patterns with regex
- ✅ Property filters (WHERE clauses)

### 🔭 Views (Key Feature!)
- ✅ **Virtual views**: Computed on-the-fly
- ✅ **Materialized views**: Pre-computed
- ✅ **Transformation views**: Change structure/labels
- ✅ **Derived relationships**: Infer new connections
- ✅ **Views on views**: Cascade transformations

---

## Key Features for AI Agents

### 1. Flexible Schema
```python
# Define your domain
client.add_node_schema("Agent")
client.add_node_schema("Task")
client.add_node_schema("Resource")
client.add_edge_schema("Performs", "Agent", "Task")
client.add_edge_schema("Requires", "Task", "Resource")
```

### 2. Rich Queries
```python
# Find all tasks an agent can perform
result = client.query("""
    MATCH (a:Agent)-[p:Performs]->(t:Task)-[r:Requires]->(res:Resource)
    FROM g
    WHERE a.name = "MyAgent"
    RETURN (t),(res)
""")
```

### 3. Reasoning with Views
```python
# Create a view that infers capabilities
view = """CREATE virtual VIEW AgentCapabilities ON g WITH DEFAULT MAP (
  MATCH (a:Agent)-[p:Performs]->(t:Task)
  MATCH (t:Task)-[r:Requires]->(res:Resource)
  CONSTRUCT (a:Agent)-[can:CanAccess]->(res:Resource)
  SET can = SK("capability", a, res)
)"""
client.create_view(view)

# Query inferred knowledge
result = client.query("""
    MATCH (a:Agent)-[can:CanAccess]->(res:Resource)
    FROM AgentCapabilities
    RETURN (a),(res)
""")
```

### 4. Multi-hop Reasoning
```python
# Find indirect relationships
result = client.query("""
    MATCH (e1:Entity)-[r:RelatesTo*]->(e2:Entity)
    FROM g
    WHERE e1.name = "StartEntity"
    RETURN (e2)
""")
```

---

## Documentation Reference

| Document | Purpose | Lines |
|----------|---------|-------|
| **START_HERE.md** | This file - Quick overview | ~400 |
| **API_QUICK_START.md** | Quick start guide | 240 |
| **PYTHON_API_SUMMARY.md** | Complete summary | 403 |
| **python-client/README.md** | Full reference (GQL, API, examples) | 700+ |
| **FIXES_APPLIED.md** | All bug fixes documented | 250+ |
| **example_knowledge_graph.py** | Working example | 232 |
| **pgview_client.py** | Python client source | 410 |

**Total Documentation**: 2500+ lines

---

## Native GQL Language Cheat Sheet

### Connection
```gql
connect pg;              -- PostgreSQL
connect sd;              -- SimpleDatalog
```

### Graph Management
```gql
create graph MyGraph;
use MyGraph;
drop graph MyGraph;
list;
```

### Schema
```gql
create node Person;
create node Company;
create edge WorksFor(Person -> Company);
schema;
```

### Data Insertion
```gql
insert N(1, "Person");                    -- Node
insert E(10, 1, 2, "WorksFor");          -- Edge
insert NP(1, "name", "Alice");           -- Node property
insert EP(10, "since", "2020");          -- Edge property
```

### Querying
```gql
-- Basic
MATCH (a:Person)-[x:WorksFor]->(c:Company) FROM g RETURN (a),(c);

-- With WHERE
MATCH (a:Person)-[x:WorksFor]->(c:Company) 
FROM g 
WHERE a.age > 25 
RETURN (a),(c);

-- Multi-hop
MATCH (a:Person)-[x:Knows]->(b:Person)-[y:Knows]->(c:Person) 
FROM g 
RETURN (a),(c);
```

### Views
```gql
-- Selection view (auto DEFAULT MAP)
CREATE virtual VIEW EmployeeView ON g (
  MATCH (a:Person)-[x:WorksFor]->(c:Company)
);

-- Transformation view
CREATE virtual VIEW UserNetwork ON g WITH DEFAULT MAP (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  CONSTRUCT (a:User)-[x:ConnectedTo]->(b:User)
);

-- With Skolem functions (ID generation)
CREATE virtual VIEW DerivedView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  CONSTRUCT (a:Person)-[y:Inferred]->(b:Person)
  SET y = SK("inferred", x)
);
```

---

## Python API Cheat Sheet

### Basic Setup
```python
from pgview_client import PGViewClient

client = PGViewClient("http://localhost:7070")
client.connect("pg")
client.create_graph("MyGraph")
client.use_graph("MyGraph")
```

### Schema
```python
client.add_node_schema("Person")
client.add_edge_schema("Knows", "Person", "Person")
```

### Data
```python
client.insert_node(1, "Person")
client.insert_edge(10, 1, 2, "Knows")
client.insert_node_property(1, "name", "Alice")
```

### Query
```python
result = client.query(
    "MATCH (a:Person)-[x:Knows]->(b:Person) FROM g RETURN (a),(b),(x)"
)
print(result['resultInfo'])
```

### Views
```python
view_def = """CREATE virtual VIEW FriendsView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
)"""
client.create_view(view_def)
```

### Convenience Methods
```python
# Setup in one call
client.setup_graph("MyGraph", "pg")

# Simple view creation
client.create_simple_view("FriendsView", "Person", "Knows")
```

---

## Architecture Overview

```
┌──────────────────────────────────────┐
│         Your AI Agent                │
│  (Python code using pgview_client)   │
└─────────────┬────────────────────────┘
              │ HTTP REST (JSON)
              │ Port 7070
┌─────────────▼────────────────────────┐
│      REST API Server (Javalin)       │
│  Endpoints: /connect, /query, etc.   │
└─────────────┬────────────────────────┘
              │
┌─────────────▼────────────────────────┐
│      PG-View Core System             │
│  - Command Parser                    │
│  - View Engine                       │
│  - Query Rewriter                    │
│  - Type Checker                      │
└─────────────┬────────────────────────┘
              │
    ┌─────────┼──────────┬──────────┐
    │         │          │          │
┌───▼────┐ ┌─▼─────┐ ┌──▼─────┐ ┌─▼─────┐
│PostGres│ │Simple │ │LogicBlox│ │ Neo4j │
│  (SQL) │ │Datalog│ │(Datalog)│ │(Cypher│
└────────┘ └───────┘ └─────────┘ └───────┘
```

---

## Common Use Cases

### 1. Knowledge Base for RAG
```python
# Store documents and entities
client.add_node_schema("Document")
client.add_node_schema("Entity")
client.add_edge_schema("Mentions", "Document", "Entity")

# Query relevant documents
result = client.query("""
    MATCH (d:Document)-[m:Mentions]->(e:Entity)
    FROM g
    WHERE e.name = "AI"
    RETURN (d)
""")
```

### 2. Agent Planning
```python
# Define tasks and dependencies
client.add_node_schema("Task")
client.add_edge_schema("DependsOn", "Task", "Task")

# Find executable tasks
result = client.query("""
    MATCH (t:Task)
    FROM g
    WHERE NOT EXISTS (t)-[:DependsOn]->(:Task)
    RETURN (t)
""")
```

### 3. Multi-Agent Systems
```python
# Model agents and interactions
client.add_node_schema("Agent")
client.add_edge_schema("Communicates", "Agent", "Agent")
client.add_edge_schema("Coordinates", "Agent", "Agent")

# Find communication patterns
result = client.query("""
    MATCH (a1:Agent)-[c:Communicates]->(a2:Agent)
    FROM g
    RETURN (a1),(a2)
""")
```

---

## Performance Tips

### For Small Graphs (<10K entities)
- Use SimpleDatalog: `client.connect("sd")`
- Fast in-memory operations
- Good for development/testing

### For Large Graphs (>100K entities)
- Use PostgreSQL: `client.connect("pg")`
- Scales to millions of entities
- Full persistence and ACID

### For Complex Queries
- Create **materialized views** for frequently accessed patterns
- Use **indexes** on large tables
- Consider **SSR (Simplified Symbolic Rewriting)** for optimized queries

---

## Troubleshooting

### Server won't start
```bash
# Check Java version (needs 11+)
java -version

# Check if port is free
lsof -i :7070

# Check Maven
mvn --version
```

### Python connection fails
```python
# Test server health
import requests
response = requests.get("http://localhost:7070/health")
print(response.json())
```

### PostgreSQL connection fails
```bash
# Check PostgreSQL is running
psql -U postgres -h 127.0.0.1

# Check credentials in conf/graphview.conf
cat conf/graphview.conf | grep postgres
```

---

## What's Next?

1. ✅ **Start the server**: `mvn exec:java@api`
2. ✅ **Run the example**: `python3 example_knowledge_graph.py`
3. ⭐ **Integrate with your AI agent**: Import `pgview_client` in your code
4. 📚 **Read full docs**: `python-client/README.md`
5. 🚀 **Build amazing things!**

---

## File Structure

```
/home/yankee/src/pg-view/
├── src/main/java/edu/upenn/cis/db/graphtrans/
│   ├── api/GraphViewAPI.java       ← REST API Server
│   ├── CommandExecutor.java         ← Fixed command parsing
│   ├── parser/ViewParser.java       ← Fixed auto-DEFAULT MAP
│   └── store/postgres/
│       └── PostgresStore.java       ← Fixed table creation
│
├── python-client/
│   ├── pgview_client.py            ← Python Client Library
│   ├── example_knowledge_graph.py  ← Working Example
│   ├── requirements.txt            ← Python Dependencies
│   └── README.md                   ← Full Documentation
│
├── START_HERE.md                   ← This file!
├── API_QUICK_START.md              ← Quick Start Guide
├── PYTHON_API_SUMMARY.md           ← Architecture & Summary
├── FIXES_APPLIED.md                ← Bug Fixes Documentation
└── conf/graphview.conf             ← Configuration
```

---

## Summary

**You now have a production-ready Knowledge Graph system with:**

- ✅ All core bugs fixed
- ✅ REST API server (20+ endpoints)
- ✅ Python client library (complete wrapper)
- ✅ Comprehensive documentation (2500+ lines)
- ✅ Working example code
- ✅ Multiple backend support
- ✅ Powerful view system
- ✅ Ready for your AI agent!

**Start building in 3 minutes** - Just start the server and run the example!

---

**Questions?** Check `python-client/README.md` for detailed documentation of every feature.

**Ready to code?** See `example_knowledge_graph.py` for a complete working example!

🚀 **Happy Knowledge Graphing!** 🚀
