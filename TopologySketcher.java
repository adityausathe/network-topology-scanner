import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.*;
import java.net.InetAddress;
import java.util.regex.*;
import java.util.*;

import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.tree.*;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

public class Draw_topology extends JFrame{
	public JScrollPane sp;
	private MiniPing[] networks;
	private int net_number;
	private EnterIP frame;
	private List<HashMap<String, ArrayList<String>>> hash;

	Draw_topology(MiniPing []networks, int net_number, EnterIP frame){
		this.frame = frame;
		this.networks = networks;
		this.net_number = net_number;

		hash = new ArrayList<HashMap<String, ArrayList<String>>>();
		
		for(int level = 0;level < 4; level++){

			hash.add(new HashMap<String, ArrayList<String>>());

			for(int i = 0;i < net_number;i++){
				if(networks[i].IP_INDEX == 0)
					continue;
				for (Map.Entry<String, String[]> entry : networks[i].traceroute.IP_PATH_MAP.entrySet()) {

				    	String[] value = entry.getValue();
					
					if(value[level] == null)
						continue;
					
					HashMap<String, ArrayList<String>> temp2= hash.get(level);
					ArrayList<String> arraylist = new ArrayList<String>();
					
					if(hash.get(level).containsKey(value[level])){
						
						ArrayList<String> next = hash.get(level).get(value[level]);
						if(next.contains(value[level+1]))
							continue;
						else if(value[level+1] != null){
							hash.get(level).get(value[level]).add(value[level+1]);
							hash.get(level).put(value[level], hash.get(level).get(value[level]));
						}
					}else{
						ArrayList<String> latest = new ArrayList<String>();
						if(value[level+1] != null)
							latest.add(value[level + 1]);
						hash.get(level).put(value[level], latest);
					}
						
				}
			}
			
		}

		String progress = "\nScan Completed.\n";
		frame.textField_2.append(progress);
		this.constructTree();
		
		
	}

	public void constructTree(){

		JTree tree;
		
		ArrayList<HashMap<String, DefaultMutableTreeNode>> treeNode;
		treeNode = new ArrayList<HashMap<String, DefaultMutableTreeNode>>();

    		for(int level=0;level<4;level++){
			treeNode.add(new HashMap<String, DefaultMutableTreeNode>());
			for (Map.Entry<String, ArrayList<String>> entry : hash.get(level).entrySet()) {
				
			    	String key = entry.getKey();
				treeNode.get(level).put(key, new DefaultMutableTreeNode(key));
			}
		}

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Localhost");

		for(int level=0;level<3;level++){
			if(level == 0){
				for(Map.Entry<String, DefaultMutableTreeNode> entry : treeNode.get(0).entrySet()) {
					root.add(entry.getValue());
				}
			}
			for(Map.Entry<String, DefaultMutableTreeNode> entry : treeNode.get(level).entrySet()) {
				DefaultMutableTreeNode parent = entry.getValue();
				
				String subnet = entry.getKey();
				ArrayList<String> children = hash.get(level).get(subnet);
				
				for(String l:children){
					parent.add(treeNode.get(level + 1).get(l));
				}
			}
		}			
		

		tree = new JTree(root);
		sp = new JScrollPane(tree);
		sp.setBounds(42, 321, 380, 110);
		
		
		add(sp);
		 
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Graph");
		this.pack();
		this.setVisible(true);
    	}
}
