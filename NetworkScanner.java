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

public class NetworkScanner {
	public JScrollPane sp;
	private BufferedReader reader;
	public  static EnterIP frame;
	public static void main(String[] args) throws  InterruptedException{
		
		NetworkScanner p = new NetworkScanner();

		p.disp();

		String start_address = p.frame.f_add;
		String end_address = p.frame.l_add;
		int SCAN_JUMP;

		if(p.frame.scan == 1)
			SCAN_JUMP = 20;
		else
			SCAN_JUMP = 10;
		String [] start_parts = start_address.split("\\.");
		String[] end_parts = end_address.split("\\.");
		
		int start_domainbyte0 = Integer.parseInt(start_parts[0]);
		int end_domainbyte0 = Integer.parseInt(end_parts[0]);

		int start_domainbyte1 = Integer.parseInt(start_parts[1]);
		int end_domainbyte1 = Integer.parseInt(end_parts[1]);

		int start_domainbyte2 = Integer.parseInt(start_parts[2]);
		int end_domainbyte2 = Integer.parseInt(end_parts[2]);

		int NUMBER_OF_THREADS = (end_domainbyte0-start_domainbyte0 + 1)*256*256 + (end_domainbyte1 - start_domainbyte1 + 1)*256 + 							(end_domainbyte2- start_domainbyte2 + 1);
 
		MiniPing[] threads = new MiniPing[NUMBER_OF_THREADS];

		int i = 0;

		int domainbyte0, domainbyte1, domainbyte2;
		int limit1, limit2;
		for(domainbyte0 = start_domainbyte0;domainbyte0 <= end_domainbyte0;domainbyte0++){
			if((domainbyte0 == start_domainbyte0) && (domainbyte0 == end_domainbyte0)){
				domainbyte1 = start_domainbyte1;
				limit1 = end_domainbyte1 + 1;
			}
			else if(domainbyte0 == start_domainbyte0){
				domainbyte1 = start_domainbyte1;
				limit1 = 256;
			}
			else if(domainbyte0 == end_domainbyte0){
				domainbyte1 = 0;
				limit1 = end_domainbyte1 + 1;
			}
			else{
				domainbyte1 = 0;
				limit1 = 256;
			}
			while(domainbyte1 < limit1){
				if((domainbyte1 == start_domainbyte1) && (domainbyte1 == end_domainbyte1)){
					domainbyte2 = start_domainbyte2;
					limit2 = end_domainbyte2 + 1;
				}
				else if(domainbyte1 == start_domainbyte1){
					domainbyte2 = start_domainbyte2;
					limit2 = 256;
				}
				else if(domainbyte1 == end_domainbyte1){
					domainbyte2 = 0;
					limit2 = end_domainbyte2 + 1;
				}
				else{
					domainbyte2 = 0;
					limit2 = 256;
				}
				while(domainbyte2 < limit2){

					threads[i] = new MiniPing(domainbyte0+ "." + domainbyte1 + "." + domainbyte2 +".*", SCAN_JUMP, frame);
					threads[i].start();
					Thread.sleep(10);
					i++;
					domainbyte2++;
				}
				domainbyte1++;
			}
		}
		for(int j = 0;j < i;j++){
			try{
				threads[j].t.join();
			}
			catch(InterruptedException e){
				System.out.println("****************Error*****************");
			}
		}
		Draw_topology topo = new Draw_topology(threads, i, frame);
		
	}
	
	public void disp() throws InterruptedException{
		
		try {
			frame = new EnterIP();
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		while(!frame.done)
			Thread.sleep(1000);
	}
		
	public String executeCommand(String command) {

		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			reader = 
                            new BufferedReader(new InputStreamReader(p.getInputStream()));

                        String line = "";			
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();

	}

}

class MiniPing implements Runnable {
	public Thread t;
	public String domainName;
	public String[] ip_list;
	public int IP_INDEX;
	public MyTraceroute traceroute;
	private int SCAN_JUMP;
	EnterIP tt;
	MiniPing( String domain, int SCAN_JUMP, EnterIP ei){
		tt = ei;
		this.domainName = domain;
		this.SCAN_JUMP = SCAN_JUMP;
		IP_INDEX = 0;
		ip_list = new String[256];
	}
	public void run() {
			findHost_n_Path obj = new findHost_n_Path();

			int i, byte_no;

			String IPADDRESS_PATTERN = 
			"(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

			String command = "nmap -sn --min-parallelism 64 " + domainName;
		
			Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);

			String output = obj.executeCommand(command);

			String[] raw_ip = output.split("\\n");

			for(i = 1;i < raw_ip.length;i++){
				Matcher matcher = pattern.matcher(raw_ip[i]);
				if (matcher.find()) {
					ip_list[IP_INDEX++] = matcher.group();
				}
			}
	
			String progress;
			progress = "Scanned " + domainName + ": " + IP_INDEX + " hosts are alive.\n";
			
			tt.textField_2.append(progress);
			if(IP_INDEX > 0){
				traceroute = new MyTraceroute(this.ip_list, this.IP_INDEX, this.SCAN_JUMP, this.tt);
				traceroute.run();
			}
	}

	
	public void start () {
		
		if (t == null)
		{
			t = new Thread (this, domainName);
			t.start ();
		}
	}
}

class MyTraceroute{
	
