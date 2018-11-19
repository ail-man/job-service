## Job Service Project

#### Used technologies
* Maven
* Spring Boot
* Quartz
* H2 DB
* RESTful services
* a lot of hidden tech-stack...

## Fulfilling of requirements

#### Flexibility
For the purpose of flexibility it was decided that Job Service would be able run commands which, actually, could be run
from the command-line. This way it can be not only Java classes (which could be executed as a Job), but also you can
execute any program as a Job.

#### Reliability
Actually, it is impossible to predict every failure situation if not to mention how to do rollback.
Therefore it was decided that application (or command), which will be executed as a job, should take care about rollback
by itself.

#### Internal Consistency
Job Service can provide you information about jobs' status at any moment of time by invoking methods ```getJobInfo()```
or ```getAllJobs```.

#### Priority
Job Service supports priority.

#### Scheduling
Job Service supports scheduling with cron expressions. Also jobs can be executed manually.

## Running and testing

#### Run with spring-boot-maven-plugin
```
mvn spring-boot:run
```
or
```
mvn clean package
java -jar <this-project-path>/target/job-service-0.0.1-SNAPSHOT.jar
```

## Rest API
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

#### Execute created job one time manually
```
curl -i -X POST http://localhost:8080/job-service/execute/job1
```
, where ```job1``` is the job's ```name``` (id), which was defined in ```job.json``` file upon creation.

#### Get job info
```
curl -i -X GET http://localhost:8080/job-service/job-info/job1
```
, where ```job1``` is the job ```name``` (id), which was defined in ```job.json``` file upon creation.

#### Delete job
```
curl -i -X POST http://localhost:8080/job-service/delete/job1
```
, where ```job1``` is the job ```name``` (id), which was defined in ```job.json``` file upon creation.

#### Get all jobs
```
curl -i -X GET http://localhost:8080/job-service/jobs
```

#### Examples of ```job.json``` file

For Linux only:
```
{
	"name":"job1",
	"command":"ping localhost -c 4",
	"cron":"0/30 * * * * ?"
}
```
For any OS:
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

### Requirements for NativeJobs
Actually, there are no special requirements. You can run everything you want (as in commandline of your favorite OS).

But if you want to see the final status of the last job execution then your Job should provide the exit status code upon
completion. Native OS commands usually provide it by default.
In simple Java program this status can be set with:
```
System.exit(0); // SUCCESS
```
or
```
System.exit(1); // FAILED
```
Exit status code ```0``` is always considered as ```SUCCESS```. Any others - as ```FAILED```.

Also be aware that for native jobs ```command```-property in job-json can depend on the current OS.

For example this command for Linux will not work for Windows:
```
ping localhost -c 5
```
For Windows we need use:
```
ping localhost -n 5
```

## Additional features

#### Local H2 console
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:<project_path>/db/data

#### H2 Database
H2 database data file is located in ```db``` folder of this project (it's skipped by ```.gitignore```) file
If you need to drop all data then just remove this directory and you will have all data created from scratch.

## What can be done better... but later
* Save logs for each separate job execution to separate log-file
* More tests
* UI

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