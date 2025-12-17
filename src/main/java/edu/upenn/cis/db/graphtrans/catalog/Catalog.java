package edu.upenn.cis.db.graphtrans.catalog;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.logicblox.connect.BloxCommand.Relation;
import com.logicblox.connect.ConnectBlox.Response;

import edu.upenn.cis.db.datalog.DatalogClause;
import edu.upenn.cis.db.datalog.DatalogParser;
import edu.upenn.cis.db.datalog.DatalogProgram;
import edu.upenn.cis.db.datalog.simpleengine.SimpleTerm;
import edu.upenn.cis.db.datalog.simpleengine.Tuple;
import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.graphtrans.datastructure.TransRuleList;
import edu.upenn.cis.db.graphtrans.graphdb.datalog.BaseRuleGen;
import edu.upenn.cis.db.graphtrans.parser.EgdParser;
import edu.upenn.cis.db.graphtrans.parser.ViewParser;
import edu.upenn.cis.db.graphtrans.store.Store;
import edu.upenn.cis.db.graphtrans.store.StoreResultSet;
import edu.upenn.cis.db.helper.Util;
import edu.upenn.cis.db.logicblox.LogicBlox;

/**
 * Catalog class.
 * @author sbnet21
 *
 */
public class Catalog {
	final static Logger logger = LogManager.getLogger(Catalog.class);

	private static Store store;
	
	public static HashMap<String, ViewCatalog> loadViewCatalog() {
		HashMap<String, ViewCatalog> catalogs = new HashMap<String, ViewCatalog>();
		
		try {
			String query = "_(n,b,t,q,l) <- " + Config.relname_catalog_view + "(n,b,t,q,l)."; 
			System.out.println("[Catalog] loadViewCatalog query: " + query);
			DatalogParser parser = new DatalogParser(new DatalogProgram());
			DatalogClause q = parser.ParseQuery(query);
			System.out.println("[Catalog] Parsed query clause: " + q);
			
			StoreResultSet rs = store.getQueryResult(q);
			System.out.println("[Catalog] Result set: " + (rs != null ? "not null" : "null"));
			
			if (rs != null) {
				ArrayList<Tuple<SimpleTerm>> result = rs.getResultSet();
				int rows = result != null ? result.size() : 0;
				System.out.println("[Catalog] Found " + rows + " rows");
				for (int i = 0; i < rows; i++) {
					String name = result.get(i).getTuple().get(0).getString();
					String base = result.get(i).getTuple().get(1).getString();
					String type = result.get(i).getTuple().get(2).getString();
					String viewQuery = result.get(i).getTuple().get(3).getString();
					long level = result.get(i).getTuple().get(4).getLong();
					System.out.println("[Catalog] View from catalog: " + name + " (" + type + ")");
					catalogs.put(name, new ViewCatalog(name, base, type, viewQuery, level));
				}
			}
		} catch (Exception e) {
			System.out.println("[Catalog] Error loading view catalog: " + e.getMessage());
			e.printStackTrace();
		}
		return catalogs;
	}
	
	public static void loadSchemaNode() {
		String query = "_(x) <- " + Config.relname_node_schema + "(x).";
		DatalogParser parser = new DatalogParser(new DatalogProgram());
		DatalogClause q = parser.ParseQuery(query);
		
		StoreResultSet rs = store.getQueryResult(q);
		ArrayList<Tuple<SimpleTerm>> result = rs.getResultSet();
		int rows = result.size();
		for (int i = 0; i < rows; i++) {
			String label = result.get(i).getTuple().get(0).getString();
			CommandExecutor.addSchemaNode(true, label);
		}
	}

	public static void loadSchemaEdge() {
		String query = "_(f,t,l) <- " + Config.relname_edge_schema + "(f,t,l).";
		DatalogParser parser = new DatalogParser(new DatalogProgram());
		DatalogClause q = parser.ParseQuery(query);
		
		StoreResultSet rs = store.getQueryResult(q);
		ArrayList<Tuple<SimpleTerm>> result = rs.getResultSet();
		int rows = result.size();
		for (int i = 0; i < rows; i++) {
			String from = result.get(i).getTuple().get(0).getString();
			String to = result.get(i).getTuple().get(1).getString();
			String label = result.get(i).getTuple().get(2).getString();
			CommandExecutor.addSchemaEdge(true, label, from, to);
		}
	}
	
