import java.io.*; 
import java.net.*; 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

class FtpCli { 

    //table used for encryption
    static int[][] table = new int[53][53];
    //PATH INITIAL
    static String path;
	
    public static void main(String argv[]) throws Exception {
	init_table();
        init_first_row();
        init_first_column();
        path = new java.io.File( "." ).getCanonicalPath() + "/";
		
			
	
	//THE OUTPUT THAT WILL COME FROM THE SERVER WILL BE STORED HERE
	String modifiedSentence;
	Socket sock = null; 

        while(true)
        {
            sock = new Socket(argv[0], Integer.parseInt(argv[1]));
            //Get the input and output stream
            PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            //get the command from the user
            Scanner reader = new Scanner(System.in);
            System.out.print("ftp > ");
            String command = reader.nextLine();
            //send the command to the server
            
            //COMMAND PROCESSING
            command = command.trim();
            String[] cmd = command.split(" ");
            if(cmd[0].equals("lls"))
            {
                out.println(cmd[0]);
                modifiedSentence = in.readLine(); 
                
                LinkedList temp = command_ls();
                while(!temp.isEmpty())
                {
                    System.out.println(temp.removeFirst());
                }
            }
            else if(cmd[0].equals("quit"))
            {
                System.exit(0);
            }
            else if(cmd[0].equals("ls"))
            {
                String line;
                out.println(cmd[0]);
                while((line = in.readLine()) != null)
				{
					System.out.println(line);
				}
            }
            else if(cmd[0].equals("get"))
            {
                String toServer = cmd[0]+" "+cmd[1];
                out.println(toServer);
                String line = null;
                //get the file from the server
                LinkedList fromServer = new LinkedList();
                while((line = in.readLine()) != null)
                {
                    fromServer.add(line);
                }
                
                //save the file <filename>_ce
                write_file(path, cmd[1]+"_ce", fromServer);
                
                //decrypt the file
                LinkedList clientEnc = new LinkedList();
                decrypt_file(path+cmd[1]+"_ce", clientEnc, "security");
                
                //store the file with name <filename>_cd
                write_file(path, cmd[1]+"_cd", clientEnc);
            }
            else if(cmd[0].equals("mkdir"))
            {
                String toServer = cmd[0]+" "+cmd[1];
                out.println(toServer);
                System.out.println(in.readLine());
            }
            else if(cmd[0].equals("cd"))
            {
                //cd <path>
                if(!cmd[1].equals(".."))
                {
                    String toServer = cmd[0]+" "+cmd[1];
                    out.println(toServer);
                    in.readLine();
                }
                //cd ..
                else
                {
                    String toServer = cmd[0]+" "+cmd[1];
                    out.println(toServer);
                    in.readLine();
                }
            }
            else if(cmd[0].equals("pwd"))
            {
                out.println(cmd[0]);
                System.out.println(in.readLine());
            }
            else
            {
                out.println(cmd[0]);
                modifiedSentence = in.readLine(); 
                System.out.println("FROM SERVER: " + modifiedSentence); 
            }
            
            
            sock.close();
        }
        
                 
    } 
    public static void init_table()
    {
        int t, y; //x and y corrdinates respecitively
        int j = 64;//int representing the current alphabet to write in the table
        t = 1;//init row
        for(int m = 0; m < 52+6; m++)
        {
            j++;
            if(j > 90 && j < 97)
                continue;
            y = 1;//init column
            for(int i = 0; i < 52+7; i++)
            {
                if(j > 90 && j < 97)
                {
                    j++;
                }
                else if(j == 123)
                    j = 65;
                else
                {
                    table[t][y] = j;
                    y++;
                    j++;
                }
            }
            t++;
        }
    }
    public static void init_first_row()
    {
        for(int i = 1; i <= 52; i++)
        {
            table[0][i] = table[1][i];
        }
    }
    public static void init_first_column()
    {
        for(int i = 1; i <=52; i++)
        {
            table [i][0] = table[i][1];
        }
    }
    public static String prepare_key(String key, String message)
    {
        //key must be the size of the message
        if(key.length() < message.length()-2)
        {
            //shouldn't include special characters at the beginning and end
            char[] out = new char[message.length()-2];
            //add part of the message in front of the key
            int j = 1; //starting index for the message
            for(int i = key.length(); i < message.length()-2;i++)
            {
                out[i] = message.charAt(j);
                j++;
            }
            
            //add the key at the beginning
            for(int i = 0; i < key.length(); i++)
            {
                out[i] = key.charAt(i);
            }
            
            return new String(out);
        }
        else
            return key;
    }
    public static void print_table()
    {
        for(int i = 0 ; i < 53; i++)
        {
            for(int g = 0 ; g < 53; g++)
            {
                System.out.print((char)table[i][g]+" ");
            }
            System.out.println();
        }
    }
    public static String encrypt_line(String key, String message)
    {
        //if key length is less than the length of the message
        if(key.length() < message.length())
        {
            int delta = message.length() - key.length();
            key = key + message.substring(1,delta);
        }
        //output String
        char[] out = new char[message.length()];
        
        //keep the first character and the last character for merging
        out[0] = message.charAt(0);
        out[out.length-1] = message.charAt(message.length()-1);
        
        for(int i = 1; i < message.length()-1; i++)
        {
            int y = 0; //COLUMN
            int t = 0; //ROW
            
            int currentM = (int) message.charAt(i);
            int currentK = (int) key.charAt(i-1);
            //get the index of the column with the matching
            //first character of the message
            for(int j = 1; j < 53; j++)
            {
                if(table[0][j] == currentM)
                    y = j;
            }
            //get the index of the row with the matching
            //first character of the key
            for(int j = 1; j < 53; j++)
            {
                if(table[j][0] == currentK)
                {
                    t = j;
                }
            }
            
            out[i] = (char)table[t][y];
        }
        return new String(out);
    }
   public static String decrypt_line(String key, String message)
    {
        int r = 0, c = 0; //the target row and column
        char[] out = new char[message.length()];
        int it = 1; //iterator for the output
        
        //the first and the last character will be copied as is
        out[0] = message.charAt(0);
        out[out.length-1] = message.charAt(message.length()-1);
        
        //get the lenght of the key
        int keyLen = key.length();
        int key_index = 0;
        for(int i = 1; i < message.length()-1; i++)
        {
            if(key_index == keyLen)//have used all the key, use some of the
                                     //decrypted message to 
            {
                
                //find the row with the matching char
                int target = (int) out[it-keyLen];
                for(int j = 1; j < 53; j++)
                {
                    if(table[j][0] == target)
                    {
                        r = j;
                    }
                }
                //find the column
                int message_target = (int) message.charAt(i);
                for(int j = 1 ; j < 53; j++)
                {
                    if(table[r][j] == message_target)
                    {
                        c = table[0][j];
                    }
                }
                
                out[it] = (char)c;
                
                it++;
            }
            else{ //use the key
                int key_target = (int) key.charAt(key_index);
                //check the first column for matching key character
                for(int j = 1; j < 53; j++)
                {
                    if(table[j][0] == key_target)
                    {
                        r = j;
                    }
                }
                //find the message
                int message_target = (int) message.charAt(i);
                
                //find the column with the matching character
                for(int j = 1; j < 53; j++)
                {
                    if(table[r][j] == message_target)
                    {
                        c = table[0][j];
                    }
                }
                out[it] = (char) c;
                it++;
                key_index++;
            }
        }
        return new String(out);
    }
    public static void merge_data(LinkedList in, LinkedList out)
    {
        for(int i = in.size()-1; i >= 0; i--)
        {
            String x = (String) in.remove(i);
            if(x.charAt(0) == '1')
            {
                i--;
                String y = (String) in.remove(i);
                String newS = (y.substring(0,y.length()-1)).concat(x.substring(1,x.length()));
                out.addFirst(newS);
            }
            else
            {
                out.addFirst(x);
            }
        }
    }
    public static void encrypt_file(String path, LinkedList out, String pass)
    {
        String line = null;
        try 
        {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                String[] temp = line.split(" ");
                String enc_line = "";
                for(int i = 0; i < temp.length; i++)
                {
                    //add padding to each letter
                    String ori_word = "0"+temp[i]+"0";
                    String encrypted_word = encrypt_line(pass,ori_word);

                    //remove padding
                    encrypted_word = encrypted_word.substring(1,encrypted_word.length()-1);
                    enc_line = enc_line + encrypted_word + " ";
                }
                out.add(enc_line);
            }   
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            System.out.println("Unable to open file '" + path + "'");                
        }
        catch(IOException ex) {
            System.out.println("Error reading file '" + path + "'");                  
        }
    }
    //PATH - where to save the file
    //NAME - the name of the file you want to save
    //IN - Input data
    public static void write_file(String path, String name, LinkedList in) throws FileNotFoundException, UnsupportedEncodingException
    {
        String loc = path+name;
        PrintWriter writer = new PrintWriter(loc, "UTF-8");
        while(!in.isEmpty())
        {
            String curr = (String) in.removeFirst();
            //don't need to print the new line for the last line
            if(in.size() == 0)
                writer.print(curr);
            else
                writer.println(curr);
        }
        writer.close();
    }
    //Decrypt the file at a [PATH] put the ouput in Linked List [OUT], use password [PASS]
    public static void decrypt_file(String path, LinkedList out, String pass)
    {
        String line = null;
        try 
        {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                String[] temp = line.split(" ");
                String enc_line = "";
                for(int i = 0; i < temp.length; i++)
                {
                    //add padding to each letter
                    String ori_word = "0"+temp[i]+"0";
                    String encrypted_word = decrypt_line(pass,ori_word);
                    //remove padding
                    encrypted_word = encrypted_word.substring(1,encrypted_word.length()-1);
                    enc_line = enc_line + encrypted_word + " ";
                }
                out.add(enc_line);
            }   
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            System.out.println("Unable to open file '" + path + "'");                
        }
        catch(IOException ex) {
            System.out.println("Error reading file '" + path + "'");                  
        }
    }
    public static LinkedList command_ls()
    {
        LinkedList out = new LinkedList();
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) 
        {
            if (listOfFiles[i].isFile()) 
            {
                String x = "File: " + listOfFiles[i].getName();
                out.add(x);
            } 
            else if (listOfFiles[i].isDirectory()) 
            {
                String x = "Directory: " + listOfFiles[i].getName();
                out.add(x);
            }
        }
        return out;
    }
    public static void command_cd(String x)
    {
        path = path+x+"/";
    }
    public static void command_mkdir(String x)
    {
        new File("./"+x).mkdir();
    }
    public static void command_parent()
    {
        path = path.substring(0, path.length()-1);
        int x = path.lastIndexOf("/");
        path= path.substring(0, x);
        path = path+"/";
    }
    public static String command_pwd()
    {
        return path;
    }
    public static void read_file(String path, String name, LinkedList out)
    {
        String line = null;
        try 
        {
            FileReader fileReader = new FileReader(path+name);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
             while((line = bufferedReader.readLine()) != null)
             {
                 out.add(line);
             }
        }
        catch(FileNotFoundException ex) {
            System.out.println("Unable to open file '" + path + "'");                
        }
        catch(IOException ex) {
            System.out.println("Error reading file '" + path + "'");                  
        }
    }
} 


