# PG-View Java Implementation: Detailed Overview

This document summarizes the architecture and major flows of the PG-View prototype, with direct references to concrete classes and files in the repository so you can navigate the code quickly.


## Build & Project

- **Build tool**: Maven (`pom.xml`)
  - Java 11 via `maven-compiler-plugin` `<source>11</source>` `<target>11</target>`.
  - ANTLR 4.8 (runtime + plugin) for parsers; sources in `src/main/antlr4/` (added via `build-helper-maven-plugin`).
  - Key deps: PostgreSQL JDBC, Neo4j 4.4 core/bolt/APOC, Log4j2, SLF4J (simple), Jackson (YAML/databind), OpenCSV, Commons IO/Lang, Z3, JSON-simple, ini4j, Jetty.
  - Exec entries (`exec-maven-plugin`): mains include `edu.upenn.cis.db.graphtrans.Client`, `...experiment.*` and helpers for Neo4j/Postgres experiments.


## Entry and Console

- **Main**: `src/main/java/edu/upenn/cis/db/graphtrans/Client.java`
  - Parses `-i/--input` (script) and `-c/--config` paths.
  - Loads config via `Config.load(...)`, initializes defaults, then `GraphTransServer.initialize()`.
  - Sets workspace and performance tracking, creates a `Console`, and calls `CommandExecutor.readCommand(filepath)`.

- **Executor**: `src/main/java/edu/upenn/cis/db/graphtrans/CommandExecutor.java`
  - Parses and runs commands through `CommandParser` (`run(...)`, `readCommand(...)`).
  - Connection lifecycle: `connect(String platform)`, `disconnect()`, `useGraph(String)`, `createGraph(String)`, `dropGraph(String)`, `listGraphs()`.
  - Schema ops: `addSchemaNode(...)`, `addSchemaEdge(...)`, `printSchema()`.
  - Constraints: `addEgd(String)`, `printEgds()`.
  - View ops: `createView(...)` builds view rules, type-checks, and delegates to the active `Store`.
  - Indexing: `createIndex(Config.IndexType, String)` generates SSR rules and materializes them (via `SSR.populateSSRRulesForAll(...)`).
  - Querying: `query(String)` parses, rewrites (via `Rewriter`, optional SSR rewriter), and executes via the `Store`.


## Configuration and Predicates

- **Config**: `src/main/java/edu/upenn/cis/db/graphtrans/Config.java`
  - Relation name constants: base (`N_g`, `E_g`, `NP_g`, `EP_g`), view (`N_<v>`, `E_<v>`, ...), helpers (`MATCH_*`, `MAP_*`, `DMAP`, `GENNEWID_*`).
  - Platform and feature flags: Postgres/Neo4j/SimpleDatalog toggles, type checking, pruning, SSR/IVM flags, etc.
  - Interpreted predicates: `=`, `>`, `<`, `>=`, `<=`, `!=` as `Predicate`s for Datalog.


## Global Server and Catalog Hooks

- **GraphTransServer**: `src/main/java/edu/upenn/cis/db/graphtrans/GraphTransServer.java`
  - Global state: `DatalogProgram program`, registered `TransRuleList`s, EGD list, SSR metadata.
  - Stores: sets `baseStore` to `SimpleDatalogStore` and (optionally) `store` to `PostgresStore` depending on `Config` flags.


## Datalog Core

- **DatalogProgram**: `src/main/java/edu/upenn/cis/db/datalog/DatalogProgram.java`
  - Holds predicates, EDB/IDB/UDF sets, rules, creation order, and Postgres index hints.
  - `addRule(DatalogClause)`: normalizes constants into equality atoms, enforces bound head variables, and tracks `GENNEWID_*` usage per EDB.
  - `getString()`/`toString()`: dump constructors and rules for debugging.

- **DatalogClause**: `src/main/java/edu/upenn/cis/db/datalog/DatalogClause.java`
  - Represents a rule or query with a single head or multiple heads and a body.
  - `normalizeVarNames()` provides stable naming for universally/existentially quantified variables.


## View Rule Generation (high-level)

- At view creation time, `CommandExecutor.populateDatalogProgramForView(TransRuleList)` delegates to `graphdb.datalog.ViewRule.addViewRuleToProgram(...)` to convert transformation rules into Datalog over base/view relations.
- View EDBs and index sets are registered to `DatalogProgram` so backends can materialize or query efficiently.


## Stores (Backend Implementations)

- **Interface**: `src/main/java/edu/upenn/cis/db/graphtrans/store/Store.java`
  - Abstractions for schema creation, view creation, running queries, adding tuples, indexing, and DB lifecycle.

- **Simple Datalog (in-memory)**: `src/main/java/edu/upenn/cis/db/graphtrans/store/simpledatalog/SimpleDatalogStore.java`
  - Uses `SimpleDatalogEngine` to execute rules and queries in memory.
  - `createView(DatalogProgram, TransRuleList)`: executes each head’s rules in creation order.

