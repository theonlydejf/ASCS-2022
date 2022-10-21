import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;



public class OpenCVTest extends JFrame
{
	private JPanel contentPane;
	static
	{
		System.out.println("knihovna...");
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.out.println("knihovna hotova!");

	}
	
	public static void main(String[] args)
	{
		OpenCVTest frame = new OpenCVTest();
	}

	public OpenCVTest()
	{
		System.out.println("okno se pousti...");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(0, 0, 1280, 720);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		setVisible(true);

		videoCap = new VideoCap();
		System.out.println("poustim vlakno...");
		new MyThread().start();
	}

	VideoCap videoCap;

	public void paint(Graphics g)
	{
		g = contentPane.getGraphics();
		g.drawImage(videoCap.getOneFrame(), 0, 0, this);
	}

	class MyThread extends Thread
	{
		@Override
		public void run()
		{
			for (;;)
			{
				repaint();
				try
				{
					Thread.sleep(30);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
	}

	static class VideoCap
	{

		VideoCapture cap;
		Mat2Image mat2Img = new Mat2Image();

		VideoCap()
		{
			cap = new VideoCapture();
			cap.open(0);
		}

		BufferedImage getOneFrame()
		{
			cap.read(mat2Img.mat);
			return mat2Img.getImage(mat2Img.mat);
		}
	}

	static class Mat2Image
	{

		Mat mat = new Mat();
		BufferedImage img;

		public Mat2Image()
		{
		}

		public Mat2Image(Mat mat)
		{
			getSpace(mat);
		}

		public void getSpace(Mat mat)
		{
			int type = 0;
			if (mat.channels() == 1)
			{
				type = BufferedImage.TYPE_BYTE_GRAY;
			}
			else if (mat.channels() == 3)
			{
				type = BufferedImage.TYPE_3BYTE_BGR;
			}
			this.mat = mat;
			int w = mat.cols();
			int h = mat.rows();
			if (img == null || img.getWidth() != w || img.getHeight() != h || img.getType() != type)
				img = new BufferedImage(w, h, type);
		}

		BufferedImage getImage(Mat mat)
		{
			getSpace(mat);
			WritableRaster raster = img.getRaster();
			DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
			byte[] data = dataBuffer.getData();
			mat.get(0, 0, data);
			return img;
		}
	}
}