	public static void loadEgdList() {
		// FIXME: currently use only one global edge list for base graph
		String query = "_(x) <- " + Config.relname_egd + "(x).";
		DatalogParser parser = new DatalogParser(new DatalogProgram());
		DatalogClause q = parser.ParseQuery(query);
		
		StoreResultSet rs = store.getQueryResult(q);
		ArrayList<Tuple<SimpleTerm>> result = rs.getResultSet();
		int rows = result.size();
		for (int i = 0; i < rows; i++) {
			String egd = result.get(i).getTuple().get(0).getString();
	        String egdRemoveSlashed = Util.removeSlashes(egd);

			GraphTransServer.getEgdList().add(EgdParser.Parse(egdRemoveSlashed));
		}
	}	
	
	public static void loadIndexList() {
		// FIXME: currently use only one global edge list for base graph
		String query = "_(view, type, label) <- " + Config.relname_catalog_index + "(view, type, label).";
		DatalogParser parser = new DatalogParser(new DatalogProgram());		
		DatalogClause q = parser.ParseQuery(query);
		
		StoreResultSet rs = store.getQueryResult(q);
		ArrayList<Tuple<SimpleTerm>> result = rs.getResultSet();
		int rows = result.size();
		GraphTransServer.getIndexList().clear();
		for (int i = 0; i < rows; i++) {
			String viewName = result.get(i).getTuple().get(0).getString();
			String type = result.get(i).getTuple().get(1).getString();
			String label = result.get(i).getTuple().get(2).getString();
			GraphTransServer.addIndexLabel(viewName, type.equals("N"), label);
		}
//		System.out.println("IndexList loaded: " + GraphTransServer.getIndexList());
	}	

	public static void loadSIndexList() {
		// FIXME: currently use only one global edge list for base graph
		String query = "_(view, query) <- " + Config.relname_catalog_sindex + "(view, query).";
		DatalogParser parser = new DatalogParser(new DatalogProgram());		
		DatalogClause q = parser.ParseQuery(query);
		
		StoreResultSet rs = store.getQueryResult(q);
		ArrayList<Tuple<SimpleTerm>> result = rs.getResultSet();
		int rows = result.size();
		GraphTransServer.getIndexList().clear();
		for (int i = 0; i < rows; i++) {
			String viewName = result.get(i).getTuple().get(0).getString();
			String queryStr = result.get(i).getTuple().get(1).getString();
			GraphTransServer.addSIndexMap(viewName, queryStr);
		}
//		System.out.println("IndexList loaded: " + GraphTransServer.getIndexList());
	}	
	
	
//	private static void loadBaseSchema() {
//		BaseRuleGen.addBaseGraphRuleBaseEDB(false);
//		
//		for (int BaseRuleGen.getPreds();
//		
//		String logic = BaseRuleGen.getBaseGraphRuleBaseEDB(false);		
//		DatalogParser parser = new DatalogParser(GraphTransServer.getProgram());
//		parser.ParseAndAddRules(logic);
//	}
	
	public static void load(Store s) {
		store = s;
		Schema.clear();
		GraphTransServer.getEgdList().clear();
//		loadBaseSchema();
		System.out.println("[Catalog] Loading view catalog from database...");
		HashMap<String, ViewCatalog> views = loadViewCatalog();
		System.out.println("[Catalog] Found " + views.size() + " views in catalog");
		
		for (HashMap.Entry<String, ViewCatalog> entry : views.entrySet()) {
			try {
				ViewCatalog viewCatalog = entry.getValue();
				System.out.println("[Catalog] Loading view: " + viewCatalog.getViewName());
				
				ViewParser parser = new ViewParser();
				String createViewQuery = viewCatalog.getQuery();
				System.out.println("[Catalog] Query: " + createViewQuery);
				TransRuleList transRuleList = parser.Parse(createViewQuery);

				if (transRuleList != null) {
					CommandExecutor.createView(true, createViewQuery, transRuleList);
					System.out.println("[Catalog] Successfully loaded view: " + viewCatalog.getViewName());
				} else {
					System.out.println("[Catalog] Failed to parse view: " + viewCatalog.getViewName());
				}
			} catch (Exception e) {
				System.out.println("[Catalog] Error loading view " + entry.getKey() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
		loadEgdList();
		loadIndexList();
		loadSIndexList();
		loadSchemaNode();
		loadSchemaEdge();
		System.out.println("[Catalog] Catalog loading complete");
	}	
}