	private Thread t;
	public int MAX;
	public String[] domainName;
	private String[] path;
	private String prev_path;
	public int[] indicesused;
	private int last_hop;
	private int prev_add = 0;
	private findHost_n_Path obj;
	private int top;
	private String IPADDRESS_PATTERN;
	private String BASE_COMMAND;
	private Pattern pattern;
	private boolean NO_UPDATE_PATH;
	private int[] changed_paths;
	private int CHANGED_INDEX;
	private int SPACE;
	private int JUMP;
	public HashMap<String, String[]> IP_PATH_MAP;
	public int[] IP_PATH_LENGTH;
	private boolean DIRECT;
	private int prev_domainIndex;
	private EnterIP frame;

	MyTraceroute(String[] domain, int MAX, int SCAN_JUMP, EnterIP frame){

		this.frame = frame;
		this.domainName = domain;
		this.MAX = MAX;
		path = new String[4];
		IP_PATH_LENGTH = new int[256];
		indicesused = new int[64];
		changed_paths = new int[64];
		IP_PATH_MAP = new HashMap<String, String[]>();
		obj = new findHost_n_Path();
		IPADDRESS_PATTERN = 
			"(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

		pattern = Pattern.compile(IPADDRESS_PATTERN);
		prev_path = "abc";
		top = 0;
		prev_add = 0;
		last_hop = 0;
		CHANGED_INDEX = 0;
		BASE_COMMAND = "mtr --raw -c 1 ";

		SPACE = 25;
		JUMP = SCAN_JUMP;
	}
	
	public void run(){

		int domainIndex = 0;
		String command;
		findHost_n_Path obj = new findHost_n_Path();
		boolean RETRACE = false;
		boolean FIRST_RUN = false;
		boolean START = true;
		NO_UPDATE_PATH = false;
		prev_domainIndex = -1;
		String[] DIRECT_PATH = new String[4];
		while(true){

			command = BASE_COMMAND + domainName[domainIndex];
			
			String progress = "Traceroute to " + domainName[domainIndex] +"\n";
			frame.textField_2.append(progress);

			int hop_count = this.runCommand(command, START, domainIndex);
		
			if(START){
				START = false;
				if(hop_count == 0){
					if(!DIRECT){
						last_hop = 0;
					}
					else{
						last_hop = 0;
						
						String[] dp = new String[4];
						dp[0] = domainName[domainIndex];
						IP_PATH_MAP.put(domainName[domainIndex],dp);
						IP_PATH_LENGTH[domainIndex] = 1;
					}
				}
				
				else{
					String[] dp = new String[4];
					System.arraycopy(path, 0, dp, 0, hop_count);
					dp[hop_count] = domainName[domainIndex];
					IP_PATH_MAP.put(domainName[domainIndex], dp);
					IP_PATH_LENGTH[domainIndex] = hop_count + 1;
				}	
				
			}
			else if(hop_count == 0){
				if(!DIRECT){
					
					last_hop = 0;
					
				}
				else{
					last_hop = 0;
				}
				for(int i = prev_domainIndex + 1;i <= domainIndex;i++){

						String [] dp = new String[4];
						dp[0] = domainName[i];
						
						IP_PATH_MAP.put(domainName[i],dp);
						IP_PATH_LENGTH[i] = hop_count + 1;
				}
			
			}
		
			else if(prev_path.equals(path[last_hop])){
				indicesused[top++] = domainIndex;
				for(int i = prev_domainIndex + 1;i <= domainIndex;i++){
					String[] dp = new String[4];
					System.arraycopy(path, 0, dp, 0, hop_count);
					dp[hop_count] = domainName[i];
					IP_PATH_MAP.put(domainName[i], dp);
					IP_PATH_LENGTH[i] = hop_count + 1;
				}
			}
			else{
				if(top != 0)
					 changed_paths[CHANGED_INDEX] = indicesused[top-1];
				else 
					changed_paths[CHANGED_INDEX] = 0;

				changed_paths[CHANGED_INDEX+1] = domainIndex;
				CHANGED_INDEX += 2;

				indicesused[top++] = domainIndex;
				
				for(int i = prev_domainIndex + 1;i <= domainIndex;i++){
					String[] dp = new String[4];
					System.arraycopy(path, 0, dp, 0, hop_count);
					dp[hop_count] = domainName[i];
					IP_PATH_MAP.put(domainName[i], dp);
					IP_PATH_LENGTH[i] = hop_count + 1;
				}

			}
			prev_domainIndex = domainIndex;
			domainIndex = this.getNextIndex(domainIndex);

			if(domainIndex == -1)
				break;

		}
		
	}

	private int getNextIndex(int domainIndex){
		if(domainIndex >= MAX-1)
			return -1;
		domainIndex += JUMP;
		while((domainIndex < MAX)){
			int add = 0;
			String[] parts;
			parts = domainName[domainIndex].split("\\.");
			
			add = Integer.parseInt(parts[3]);
			if((add-prev_add)>SPACE){
				prev_add = add;
				return domainIndex;
			}
			domainIndex += JUMP;
		}
		
		return (MAX-1);
	}
	private int runCommand(String command, boolean START, int domainIndex){
			int i;
			
			String output = obj.executeCommand(command);

			if(!START){
				if(path[last_hop] != null)
					prev_path = path[last_hop];
			}
			DIRECT = false;
			String[] raw_ip = output.split("\\n");
			int hop = 0;
			for(i = 0;i < raw_ip.length;i++){
				Matcher matcher = pattern.matcher(raw_ip[i]);
				if (matcher.find()) {
					if(domainName[domainIndex].equals(matcher.group()))
						DIRECT = true;
					else
						path[hop++] = matcher.group();
				}
			}
			
			if(hop < 1)
				return hop;
			last_hop = hop - 1;
			return hop;
			
	}
	
}

