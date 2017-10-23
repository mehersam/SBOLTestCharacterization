package class_iterations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.sbolstandard.core2.Annotation;
import org.sbolstandard.core2.Collection;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Cut;
import org.sbolstandard.core2.FunctionalComponent;
import org.sbolstandard.core2.GenericLocation;
import org.sbolstandard.core2.Interaction;
import org.sbolstandard.core2.Location;
import org.sbolstandard.core2.MapsTo;
import org.sbolstandard.core2.Model;
import org.sbolstandard.core2.Module;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.Range;
import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SequenceAnnotation;
import org.sbolstandard.core2.SequenceConstraint;
import org.sbolstandard.core2.TopLevel;

public class count_classes {

	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static String file_header = "";
	private static final String file_name = "count_classes.csv";
	private static FileWriter fileWriter = null;
	// map class to number of times it appears in each file
	private static HashMap<String, Integer> class_counts = new HashMap<String, Integer>();
	private static HashMap<String, HashMap<String, Integer>> class_counts_2d = null;
	private static Set<String> list_of_files = new HashSet<String>();
	private static Set<HashSet<String>> clusters = new HashSet<HashSet<String>>(); 
	private static HashSet<String> keys = new HashSet<String>();

	public static void main(String[] args)
			throws SBOLValidationException, IOException, SBOLConversionException, URISyntaxException {
		initialize_classes();
		fileWriter = new FileWriter(file_name);
		fileWriter.append(COMMA_DELIMITER);
		file_header = "";
		for (String data_type : class_counts.keySet())
			file_header += data_type + ",";

		fileWriter.append(file_header.toString());
		class_counts_2d = new HashMap<String, HashMap<String, Integer>>();
		// for each file in the examples folder, call count_classes_test
		for (File f : new File("./SBOL2/").listFiles()) {
			initialize_classes(); // resets the data types' count to 0
			count_classes_test(f); // counts instances of each data type
			class_counts_2d.put(f.getName(), class_counts); // associates file
															// with the counts
															// of each type
			list_of_files.add(f.getName()); // list of total files
		}
		try {
			fileWriter.flush();
			fileWriter.close();
		} catch (Exception e) {
			System.out.println("Attempted to flush and close writer");
			e.printStackTrace();
		}
		create_clusters();

	}

	private static void create_clusters() throws SBOLValidationException, IOException, SBOLConversionException {
		HashMap<HashSet<String>, Set<String>> data_types_clusters = new HashMap<HashSet<String>, Set<String>>();

		while (list_of_files.size() != 0) {
			int rand = new Random().nextInt(list_of_files.size());
			String file = (String) list_of_files.toArray()[rand];
			list_of_files.remove(file);
			HashSet<String> cluster_to_add = new HashSet<String>();
			Set<String> data_types = new HashSet<String>();
			cluster_to_add.add(file);

			for (String type : class_counts_2d.get(file).keySet()) {
				if (class_counts_2d.get(file).get(type) != 0)
					data_types.add(type);
			}
			HashSet<String> files_to_remove = new HashSet<String>();
			for (String s : list_of_files) // check against every other file
			{
				//get the data for the file chosen g
				HashMap<String, Integer> set_given = class_counts_2d.get(file); 
				//get the data for every other file
				HashMap<String, Integer> set_to_check = class_counts_2d.get(s); 

				Boolean flag = true;
				for (String dt : set_given.keySet()) // for each data type
				{
					if (set_given.get(dt) != 0 && set_to_check.get(dt) == 0) {
						flag = false;
						break;
					} else if (set_given.get(dt) == 0 && set_to_check.get(dt) != 0) {
						flag = false;
						break;
					}
				}

				if (flag) {
					cluster_to_add.add(s);
					files_to_remove.add(s);
				}
			}
			clusters.add(cluster_to_add);
			list_of_files.removeAll(files_to_remove);
			//files and then the data_types for that group of files
			data_types_clusters.put(cluster_to_add, data_types); 
		}

		// create actual cluster objects and put them in a list to make graph
		// from
		int id_count = 1;
		ArrayList<cluster> nodes = new ArrayList<cluster>();
		for (HashSet<String> s : data_types_clusters.keySet()) {
			nodes.add(new cluster(id_count, data_types_clusters.get(s), s));
			id_count++;
		}

		make_graph(nodes);

	}

