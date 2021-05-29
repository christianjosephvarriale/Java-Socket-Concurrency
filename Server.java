import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;


class Server {

	private static Integer processed_bytes = 0;
	private static Boolean data_processed = false;
	private static Map<String,Integer> degree_map = new ConcurrentHashMap<String,Integer>(); 

	static class DegreeCalculatorThread implements Runnable  {    

		private String line;
		private int len;
	
		public DegreeCalculatorThread(String line, int len)  {   
			this.line = line;
			this.len = len;
		}  
	
		public void run()  {  
			String[] vertices = line.split("\\s+");
			for (int i=0; i< vertices.length; i++) {
				updateCount(vertices[i]);
			}
	
			synchronized ( processed_bytes ) {
				processed_bytes = processed_bytes + line.length();
	
				if ( processed_bytes >= len ) {
					data_processed = true;
				}
			}	
		}  
	
		private void updateCount(String vertex)  {  
			
			Integer oldVal, newVal;  
			Integer cnt = degree_map.get(vertex);  
				
			if (cnt == null)  {  
				oldVal = degree_map.putIfAbsent(vertex,  1);  
				if (oldVal == null)  return;  
			}  
			do {  
				oldVal = degree_map.get(vertex);  
				newVal = (oldVal == null) ? 1 : (oldVal + 1);  
			} while  (!degree_map.replace(vertex, oldVal, newVal)); 
		} 
	}     

	public static void main(String args[]) throws Exception {
		
		if (args.length != 1) {
			System.out.println("usage: java Server port");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);

		ServerSocket ssock = new ServerSocket(port);
		System.out.println("listening on port " + port);
		while(true) {
			try {
				/*
				  YOUR CODE GOES HERE
				  
				  - add an inner loop to read requests from this connection
				    repeatedly (client may reuse the connection for multiple
				    requests)
				  - for each request, compute an output and send a response
				  - each message has a 4-byte header followed by a payload
				  - the header is the length of the payload
				    (signed, two's complement, big-endian)
				  - the payload is a string (UTF-8)
				  - the inner loop ends when the client closes the connection
				*/
 
				// accept a connection from the server socket

				Socket clientSocket = ssock.accept();

				
				DataInputStream in = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream dout = new DataOutputStream( clientSocket.getOutputStream());
				
				System.out.println("accepted connection");

				int numThreads = 10;

				ExecutorService pool = Executors.newFixedThreadPool(numThreads);


				// while we are connected, keep going
				while ( clientSocket.isConnected() ) {

					String line;
					int len;
					int chunkSize = 10000000;
					int slack = 100;
					Boolean bytes_read = false;
					int read_bytes = 0;
					byte[] buffer;
					char extraChar;
					int count = 0;

					try {
						// the first line also contains the header, so extract that
						len = in.readInt();
					} catch (Exception NullPointerException) {
						System.out.println( "Client closed the connection" );
						return;
					}

					try {
						if ( read_bytes + chunkSize > len ) { // if we can't read a full chunk then read everything left
							buffer = new byte[len - read_bytes + slack];
							in.readFully(buffer, 0, len - read_bytes);

							// read until we hit a new line
							while ( extraChar = in.readChar() != '\n' ) {
								buffer[len - read_bytes + count] =(byte) extraChar;
								count++;
							}
							buffer[len - read_bytes + count + 1] =(byte) extraChar;

						} else {
							buffer = new byte[chunkSize + slack];
							in.readFully(buffer, 0, chunkSize);

							// read until we hit a new line
							while ( extraChar = in.readChar() != '\n' ) {
								buffer[chunkSize + count] =(byte) extraChar;
								count++;
							}
							buffer[chunkSize + count + 1] =(byte) extraChar;
						}

						line = new String(buffer);
						read_bytes = read_bytes + line.length();
					} catch (Exception IOException) {
						System.out.println( "Client closed the connection" );
						return;
					}


					while ( !bytes_read ) {

						pool.submit(new DegreeCalculatorThread( line, len ));


						if ( read_bytes >= len ) {
							while ( !data_processed ) {
							}
							bytes_read = true;
						}
						else { // read a chunk from the buffer
							try {
								if ( read_bytes + chunkSize > len ) { // if we can't read a full chunk then read everything left
									buffer = new byte[len - read_bytes + slack];
									in.readFully(buffer, 0, len - read_bytes);
								
									while ( extraChar = in.readChar() != '\n' ) {
										buffer[len - read_bytes + count] =(byte) extraChar;
										count++;
									}
									buffer[len - read_bytes + count + 1] =(byte) extraChar;
								} else {
									buffer = new byte[chunkSize + slack];
									in.readFully(buffer, 0, chunkSize);
									
									while ( extraChar = in.readChar() != '\n' ) {
										buffer[chunkSize + count] =(byte) extraChar;
										count++;
									}
									buffer[chunkSize + count + 1] =(byte) extraChar;
								}
								
								// convert the buffer to a string
								line = new String(buffer);
								read_bytes = read_bytes + line.length();
							} catch (Exception IOException) {
								return;
							}
						}
					}
					
					pool.shutdown();

					try {
						pool.awaitTermination(1, TimeUnit.DAYS);
					} catch (InterruptedException e) { 
						System.exit(1); 
					};

					System.out.println( "Threads terminated" );

					// process each vertex
					ArrayList<String> listOfVertices = new ArrayList<String>( degree_map.keySet() );
					ArrayList<String> outputLst = new ArrayList<String>();
					
					for ( int i = 0; i < listOfVertices.size(); i++ ) {
						outputLst.add(listOfVertices.get( i ) + ' ' + degree_map.get( listOfVertices.get( i ) ) ); 
					}

					String joinedString = String.join("\n", outputLst);
					byte[] outputBytes = joinedString.getBytes();

					dout.writeInt(outputBytes.length);
					dout.write( outputBytes );
					dout.flush();

			}

			// close connections
			in.close();
			dout.close();
			ssock.close();
			clientSocket.close();


			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}


