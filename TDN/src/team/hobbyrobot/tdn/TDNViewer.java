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
        TDNRoot root = TDNRoot.readFromStream(new BufferedReader(reader));
        System.out.println(root.toString());
    }

}
