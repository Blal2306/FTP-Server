To execute the program:
1) cd to the directory containing the source code files and the Makefile
2) execute the "make" command, ignore the warnings, the code should run fine
3) This generates two files FtpServ.class and FtpCli.class
4) To start the server type : java FtpServ <server port #>
5) To start the client type : java FtpCli <server_domain e.g bingsuns.binghamton.edu> <server_port>
6) After the client is running, the client can accept any of the folloing commands:
	ls
	lls
	cd
	pwd
	mkdir
	get
	quit
