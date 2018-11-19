## Job Service Project

#### Used technologies
* Quartz
* Maven
* Spring Boot
* H2 DB
* RESTful services
* a lot of hidden tech-stack...

## Fulfilling of requirements

#### Flexibility
For the purpose of flexibility it was decided that Job Service would be able run commands which, actually, could be run
from the commandline. This way it can be not only Java classes (which could be executed as a job), but also you can
execute any program as a job.

#### Reliability
Actually, it is impossible to predict every failure situation if not to mention how to do rollback.
Therefore it was decided that application (or command), which will be executed as a job, should take care about rollback
by itself.

#### Internal Consistency
Job Service can provide you information about jobs' status at any moment of time by invoking methods ```getJobInfo()```
or ```getAllJobs()```.

#### Priority
Yes, this Job Service supports jobs prioritization.

#### Scheduling
Of course, this Job Service supports scheduling with cron expressions (otherwise, why it should exist... :)

Also jobs can be executed manually.

#### Configuration
You can configure the maximum thread pool capacity with in
```<project_path>/src/main/resources/application.properties``` file:
```
spring.quartz.properties.org.quartz.threadPool.threadCount=5
```

## Running and testing

#### Run with spring-boot-maven-plugin
```
mvn clean test spring-boot:run
```
or
```
mvn clean package
java -jar <this-project-path>/target/job-service-0.0.1-SNAPSHOT.jar
```

## Manual testing of Rest API
All commands illustrated below are for Linux commandline.

For Windows commandline you will probably need to send the JSON content as a text (not as a file) with CURL utility.

Example of using CURL in Windows is:
```
curl -i -X POST http://localhost:8080/job-service/create -H "Content-Type: application/json" -d "{\"name\":\"job1\",\"command\":\"ping localhost -n 4\",\"cron\":\"0/20 * * * * ?\"}"
```

#### Create new job
```
curl -i -X POST http://localhost:8080/job-service/create -H "Content-Type: application/json" -d @job.json
```
, where ```job.json``` is file with job description in JSON format.

#### Update existing job
```
curl -i -X POST http://localhost:8080/job-service/update -H "Content-Type: application/json" -d @job.json
```
, where ```job.json``` contains ```name``` of existing job. 


#### Examples of ```job.json``` file

For Linux only:
```
{
	"name":"job1",
	"command":"ping localhost -c 4",
	"cron":"0/30 * * * * ?"
}
```
or
```
{
	"name":"job2",
	"command":"echo 'hello' >> test_job2",
	"cron":"0/20 * * * * ?",
	"priority":"10"
}
```
or
```
{
	"name":"job3",
	"command":"java -jar some-job.jar",
	"cron":"0 10,44 14 ? 3 WED",
	"priority":"15"
}
```
or even this
```
{
	"name":"job4",
	"command":"mvn -f '<another-spring-boot-project-path>/pom.xml' spring-boot:run",
	"cron":"0 0 0/1 * * * 2018-2019"
}
```
For necessary cron expressions see
http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html

#### Execute created job once manually
```
curl -i -X POST http://localhost:8080/job-service/execute/job1
```
, where ```job1``` is the job's ```name```, which was defined in ```job.json``` file upon creation.

#### Get job info
```
curl -i -X GET http://localhost:8080/job-service/job-info/job1
```
, where ```job1``` is the job ```name```, which was defined in ```job.json``` file upon creation.

#### Delete job
```
curl -i -X POST http://localhost:8080/job-service/delete/job1
```
, where ```job1``` is the job ```name```, which was defined in ```job.json``` file upon creation.

#### Get all jobs
```
curl -i -X GET http://localhost:8080/job-service/jobs
```

### Requirements for Native Jobs
All commands above are considered as a Native Jobs because they can be run with native commandline of your favorite OS.

To handle this commands ```NativeJob``` class is used from Quartz 2.3.1-SNAPSHOT (I'm sorry for using not released
yet functionality).

Actually, there are no special requirements for Native Jobs.

But if you want to monitor the result status of the last job execution, then your job should provide the exit status
code upon completion. Native OS commands usually provide it by default.
In simple Java program this status can be set with:
```
System.exit(0); // SUCCESS
```
or
```
System.exit(1); // FAILED
```
Exit status code ```0``` is always considered as ```SUCCESS```. Any others - as ```FAILED```.

Also be aware that for native jobs ```command```-property in json can depend on the current OS.

For example this command for Linux will not work for Windows:
```
ping localhost -c 5
```
For Windows we need use:
```
ping localhost -n 5
```

## Extending this Job Management System with Java classes
To provide other developers possibility to create jobs right inside this system the class ```JavaJobRequest``` was
provided. Because of the lack of time, this class is ugly and not so convenient to use... but you can check how
to create the job instance in Java code (check for ```JavaJobRequestImpl``` inside ```JavaJobIntegrationTest```)

## Additional features

#### Job Execution History
For each job this application stores all execution history in H2 DB. You can check it out in table
```JOB_EXECUTION_HISTORY ``` via H2 DB console

#### Local H2 DB console
You can see all DB tables in H2 DB management console. Just open the page while application is running:
```
http://localhost:8080/h2-console
```
and provide property for connection
```
JDBC URL: jdbc:h2:<project_path>/db/data
```
,where ```<project_path>``` is full path of current project.

#### Manual H2 Database Cleanup
H2 database data is located in ```db``` folder of this project (it's skipped by ```.gitignore```).
If you need to drop all data then just remove this directory and restart the application.
After that you will have all tables recreated.

## What can be done better... but later
* Stop and pause jobs
* Support of fixed-delay and fixed-rate schedules
* Clearer API and code
* Improve tests and add more tests
* Using Docker
* Save logs for each separate job execution to separate log-file
* Split REST service to separate maven module. I would allow to use only common service functionality.
* UI
* to be continued...

## Postscript
Since development is an endless process, therefore the requirements - are the only thing which able to stop a developer
from the endless improvement of the system.

For this project the requirements were fulfilled one commit ago, and the last commit just simply brought more chaos
to the universe by adding more rows of imperfect code (and more bugs accordingly) :)

#### Resources used
* http://www.quartz-scheduler.org/documentation/
* https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/
* https://www.concretepage.com/spring-boot/spring-boot-crudrepository-example
* https://www.freeformatter.com/cron-expression-generator-quartz.html
* https://www.baeldung.com/quartz
* https://www.baeldung.com/run-shell-command-in-java
* https://www.baeldung.com/spring-boot-testing
* http://www.springboottutorial.com/spring-boot-and-h2-in-memory-database
* http://www.springboottutorial.com/spring-boot-exception-handling-for-rest-services
* https://www.mkyong.com/java/how-to-list-all-jobs-in-the-quartz-scheduler/
* http://www.quartz-scheduler.org/documentation/quartz-2.x/examples/
* https://github.com/quartz-scheduler/quartz/blob/master/quartz-jobs/src/main/java/org/quartz/jobs/NativeJob.java
* https://medium.com/@himsmittal/quartz-plugins-a-must-have-for-all-quartz-implementations-7ca01e98e620
* and many more...