package team.hobbyrobot.tdn;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import team.hobbyrobot.tdn.core.TDNRoot;

public class TDNViewer {

    public static void main(String[] args) throws IOException 
    {
        Scanner scanner = new Scanner(System.in);
        String tdnStr = scanner.nextLine();
        StringReader reader = new StringReader(tdnStr);
        /*int rootCnt = 0;
        int cnt = 0;
        while(true)
        {
        	int i = reader.read();
        	if(i < 0)
        		break;
        	
        	if((char)i == ';')
        		cnt--;
        	if((char)i == '|')
        		cnt++;
        	if(cnt < 0)
        	{
        		rootCnt++;
        		cnt = 0;
        	}
        }
        reader.reset();*/
        while(true)
        {
        	reader.mark(1);
        	int i = reader.read();
        	if(i < 0)
        		break;
        	reader.reset();
	        TDNRoot root = TDNRoot.readFromStream(new BufferedReader(reader));
	        if(root.toString().length() <= 4)
	        	break;
	        System.out.println(root.toString());
        }
    }

}
