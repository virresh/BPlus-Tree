DBMS HW 3
Done by 
Viresh Gupta 2016118

This is an implementation of BPlus tree that handles duplicates using an overflow bucket system.
Implementation Logistics:
A randomly generated data file is created if it doesn't exist (always named data.txt)
The BTree index is stored as a serialised file into index.dat
Index can be made on any one of the four columns, chosen at runtime

A sample program run:

	viresh@viresh-PC:~/Desktop/dbms3$ javac Driver.java 
	viresh@viresh-PC:~/Desktop/dbms3$ java Driver 
	Enter the record Type to build up the index on: (1/2/3/4)
	1
	What do you want to do ?
	0. Quit
	1. Find V
	2. Find All V
	3. Range Query of lV to rV
	4. Add a new node
	5. Delete V
	0
	viresh@viresh-PC:~/Desktop/dbms3$ java Driver 
	Enter the record Type to build up the index on: (1/2/3/4)
	1
	What do you want to do ?
	0. Quit
	1. Find V
	2. Find All V
	3. Range Query of lV to rV
	4. Add a new node
	5. Delete V
	2
	Enter a value: 
	1614
	3148 1614 hlchkfkmfdulvtxgzoqv ubswsvfgub 0000091432
	3048 1614 alchkfkmfdulvtxgzoqv ubswsvfgub 0010091432
	What do you want to do ?
	0. Quit
	1. Find V
	2. Find All V
	3. Range Query of lV to rV
	4. Add a new node
	5. Delete V
	0
	viresh@viresh-PC:~/Desktop/dbms3$ java Driver 
	Enter the record Type to build up the index on: (1/2/3/4)
	1
	What do you want to do ?
	0. Quit
	1. Find V
	2. Find All V
	3. Range Query of lV to rV
	4. Add a new node
	5. Delete V
	2
	Enter a value: 
	1614
	3148 1614 hlchkfkmfdulvtxgzoqv ubswsvfgub 0000091432
	3048 1614 alchkfkmfdulvtxgzoqv ubswsvfgub 0010091432
	3148 1614 hlchkfkmfdulvtxgzoqv ubswsvfgub 0000091432
	What do you want to do ?
	0. Quit
	1. Find V
	2. Find All V
	3. Range Query of lV to rV
	4. Add a new node
	5. Delete V
	0
	