#### Resources used
* https://www.baeldung.com/run-shell-command-in-java
* http://www.quartz-scheduler.org/documentation/
* https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/
* http://www.springboottutorial.com/spring-boot-and-h2-in-memory-database
* https://www.baeldung.com/quartz
* https://www.mkyong.com/java/how-to-list-all-jobs-in-the-quartz-scheduler/
* https://www.baeldung.com/spring-boot-testing
* http://www.quartz-scheduler.org/documentation/quartz-2.x/examples/
* https://github.com/quartz-scheduler/quartz/blob/master/quartz-jobs/src/main/java/org/quartz/jobs/NativeJob.java
* https://medium.com/@himsmittal/quartz-plugins-a-must-have-for-all-quartz-implementations-7ca01e98e620
* https://www.concretepage.com/spring-boot/spring-boot-crudrepository-example
* http://www.springboottutorial.com/spring-boot-exception-handling-for-rest-services

#### Local H2 console
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:~/projects/com.ail/job-service/db/data

#### Get all jobs
```
curl -i http://localhost:8080/job-service/jobs
```
#### Create new job
```
curl -i -X POST http://localhost:8080/job-service/create -H "Content-Type: application/json" -d @job1.json
```
job1.json:
```
{
	"name":"job1",
	"command":"ping localhost -c 4",
	"cron":"0/30 * * * * ?"
}
```
job2.json:
```
{
	"name":"job2",
	"command":"echo 'hello' >> test_job2",
	"cron":"0/20 * * * * ?",
	"priority":"10"
}
```
#### Execute created job one time manually
```
curl -i -X POST http://localhost:8080/job-service/execute/job1
curl -i -X POST http://localhost:8080/job-service/execute/job2
```