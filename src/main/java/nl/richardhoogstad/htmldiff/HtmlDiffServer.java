package nl.richardhoogstad.htmldiff;


import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

public class HtmlDiffServer {
	public static HtmlDiffHandler handler;

	public static HtmlDiffService.Processor processor;

	public static void main(String[] args) {
		try {
			handler = new HtmlDiffHandler();
			processor = new HtmlDiffService.Processor(handler);

			final Runnable simple = new Runnable() {
				public void run() {
					simple(processor);
				}
			};

			new Thread(simple).start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void simple(HtmlDiffService.Processor processor) {
		try {
			TServerTransport serverTransport = new TServerSocket(9191);
			TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));
			System.out.println("Starting the simple server...");
			server.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
