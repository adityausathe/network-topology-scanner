import java.awt.BorderLayout;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.*;
import java.util.regex.*;

public class InputPrompt extends JFrame{
	public JScrollPane sp;
	private JPanel contentPane;
	private JTextField textField;
	private JTextField textField_1;
	public JTextArea textField_2;
	public String f_add;
	public String l_add;
	public boolean done;
	public int scan;
	public JRadioButton rdbtnFastScan;
	public JRadioButton rdbtnNormalScan;
	public JLabel lblProgress;

	public InputPrompt() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 477);
		contentPane = new JPanel();
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
		
		textField_1 = new JTextField();
		textField_1.setBounds(163, 156, 179, 19);
		contentPane.add(textField_1);
		textField_1.setColumns(10);
		
		JButton btnGo = new JButton("GO!!");
		btnGo.setBounds(160, 261, 117, 25);
		contentPane.add(btnGo);
		
		textField_2 = new JTextArea();
		textField_2.setBounds(42, 321, 359, 96);
		contentPane.add(textField_2);
		textField_2.setColumns(10);
		sp = new JScrollPane(textField_2);
		sp.setBounds(42, 321, 380, 110);
		contentPane.add(sp);
		
		lblProgress = new JLabel("Progress:");
		lblProgress.setBounds(42, 294, 70, 15);
		contentPane.add(lblProgress);
		
		rdbtnFastScan = new JRadioButton("Fast Scan");
		rdbtnFastScan.setBounds(60, 202, 149, 23);
		contentPane.add(rdbtnFastScan);
		
		rdbtnNormalScan = new JRadioButton("Normal Scan");
		rdbtnNormalScan.setBounds(213, 202, 149, 23);
		contentPane.add(rdbtnNormalScan);
		
	
		btnGo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(rdbtnFastScan.isSelected())
					scan = 1;
				else if(rdbtnNormalScan.isSelected())
					scan = 0;
				f_add = textField.getText();
				l_add = textField_1.getText();

				String IPADDRESS_PATTERN = 
				"(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

				Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);

				Matcher matcher1 = pattern.matcher(f_add);
				Matcher matcher2 = pattern.matcher(l_add);

				if (matcher1.find() && matcher2.find()){
					done = true;
					textField_2.setText("");
				}
				else
					textField_2.setText("Enter Valid Address.\n");
				
			}
		});
	     ButtonGroup bG = new ButtonGroup();
	     bG.add(rdbtnFastScan);
	     bG.add(rdbtnNormalScan);
	     this.add(rdbtnFastScan);
	     this.add(rdbtnNormalScan);
	     rdbtnFastScan.setSelected(true);
	     this.setVisible(true);
	}
}

