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

class FtpServ { 
    
    //table used for encryption
    static int[][] table = new int[53][53];
    //PATH INITIAL
    static String path;
	
	
	
	
    public static void main(String argv[]) throws Exception{
        
        init_table();
        init_first_row();
        init_first_column();
        path = new java.io.File( "." ).getCanonicalPath() + "/";
		
		
		
		
	//THE THING THAT CAME FROM THE CLIENT
	String clientSentence;
		
	//THE THING THAT NEEDS TO GO BACK TO THE CLIENT
	String capitalizedSentence;
		
	//LISTEN FOR CONNECTION ON THIS PORT #
	ServerSocket listen = new ServerSocket(Integer.parseInt(argv[0]));
	while(true) 
	{
            Socket conn = listen.accept(); 
			
            //GET THE INPUT FROM THE INCOMMING CONNECTION
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream())); 

            //THE THINGS GOING BACK TO THE CLINET
            PrintWriter out = new PrintWriter(conn.getOutputStream(),true);
            
            //get the request from the user
            clientSentence = in.readLine();
            clientSentence = clientSentence.trim();
            String[] input_command = clientSentence.split(" ");
            System.out.println("FROM CLIENT:" + clientSentence);
            //LS command
            if(input_command[0].equals("ls"))
            {
                LinkedList x = command_ls();
                while(!x.isEmpty())
                {
                    out.println(x.removeFirst());
                }
            }
            else if(input_command[0].equals("get"))
            {
                //get the name of the file
                String file_name = input_command[1];
                LinkedList output = new LinkedList();
                //encrypt the file
                encrypt_file(path+file_name, output, "security");
                write_file(path, file_name+"_se", output);
                
                //send the file to the client
                LinkedList toClient = new LinkedList();
                read_file(path, file_name+"_se",toClient);
                while(!toClient.isEmpty())
                {
                    out.println(toClient.removeFirst());
                }
            }
            else if(input_command[0].equals("mkdir"))
            {
                command_mkdir(input_command[1]);
                out.println("Directory created ...");
            }
            else if(input_command[0].equals("cd") && !input_command[1].equals(".."))
            {
                command_cd(input_command[1]);
                out.println("Moved to subdirectory ...");
            }
            else if(input_command[0].equals("cd") && input_command[1].equals(".."))
            {
                command_parent();
                out.println("Moved to the parent Directory ...");
            }
            else if(input_command[0].equals("pwd"))
            {
                out.println(command_pwd());
            }
            else
            {
                capitalizedSentence = clientSentence.toUpperCase();
                //send the response back to the client
                out.println(capitalizedSentence+" : is not a valid command ....");
            }
            conn.close();
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

