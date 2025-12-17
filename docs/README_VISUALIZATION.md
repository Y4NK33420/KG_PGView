# PG-View Graph Visualization Guide

Complete guide to setting up and using the PG-View Knowledge Graph system with visualization features.

## 🚀 Quick Start

### Prerequisites
- Java 11+
- Maven 3.6+
- PostgreSQL 14+
- Python 3.7+
- Modern web browser

### 1. Setup PostgreSQL
```bash
# Install PostgreSQL (Ubuntu/Debian)
sudo apt install postgresql-14

# Configure postgres user with password 'postgres@'
sudo su - postgres
psql -U postgres
ALTER USER postgres WITH PASSWORD 'postgres@';
\q

# Update pg_hba.conf to use md5 authentication
sudo vi /etc/postgresql/14/main/pg_hba.conf
# Change 'peer' to 'md5' for local connections
sudo service postgresql restart
```

### 2. Start the REST API Server
```bash
cd /path/to/pg-view

# Compile the project
mvn clean compile

# Start the API server (runs on port 7070)
mvn exec:java@api
```

### 3. Start the Web UI Server
```bash
# In a new terminal
cd /path/to/pg-view/web-ui
python3 -m http.server 8080
```

### 4. Access the Application
- **Web UI**: http://localhost:8080
- **API**: http://localhost:7070
- **Health Check**: http://localhost:7070/health

## 📊 Creating Dummy Data

### Generate Sample Graph Data (500 nodes, 3000+ edges)

```bash
# 1. Generate the dummy data
python3 scripts/generate_dummy_data.py

# 2. Load data into database
python3 scripts/load_dummy_data.py
```

This creates a database called `DummyGraph` with:
- **300 Person nodes** (name, age, city, email)
- **100 Company nodes** (name, industry, size, founded)
- **100 Product nodes** (name, price, category, rating)
- **3,180+ edges** (works_at, knows, bought, produces)

### Manual Data Creation

```bash
# Use the provided GQL scripts
mvn exec:java@console -Dexec.args="scripts/create_sample_graph.gql"
```

## 🎨 Using Graph Visualization

### Via Web UI (Recommended)

1. **Connect to Platform**
   - Open http://localhost:8080
   - Go to "🔌 Connection" tab
   - Select "PostgreSQL" and click "Connect"

2. **Select Database**
   - Go to "📊 Graphs" tab
   - Select "DummyGraph" from dropdown
   - Click "Use Graph"

3. **Visualize**
   - Go to "📈 Visualize" tab
   - Choose visualization source:
     - **Base Graph**: See all data
     - **View**: Select a specific view
     - **Custom Query**: Enter your own MATCH query
   - Adjust node limit (10-1000)
   - Click "🎨 Visualize"

4. **Interact**
   - Drag nodes to rearrange
   - Scroll to zoom
   - Click nodes to see details
   - Click "💾 Export PNG" to save

### Via Python Client

```python
from python-client.pgview_client import PGViewClient

client = PGViewClient()
client.connect('pg')
client.use_graph('DummyGraph')

# Query data for visualization
result = client.query(
    'MATCH (p:Person)-[r:knows]->(p2:Person) FROM g RETURN (p),(r),(p2)'
)
print(result)
```

## 🔧 Sample Operations

### Creating a Graph

**Via Web UI:**
1. Go to "📊 Graphs" tab
2. Enter graph name: `MyGraph`
3. Click "Create Graph"

**Via Python:**
```python
client = PGViewClient()
client.connect('pg')
client.create_graph('MyGraph')
client.use_graph('MyGraph')
```

**Via GQL Script:**
```gql
connect pg;
create graph MyGraph;
use MyGraph;
```

### Adding Schema

**Via Web UI:**
1. Go to "🏗️ Schema" tab
2. Add node type: `Person`
3. Add edge type: `knows` (Person → Person)

**Via Python:**
```python
client.add_schema_node('Person')
client.add_schema_edge('knows', 'Person', 'Person')
```

**Via GQL:**
```gql
add node Person;
add edge knows Person Person;
```

### Inserting Data

**Via Web UI:**
1. Go to "📝 Data" tab
2. Enter command: `insert N(1, "Person");`
3. Enter command: `insert E(100, 1, 2, "knows");`

**Via Python:**
```python
# Insert nodes (note: use N not N_g)
client.execute('insert N(1, "Person");')
client.execute('insert N(2, "Person");')

# Insert edge
client.execute('insert E(100, 1, 2, "knows");')

# Insert properties
client.execute('insert NP(1, "name", "Alice");')
client.execute('insert NP(1, "age", "30");')
```

### Querying Data

**Via Web UI:**
1. Go to "🔍 Query" tab
2. Enter query: `MATCH (p:Person) FROM g RETURN (p)`
3. Click "Execute Query"

**Via Python:**
```python
result = client.query(
    'MATCH (p:Person)-[r:knows]->(p2:Person) FROM g RETURN (p),(r),(p2)'
)
print(result)
```

### Creating Views

**Via Web UI:**
1. Go to "👁️ Views" tab
2. Enter definition:
```gql
CREATE virtual VIEW FriendsView ON g (
  MATCH (p:Person)-[k:knows]->(p2:Person)
)
```
3. Click "Create View"

