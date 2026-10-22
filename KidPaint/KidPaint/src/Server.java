import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
public class Server {
	ServerSocket serverSocket;
	ArrayList<Socket> list = new ArrayList();
	DatagramSocket dataSocket,dataSocket2;
	int port=6666;
	
	public Server() throws IOException {
		dataSocket = new DatagramSocket(port);
		dataSocket2= new DatagramSocket(6665);
		serverSocket = new ServerSocket(port);
		Thread th = new Thread(() -> {
			try {
				receive();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		th.start();
		
		while (true) {
			Socket clientSocket = serverSocket.accept();
			Thread t = new Thread(() -> {
				synchronized (list) {
					list.add(clientSocket);
				}
				try {
					serve(clientSocket);
				} catch (IOException ex) {
				}
				synchronized (list) {
					list.remove(clientSocket);
				}
			}
			);
			t.start();
		}
		
	}
	public void receive() throws SocketException, IOException, UnknownHostException { // udp receive
		DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
		System.out.println("Waiting for broadcast");
		dataSocket.receive(packet);
		System.out.println("received");
		byte[] data = packet.getData();
		String str = new String(data, 0, packet.getLength());
		String srcAddr = packet.getAddress().toString();
		int srcPort = packet.getPort();
		System.out.println("received request!"); //done
		
		String Addr=srcAddr.replace("/","");
		if (str.equals("ConnectRequest")) {
			send(Addr,srcPort);
			str = "";
		}
	}
	
	public void send(String Addr,int srcPort) throws IOException { // udp send
		String serN = "Studio 1";
		byte[] msg = serN.getBytes();
		InetAddress dest = InetAddress.getByName(Addr);
		DatagramPacket packet = new DatagramPacket(msg, msg.length, dest, srcPort);
		dataSocket.send(packet);
		System.out.println("srcPort="+srcPort+"---package sent!");
	}
	
	private void handleChatMessage(DataInputStream in) throws IOException {
		byte[] buffer = new byte[1024];
		int len = in.readInt();
		in.read(buffer, 0, len);
		System.out.println(new String(buffer, 0, len));
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				try {
					Socket s = list.get(i);
					DataOutputStream out = new DataOutputStream(s.getOutputStream());
					out.writeInt(0);
					out.writeInt(len);
					out.write(buffer, 0, len);
					out.flush();
				} catch (IOException ex) {
					System.out.println("The client is disconnected already!");
				}
			}
		}
	}
	private void handlePixelMessage(DataInputStream in) throws IOException {
		int color = in.readInt();
		int x = in.readInt();
		int y = in.readInt();
		System.out.printf("%d-(%d,%d)\n", x, y);
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				try {
					Socket s = list.get(i);
					DataOutputStream out = new DataOutputStream(s.getOutputStream());
					out.writeInt(1);
					out.writeInt(color);
					out.writeInt(x);
					out.writeInt(y);
					out.flush();
				} catch (IOException ex) {
				}
			}
		}
	}
	void serve(Socket clientSocket) throws IOException {					//private         			
		DataInputStream in = new DataInputStream(clientSocket.getInputStream());
		while (true) {
			int type = in.readInt();
			switch (type) {
			case 0:
				handleChatMessage(in);
				break;
			case 1:
				handlePixelMessage(in);
				break;
			case 2:
				// TODO
			default:
				// TODO
			}
		}
	}
	public static void main(String[] args) throws IOException {
		new Server();
	}
}

