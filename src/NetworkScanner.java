import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.*;
import java.util.regex.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;

public class NetworkScanner {
    private InputPromptUI frame;

    public static void main(String[] args) {
        try {
            new NetworkScanner().run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() throws InterruptedException {
        display();

        String start_address = frame.f_add;
        String end_address = frame.l_add;
        int scanStride;

        if (frame.scan == 1)
            scanStride = 20;
        else
            scanStride = 10;
        String[] start_parts = start_address.split("\\.");
        String[] end_parts = end_address.split("\\.");

        int start_domainbyte0 = Integer.parseInt(start_parts[0]);
        int end_domainbyte0 = Integer.parseInt(end_parts[0]);

        int start_domainbyte1 = Integer.parseInt(start_parts[1]);
        int end_domainbyte1 = Integer.parseInt(end_parts[1]);

        int start_domainbyte2 = Integer.parseInt(start_parts[2]);
        int end_domainbyte2 = Integer.parseInt(end_parts[2]);

        // todo: use thread-pool
        int numberOfThreads = (end_domainbyte0 - start_domainbyte0 + 1) * 256 * 256 + (end_domainbyte1 - start_domainbyte1 + 1) * 256 + (end_domainbyte2 - start_domainbyte2 + 1);

        MiniPing[] pingPools = new MiniPing[numberOfThreads];

        int poolcount = 0;

        int domainbyte0, domainbyte1, domainbyte2;
        int limit1, limit2;
        for (domainbyte0 = start_domainbyte0; domainbyte0 <= end_domainbyte0; domainbyte0++) {
            if ((domainbyte0 == start_domainbyte0) && (domainbyte0 == end_domainbyte0)) {
                domainbyte1 = start_domainbyte1;
                limit1 = end_domainbyte1 + 1;
            } else if (domainbyte0 == start_domainbyte0) {
                domainbyte1 = start_domainbyte1;
                limit1 = 256;
            } else if (domainbyte0 == end_domainbyte0) {
                domainbyte1 = 0;
                limit1 = end_domainbyte1 + 1;
            } else {
                domainbyte1 = 0;
                limit1 = 256;
            }
            while (domainbyte1 < limit1) {
                if ((domainbyte1 == start_domainbyte1) && (domainbyte1 == end_domainbyte1)) {
                    domainbyte2 = start_domainbyte2;
                    limit2 = end_domainbyte2 + 1;
                } else if (domainbyte1 == start_domainbyte1) {
                    domainbyte2 = start_domainbyte2;
                    limit2 = 256;
                } else if (domainbyte1 == end_domainbyte1) {
                    domainbyte2 = 0;
                    limit2 = end_domainbyte2 + 1;
                } else {
                    domainbyte2 = 0;
                    limit2 = 256;
                }
                while (domainbyte2 < limit2) {

                    pingPools[poolcount] = new MiniPing(domainbyte0 + "." + domainbyte1 + "." + domainbyte2 + ".*", scanStride, frame);
                    pingPools[poolcount].start();
                    Thread.sleep(10);
                    poolcount++;
                    domainbyte2++;
                }
                domainbyte1++;
            }
        }
        for (int j = 0; j < poolcount; j++) {
            try {
                pingPools[j].t.join();
            } catch (InterruptedException e) {
                System.out.println("****************Error*****************");
            }
        }
        new TopologyUI(pingPools, poolcount, frame).run();

    }

    private void display() throws InterruptedException {

        try {
            frame = new InputPromptUI();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // todo: replace spinning
        while (!frame.done)
            Thread.sleep(1000);
    }

    private String executeCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }

    class MiniPing implements Runnable {
        Thread t;
        String domainName;
        String[] ipList;
        int ipIndex;
        MyTraceroute traceroute;
        private int scanStride;
        InputPromptUI tt;

        MiniPing(String domain, int scanStride, InputPromptUI promptWindow) {
            tt = promptWindow;
            this.domainName = domain;
            this.scanStride = scanStride;
            ipIndex = 0;
            ipList = new String[256];
        }

        public void run() {
            int i, byte_no;

            String IPADDRESS_PATTERN =
                    "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

            String command = "nmap -sn --min-parallelism 64 " + domainName;

            Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);

            String output = executeCommand(command);

            String[] raw_ip = output.split("\\n");

            for (i = 1; i < raw_ip.length; i++) {
                Matcher matcher = pattern.matcher(raw_ip[i]);
                if (matcher.find()) {
                    ipList[ipIndex++] = matcher.group();
                }
            }

            String progress;
            progress = "Scanned " + domainName + ": " + ipIndex + " hosts are alive.\n";

            tt.textArea.append(progress);
            if (ipIndex > 0) {
                traceroute = new MyTraceroute(this.ipList, this.ipIndex, this.scanStride, this.tt);
                traceroute.run();
            }
        }


        void start() {

            if (t == null) {
                t = new Thread(this, domainName);
                t.start();
            }
        }
    }

    class MyTraceroute {

        private Thread t;
        int max;
        String[] domainName;
        private String[] path;
        private String prevPath;
        int[] indicesused;
        private int lastHop;
        private int prevAdd = 0;
        private int top;
        private String ipaddressPattern;
        private String baseCommand;
        private Pattern pattern;
        private boolean noUpdatePath;
        private int[] changed_paths;
        private int changedIndex;
        private int space;
        private int jump;
        HashMap<String, String[]> ipPathMap;
        int[] ipPathLength;
        private boolean direct;
        private int prevDomainIndex;
        private InputPromptUI frame;

        MyTraceroute(String[] domain, int max, int SCAN_JUMP, InputPromptUI frame) {

            this.frame = frame;
            this.domainName = domain;
            this.max = max;
            path = new String[4];
            ipPathLength = new int[256];
            indicesused = new int[64];
            changed_paths = new int[64];
            ipPathMap = new HashMap<String, String[]>();
            ipaddressPattern =
                    "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

            pattern = Pattern.compile(ipaddressPattern);
            prevPath = "abc";
            top = 0;
            prevAdd = 0;
            lastHop = 0;
            changedIndex = 0;
            baseCommand = "mtr --raw -c 1 ";

            space = 25;
            jump = SCAN_JUMP;
        }

        void run() {

            int domainIndex = 0;
            String command;
            boolean start = true;
            noUpdatePath = false;
            prevDomainIndex = -1;
            while (true) {

                command = baseCommand + domainName[domainIndex];

                String progress = "Traceroute to " + domainName[domainIndex] + "\n";
                frame.textArea.append(progress);

                int hop_count = this.runCommand(command, start, domainIndex);

                if (start) {
                    start = false;
                    if (hop_count == 0) {
                        if (!direct) {
                            lastHop = 0;
                        } else {
                            lastHop = 0;

                            String[] dp = new String[4];
                            dp[0] = domainName[domainIndex];
                            ipPathMap.put(domainName[domainIndex], dp);
                            ipPathLength[domainIndex] = 1;
                        }
                    } else {
                        String[] dp = new String[4];
                        System.arraycopy(path, 0, dp, 0, hop_count);
                        dp[hop_count] = domainName[domainIndex];
                        ipPathMap.put(domainName[domainIndex], dp);
                        ipPathLength[domainIndex] = hop_count + 1;
                    }

                } else if (hop_count == 0) {
                    if (!direct) {

                        lastHop = 0;

                    } else {
                        lastHop = 0;
                    }
                    for (int i = prevDomainIndex + 1; i <= domainIndex; i++) {

                        String[] dp = new String[4];
                        dp[0] = domainName[i];

                        ipPathMap.put(domainName[i], dp);
                        ipPathLength[i] = hop_count + 1;
                    }

                } else if (prevPath.equals(path[lastHop])) {
                    indicesused[top++] = domainIndex;
                    for (int i = prevDomainIndex + 1; i <= domainIndex; i++) {
                        String[] dp = new String[4];
                        System.arraycopy(path, 0, dp, 0, hop_count);
                        dp[hop_count] = domainName[i];
                        ipPathMap.put(domainName[i], dp);
                        ipPathLength[i] = hop_count + 1;
                    }
                } else {
                    if (top != 0)
                        changed_paths[changedIndex] = indicesused[top - 1];
                    else
                        changed_paths[changedIndex] = 0;

                    changed_paths[changedIndex + 1] = domainIndex;
                    changedIndex += 2;

                    indicesused[top++] = domainIndex;

                    for (int i = prevDomainIndex + 1; i <= domainIndex; i++) {
                        String[] dp = new String[4];
                        System.arraycopy(path, 0, dp, 0, hop_count);
                        dp[hop_count] = domainName[i];
                        ipPathMap.put(domainName[i], dp);
                        ipPathLength[i] = hop_count + 1;
                    }

                }
                prevDomainIndex = domainIndex;
                domainIndex = this.getNextIndex(domainIndex);

                if (domainIndex == -1)
                    break;

            }

        }

        private int getNextIndex(int domainIndex) {
            if (domainIndex >= max - 1)
                return -1;
            domainIndex += jump;
            while ((domainIndex < max)) {
                int add = 0;
                String[] parts;
                parts = domainName[domainIndex].split("\\.");

                add = Integer.parseInt(parts[3]);
                if ((add - prevAdd) > space) {
                    prevAdd = add;
                    return domainIndex;
                }
                domainIndex += jump;
            }

            return (max - 1);
        }

        private int runCommand(String command, boolean START, int domainIndex) {
            int i;

            String output = executeCommand(command);

            if (!START) {
                if (path[lastHop] != null)
                    prevPath = path[lastHop];
            }
            direct = false;
            String[] raw_ip = output.split("\\n");
            int hop = 0;
            for (i = 0; i < raw_ip.length; i++) {
                Matcher matcher = pattern.matcher(raw_ip[i]);
                if (matcher.find()) {
                    if (domainName[domainIndex].equals(matcher.group()))
                        direct = true;
                    else
                        path[hop++] = matcher.group();
                }
            }

            if (hop < 1)
                return hop;
            lastHop = hop - 1;
            return hop;

        }

    }

    public static class TopologyUI extends JFrame {
        JScrollPane sp;
        private MiniPing[] networks;
        private int netNumber;
        private InputPromptUI frame;
        private List<HashMap<String, ArrayList<String>>> hash;

        TopologyUI(MiniPing[] networks, int netNumber, InputPromptUI frame) {
            this.frame = frame;
            this.networks = networks;
            this.netNumber = netNumber;
        }

        void run() {
            hash = new ArrayList<HashMap<String, ArrayList<String>>>();

            for (int level = 0; level < 4; level++) {

                hash.add(new HashMap<String, ArrayList<String>>());

                for (int i = 0; i < netNumber; i++) {
                    if (networks[i].ipIndex == 0)
                        continue;
                    for (Map.Entry<String, String[]> entry : networks[i].traceroute.ipPathMap.entrySet()) {

                        String[] value = entry.getValue();

                        if (value[level] == null)
                            continue;

                        HashMap<String, ArrayList<String>> temp2 = hash.get(level);
                        ArrayList<String> arraylist = new ArrayList<String>();

                        if (hash.get(level).containsKey(value[level])) {

                            ArrayList<String> next = hash.get(level).get(value[level]);
                            if (next.contains(value[level + 1]))
                                continue;
                            else if (value[level + 1] != null) {
                                hash.get(level).get(value[level]).add(value[level + 1]);
                                hash.get(level).put(value[level], hash.get(level).get(value[level]));
                            }
                        } else {
                            ArrayList<String> latest = new ArrayList<String>();
                            if (value[level + 1] != null)
                                latest.add(value[level + 1]);
                            hash.get(level).put(value[level], latest);
                        }

                    }
                }

            }

            String progress = "\nScan Completed.\n";
            frame.textArea.append(progress);
            this.constructTree();
        }

        private void constructTree() {

            JTree tree;

            ArrayList<HashMap<String, DefaultMutableTreeNode>> treeNode;
            treeNode = new ArrayList<HashMap<String, DefaultMutableTreeNode>>();

            for (int level = 0; level < 4; level++) {
                treeNode.add(new HashMap<String, DefaultMutableTreeNode>());
                for (Map.Entry<String, ArrayList<String>> entry : hash.get(level).entrySet()) {

                    String key = entry.getKey();
                    treeNode.get(level).put(key, new DefaultMutableTreeNode(key));
                }
            }

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Localhost");

            for (int level = 0; level < 3; level++) {
                if (level == 0) {
                    for (Map.Entry<String, DefaultMutableTreeNode> entry : treeNode.get(0).entrySet()) {
                        root.add(entry.getValue());
                    }
                }
                for (Map.Entry<String, DefaultMutableTreeNode> entry : treeNode.get(level).entrySet()) {
                    DefaultMutableTreeNode parent = entry.getValue();

                    String subnet = entry.getKey();
                    ArrayList<String> children = hash.get(level).get(subnet);

                    for (String l : children) {
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

    static class InputPromptUI extends JFrame {
        private JTextField textField;
        private JTextField textField1;
        JTextArea textArea;
        String f_add;
        String l_add;
        boolean done;
        int scan;
        private JRadioButton rdBtnFastScan;
        private JRadioButton rdBtnNormalScan;

        InputPromptUI() {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setBounds(100, 100, 450, 477);
            JPanel contentPane = new JPanel();
            contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
            setContentPane(contentPane);
            contentPane.setLayout(null);

            done = false;


            JLabel lblE = new JLabel("Start IP:");
            lblE.setBounds(42, 119, 70, 15);
            contentPane.add(lblE);

            JLabel lblEnterIp = new JLabel("End IP:");
            lblEnterIp.setBounds(42, 158, 70, 15);
            contentPane.add(lblEnterIp);

            textField = new JTextField();
            textField.setBounds(163, 117, 179, 19);
            contentPane.add(textField);
            textField.setColumns(10);

            textField1 = new JTextField();
            textField1.setBounds(163, 156, 179, 19);
            contentPane.add(textField1);
            textField1.setColumns(10);

            JButton btnGo = new JButton("GO!!");
            btnGo.setBounds(160, 261, 117, 25);
            contentPane.add(btnGo);

            textArea = new JTextArea();
            textArea.setBounds(42, 321, 359, 96);
            contentPane.add(textArea);
            textArea.setColumns(10);
            JScrollPane sp = new JScrollPane(textArea);
            sp.setBounds(42, 321, 380, 110);
            contentPane.add(sp);

            JLabel lblProgress = new JLabel("Progress:");
            lblProgress.setBounds(42, 294, 70, 15);
            contentPane.add(lblProgress);

            rdBtnFastScan = new JRadioButton("Fast Scan");
            rdBtnFastScan.setBounds(60, 202, 149, 23);
            contentPane.add(rdBtnFastScan);

            rdBtnNormalScan = new JRadioButton("Normal Scan");
            rdBtnNormalScan.setBounds(213, 202, 149, 23);
            contentPane.add(rdBtnNormalScan);


            btnGo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (rdBtnFastScan.isSelected())
                        scan = 1;
                    else if (rdBtnNormalScan.isSelected())
                        scan = 0;
                    f_add = textField.getText();
                    l_add = textField1.getText();

                    String IPADDRESS_PATTERN =
                            "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

                    Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);

                    Matcher matcher1 = pattern.matcher(f_add);
                    Matcher matcher2 = pattern.matcher(l_add);

                    if (matcher1.find() && matcher2.find()) {
                        done = true;
                        textArea.setText("");
                    } else
                        textArea.setText("Enter Valid Address.\n");

                }
            });
            ButtonGroup bG = new ButtonGroup();
            bG.add(rdBtnFastScan);
            bG.add(rdBtnNormalScan);
            this.add(rdBtnFastScan);
            this.add(rdBtnNormalScan);
            rdBtnFastScan.setSelected(true);
            this.setVisible(true);
        }
    }
}