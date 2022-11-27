package team.hobbyrobot.ascsvehicle.testing;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map.Entry;

import lejos.robotics.navigation.Pose;
import team.hobbyrobot.ascsvehicle.navigation.TDNPoseCorrectionProvider;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.SocketLoggerEndpointRegisterer;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.api.Service;
import team.hobbyrobot.net.api.TDNAPIServer;
import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.net.api.streaming.TDNReceiver;
import team.hobbyrobot.net.api.streaming.TDNReceiverListener;
import team.hobbyrobot.subos.navigation.CorrectablePoseProvider;
import team.hobbyrobot.tdn.base.TDNArray;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNRootParser;
import team.hobbyrobot.tdn.core.TDNValue;

public class SimulatedRobot implements TDNReceiverListener
{
    public static final String HELP = 
              "Usage: java -jar SimulatedRobot.jar <loggerPort> <apiPort> <correctorPort> <serviceNames>\n"
            + "Params:\n"
            + "loggerPort:    Port on which logger's endpoint registerer is listening on\n"
            + "apiPort:       Port on which TDN API listens for clients\n"
            + "correctorPort: Port on which simulated robot listens for pose corrections\n"
            + "serviceNames:  Names of services which will be registered. For multiple services, use\n"
            + "               \",\" between each service name, without spaces. Eg: MyService1,MyService2";
    
    
    public static void main(String[] args) throws IOException 
    {
        if(args[0].toLowerCase().equals("help"))
        {
            System.out.println(HELP);
            return;
        }
            
        
        int loggerPort = Integer.parseInt(args[0]);
        int apiPort = Integer.parseInt(args[1]);
        int correctorPort = Integer.parseInt(args[2]);
        String[] serviceNames = args[3].split("\\,");
        
        System.out.println("Starting logger...");
        Logger logger = new Logger("LOGGER");
        Logger errorLogger = logger.createSubLogger("ERROR");
        logger.registerEndpoint(new PrintWriter(System.out));
        SocketLoggerEndpointRegisterer registerer = new SocketLoggerEndpointRegisterer(logger, loggerPort);
        registerer.startRegisteringClients();
        
        TDNAPIServer api = new TDNAPIServer(apiPort, logger, errorLogger);
        for(String name : serviceNames)
        {
            api.registerService(name, new TestService());
        }
        api.setVerbosity(VerbosityLogger.DETAILED_OVERVIEW);
        Thread apiThread = api.createThread();
        apiThread.start();
        api.startRegisteringClients();
        
        TDNReceiver receiver = new TDNReceiver(correctorPort);
        receiver.addListener(new SimulatedRobot());
        receiver.start();
        
        System.in.read();
        
        registerer.stopRegisteringClients();
        logger.close();
        api.close();
        receiver.stopReceiving();
    }
    
    long millis = System.currentTimeMillis();
    @Override
    public void rootReceived(TDNRoot root)
    {
        System.out.println("receive took " + (millis - System.currentTimeMillis()));
        millis = System.currentTimeMillis();
        float x = root.get("x").as();
        float y = root.get("y").as();
        float heading = root.get("heading").as();
        
        System.out.println("Received: x=" + x + ", y=" + y + ", h=" + heading);
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
            
            switch(request)
            {
            	
            }
            
            return new TDNRoot();
                    /*.insertValue("mojePole", array)
                    .insertValue("mojeCeleCislo", new TDNValue(69420, TDNParsers.INTEGER))
                    .insertValue("poleObjektu", new TDNValue(new TDNArray
                            (
                                new Object[] { root, root, root },
                                TDNParsers.ROOT
                            ), TDNParsers.ARRAY));*/
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

    @Override
    public void tdnSenderConnected() {
        // TODO Auto-generated method stub
        
    }
}
