package class_iterations;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.sbolstandard.core2.AccessType;
import org.sbolstandard.core2.Activity;
import org.sbolstandard.core2.Agent;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.EDAMOntology;
import org.sbolstandard.core2.OrientationType;
import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SBOLWriter;
import org.sbolstandard.core2.SequenceAnnotation;

public class fixTests {

	public static void main(String[] args) throws SBOLValidationException, IOException, SBOLConversionException, URISyntaxException {
		// TODO Auto-generated method stub
		
		SBOLDocument doc = setup();
		doc = SBOLReader.read("./SBOLTestSuite/SBOL2/ComponentDefinitionOutput.xml");
		
		URI dna = new URI("http://www.biopax.org/release/biopax-level3.owl#DnaRegion");
		ComponentDefinition gl_cd = doc.createComponentDefinition("GenericLocationComponent", ComponentDefinition.DNA); 
		SequenceAnnotation gl_sa = gl_cd.createSequenceAnnotation("gl_seqAnn", "GenericLocation", OrientationType.INLINE);
		gl_sa.addGenericLocation("gl");
		
		SBOLWriter.write(doc, "./SBOLTestSuite/SBOL2/ComponentDefinitionOutput_gl.xml");
		
		doc = setup();

		doc = SBOLReader.read("./SBOLTestSuite/SBOL2/singleComponentDefinition.xml");
		ComponentDefinition cd_comp = doc.createComponentDefinition("tetR", ComponentDefinition.DNA);
		cd_comp.createComponent("component", AccessType.PUBLIC, "generic_promoter");
		
		SBOLWriter.write(doc, "./SBOLTestSuite/SBOL2/singleComponentDefinition_component.xml");

		doc = setup();

		doc = SBOLReader.read("./SBOLTestSuite/SBOL2/Provenance_SpecifyCutOperation.xml");
		Agent unknown = doc.createAgent("unknown");
		Activity act = doc.createActivity("generic_activity");
		act.createAssociation("act_assoc", unknown.getIdentity());
		SBOLWriter.write(doc, "./SBOLTestSuite/SBOL2/Provenance_SpecifyCutOperation_redone.xml");
		doc = setup();
		doc = SBOLReader.read("./SBOLTestSuite/SBOL2/Provenance_SpecifyCutOperation_redone.xml");

		for(Activity a : doc.getActivities())
			System.out.println(a.getAssociations().size());
	}
	
	public static SBOLDocument setup()
	{
		SBOLDocument doc = new SBOLDocument();
		String version = "1.0";
		// read serialized file into a document, then
		SBOLReader.setCompliant(false);
		SBOLReader.setURIPrefix("http://www.async.ece.utah.edu");
		SBOLReader.setVersion(version);
		
		return doc;
	}

}
