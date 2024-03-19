/** Web server program
 *
 *  @author Luke Kastern, Rogitha Luecke, Mitchell Volk
 *
 *  @version CS 391 - Fall 2024 - A3
 **/

import java.io.*;
import java.net.*;
import java.util.*;

public class A3
{
    static ServerSocket serverSocket = null;  
    // listening socket
    static int portNumber = 5555;             
    // port on which server listens
    static Socket clientSocket = null;        
    // socket to a client
    
    /* Start the server then repeatedly wait for a connection 
    request, accept, and start a new thread to service the request
     */
    public static void main(String args[])
    {
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("%% Server started: " + serverSocket);

            while (true)
            {
                System.out.println("%% Waiting for a client...\n");
                clientSocket = serverSocket.accept();
                System.out.println("%% New connection" + 
                    " established: " + clientSocket);
                (new Thread( new WebServer(clientSocket))).start();
            }
        } catch (IOException e) {
            System.out.println("Server encountered an error." +
                " Shutting down...");
        }
    }// main method
}// A3 class

class WebServer implements Runnable
{
    static int numConnections = 0;   // number of ongoing connections
    Socket clientSocket = null;      // socket to client    
    BufferedReader in = null;        // input stream from client
    DataOutputStream out = null;     // output stream to client

    /* Store a reference to the client socket, update and display the
    number of connected clients, and open I/O streams
    **/
    WebServer(Socket clientSocket)
    {
        this.clientSocket = clientSocket;
        numConnections++;
        System.out.println("%% [# of conncected clients: " + 
                            numConnections + "]");
        
        try{               
            openStreams(clientSocket);
        } catch (IOException e){
            System.err.println("Error in WebServer(): " 
                + e.getMessage());    
        } 

    }// constructor

    /* Each WebServer thread processes one HTTP GET request and
    then closes the connection
    **/
    public void run()
    {
        try{
            openStreams(clientSocket);
            processRequest();
        } catch(IOException e){
            System.err.println("Error in run(): " + 
                e.getMessage());
        } finally {
            close();
        }
    }// run method

    /* Parse the request then send the appropriate HTTP response
    making sure to handle all of the use cases listed in the A3
    handout, namely codes 200, 404, 418, 405, and 503 responses.
    **/
    void processRequest()
    {
        String[] request;
        try{
            request = parseRequest();

            System.out.println("*** Response ***");

            if(!request[0].equals("GET")){
                writeCannedResponse(request[2], 405, 
                "Method not allowed");
                return;
            }
            switch (request[1]) {
                
                case "/coffee":
                    writeCannedResponse(request[2], 418, 
                    "I'm a teapot");
                    break;
                case "/tea/coffee": 
                    writeCannedResponse(request[2], 503,
                    "Coffee is temporarily unavailable");
                    break;
                default:
                    File file = new File("." + request[1]);
                    if(!file.exists()) {
                        write404Response(request[2], request[1]);
                    } else {
                        byte[] fileContents = loadFile(file);
                        write200Response(request[2], 
                                        fileContents, 
                                        request[1]);
                    }
            }
        } catch (IOException e){
            System.err.println("Error in processRequest(): " 
                + e.getMessage());
        }

    }// processRequest method

    /* Read the HTTP request from the input stream line by line 
    up to and including the empty line between the header and 
    the body. Send to the console every line read (except the last,
    empty line). Then extract from the first line the HTTP command,
    the path to the requested file, and the protocol description 
    string and return these three strings in an array.
    **/
    String[] parseRequest() throws IOException
    {
        String[] returnMe = new String[3];
        String line = in.readLine();
        System.out.println("*** request ***\n" +
                            "     " + line);
        Scanner sc = new Scanner(line);
        for(int i = 0; i < returnMe.length && sc.hasNext(); i++){
            returnMe[i] = sc.next();
        }
        line = in.readLine();
        while(line != null && !line.isEmpty()){
            System.out.println("     " + line);
            line = in.readLine();
        }
        System.out.println();
        sc.close();
        return returnMe;
    }// parseRequest method

