package team.hobbyrobot.net.api.desktop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import team.hobbyrobot.utils.ProgressReporter;

public class ProgressWindow extends JFrame
{
	public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
	public static final Font PROGRESS_LBL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

	public static void main(String[] args) throws InterruptedException
	{
		ProgressReporter reporter = new ProgressReporter();
		ProgressWindow win = new ProgressWindow("Test progress", reporter);

		Thread.sleep(1000);
		reporter.setProgress(10);
		reporter.setMessage("Ahoj 1");
		reporter.setReportChanged();
		Thread.sleep(300);
		reporter.setProgress(20);
		reporter.setMessage("Ahoj 2");
		reporter.setReportChanged();
		Thread.sleep(300);
		reporter.setProgress(30);
		reporter.setMessage("Ahoj 3");
		reporter.setReportChanged();
		Thread.sleep(300);
		reporter.setProgress(40);
		reporter.setMessage("Ahoj 4");
		reporter.setReportChanged();
		Thread.sleep(400);
		reporter.setProgress(80);
		reporter.setMessage("Ahoj 5");
		reporter.setReportChanged();
		Thread.sleep(600);
		reporter.setProgress(100);
		reporter.setMessage("Ahoj final");
		reporter.setDone();
	}

	private JProgressBar _progressBar;
	private JLabel _progressLbl;

	private Timer _updateTimer = new Timer();
	private ProgressReporter _reporter;
	
	public ProgressWindow(String title, ProgressReporter reporter)
	{
		_reporter = reporter;

		setTitle("TDN API Client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(true);
		setPreferredSize(new Dimension(250, 100));

		Box box = new Box(BoxLayout.Y_AXIS);
		box.setBorder(new EmptyBorder(10, 10, 15, 10));
		box.add(Box.createVerticalGlue());

		JLabel titleLbl = new JLabel(title);
		titleLbl.setFont(TITLE_FONT);
		titleLbl.setHorizontalAlignment(JLabel.CENTER);
		titleLbl.setAlignmentX(CENTER_ALIGNMENT);
		box.add(titleLbl);

		_progressBar = new JProgressBar(0, 100);
		box.add(_progressBar);

		_progressLbl = new JLabel("Currently nothing is happening");
		_progressLbl.setFont(PROGRESS_LBL_FONT);
		_progressLbl.setHorizontalAlignment(JLabel.CENTER);
		_progressLbl.setAlignmentX(CENTER_ALIGNMENT);
		box.add(_progressLbl);

		box.add(Box.createVerticalGlue());
		//_paintPanel.addMouseListener(this);

		add(box);
		pack();

		setLocationRelativeTo(null);

		setVisible(true);

		_updateTimer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				update();
			}
		}, 100, 100);
	}

	private void update()
	{
		if (!_reporter.checkReportChanged())
			return;
		_progressBar.setValue((int) _reporter.getProgress());
		_progressLbl.setText(_reporter.getMessage());

		if (_reporter.isDone())
		{
			_updateTimer.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					setVisible(false);
					dispose();
					_updateTimer.cancel();
					_updateTimer.purge();
				}
			}, 500);
			return;
		}
	}
}
