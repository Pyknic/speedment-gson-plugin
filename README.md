# speedment-gson-plugin
A plugin for Speedment that makes it easy to integrate with Gson by generating all the type adapters required to serialize and deserialize Speedment entities.

## Installation
To use the plugin, add the following to your `pom.xml`-file.

```xml
<build>
  <plugins>
      <plugin>
          <groupId>com.speedment</groupId>
          <artifactId>speedment-maven-plugin</artifactId>
          <version>2.3.5</version>
          
          <dependencies>
              <dependency>
                  <groupId>com.speedment.plugins</groupId>
                  <artifactId>gson</artifactId>
                  <version>1.0.0</version>
              </dependency>
              
              <!-- Database Connector Dependency -->
          </dependencies>
          
          <configuration>
              <components>
                  <component implementation="com.speedment.plugins.gson.GsonComponentInstaller" />
              </components>
          </configuration>
      </plugin>
  </plugins>
</build>

<dependencies>
  <dependency>
      <groupId>com.speedment.plugins</groupId>
      <artifactId>gson</artifactId>
      <version>1.0.0-SNAPSHOT</version>
  </dependency>
  
  <dependency>
      <groupId>com.speedment</groupId>
      <artifactId>speedment</artifactId>
      <version>2.3.5</version>
  </dependency>
  
  <!-- Database Connector Dependency -->
</dependencies>
```

The latest supported Speedment version is `2.3.5`. Please note that the plugin is not available in the Maven Central Repository, so you have to download and build it yourself from the Git repository.

## Usage
Once the dependency and plugin configuration tags have been added to the `pom.xml`, Gson type adapters should be generated automatically when you regenerate your project.

### Serialization
```java
Speedment speedment = new ExampleApplication().build();
Manager<Hare> hares = speedment.managerOf(Hare.class);

System.out.println(hares.toJson(new HareImpl().setName("Harry").setAge(8)));
```

Outputs:
```
{"name":"Harry","age":8}
```

### Deserialization
```
final String json = "{\"name\":\"Harry\",\"age\":8}";
System.out.println(hares.fromJson(json));
```

Outputs:
```
HareImpl { id = null, name = Harry, age = 8 }
```

## License
Copyright 2016 Speedment, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