    /* Given a File object for a file that we know is stored on the
       server, return the contents of the file as a byte array
    **/
    byte[] loadFile(File file)
    {
        byte returnMe[] = new byte[(int)file.length()];
        try{
            FileInputStream input = new FileInputStream(file);
            input.read(returnMe);
            input.close();
        } catch (IOException e){
            System.out.println("File could not be loaded: " +
                               e.getMessage());
        }
        return returnMe;

    }// loadFile method

    /* Given an HTTP protocol description string, a byte array, 
    and a file name, send back to the client a 200 HTTP response 
    whose body is the input byte array. The file name is used 
    to determine the type ofWeb resource that is being returned. 
    The set of required header fields and file types 
    is spelled out in the A3 handout.
    **/
    void write200Response(String protocol, byte[] body,
                                     String pathToFile) {
    try {
        String message = String.format("%s 200 Document Follows\n" +
                                       "Content-Length: %d\n\n",
                                       protocol, body.length);
        out.write(message.getBytes());
        out.write(body);
        out.flush();
        System.out.println(String.format("     %s 200 Document " +
                                         "Follows\n     Con"     +
                                         "tent-Length: %d\n     ",
                                         protocol, body.length));
        System.out.println("     <file contents not shown>\n");
        
    } catch (IOException e) {
        System.out.println(e);
    }
}


    /* Given an HTTP protocol description string 
    and a path that does not referto any of the existing files 
    on the server, return to the client a 404 HTTP response whose 
    body is a dynamically created page whose contentis spelled out 
    in the A3 handout. The only HTTP header to be included 
    in the response is "Content-Type".
    **/
    void write404Response(String protocol, String pathToFile)
    {
        try{
            String message = String.format("%s 404 Not found\n"     +
            "Content-Type: text/html\n\n"                           +
            "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"   +
            "<title>Page not found</title></head><body>"            +
            "<h1>HTTP Error 404 Not Found</h1><h2>The file<span"    +
            " style=\"color: red\">%s</span> does not exist on this"+
            " server.</h2></html></body>", protocol, pathToFile);
            System.out.println("     " + new String(message)
                      .replaceAll("\n", "\n     "));
            out.writeUTF(message);

            System.out.println();
        } catch(IOException e ){
            System.err.println("Error in write404Response(): " 
                + e.getMessage());
        }
    }// write404Response method

    /* Given an HTTP protocol description string, a byte array, 
    and a file name, send back to the client a 200 HTTP response 
    whose body is the input byte array. The file name is used to 
    determine the type of Web resource that is being returned. 
    The only HTTP header to be included 
    in the response is "Content-Type".
    **/
    void writeCannedResponse(String protocol, int code, 
                             String description) {
    try {
        String message = String.format("%s %d %s\n" +
            "Content-Type: text/html\n\n", 
                protocol, code, description);
        byte[] cannedFile = loadFile(new File(String.format
                                    ("./html/%d.html", code)));
        out.write(message.getBytes());
        out.write(cannedFile);
        out.flush();
        System.out.println(String.format("     %s %d %s\n     " +
                                    "Content-Type: text/html\n", 
                                    protocol, code, description));
        System.out.println("     <contents of html/" + code +
                           ".html not shown>\n");
    } catch (IOException e) {
        System.err.println("Error in writeCannedResponse(): " +  
                            e.getMessage());
    }
}


    /* open the necessary I/O streams and initialize the in and out
       variables; this method does not catch any IO exceptions.
    **/    
    void openStreams(Socket clientSocket) throws IOException
    {
        in = new BufferedReader(new InputStreamReader(
                                clientSocket.getInputStream()));
        out = new DataOutputStream(clientSocket.getOutputStream());
    }// openStreams method

    /* close all open I/O streams and sockets; also update and 
    display the number of connected clients.
    **/
    void close() {
    try (BufferedReader closedIn = in;
         DataOutputStream closedOut = out;
         Socket closedClientSocket = clientSocket) {

        numConnections--;
        System.out.println("%% Connection released: "     + 
                            clientSocket);
        System.out.println("%% [# of connected clients: " + 
                            numConnections + "]");
    } catch (IOException e) {
        System.err.println("Error in close(): " + e.getMessage());
    }
}
}// WebServer class