	private static void make_graph(ArrayList<cluster> _nodes) throws IOException {
		// have all the clusters, must create connections between them
		for (int i = 0; i < _nodes.size(); i++) // parent
		{
			for (int j = 0; j < _nodes.size(); j++) // child
			{
				if (_nodes.get(i) == _nodes.get(j)) // parent == child; same
													// node
					continue;
				//child not a subset of parent so no relation
				if (!_nodes.get(i).get_data_types().containsAll(_nodes.get(j).get_data_types())) 
					continue;
				boolean flag = true;
				for (int k = 0; k < _nodes.size(); k++) // rest of clusters
				{
					//rest of clusters not parent or child
					if (_nodes.get(k) == _nodes.get(j) || _nodes.get(k) == _nodes.get(i)) { 
						continue;
					}
					//if other is a subset of parent
					if (_nodes.get(i).get_data_types().containsAll(_nodes.get(k).get_data_types())) 
						//parent and child don't have direct relations
						if (_nodes.get(k).get_data_types().containsAll(_nodes.get(j).get_data_types())) { 
							flag = false;
							break;
						}
				}
				if (flag)
					_nodes.get(i).add_connections(_nodes.get(j));
				//parent and child have a direct connection so set parent's connections to have child
			}

		}
		draw_graph(_nodes);
		data(_nodes);
	}

	private static String which_type(cluster c) {
		Set<String> data_types = c.get_data_types();

		if (data_types.size() > 0) {
			
			// structural
			if ((!data_types.contains("ModuleDefinition") && !data_types.contains("Model"))
				 && !data_types.contains("Collection") && !data_types.contains("GenericTopLevel"))
				return "yellow";

			// functional
			if ((!data_types.contains("Sequence") && !data_types.contains("ComponentDefinition"))
					&& !data_types.contains("Collection") && !data_types.contains("GenericTopLevel"))
				return "green";

			// auxiliary classes only
			if ((data_types.contains("Collection") || data_types.contains("GenericTopLevel"))
					&& (!data_types.contains("Sequence") && !data_types.contains("ComponentDefinition")
					&& !data_types.contains("ModuleDefinition") && !data_types.contains("Model"))) {
					return "royalblue";
				}
			// structural && functional
			if ((data_types.contains("ModuleDefinition") || data_types.contains("Model"))
					&& (data_types.contains("Sequence") || data_types.contains("ComponentDefinition")))
				return "salmon";
		}

		return "yellow";// "violet";

	}

