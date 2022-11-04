package team.hobbyrobot.net.api.tests;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map.Entry;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.api.Service;
import team.hobbyrobot.net.api.TDNAPIClient;
import team.hobbyrobot.net.api.TDNAPIServer;
import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.tdn.base.TDNArray;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNRootParser;
import team.hobbyrobot.tdn.core.TDNValue;

public class TDNAPITestServer 
{
    /** First argument is port, second is service name */
    public static void main(String[] args) throws UnknownHostException, IOException
    {
        Logger serverLogger = new Logger("SERVER");
        serverLogger.registerEndpoint(new PrintWriter(System.out));
        
        System.out.println("Using service name \"" + args[1] + "\"");
        int port = Integer.parseInt(args[0]);
        System.out.println("Starting server on port " + port);
        System.out.println("Press any key to close the server...");
        
        TDNAPIServer server = new TDNAPIServer(port, serverLogger, serverLogger.createSubLogger("ERR"));
        server.registerService(args[1], new TestService());
        server.setVerbosity(VerbosityLogger.DEBUGGING);
        Thread serverThread = server.createThread();
        serverThread.start();
        server.startRegisteringClients();
        
        System.in.read();
        server.close();

/*      System.in.read();
        System.out.println("Stopping");
        try
        {
            server.stop();          
        }
        catch(IOException ex)
        {
            System.out.println("exception cought");
        }
        System.in.read();*/
        //System.exit(0);
    }

    public static class TestService implements Service
    {
        private static Logger logger;
        
        static
        {
            logger = new Logger("SERVICE");
            logger.registerEndpoint(new PrintWriter(System.out));
        }
        
        @Override
        public TDNRoot processRequest(String request, TDNRoot params, Socket client)
            throws UnknownRequestException, RequestParamsException, RequestGeneralException
        {
            logger.log("rqst:");
            logger.log(">\t" + request);
            logger.log("params:");
            logRoot(params, logger, ">\t");
            TDNValue array = new TDNValue(new TDNArray(new Object[] {0, 1, 2, 3, 4}, TDNParsers.INTEGER), TDNParsers.ARRAY);
            TDNRoot root = new TDNRoot();
            root.insertValue("cislo", new TDNValue(420.69f, TDNParsers.FLOAT));
            root.insertValue("druhePole", array);
            root.insertValue("mujText", new TDNValue("Ahoj, tohle je text", TDNParsers.STRING));
            
            return new TDNRoot()
                    .insertValue("mojePole", array)
                    .insertValue("mojeCeleCislo", new TDNValue(69420, TDNParsers.INTEGER))
                    .insertValue("poleObjektu", new TDNValue(new TDNArray
                            (
                                new Object[] { root, root, root },
                                TDNParsers.ROOT
                            ), TDNParsers.ARRAY));
        }

        @Override
        public void init()
        {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    public static void logRoot(TDNRoot root, Logger logger, String prefix)
    {
        logger.log(prefix + "(");
        StringBuilder sb = new StringBuilder();
        for (Entry<String, TDNValue> val : root)
        {
            sb.append(val.getKey());
            sb.append(": ");
            if (val.getValue().value instanceof TDNRoot)
            {
                logRoot((TDNRoot) val.getValue().value, logger, prefix);
                continue;
            }
            if (val.getValue().value instanceof TDNArray)
            {
                logger.log(prefix + sb.toString() + "[");
                sb = new StringBuilder();
                TDNArray arr = (TDNArray) val.getValue().value;
                for (Object item : arr)
                {
                    sb.append(",");
                    if (arr.itemParser.typeKey().equals(new TDNRootParser().typeKey()))
                        logRoot((TDNRoot) item, logger, prefix);
                    else
                    {
                        logger.log(prefix + sb.toString() + item);
                        sb = new StringBuilder();
                    }
                    continue;
                }
                logger.log(prefix + sb.toString() + "]");
                sb = new StringBuilder();
                continue;
            }
            logger.log(prefix + sb.toString() + val.getValue().value);
            sb = new StringBuilder();
        }
        logger.log(prefix + sb.toString() + ")");
    }
}
