import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Color;
import javax.swing.border.LineBorder;

enum PaintMode {
	Pixel, Area
};

public class UI extends JFrame {
	private JTextField msgField;
	private JTextArea chatArea;
	private JPanel pnlColorPicker;
	private JPanel paintPanel;
	private JToggleButton tglPen;
	private JToggleButton tglBucket;
	private static UI instance;
	private int selectedColor = -543230; // golden
	int[][] data = new int[50][50]; // pixel color data array
	int blockSize = 16;
	PaintMode paintMode = PaintMode.Pixel;
	Socket socket; // own
	DataInputStream in;
	DataOutputStream out;
	String request = "ConnectRequest";
	int port = 5555;
	DatagramSocket dataSocket; // udp send
	ArrayList<String> serName = new ArrayList<String>(); // udp receive
	ArrayList<String> serAddr = new ArrayList<String>();
	ArrayList<Integer> serPort = new ArrayList<Integer>();

	ArrayList<Socket> list = new ArrayList();
	static Server serClass;

	/**
	 * get the instance of UI. Singleton design pattern.
	 *
	 * @return
	 * @throws IOException
	 */
	public static UI getInstance() throws IOException {
		if (instance == null)
			serClass = new Server();
		instance = new UI();
		return instance;
	}

	private void receiveChatMessage(DataInputStream in) throws IOException {
		byte[] buffer = new byte[1024];
		int len = in.readInt();
		in.read(buffer, 0, len);
		String msg = new String(buffer, 0, len);
		System.out.println(msg);
		SwingUtilities.invokeLater(() -> {
			chatArea.append(msg + "\n");
		});
	}

	private void receivePixelMessage(DataInputStream in) throws IOException {
		int color = in.readInt();
		int x = in.readInt();
		int y = in.readInt();
		System.out.printf("%d-(%d,%d)\n", color, x, y);
		paintPixel(color, x, y);
	}

	public String getName() {
		String s = JOptionPane.showInputDialog(null, "user name: ", null, JOptionPane.PLAIN_MESSAGE);
		return s;
	}

	public void send() throws IOException { // send UDP request[need receive]
		byte[] msg = request.getBytes();
		InetAddress dest = InetAddress.getByName("255.255.255.255");
		DatagramPacket packet1 = new DatagramPacket(msg, msg.length, dest, 6666);
		dataSocket.send(packet1);
		System.out.println("send request+packet1= " + packet1.getPort());// done
	}

	public void receive() throws SocketException, IOException { // udp receive
		DatagramPacket packet2 = new DatagramPacket(new byte[1024], 1024);
		System.out.println("waiting for receiving");
		dataSocket.receive(packet2);
		System.out.println("received");
		byte[] data = packet2.getData();
		String str = new String(data, 0, packet2.getLength());
		String Addr = packet2.getAddress().toString();
		int srcPort = packet2.getPort();

		serName.add("Create");
		serAddr.add("127.0.0.1");
		serPort.add(6666);

		String srcAddr = Addr.replace("/", "");
		if (!str.equals(request)) {
			System.out.println("received package!");
			serName.add(str);
			serAddr.add(srcAddr);
			serPort.add(srcPort);
			System.out.println("serName= " + serName);
			System.out.println("serAddr= " + serAddr);
			System.out.println("serPort= " + serPort);
		}
	}

	public String start() { // keep sending request
		while (serName.size() == 0) {
			try {
				send();
				Thread.sleep(1000);
				System.out.println("a loop");
			} catch (IOException | InterruptedException e) {
				System.err.printf("Unsuccessful boardcast");
			}
		}
		String chosenSer = chooseServer();
		return chosenSer;

		// tentative
		/*
		 * String chosenSer = chooseServer(); if (chosenSer.equals("Create")) { try {
		 * begin(); } catch (IOException e) { e.printStackTrace(); } } return chosenSer;
		 */
	}

	private String chooseServer() {
		String[] ser = serName.toArray(new String[serName.size()]);
		String s = (String) JOptionPane.showInputDialog(null, "Choose a server", null, JOptionPane.PLAIN_MESSAGE, null,
				ser, null);
		return s;
	}

	private int getServerIndex(String Chosenserver) {
		for (String value : serName) {
			System.out.println("server name = " + value);
		}
		int pos = serName.indexOf(Chosenserver);
		System.out.println("pos = " + pos);
		return pos;
	}