**Via Python:**
```python
view_def = """
CREATE virtual VIEW FriendsView ON g (
  MATCH (p:Person)-[k:knows]->(p2:Person)
)
"""
client.execute(view_def)
```

## 📁 Directory Structure

```
pg-view/
├── src/main/java/          # Java source code
│   └── edu/upenn/cis/db/
│       ├── graphtrans/      # Core graph transformation logic
│       │   ├── api/         # REST API (GraphViewAPI.java)
│       │   ├── parser/      # Query parsers
│       │   └── store/       # Backend stores (PostgreSQL, SimpleDatalog)
│       └── postgres/        # PostgreSQL JDBC wrapper
├── web-ui/
│   └── index.html           # Web UI with visualization
├── python-client/
│   ├── pgview_client.py     # Python client library
│   ├── example_knowledge_graph.py
│   └── requirements.txt
├── scripts/
│   ├── generate_dummy_data.py    # Generate sample data
│   ├── load_dummy_data.py        # Load data into DB
│   ├── create_sample_graph.gql   # Sample GQL script
│   └── sample_queries.gql        # Example queries
├── conf/
│   └── graphview.conf       # Configuration file
├── pom.xml                  # Maven configuration
└── README.md                # Main documentation
```

## 🔌 API Endpoints

### Core Operations
- `POST /connect` - Connect to platform (pg, sd, lb, n4)
- `POST /graph/create` - Create new graph
- `POST /graph/use` - Select graph to use
- `GET /graphs` - List all graphs
- `GET /status` - Get system status

### Schema Management
- `POST /schema/node` - Add node type
- `POST /schema/edge` - Add edge type
- `GET /schema` - Get current schema

### Data Operations
- `POST /execute` - Execute GQL command
- `POST /query` - Execute query
- `POST /data/insert` - Insert data

### View Management
- `POST /view/create` - Create view
- `GET /views` - List all views
- `GET /program` - Get Datalog program

## 🐛 Troubleshooting

### API Server Won't Start
```bash
# Check if port 7070 is in use
netstat -tuln | grep 7070

# Kill existing process if needed
pkill -f "exec:java@api"

# Restart
mvn exec:java@api
```

### PostgreSQL Connection Issues
```bash
# Check PostgreSQL is running
sudo service postgresql status

# Test connection
psql -U postgres -h 127.0.0.1 -p 5432

# Check pg_hba.conf authentication
sudo cat /etc/postgresql/14/main/pg_hba.conf | grep "local.*postgres"
# Should show: local   all   postgres   md5
```

### Web UI Not Loading
```bash
# Check if port 8080 is in use
netstat -tuln | grep 8080

# Restart web server
cd web-ui
python3 -m http.server 8080
```

### CORS Errors in Browser
The API includes CORS headers. If you still see errors:
1. Clear browser cache
2. Restart API server
3. Check browser console for specific error

### Data Not Persisting
- Ensure you're connected to PostgreSQL (`pg`) not Simple Datalog (`sd`)
- Simple Datalog stores data in memory only
- Check status endpoint: `curl http://localhost:7070/status`

## 📚 Additional Resources

- **Paper**: See `DBMSPaper.pdf` for implementation details
- **API Documentation**: See `API_QUICK_START.md`
- **Python Client**: See `python-client/README.md`
- **Implementation Details**: See `Implementation_detail.md`

## 🎯 Example Workflows

### Workflow 1: Quick Test
```bash
# Start servers (2 terminals)
mvn exec:java@api
python3 -m http.server 8080 --directory web-ui

# Load sample data
python3 scripts/load_dummy_data.py

# Open browser
xdg-open http://localhost:8080
```

### Workflow 2: Python Integration
```python
from python-client.pgview_client import PGViewClient

# Setup
client = PGViewClient()
client.connect('pg')
client.create_graph('KnowledgeGraph')
client.use_graph('KnowledgeGraph')

# Add schema
client.add_schema_node('Person')
client.add_schema_node('Company')
client.add_schema_edge('works_at', 'Person', 'Company')

# Insert data
client.execute('insert N(1, "Person");')
client.execute('insert N(2, "Company");')
client.execute('insert E(100, 1, 2, "works_at");')

# Query
result = client.query('MATCH (p:Person)-[r]->(c:Company) FROM g RETURN (p),(r),(c)')
print(result)
```

### Workflow 3: View and Visualize
1. Create data (via any method above)
2. Create view in Web UI "👁️ Views" tab
3. Go to "📈 Visualize" tab
4. Select "View" as source
5. Choose your view from dropdown
6. Click "🎨 Visualize"
7. Export as PNG

## 🔐 Security Notes

- Default PostgreSQL password is `postgres@` - **Change in production!**
- API has no authentication - **Add auth for production use**
- CORS is set to `*` - **Restrict origins in production**

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review API logs: `tail -f /tmp/api_server.log`
3. Check PostgreSQL logs: `sudo tail -f /var/log/postgresql/postgresql-14-main.log`

## 🎉 Success Checklist

- [ ] API server running on port 7070
- [ ] Web UI accessible on port 8080
- [ ] PostgreSQL connected and working
- [ ] Sample data loaded (500 nodes, 3000+ edges)
- [ ] Can visualize graphs in browser
- [ ] Can execute queries
- [ ] Can create views
- [ ] Can switch between platforms (pg/sd)

---

**Ready to start? Run the quick start commands above!** 🚀