- **Postgres**: `src/main/java/edu/upenn/cis/db/graphtrans/store/postgres/PostgresStore.java`
  - `createSchema(...)` creates SQL tables for EDBs (and indexes on base relations).
  - `getSqlForDatalogClause(...)` translates a `DatalogClause` into SQL by:
    - Handling positive atoms (FROM/CROSS JOIN with equality WHEREs),
    - Interpreted atoms (comparisons/equalities into WHERE),
    - Head projection (SELECT DISTINCT with aliases),
    - Negative atoms via LEFT JOIN composition with appropriate WHERE conditions.
  - `createView(...)` builds `CREATE VIEW` or IVM-style `INSERT INTO` statements and optional triggers for SSR/IVM.

- **Neo4j (embedded)**: `src/main/java/edu/upenn/cis/db/graphtrans/store/neo4j/Neo4jStore.java`
  - Starts an embedded server (`Neo4jServerThread`), supports Cypher execution.
  - `createView(DatalogProgram, TransRuleList)`: generates Cypher via `graphdb.neo4j.TranslatorToCypher.getCypherForCreateView(...)` and executes statements.
  - `getQueryResult(String)`: translates a user query string to Cypher and runs it.


## Command Flow Highlights

- **Create Graph**: `CommandExecutor.createGraph(...)`
  - Clears `Schema`, creates DB via `Store`, adds base relations (`BaseRuleGen.addRule()`), calls `Store.createSchema(...)` for each EDB.

- **Create View**: `CommandExecutor.createView(...)`
  - Optional type checking (`checkRuleValidity` uses `typechecker.RuleOverlapCheck` and `typechecker.OutputViewCheck`).
  - Builds view rules in `DatalogProgram` and calls `Store.createView(...)`.
  - Neo4j mode adjusts between copy/update vs overlay semantics via `Config` flags.

- **Create Index (SSR)**: `CommandExecutor.createIndex(...)`
  - Populates SSR rules (`SSR.populateSSRRulesForAll(...)`), registers rewriting rules, and materializes index relations in the backend.

- **Query**: `CommandExecutor.query(String)`
  - Parses into a `DatalogClause` (via `parser.QueryParser`), may apply SSR rewriting (`datalog.QueryRewriterSubstitution`), rewrites to an executable program (`rewriter.Rewriter`), and delegates to `Store.getQueryResult(...)`.


## Notes for Navigation

- Entry and orchestration: `graphtrans/Client.java`, `graphtrans/CommandExecutor.java`, `graphtrans/GraphTransServer.java`, `graphtrans/Config.java`.
- Datalog model and engine: `datalog/DatalogProgram.java`, `datalog/DatalogClause.java`, `graphtrans/store/simpledatalog/*`.
- SQL backend: `graphtrans/store/postgres/PostgresStore.java`.
- Neo4j backend: `graphtrans/store/neo4j/Neo4jStore.java`, `graphtrans/graphdb/neo4j/*`.
- Rule generation and base schema: `graphtrans/graphdb/datalog/*`.
- Parsers and command grammar: `src/main/antlr4/` and `graphtrans/parser/*`.


## Parsing and Grammars

- **Command parsing**: `graphtrans/parser/CommandParser.java`
  - Visits `GraphTransQueryParser.cmd` and dispatches to `CommandExecutor` methods like `connect()`, `createGraph()`, `useGraph()`, `createView()`, `query()`.
  - Handles options toggling (e.g., `typecheck`, `prunetypecheck`, `ivm`).

- **View parsing**: `graphtrans/parser/ViewParser.java`
  - `visitCreate_view(...)` extracts `viewName`, `baseName` (default `g`), `viewType` (`virtual` default), and `WITH DEFAULT MAP` flag.
  - Builds a `TransRuleList` and parses each `TRANS RULE` via `TransRuleParser`.

- **Transformation rule parsing**: `graphtrans/parser/TransRuleParser.java`
  - MATCH: builds `N(...)` and `E(...)` atoms (supports negated edges `!(...)`). Label regex on edges is allowed; single-node terms must carry a label in MATCH.
  - WHERE: pushes interpreted atoms (comparisons) and Skolem functions into `TransRule` (e.g., `SET x = SK("name", v1, v2)`).
  - CONSTRUCT: collects new nodes/edges and identifies new vars; populates `patternAdd` and `patternConstruct`.
  - MAP FROM ... TO ...: fills `mapFromToMap` for variable merges.
  - REMOVE: records node/edge vars to delete.