	/*
	 * private void host() { //create server try { serClass = new Server(); Thread
	 * serverThread = new Thread(() -> { try { serClass.begin(); } catch
	 * (IOException e) { e.printStackTrace(); } }); serverThread.start(); } catch
	 * (IOException e) { e.printStackTrace(); } }
	 */

	private void receive(DataInputStream in) {
		try {
			while (true) {
				int type = in.readInt();
				switch (type) {
				case 0:
					receiveChatMessage(in);
					break;
				case 1:
					receivePixelMessage(in);
				case 2:
					// TODO
					break;
				default:
					// TODO
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * private constructor. To create an instance of UI, call UI.getInstance()
	 * instead.
	 *
	 * @throws IOException
	 */
	private UI() throws IOException {
		// serClass = this.serClass;

		dataSocket = new DatagramSocket(port);
		// dataSocket2 = new DatagramSocket(6665);
		Thread th = new Thread(() -> {
			try {
				receive();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		th.start();

		String name = getName();
		String server = start();
		int pos = getServerIndex(server);
		String socketAddr = serAddr.get(pos);
		int socketPort = serPort.get(pos);
		System.out.println("pos= " + pos);
		System.out.println("wait for socket connect");
		if (pos == 0) {

			ServerSocket serverSocket = new ServerSocket(port); // create server
			while(true) {
				Thread sert = new Thread(() -> {
			
				try {
					serClass.receive();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			sert.start();

			socket = new Socket(socketAddr, socketPort);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				Thread ts = new Thread(() -> {
					synchronized (list) {
						list.add(clientSocket);
					}
					try {
						serClass.serve(clientSocket);
					} catch (IOException ex) {
					}
					synchronized (list) {
						list.remove(clientSocket);
					}
				});
				ts.start();
			}

		} else {
			System.out.println("socketAddr= " + socketAddr + "& socketPort= " + socketPort);
			socket = new Socket(socketAddr, socketPort);
		}
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		System.out.println("connected");
		JPanel basePanel = new JPanel();
		getContentPane().add(basePanel, BorderLayout.CENTER);
		basePanel.setLayout(new BorderLayout(0, 0));
		Thread t = new Thread(() -> {
			receive(in);
		});
		t.start();
		setTitle("KidPaint");
		paintPanel = new JPanel() {
			// refresh the paint panel
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				Graphics2D g2 = (Graphics2D) g; // Graphics2D provides the setRenderingHints method
				// enable anti-aliasing
				RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHints(rh);
				// clear the paint panel using black
				g2.setColor(Color.black);
				g2.fillRect(0, 0, this.getWidth(), this.getHeight());
				// draw and fill circles with the specific colors stored in the data array
				for (int x = 0; x < data.length; x++) {
					for (int y = 0; y < data[0].length; y++) {
						g2.setColor(new Color(data[x][y]));
						g2.fillArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
						g2.setColor(Color.darkGray);
						g2.drawArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
					}
				}
			}
		};
		paintPanel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			// handle the mouse-up event of the paint panel
			@Override
			public void mouseReleased(MouseEvent e) {
				if (paintMode == PaintMode.Area && e.getX() >= 0 && e.getY() >= 0)
					paintArea(e.getX() / blockSize, e.getY() / blockSize);
			}
		});
		paintPanel.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0)
					paintPixel(e.getX() / blockSize, e.getY() / blockSize);
				try {
					out.writeInt(1);
					out.writeInt(selectedColor);
					out.writeInt(e.getX() / blockSize);
					out.writeInt(e.getY() / blockSize);
					out.flush();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
			}
		});
		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));
		JScrollPane scrollPaneLeft = new JScrollPane(paintPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		basePanel.add(scrollPaneLeft, BorderLayout.CENTER);
		JPanel toolPanel = new JPanel();
		basePanel.add(toolPanel, BorderLayout.NORTH);
		toolPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		pnlColorPicker = new JPanel();
		pnlColorPicker.setPreferredSize(new Dimension(24, 24));
		pnlColorPicker.setBackground(new Color(selectedColor));
		pnlColorPicker.setBorder(new LineBorder(new Color(0, 0, 0)));
		// show the color picker
		pnlColorPicker.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				ColorPicker picker = ColorPicker.getInstance(UI.instance);
				Point location = pnlColorPicker.getLocationOnScreen();
				location.y += pnlColorPicker.getHeight();
				picker.setLocation(location);
				picker.setVisible(true);
			}
		});
		toolPanel.add(pnlColorPicker);
		tglPen = new JToggleButton("Pen");
		tglPen.setSelected(true);
		toolPanel.add(tglPen);
		tglBucket = new JToggleButton("Bucket");
		toolPanel.add(tglBucket);
		// change the paint mode to PIXEL mode
		tglPen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(true);
				tglBucket.setSelected(false);
				paintMode = PaintMode.Pixel;
			}
		});
		// change the paint mode to AREA mode
		tglBucket.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(true);
				paintMode = PaintMode.Area;
			}
		});
		JPanel msgPanel = new JPanel();
		getContentPane().add(msgPanel, BorderLayout.EAST);
		msgPanel.setLayout(new BorderLayout(0, 0));
		msgField = new JTextField(); // text field for inputting message
		msgPanel.add(msgField, BorderLayout.SOUTH);
		// handle key-input event of the message field
		msgField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == 10) { // if the user press ENTER
					String Ntext = name + " : " + msgField.getText();
					onTextInputted(Ntext);
					msgField.setText("");
				}
			}
		});
		chatArea = new JTextArea(); // the read only text area for showing messages
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		JScrollPane scrollPaneRight = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneRight.setPreferredSize(new Dimension(300, this.getHeight()));
		msgPanel.add(scrollPaneRight, BorderLayout.CENTER);
		this.setSize(new Dimension(800, 600));
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
	}

	/**
	 * it will be invoked if the user selected the specific color through the color
	 * picker
	 *
	 * @param colorValue - the selected color
	 */
	public void selectColor(int colorValue) {
		SwingUtilities.invokeLater(() -> {
			selectedColor = colorValue;
			pnlColorPicker.setBackground(new Color(colorValue));
		});
	}

	/**
	 * it will be invoked if the user inputted text in the message field
	 *
	 * @param text - user inputted text
	 */
	private void onTextInputted(String text) {
		chatArea.setText(chatArea.getText() + text + "\n");
		try {
			out.writeInt(0);
			out.writeInt(text.length());
			out.write(text.getBytes());
			out.flush();
		} catch (IOException ex) {
			// TODO:handle error
		}
	}

	/**
	 * change the color of a specific pixel
	 *
	 * @param col, row - the position of the selected pixel
	 */
	public void paintPixel(int col, int row) {
		paintPixel(selectedColor, col, row);
	}

	public void paintPixel(int color, int col, int row) {
		if (col >= data.length || row >= data[0].length)
			return;
		data[col][row] = color;
		paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);
	}

	/**
	 * change the color of a specific area
	 *
	 * @param col, row - the position of the selected pixel
	 * @return a list of modified pixels
	 */
	public List paintArea(int col, int row) {
		LinkedList<Point> filledPixels = new LinkedList<Point>();
		if (col >= data.length || row >= data[0].length)
			return filledPixels;
		int oriColor = data[col][row];
		LinkedList<Point> buffer = new LinkedList<Point>();
		if (oriColor != selectedColor) {
			buffer.add(new Point(col, row));
			while (!buffer.isEmpty()) {
				Point p = buffer.removeFirst();
				int x = p.x;
				int y = p.y;
				if (data[x][y] != oriColor)
					continue;
				data[x][y] = selectedColor;
				filledPixels.add(p);
				if (x > 0 && data[x - 1][y] == oriColor)
					buffer.add(new Point(x - 1, y));
				if (x < data.length - 1 && data[x + 1][y] == oriColor)
					buffer.add(new Point(x + 1, y));
				if (y > 0 && data[x][y - 1] == oriColor)
					buffer.add(new Point(x, y - 1));
				if (y < data[0].length - 1 && data[x][y + 1] == oriColor)
					buffer.add(new Point(x, y + 1));
			}
			paintPanel.repaint();
		}
		return filledPixels;
	}

	/**
	 * set pixel data and block size
	 *
	 * @param data
	 * @param blockSize
	 */
	public void setData(int[][] data, int blockSize) {
		this.data = data;
		this.blockSize = blockSize;
		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));
		paintPanel.repaint();
	}
}
