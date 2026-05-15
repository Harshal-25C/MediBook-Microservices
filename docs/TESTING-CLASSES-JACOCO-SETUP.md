# MediBook Testing Classes Setup

## What is included

All services contains JUnit 5 + Mockito test classes under each module's `src/test/java` folder.

Covered business services:

- `admin-service`
- `auth-service`
- `provider-service`
- `schedule-service`
- `appointment-service`
- `payment-service`
- `review-service`
- `notification-service`
- `record-service`

Small application-class smoke tests are also included for:

- `api-gateway`
- `eureka-server`

## How to install

1. Extract this zip.
2. Copy each module folder from this package into your MediBook backend root.
3. Allow overwrite/replace for existing test files when asked.

Example target structure:

```text
MediBook-Microservices-medibook-deployment/
  auth-service/
    src/test/java/com/medibook/auth/service/impl/AuthServiceImplTest.java
  appointment-service/
    src/test/java/com/medibook/appointment/service/impl/AppointmentServiceImplTest.java
  ...
```

## Java setup

Use Java 17 or Java 21. The extracted project failed under JDK 25 because Lombok `1.18.30` did not generate getters/builders correctly.

PowerShell example:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## Run tests

From each service folder:

```powershell
mvn test
```

Or run the main service modules in PowerShell:

```powershell
$modules=@(
  "admin-service","auth-service","provider-service","schedule-service",
  "appointment-service","payment-service","review-service",
  "notification-service","record-service"
)
foreach($m in $modules){
  Push-Location $m
  mvn test
  Pop-Location
}
```

## Run JaCoCo

From each service folder:

```powershell
mvn org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.11:report
```

Open the HTML report:

```text
target/site/jacoco/index.html
```

## Verified service implementation coverage

Measured with JaCoCo instruction coverage for each `*ServiceImpl` class:

```text
AdminServiceImpl         100.00%
AuthServiceImpl           93.92%
ProviderServiceImpl       92.16%
ScheduleServiceImpl       92.35%
AppointmentServiceImpl    81.54%
PaymentServiceImpl        81.15%
ReviewServiceImpl         97.47%
NotificationServiceImpl   88.71%
RecordServiceImpl         89.67%
```

## Optional permanent JaCoCo plugin

Add this plugin inside each service `pom.xml` under `<build><plugins>` if you want `mvn test` to always generate JaCoCo reports:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