	private static void draw_graph(ArrayList<cluster> _nodes) throws IOException {
		System.out.println("number of nodes: " + _nodes.size());
		data(_nodes);
		File f = new File("redo.dot");
		File data = new File("class_relation_data.txt");

		if (!f.exists() || !data.exists()) {
			f.createNewFile();
			data.createNewFile();
		}

		FileWriter fw = new FileWriter(f.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		FileWriter fw_data = new FileWriter(data.getAbsoluteFile());
		BufferedWriter bw_data = new BufferedWriter(fw_data);

		bw.write("digraph G {\n\n");
		bw.write(_nodes.size() + 1 + " [label=root");
		bw.write("]");
		bw.write("\n\n");

		for (cluster c : _nodes) {
			if (c.get_data_types().size() > 0) {
				bw.write(c.get_cluster_id() + " [label = ");
				bw.write((char) 34);
				bw.write(c.get_cluster_id() + "%n");
				bw.write(c.get_data_types().size() + "(" + c.get_files().size() + ")");
				bw.write((char) 34);
				String color = which_type(c);
				if (color != "")
					bw.write("color=" + color + " " + "style=filled");
				bw.write("]");
				bw.write("\n\n");
			}
		}

		// go through each relation
		for (cluster c : _nodes) {
			for (cluster conn : c.get_connections()) {
				// Check if conn.data_types.size > 0; otherwise don't print
				bw.write(c.get_cluster_id() + " -> " + conn.get_cluster_id() + "\n");
			}
			bw.write("\n");
		}
		bw.write("}");
		bw.close();

		for (cluster c : _nodes) {
			bw_data.write(c.get_cluster_id() + "");
			bw_data.write("\n");
			bw_data.write("File count: " + c.get_files().size() + "\n");
			bw_data.write("Files: ");
			for (String file : c.get_files())
				bw_data.write(file + ", ");
			bw_data.write("\n");
			bw_data.write("Connections: ");
			for (cluster conn : c.get_connections())
				bw_data.write(conn.get_cluster_id() + ", ");
			bw_data.write("\n");
			bw_data.write("Data Types: ");
			for (String type : c.get_data_types())
				bw_data.write(type + ", ");
			bw_data.write("\n************************************\n");

		}

		bw_data.close();
	}

	private static boolean is_source(ArrayList<cluster> _nodes, cluster c) {

		boolean source_flag = true;
		for (cluster node : _nodes)
			for (cluster conn : node.get_connections())
				if (conn.equals(c))
					source_flag = false;

		return source_flag;

	}

	private static void data(ArrayList<cluster> _nodes) {

		for (cluster c : _nodes) {
			System.out.println(c.get_cluster_id() + "\n");
			System.out.println("Files: ");
			for (String file : c.get_files())
				System.out.println(file + ", ");
			System.out.println("Connections: ");
			for (cluster conn : c.get_connections())
				System.out.println(conn.get_cluster_id() + ", ");
			System.out.println("Data Types: ");
			for (String type : c.get_data_types())
				System.out.println(type + ", ");
			System.out.println("\n************************************\n");

		}

	}

	private static void count_classes_test(File file)
			throws SBOLValidationException, IOException, SBOLConversionException {

		SBOLDocument doc = new SBOLDocument();

		// read serialized file into a document, then
		SBOLReader.setCompliant(false);
		SBOLReader.setURIPrefix("http://www.async.ece.utah.edu");
		SBOLReader.setVersion("");

		if (file.getName().contains(".xml"))
			doc = SBOLReader.read(file);

		// place toplevel objects
		// class_counts.put("TopLevel", doc.getTopLevels().size());
		class_counts.put("Collection", doc.getCollections().size());
		class_counts.put("ComponentDefinition", doc.getComponentDefinitions().size());
		class_counts.put("Model", doc.getModels().size());
		class_counts.put("ModuleDefinition", doc.getModuleDefinitions().size());
		class_counts.put("Sequence", doc.getSequences().size());
		class_counts.put("GenericTopLevel", doc.getGenericTopLevels().size());

		for (TopLevel TL : doc.getTopLevels()) {
			class_counts.put("Annotation", class_counts.get("Annotation") + TL.getAnnotations().size());
//			for (Annotation a : TL.getAnnotations())
//				put_annotations(a);
		}

//		for (Collection c : doc.getCollections()) {
//			for (Annotation a : c.getAnnotations())
//				put_annotations(a);
//		}

		for (ComponentDefinition cd : doc.getComponentDefinitions()) {
//			for (Annotation a : cd.getAnnotations()) {
//				put_annotations(a);
//			}

			class_counts.put("Component", class_counts.get("Component") + cd.getComponents().size());
			for (Component c : cd.getComponents()) {
//				for (Annotation a : c.getAnnotations())
//					put_annotations(a);
				class_counts.put("MapsTo", class_counts.get("MapsTo") + c.getMapsTos().size());

//				class_counts.put("Component-MapsTo", class_counts.get("Component-MapsTo") + c.getMapsTos().size());

//				for (MapsTo mp : c.getMapsTos())
//					for (Annotation a : mp.getAnnotations())
//						put_annotations(a);
			}

			class_counts.put("SequenceAnnotation",
					class_counts.get("SequenceAnnotation") + cd.getSequenceAnnotations().size());
			for (SequenceAnnotation sa : cd.getSequenceAnnotations()) {
//				for (Annotation a : sa.getAnnotations())
//					put_annotations(a);
//				class_counts.put("Location", class_counts.get("Location") + sa.getLocations().size());

				for (Location l : sa.getLocations()) {
//					for (Annotation a : l.getAnnotations())
//						put_annotations(a);
					if (l instanceof Cut)
						class_counts.put("Cut", class_counts.get("Cut") + 1);
					if (l instanceof Range)
						class_counts.put("Range", class_counts.get("Range") + 1);
					if (l instanceof GenericLocation)
						class_counts.put("GenericLocation", class_counts.get("GenericLocation") + 1);
				}
			}
			class_counts.put("SequenceConstraint",
					class_counts.get("SequenceConstraint") + cd.getSequenceConstraints().size());
//			for (SequenceConstraint sc : cd.getSequenceConstraints())
//				for (Annotation a : sc.getAnnotations())
//					put_annotations(a);
		}

//		for (Model m : doc.getModels()) {
//			for (Annotation a : m.getAnnotations())
//				put_annotations(a);
//		}

		for (ModuleDefinition md : doc.getModuleDefinitions()) {
			class_counts.put("FunctionalComponent",
					class_counts.get("FunctionalComponent") + md.getFunctionalComponents().size());
			for (FunctionalComponent fc : md.getFunctionalComponents()) {
////				for (Annotation a : fc.getAnnotations())
////					put_annotations(a);
				class_counts.put("MapsTo", class_counts.get("MapsTo") + fc.getMapsTos().size());
//				class_counts.put("FC-MapsTo", class_counts.get("FC-MapsTo") + fc.getMapsTos().size());
//
//				for (MapsTo mp : fc.getMapsTos())
//					for (Annotation a : mp.getAnnotations())
//						put_annotations(a);
//
			}

			class_counts.put("Interaction", class_counts.get("Interaction") + md.getInteractions().size());
			for (Interaction i : md.getInteractions()) {
				class_counts.put("Participation", class_counts.get("Participation") + i.getParticipations().size());
//				for (Annotation a : i.getAnnotations())
//					put_annotations(a);
			}

			class_counts.put("Model", class_counts.get("Model") + md.getModels().size());

			class_counts.put("Module", class_counts.get("Module") + md.getModules().size());
			for (Module m : md.getModules()) {
//				for (Annotation a : m.getAnnotations())
//					put_annotations(a);
				//class_counts.put("Module-MapsTo", class_counts.get("Module-MapsTo") + m.getMapsTos().size());
				class_counts.put("MapsTo", class_counts.get("MapsTo") + m.getMapsTos().size());

//				for (MapsTo mp : m.getMapsTos())
//					for (Annotation a : mp.getAnnotations())
//						put_annotations(a);

			}
		}
		// GenericTopLevel's have nothing but annotations
		create_spreadsheet(file.getName());

	}

//	private static void put_annotations(Annotation a) {
//		if (a.isBooleanValue()) {
//			class_counts.put("Boolean_Annotation", class_counts.get("Boolean_Annotation") + 1);
//		} else if (a.isIntegerValue()) {
//			class_counts.put("Integer_Annotation", class_counts.get("Integer_Annotation") + 1);
//		} else if (a.isStringValue()) {
//			class_counts.put("String_Annotation", class_counts.get("String_Annotation") + 1);
//		} else if (a.isDoubleValue())// must be a double value
//		{
//			class_counts.put("Double_Annotation", class_counts.get("Double_Annotation") + 1);
//		} else // must be a URI value
//		{
//			class_counts.put("URI_Annotation", class_counts.get("URI_Annotation") + 1);
//		}
//		if (a.isNestedAnnotations()) {
//			class_counts.put("Nested_Annotation", class_counts.get("Nested_Annotation") + 1);
//			for (Annotation sub_annotation : a.getAnnotations()) {
//				put_annotations(sub_annotation);
//			}
//		}
//	}

	private static void create_spreadsheet(String filename) {
		try {
			fileWriter.append(NEW_LINE_SEPARATOR);
			fileWriter.append(filename);
			fileWriter.append(COMMA_DELIMITER);
			for (String data_type : class_counts.keySet()) {
				fileWriter.append(class_counts.get(data_type).toString());
				fileWriter.append(COMMA_DELIMITER);
			}

		} catch (Exception e) {
			System.out.println("Error when writing file data" + filename);

		}
	}

	private static String abbreviations(String word) {
		switch (word) {
		case "Annotation":
			return "Ann";
		case "Boolean_Annotation":
			return "BAnn";
		case "Integer_Annotation":
			return "IAnn";
		case "String_Annotation":
			return "SAnn";
		case "Nested_Annotation":
			return "NAnn";
		case "Double_Annotation":
			return "DAnn";
		case "URI_Annotation":
			return "UAnn";
		case "Collection":
			return "Col";
		case "Component":
			return "Comp";
		case "ComponentDefinition":
			return "CD";
		case "FunctionalComponent":
			return "FC";
		case "GenericLocation":
			return "GL";
		case "GenericTopLevel":
			return "GTL";
		case "Interaction":
			return "I";
		case "Location":
			return "L";
		case "MapsTo":
			return "MapsTo";
		case "FC-MapsTo":
			return "FCMaps";
		case "Module-MapsTo":
			return "ModMaps";
		case "Model":
			return "Mdl";
		case "Module":
			return "Mod";
		case "ModuleDefinition":
			return "MD";
		case "Participation":
			return "P";
		case "Range":
			return "R";
		case "Sequence":
			return "S";
		case "SequenceAnnotation":
			return "SA";
		case "SequenceConstraint":
			return "SC";
		// case "TopLevel" : return "TL";
		default:
			return word;
		}

	}

	private static void initialize_classes() {
		class_counts = new HashMap<String, Integer>();

		class_counts.put("Annotation", 0);
//		class_counts.put("Boolean_Annotation", 0);
//		class_counts.put("Integer_Annotation", 0);
//		class_counts.put("String_Annotation", 0);
//		class_counts.put("Nested_Annotation", 0);
//		class_counts.put("Double_Annotation", 0);
//		class_counts.put("URI_Annotation", 0);
		class_counts.put("Collection", 0);
		class_counts.put("Component", 0);
		class_counts.put("ComponentDefinition", 0);
		class_counts.put("Cut", 0);
		class_counts.put("FunctionalComponent", 0);
		class_counts.put("GenericLocation", 0);
		class_counts.put("GenericTopLevel", 0);
		class_counts.put("Interaction", 0);
//		class_counts.put("Location", 0);
		class_counts.put("MapsTo", 0);
//		class_counts.put("Component-MapsTo", 0);
//		class_counts.put("FC-MapsTo", 0);
//		class_counts.put("Module-MapsTo", 0);
		class_counts.put("Model", 0);
		class_counts.put("Module", 0);
		class_counts.put("ModuleDefinition", 0);
		class_counts.put("Participation", 0);
		class_counts.put("Range", 0);
		class_counts.put("Sequence", 0);
		class_counts.put("SequenceAnnotation", 0);
		class_counts.put("SequenceConstraint", 0);
		// class_counts.put("TopLevel", 0);
		keys.addAll(class_counts.keySet());

	}

}