- **Query parsing**: `graphtrans/parser/QueryParser.java`
  - Parses MATCH/WHERE/RETURN/FROM into a `DatalogClause` with `_` head (via `Config.relname_query`).
  - Appends `_<from>` suffix to base/view relations to scope to a graph or view.
  - WHERE conditions on properties are normalized to interpreted atoms over `NP`/`EP` via `ParserHelper`.

- **EGD parsing**: `graphtrans/parser/EgdParser.java`
  - LHS supports `N`, `E`, `NP`, `EP` atoms; RHS supports equality atoms only.


## Base and Catalog Predicates

- **BaseRuleGen**: `graphtrans/graphdb/datalog/BaseRuleGen.java`
  - Catalog EDBs: `CATALOG_VIEW(name,base,type,rule,level)`, `CATALOG_INDEX(view,type,label)`, `CATALOG_SINDEX(view,query)`, `EGD(constraint)`, and schema relations `N_schema`, `E_schema`.
  - Base graph EDBs: `N_g(id,label)`, `E_g(id,from,to,label)`, `NP_g(id,prop,val)`, `EP_g(id,prop,val)`.


## View Rule Generation (deeper)

- **Entry**: `graphtrans/graphdb/datalog/ViewRule.addViewRuleToProgram(...)`
  - Initializes per-view symbols (`MAP_<v>`, `DMAP_<v>`, `N_ADD_<v>`, `E_DEL_<v>`, `N_<v>`, `E_<v>`, etc.).
  - For each rule: `addMatchRule`, `addMapRules`, `addAddRemoveRules`. Optionally derives default `DMAP`/`E_<v>` rules.

- **Match rules**: `addMatchRule(...)`
  - Head: `MATCH_<view>_<i>(vars)` from all non-path variables in MATCH.
  - Body: base atoms from MATCH with `_<base>` suffix, filtering out `NP/EP` and some interpreted atoms when `useWhereClause=false`.
  - Path labels: if an edge label is a regex like `(AB)*`, generates recursion via a synthetic `REC_<id>` relation to encode reachability over `E_g`/`N_g`.
  - Adds per-column index hints to `DatalogProgram` for `MATCH_*`.

- **Map rules and constructors**: `addMapRules(...)`
  - Emits per-construct-atom heads under view relations (`N_<v>`, `E_<v>`) with label equalities in the body.
  - For Skolemized ids, injects UDF atoms `GENNEWID_MAP_<view>_<name>(...,id)` and constructor rules `GENNEWID_CONST_<view>_<name>(...)` + `GENNEWID_<view>(id)`; registers constructors for LogicBlox.

- **Add/Remove rules**: `addAddRemoveRules(...)`
  - Creates `N_DEL_<v>(id)`/`E_DEL_<v>(id)` heads when `DELETE` lists vars.

- **Default view rules**: `addViewRules()`
  - `DMAP_<v>(src,dst)` identity mapping and copy from explicit `MAP_<v>` if present.
  - `N_<v>(id,label) <- N_g(id,label), DMAP_<v>(id,_) , !N_DEL_<v>(id)`
  - `E_<v>(id,src2,dst2,label) <- E_g(id,src,dst,label), DMAP_<v>(src,src2), DMAP_<v>(dst,dst2), !E_DEL_<v>(id)`.


## SSR (Substitution Subgraph Relations)

- **Rules creation**: `datalog/SSR.java`
  - Non-default mapping: build a single `INDEX_<view>(vars)` over the MATCH pattern with parallel rewriting rule from view atoms.
  - Default mapping: for each new var (`newNodeVars`/`newEdgeVars`), `createSSRForNewVariable` produces:
    - Constructor rules for GENNEWID and MAP UDFs.
    - `INDEX_<view>_<i>(...)` head with base MATCH atoms plus `GENNEWID_MAP_*` atom for the new var.
    - Rewriting rule using `N_<view>`/`E_<view>` atoms to cover the constructed subgraph.
  - Property index rules: for each map `dst <- src`, add `INDEX_<view>_<i>_NP(dst,key,value) <- NP_g(src,key,value), INDEX_<view>_<i>(...)`.

- **Coveredness**: `isCoveringIndex(...)` delegates to `SSRHelper.testCoverednessOnSchemas(...)` against schema and SSR graphs in `GraphTransServer.getBaseStore()`.

- **Exposure**: `getRulesForCreation()` and `getRulesForRewriting()` feed `CommandExecutor.createIndex(...)` for materialization and query rewriting, respectively.


## Query Rewriting with SSR

- **Rewriter (structural)**: `datalog/rewriter/Rewriter.java`
  - Unfolds positive/negative IDBs and UDFs into a flat body using handler helpers, and emits `QUERY(u0,...) <- ...` in a fresh `DatalogProgram`.

