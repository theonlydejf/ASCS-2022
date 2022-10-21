package team.hobbyrobot.net.api.desktop;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

public class ClientWindow extends JFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private TDNViewer _tdnViewer;
	
	public static void main(String[] args)
	{
		ClientWindow win = new ClientWindow();
	}
	
	public ClientWindow()
	{
		setLayout(new BorderLayout());
		setTitle("TDN API Client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		
		_tdnViewer = new TDNViewer();
		_tdnViewer.setPreferredSize(new Dimension(500, 500));
		getContentPane().add(_tdnViewer);
		
		//_paintPanel.addMouseListener(this);

		pack();

		setLocationRelativeTo(null);

		setVisible(true);

	}
}
