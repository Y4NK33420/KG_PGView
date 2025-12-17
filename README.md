# PG-View: Property Graph Views & Knowledge Graph System

A powerful knowledge graph system with support for virtual and materialized views over property graphs. Features a REST API, Python client library, interactive web interface, and AI-powered natural language querying.

> Built upon the research from ["Implementing Views for Property Graphs"](https://dl.acm.org/doi/abs/10.1145/3654949) (SIGMOD 2024) by Han & Ives, University of Pennsylvania. Extended as part of a DBMS course project with production-ready features and modern tooling.

## ✨ Features

- **🔍 Property Graph Views**: Create virtual and materialized views with pattern matching, filtering, and transformations
- **🌐 REST API**: 20+ endpoints for graph management, querying, and view operations (port 7070)
- **🐍 Python Client**: Full-featured library with type hints and comprehensive examples
- **🎨 Web UI**: Interactive interface with graph visualization powered by Vis.js
- **🤖 Graph RAG**: Natural language querying using Google Gemini AI
- **💾 Multi-Backend**: Support for PostgreSQL, SimpleDatalog, Neo4j, and LogicBlox
- **⚡ Query Optimization**: SSR (Substitution Subgraph Relations) indexes and rewriting
- **✅ Type Checking**: Z3-powered validation for view definitions

## 🚀 Quick Start

### Prerequisites

- Java 11+
- Maven 3.6+
- PostgreSQL 14+ (or use SimpleDatalog for in-memory testing)
- Python 3.7+ (for Python client and scripts)

### Installation

```bash
# Clone the repository
git clone https://github.com/PennGraphDB/pg-view.git
cd pg-view

# Compile the project
mvn clean compile

# Configure database connection (optional, defaults work for local PostgreSQL)
# Edit conf/graphview.conf if needed
```

### Start the REST API Server

```bash
mvn exec:java@api -Dexec.args="conf/graphview.conf"
```

Server runs at `http://localhost:7070`

### Access the Web Interface

```bash
# In a new terminal
cd web-ui
python3 -m http.server 8080
```

Open `http://localhost:8080` in your browser

### Use the Python Client

```bash
cd python-client
pip install -r requirements.txt
python3 example_knowledge_graph.py
```

## 📖 Usage Examples

### Python Client

```python
from pgview_client import PGViewClient

# Initialize client
client = PGViewClient("http://localhost:7070")

# Connect and setup graph
client.connect("pg")  # PostgreSQL backend
client.create_graph("MyKnowledgeGraph")
client.use_graph("MyKnowledgeGraph")

# Define schema
client.add_node_schema("Person")
client.add_node_schema("Company")
client.add_edge_schema("WorksFor", "Person", "Company")

# Insert data
client.insert_node(1, "Person")
client.insert_node_property(1, "name", "Alice")
client.insert_node_property(1, "age", "30")

client.insert_node(2, "Company")
client.insert_node_property(2, "name", "TechCorp")

client.insert_edge(100, 1, 2, "WorksFor")

# Query the graph
result = client.query("""
    MATCH (p:Person)-[w:WorksFor]->(c:Company) 
    FROM g 
    WHERE p.age > 25
    RETURN (p),(c)
""")
print(result)

# Create a view
view_def = """
CREATE virtual VIEW Employees ON g (
  MATCH (p:Person)-[w:WorksFor]->(c:Company)
)
"""
client.create_view(view_def)

# Query the view
result = client.query("MATCH (p:Person)-[w]->(c:Company) FROM Employees RETURN (p),(c)")
```

### REST API

```bash
# Connect to PostgreSQL
curl -X POST http://localhost:7070/connect \
  -H "Content-Type: application/json" \
  -d '{"platform":"pg"}'

# Create a graph
curl -X POST http://localhost:7070/graph/create \
  -H "Content-Type: application/json" \
  -d '{"name":"TestGraph"}'

# Use the graph
curl -X POST http://localhost:7070/graph/use \
  -H "Content-Type: application/json" \
  -d '{"name":"TestGraph"}'

# Execute a query
curl -X POST http://localhost:7070/query \
  -H "Content-Type: application/json" \
  -d '{"query":"MATCH (p:Person) FROM g RETURN (p)"}'
```

### Natural Language Queries (Graph RAG)

```bash
# Set your Gemini API key
export GEMINI_API_KEY="your-api-key"

# Ask questions in plain English
curl -X POST http://localhost:7070/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"Who are the people in the IT department?"}'
```

## 📚 GQL Language Reference

### Graph Management
```gql
connect pg;              -- Connect to PostgreSQL
connect sd;              -- Connect to SimpleDatalog (in-memory)
create graph MyGraph;
use MyGraph;
list;                    -- List all graphs
```

### Schema Definition
```gql
create node Person;
create node Company;
create edge WorksFor(Person -> Company);
schema;                  -- View current schema
```

### Data Insertion
```gql
insert N(1, "Person");                    -- Insert node
insert E(100, 1, 2, "WorksFor");          -- Insert edge
insert NP(1, "name", "Alice");            -- Insert node property
insert EP(100, "since", "2020");          -- Insert edge property
```

### Querying
```gql
-- Basic pattern matching
MATCH (a:Person)-[x:WorksFor]->(c:Company) FROM g RETURN (a),(c),(x);

-- With conditions
MATCH (a:Person)-[x:WorksFor]->(c:Company) 
FROM g 
WHERE a.age > 25 AND c.industry = "Tech"
RETURN (a),(c);

-- Multi-hop queries
MATCH (a:Person)-[x:Knows]->(b:Person)-[y:Knows]->(c:Person) 
FROM g 
RETURN (a),(c);

-- Path patterns with regex
MATCH (a:Person)-[x:Knows*]->(b:Person) 
FROM g 
RETURN (a),(b);
```

### View Creation
```gql
-- Selection view (filters existing graph)
CREATE virtual VIEW Employees ON g (
  MATCH (p:Person)-[w:WorksFor]->(c:Company)
);

-- Transformation view (changes structure)
CREATE virtual VIEW UserNetwork ON g WITH DEFAULT MAP (
  MATCH (a:Person)-[k:Knows]->(b:Person)
  CONSTRUCT (a:User)-[k:ConnectedTo]->(b:User)
);

-- View with derived relationships
CREATE virtual VIEW Colleagues ON g (
  MATCH (p1:Person)-[w1:WorksFor]->(c:Company)
  MATCH (p2:Person)-[w2:WorksFor]->(c:Company)
  CONSTRUCT (p1:Person)-[coworker:ColleagueOf]->(p2:Person)
  SET coworker = SK("colleague", p1, p2)
);

-- Query a view
MATCH (u:User)-[c:ConnectedTo]->(u2:User) FROM UserNetwork RETURN (u),(u2);
```

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│              User Interfaces                             │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │  Web UI    │  │  Python    │  │  REST API  │        │
│  │  (Vis.js)  │  │  Client    │  │  (curl)    │        │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘        │
└────────┼────────────────┼────────────────┼──────────────┘
         │                │                │
         └────────────────┼────────────────┘
                          │ HTTP/JSON
         ┌────────────────▼────────────────┐
         │  REST API Server (Javalin)       │
         │  - Graph Management              │
         │  - Query Execution               │
         │  - View Operations               │
         │  - Graph RAG (Gemini AI)         │
         └────────────────┬────────────────┘
                          │
         ┌────────────────▼────────────────┐
         │  PG-View Core Engine             │
         │  - Command Parser                │
         │  - View Engine                   │
         │  - Query Rewriter                │
         │  - Type Checker (Z3)             │
         │  - Datalog Translator            │
         └────────────────┬────────────────┘
                          │
    ┌────────────┬────────┼────────┬─────────────┐
    │            │        │        │             │
┌───▼────┐  ┌───▼────┐  ┌▼────┐  ┌▼─────┐  ┌──▼──────┐
│Postgres│  │Simple  │  │Logic│  │ Neo4j│  │ Gemini  │
│  SQL   │  │Datalog │  │Blox │  │Cypher│  │   AI    │
└────────┘  └────────┘  └─────┘  └──────┘  └─────────┘
```

## 📁 Project Structure

```
pg-view/
├── src/main/java/          # Java source code
│   └── edu/upenn/cis/db/graphtrans/
│       ├── api/            # REST API server (GraphViewAPI.java, GraphRAGService.java)
│       ├── parser/         # GQL parsers
│       ├── store/          # Backend implementations (PostgreSQL, Neo4j, etc.)
│       ├── datalog/        # Datalog engine and rewriting
│       └── typechecker/    # Z3-based type checking
├── python-client/
│   ├── pgview_client.py    # Python client library
│   ├── example_knowledge_graph.py
│   └── requirements.txt
├── web-ui/
│   └── index.html          # Interactive web interface
├── scripts/                # Utility scripts
│   ├── generate_dummy_data.py
│   └── load_dummy_data.py
├── docs/                   # Comprehensive documentation
│   ├── START_HERE.md       # Quick start guide
│   ├── PYTHON_API_SUMMARY.md
│   ├── GRAPH_RAG_README.md
│   └── API_QUICK_START.md
├── conf/
│   └── graphview.conf      # Configuration file
└── pom.xml                 # Maven build configuration
```

## 🗄️ Backend Support

| Backend | Code | Use Case | Persistence | Scale |
|---------|------|----------|-------------|-------|
| **PostgreSQL** | `pg` | Production, large graphs | Full ACID | Millions of nodes |
| **SimpleDatalog** | `sd` | Development, testing | In-memory | Thousands of nodes |
| **LogicBlox** | `lb` | Advanced analytics | Full | Large scale |
| **Neo4j** | `n4` | Native graph DB | Full | Large scale |

## 🔧 API Endpoints

### Core Operations
- `GET /health` - Server health check
- `POST /connect` - Connect to backend platform
- `POST /graph/create` - Create new graph
- `POST /graph/use` - Switch to graph
- `GET /graphs` - List all graphs
- `DELETE /graph/{name}` - Delete graph

### Schema Management
- `POST /schema/node` - Add node type
- `POST /schema/edge` - Add edge type
- `GET /schema` - Get current schema

### Data Operations
- `POST /data/insert` - Insert node/edge/property
- `POST /data/import` - Import from CSV

### View Operations
- `POST /view/create` - Create view
- `GET /views` - List all views
- `GET /program` - Get Datalog program

### Query Operations
- `POST /query` - Execute GQL query
- `POST /execute` - Execute raw command
- `POST /execute-batch` - Batch operations

### AI Features
- `POST /rag/ask` - Natural language query (requires `GEMINI_API_KEY`)

## 📊 Sample Data

Generate and load sample data for testing:

```bash
# Generate 500 nodes and 3000+ edges (Person, Company, Product)
python3 scripts/generate_dummy_data.py

# Load into database
python3 scripts/load_dummy_data.py
```

## 🔬 PostgreSQL Setup

### Install PostgreSQL

```bash
# Ubuntu/Debian
sudo apt install postgresql-14

# macOS
brew install postgresql@14
```

### Configure Authentication

```bash
# Switch to postgres user
sudo su - postgres
psql -U postgres

# In PostgreSQL shell
ALTER USER postgres WITH PASSWORD 'postgres@';
\q

# Edit pg_hba.conf to use md5 authentication
sudo vi /etc/postgresql/14/main/pg_hba.conf
# Change 'peer' to 'md5' for local connections

# Restart PostgreSQL
sudo service postgresql restart
```

## 🧪 Advanced Features

### SSR Indexes (Query Optimization)

Create indexes for faster query execution:

```gql
create index ssr Person knows;
```

This materializes substitution subgraph relations for efficient query rewriting.

### Materialized Views

Pre-compute views for instant access:

```gql
CREATE materialized VIEW FrequentFriends ON g (
  MATCH (a:Person)-[k:knows]->(b:Person)
  WHERE a.friendship_score > 80
);
```

### Type Checking

Enable type checking to validate view definitions:

```gql
option typecheck on;
```

Uses Z3 theorem prover to detect rule overlaps and ensure consistency.

## 📖 Documentation

- **[START_HERE.md](docs/START_HERE.md)** - Comprehensive quick start guide
- **[PYTHON_API_SUMMARY.md](docs/PYTHON_API_SUMMARY.md)** - Python client reference
- **[GRAPH_RAG_README.md](docs/GRAPH_RAG_README.md)** - Natural language querying guide
- **[API_QUICK_START.md](docs/API_QUICK_START.md)** - REST API quick reference
- **[README_VISUALIZATION.md](docs/README_VISUALIZATION.md)** - Web UI guide
- **[Implementation_detail.md](docs/Implementation_detail.md)** - Java architecture details

## 🔒 Security Notes

⚠️ **For development/testing only** - Configure security for production use:

- Default PostgreSQL password is `postgres@` - **change this**
- API has no authentication - **add auth layer**
- CORS is set to `*` - **restrict origins**
- Gemini API key exposed via environment - **use secrets management**

## 🐛 Troubleshooting

### Server won't start
```bash
# Check Java version
java -version  # Should be 11+

# Check port availability
lsof -i :7070

# Check Maven
mvn --version
```

### PostgreSQL connection fails
```bash
# Check PostgreSQL is running
sudo service postgresql status

# Test connection
psql -U postgres -h 127.0.0.1

# Verify credentials in conf/graphview.conf
```

### Web UI CORS errors
- Restart API server
- Clear browser cache
- Check browser console for specific errors

## 🤝 Contributing

This project welcomes contributions! Areas for enhancement:

- Additional backend support (e.g., DuckDB, SQLite)
- Query optimization techniques
- Incremental view maintenance (IVM)
- Performance benchmarking tools
- Additional LLM providers for RAG
- Enhanced web UI features

## 🙏 Acknowledgments

Based on research by Soonbo Han and Zachary G. Ives (University of Pennsylvania) published in SIGMOD Record 2024. Original paper: ["Implementing Views for Property Graphs"](https://sigmodrecord.org/publications/sigmodRecord/2503/pdfs/14_property-han.pdf)