- **Substitution-based**: `datalog/QueryRewriterSubstitution.java`
  - Builds a canonical database in workspace `"_QUERY_REWRITER"` using `GraphTransServer.getBaseStore()` (schemas for `N_<from>`/`E_<from>`).
  - Inserts canonical tuples for each positive body atom; runs each SSR rewriting rule against this DB.
  - For each match, emits an `INDEX_*` atom; tracked vars inform rewriting of `NP_*`/`EP_*` property atoms to `INDEX_*_NP/EP`.
  - Remaining atoms that were not covered stay as-is; returns a single `DatalogClause` with the original head.


## Type Checking

- **Rule overlap**: `typechecker/RuleOverlapCheck.java`
  - Z3 Fixedpoint encodes `N/E/NP/EP` and colored variants (`NCOLOR/ECOLOR`).
  - Adds default EGDs (unique labels per id) and user EGDs (`Egd` via `EgdParser`).
  - For each rule pair, asserts the existence of an overlap on affected variables; UNSAT => well-behaved, SAT => reports a violating pair.

- **Output validity**: `typechecker/OutputViewCheck.java`
  - Intended to encode EGDs over before/after (`N/E` vs `N1/E1`) to ensure transformed outputs satisfy constraints. Currently short-circuits to `true` (see early `if (1==1)`), leaving the detailed encoding in place for future enablement.


## Backends: Postgres and Simple Datalog (details)

- **Postgres translation**: `store/postgres/PostgresStore.java`
  - Positive atoms: tables aliased `R<i>`; equi-joins are synthesized from shared variables.
  - Interpreted atoms: comparisons become `WHERE` predicates; var-var equality supported.
  - Negative atoms: compiled to `LEFT JOIN ... ON ...` with additional `IS NOT NULL` filters.
  - Head projection: `SELECT DISTINCT` over aliases; for `GENNEWID_MAP_*` the head uses `GENNEWID_CONST('<skolem>', VARIADIC Array[...]) AS _i`.
  - IVM: can create tables and triggers for `INDEX_*` relations; example trigger hooks on `e_g` inserts.

- **In-memory engine**: `store/simpledatalog/SimpleDatalogStore.java`, `datalog/simpleengine/*`
  - `SimpleDatalogEngine.executeRule(...)`: expands interpreted atoms, orders body (UDFs and negations last), joins (`Relation.join`) with equality pushdown, then `filter(...)` on remaining predicates.
  - Negation: `Relation.notin(...)` performs anti-semi-join.
  - Head processing: emits tuples; recognizes `GENNEWID_CONST_*` to materialize `GENNEWID_MAP_*` relations with fresh ids (`SimpleDatalogEngine.getNewId()`).


## Data Structures

- **TransRule**: `graphtrans/datastructure/TransRule.java`
  - Holds `patternMatch`, `patternConstruct`, `patternBefore/After`, `patternAdd/Remove`, `patternAfterForIndexing`, affected var sets, star variables, label maps, `mapFromToMap`, `SkolemFunctionMap`, and delete sets.
  - `computePatterns()`: builds `patternAfterForIndexing` by applying mappings, additions, and deletions; tracks `affectedVariables`.

- **TransRuleList**: `graphtrans/datastructure/TransRuleList.java`
  - Aggregates rules with metadata (`viewName`, `baseName`, `viewType`, `level`, `isDefaultMap`), plus SSR metadata (`indexType`, `indexRuleList`, `outputPatternList`).

- **Egd**: `graphtrans/datastructure/Egd.java` stores LHS/RHS atoms for EGDs.


## Neo4j Translator

- **Cypher generation**: `graphtrans/graphdb/neo4j/TranslatorToCypher.java`
  - Modes via `Neo4jViewMode`: `COPY_AND_UPDATE`, `UPDATE_IN_PLACE`, `OVERLAY` selected from `TransRuleList.viewType`.
  - Non-default mapping:
    - COPY_AND_UPDATE: clones subgraph via `apoc.refactor.cloneSubgraph`, tags with view name.
    - OVERLAY: annotates matched nodes with a view tag; for default map, uses `apoc.periodic.iterate` to apply MAP/DELETE/CONSTRUCT in batches.
  - MAP in UPDATE_IN_PLACE: `apoc.refactor.mergeNodes(...)` followed by `apoc.create.setLabels(...)` to set target labels.
  - OVERLAY CONSTRUCT/DELETE: `CREATE` new nodes/edges with `{c:1,d:99}` flags; mark sources/old edges with `d=1`.
  - Query translation delegates to `QueryToCypherParser.getCypher(...)` using stored view mode/flags.


## Notes and Constraints

- **Single-node terms** in `QueryParser` must carry a label; otherwise an exception is thrown.
- **Anonymous `_`** variables are expanded appropriately by parsers; `TransRuleParser` creates synthetic placeholders when `_` appears in MATCH.
- **Path regex** in `ViewRule`: recursive Datalog (`REC_*`) encodes sequences like `(AB)*` using `E_g`/`N_g`.
