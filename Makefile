all: FtpCli.class FtpServ.class

FtpCli.class: FtpCli.java
	javac FtpCli.java -Xlint

FtpServ.class: FtpServ.java
	javac FtpServ.java -Xlint

clean:
	rm -f *.class 