package jarhead.neat;

import jarhead.ConnectionGene;
import jarhead.Genome;
import jarhead.NodeGene;
import jarhead.NodeGene.TYPE;

import java.util.LinkedList;
import java.util.Random;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.spriteManager.SpriteManager;

//TODO: stylesheet with depth and boundaryNodes
//TODO: sprite labelling of hidden and boundaryNodes with color coding
/**
 * 
 * @author Hydrozoa
 *
 */
public class NetworkPrinter {

	protected String STYLESHEET =
			"edge {" +
	        		"text-background-mode: rounded-box;" +
	        		"text-background-color: black;" +
	        		"text-alignment: center;" +
	        		"text-color: white;" +
	        		"arrow-size: 4;" +
	        		"text-size: 10;" +
	        		"shape: blob;" +
	        "}" +
	        		
			"edge.inactive {" +
				"fill-color:gray;" +
			"}" +
	        		
	        "node {" +
	        		"fill-color: black;" +
	        		"text-background-mode: rounded-box;" +
	        		"text-background-color: black;" +
	        		"text-alignment: center;" +
	        		"text-color: black;" +
	        		"size: 20;" +
	        		"text-size: 10;" +
	        "}" +
	        
	        "node.i {" +
	        	"fill-color: red;" +
	        "}" +
	        
	        "node.h {" +
	        	"fill-color: green;" +
	        "}" +
	        
			"node.o {" +
				"fill-color: blue;" +
			"}" +
				
			"sprite {" +
				"shape: box;" +
				"size: 16px, 26px;" +
				"fill-mode: image-scaled;" +
				"fill-image: url('mapPinSmall.png');" +
			"}";
	
	Graph graph = new MultiGraph("Network");

	SpriteManager sman = new SpriteManager(graph);
	

	Random random = new Random(1337L);

	public NetworkPrinter(Genome gene) {
		graph.addAttribute("ui.stylesheet", STYLESHEET);

		LinkedList<NodeGene> inputs = new LinkedList<NodeGene>();
		LinkedList<NodeGene> outputs = new LinkedList<NodeGene>();
		LinkedList<NodeGene> hidden = new LinkedList<NodeGene>();

		for (NodeGene node : gene.getNodeGenes().values()) {
			if (node.getType() == TYPE.INPUT) {
				inputs.add(node);
			} else if (node.getType() == TYPE.OUTPUT) {
				outputs.add(node);
			} else { 
				hidden.add(node);
			}
		}

		for (int i = 0; i < inputs.size(); i++) {
			NodeGene nodeGene = inputs.get(i);
			Node n = graph.addNode("N" + nodeGene.getId());
			n.addAttribute("ui.label", "id=" + nodeGene.getId());
			n.addAttribute("layout.frozen");
			n.addAttribute("y", 0);
			n.addAttribute("x", 1f / (inputs.size() + 1) * (i + 1));
			n.addAttribute("ui.class", "i");
		}

		for (int i = 0; i < outputs.size(); i++) {
			NodeGene nodeGene = outputs.get(i);
			Node n = graph.addNode("N" + nodeGene.getId());
			n.addAttribute("ui.label", "id=" + nodeGene.getId());
			n.addAttribute("layout.frozen");
			n.addAttribute("y", 1);
			n.addAttribute("x", 1f / (inputs.size() + 1) * (i + 1));
			n.addAttribute("ui.class", "o");
		}

		hidden.sort((NodeGene n1, NodeGene n2)->{
			return n1.getDepth () - n2.getDepth();
		});
		for (int i = 0; i < hidden.size(); i++) {
			NodeGene nodeGene = hidden.get(i);
			Node n = graph.addNode("N" + nodeGene.getId());
			n.addAttribute("ui.label", "Depth=" + nodeGene.getDepth());
			n.addAttribute("layout.frozen");
//			n.addAttribute("y", random.nextFloat());
			n.addAttribute("y", 1f / (hidden.get(hidden.size()-1).getDepth() + 1) * (nodeGene.getDepth() + 0.50f)); //hiddensize needs to be maxDepth
			n.addAttribute("x", random.nextFloat());
			n.addAttribute("ui.class", "h");
		}

		for (ConnectionGene connection : gene.getConnectionGenes().values()) {
			Edge e = graph.addEdge("C" + connection.getInnovation(), "N" + connection.getInNode(),
					"N" + connection.getOutNode(), true);
//			e.addAttribute("ui.label", "w=" + connection.getWeight() + "\n" + " in=" + connection.getInnovation());
			
			if (!connection.isExpressed()) {
				e.addAttribute("ui.class", "inactive");
			}
		}
	}

	public void displayGraph() {
		graph.display();
	}
}
