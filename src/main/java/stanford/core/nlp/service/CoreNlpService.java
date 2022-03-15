package stanford.core.nlp.service;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import guru.nidi.graphviz.rough.FillStyle;
import guru.nidi.graphviz.rough.Roughifyer;
import org.springframework.stereotype.Service;
import stanford.core.nlp.model.Data;

import java.io.File;
import java.util.Properties;

@Service
public class CoreNlpService {

    public void processRequest(Data input) {
        // Load stanford nlp
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,parse");
        props.setProperty("pos.model", "edu/stanford/nlp/models/pos-tagger/arabic.tagger");
        props.setProperty("parse.model", "edu/stanford/nlp/models/lexparser/arabicFactored.ser.gz");
        props.setProperty("segment.model", "edu/stanford/nlp/models/segmenter/arabic/arabic-segmenter-atb+bn+arztrain.ser.gz");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreDocument doc = new CoreDocument(input.getContent());
        // Apply annotate
        pipeline.annotate(doc);

        doc.sentences().forEach(sentence -> {
            // Get Tree object
            Tree tree = sentence.constituencyParse();
            try {
                String result = "";
                // Get Penn string
                if (tree.pennString() != null) {
                    result = tree.pennString().replaceAll("\\r\\n", "");
                    input.setResult(result);
                }
                // Get semantic graph
                SemanticGraph semanticGraph = getSemanticGraph(tree, SemanticGraphFactory.Mode.COLLAPSED_TREE);
                // Create Graph
                MutableGraph g = new Parser().read(semanticGraph.toDotFormat("g1", CoreLabel.OutputFormat.VALUE_TAG));
                String filename = "ex4-" + System.currentTimeMillis() + ".svg";
                File newFile = new File("c:/nlp/images/" + filename);
                Graphviz.fromGraph(g).processor(new Roughifyer()
                    .fillStyle(FillStyle.zigzagLine())
                    .font("*", "Adobe Kaiti Std")
                ).width(700).yInvert(true).render(Format.SVG).toFile(newFile);
                input.setImg("/content/" + filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private SemanticGraph getSemanticGraph(Tree constituencyParse, SemanticGraphFactory.Mode mode) {
        SemanticGraph semanticGraph = SemanticGraphFactory.
            makeFromTree(constituencyParse,
                mode,
                GrammaticalStructure.Extras.MAXIMAL, null);
        SemanticGraphFactory.
            generateUncollapsedDependencies(constituencyParse).prettyPrint();
        return semanticGraph;
    }
}