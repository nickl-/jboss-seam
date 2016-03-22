#JBoss Seam - Contextual Component framework for Java EE


This software is distributed under the terms of the FSF Lesser GNU
Public License (see http://www.gnu.org/licenses/lgpl-3.0.txt). 

##Seam 2: Next generation enterprise Java development

This is a modified fork of https://github.com/seam2/jboss-seam.

Seam 2.3 targets Java EE 6 capabilities such as JSF2 and JPA2 on the JBoss Enterprise Application Platform 6 (JBoss AS 7) 
Seam 2.3 also supports RichFaces 4

Seam 2 is a powerful open source development platform for building rich Internet applications in Java. Seam integrates technologies such as Asynchronous JavaScript and XML (AJAX), JavaServer Faces (JSF), Java Persistence (JPA), Enterprise Java Beans (EJB 3.1) and Business Process Management (BPM) into a unified full-stack solution, complete with sophisticated tooling.

Seam has been designed from the ground up to eliminate complexity at both architecture and API levels. It enables developers to assemble complex web applications using simple annotated Java classes, a rich set of UI components, and very little XML. Seam's unique support for conversations and declarative state management can introduce a more sophisticated user experience while at the same time eliminating common bugs found in traditional web applications. 

## Get Up And Running Quick

1. Install JBoss AS 7.1.1.Final  
2. Start JBoss AS by typing `bin/standalone.sh` in the JBoss AS home directory
3. In the link:examples/booking[] directory, type `mvn clean package` and check  for any error messages.
4. In the booking-ear directory run:
    `mvn jboss-as:deploy`   
5. Point your browser to http://localhost:8080/seam-booking/      
6. Register an account, search for hotels, book a room...

##Learn more

* Read the documentation in http://seamframework.org/Seam2/Documentation.html


##Compiling from sources

You need an install of Maven 3.0.x


To build Seam from github, just run 

```bash

	git clone https://github.com/albfernandez/jboss-seam.git
	cd jboss-seam
	mvn -Pdistribution clean package
	
``` 

When finished you have the complete seam distribution in ``distribution/target/``

If you are making changes to source code and want to test quickly without making a full release, you can type

    mvn clean package

Resulting jar files will be in each sub-project target directory.
