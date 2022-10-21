import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.visp.core.VpImageRGBa;
import org.visp.core.VpImageUChar;
import org.visp.io.VpImageIo;

public class ViSPTest extends JFrame
{
	private ImagePanel _panel;
	private VpImageUChar I;
	
	static
	{
		System.loadLibrary("visp_java350");
	}
	
	public static void main(String[] args)
	{
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				new ViSPTest();
			}
		});
	}

	public ViSPTest()
	{
		setLayout(new BorderLayout());
		setTitle("TDN API Client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(true);
		
		I = new VpImageUChar();
		
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.jpeg, *.png, *.pgm, *.ppm)", "jpg", "jpeg",
			"png", "pgm", "ppm"));
		//int returnVal = fc.showOpenDialog(ViSPTest.this);
		if (false)//returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			VpImageIo.read(I, file.getAbsolutePath());
			System.out.println(file.getAbsolutePath());
		}
		else
		{
		}
		
		VpImageIo.read(I, "/Users/david/Downloads/set-of-20-magnetic-apriltags-15-x-15cm_2.jpeg");
		_panel = new ImagePanel(toBufferedImage(I));
		_panel.setPreferredSize(new Dimension(500, 500));
		getContentPane().add(_panel);
		pack();
		//_paintPanel.addMouseListener(this);

		setLocationRelativeTo(null);

		setVisible(true);
	}
	
	public BufferedImage toBufferedImage(VpImageUChar image)
	{
		int type = BufferedImage.TYPE_BYTE_GRAY;
		byte[] b = image.getPixels(); // get all the pixels
		BufferedImage I = new BufferedImage(image.cols(), image.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) I.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return I;
	}

	static class ImagePanel extends JPanel
	{
		private BufferedImage _img;
		
		public ImagePanel(BufferedImage img)
		{
			_img = img;
		}
		
		@Override
		public void paintComponent(Graphics g)
		{
			((Graphics2D)g).drawImage(_img, null, 0, 0);
		}
	}
}
