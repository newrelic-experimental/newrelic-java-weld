<a href="https://opensource.newrelic.com/oss-category/#new-relic-experimental"><picture><source media="(prefers-color-scheme: dark)" srcset="https://github.com/newrelic/opensource-website/raw/main/src/images/categories/dark/Experimental.png"><source media="(prefers-color-scheme: light)" srcset="https://github.com/newrelic/opensource-website/raw/main/src/images/categories/Experimental.png"><img alt="New Relic Open Source experimental project banner." src="https://github.com/newrelic/opensource-website/raw/main/src/images/categories/Experimental.png"></picture></a>
 
![GitHub forks](https://img.shields.io/github/forks/newrelic-experimental/newrelic-java-weld?style=social)
![GitHub stars](https://img.shields.io/github/stars/newrelic-experimental/newrelic-java-weld?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/newrelic-experimental/newrelic-java-weld?style=social)

![GitHub all releases](https://img.shields.io/github/downloads/newrelic-experimental/newrelic-java-weld/total)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/newrelic-experimental/newrelic-java-weld)
![GitHub last commit](https://img.shields.io/github/last-commit/newrelic-experimental/newrelic-java-weld)
![GitHub Release Date](https://img.shields.io/github/release-date/newrelic-experimental/newrelic-java-weld)


![GitHub issues](https://img.shields.io/github/issues/newrelic-experimental/newrelic-java-weld)
![GitHub issues closed](https://img.shields.io/github/issues-closed/newrelic-experimental/newrelic-java-weld)
![GitHub pull requests](https://img.shields.io/github/issues-pr/newrelic-experimental/newrelic-java-weld)
![GitHub pull requests closed](https://img.shields.io/github/issues-pr-closed/newrelic-experimental/newrelic-java-weld)  
   

# New Relic Java Agent Instrumentation for Weld

This instrumentation serves as a custom extension for the New Relic Java Agent, enhancing observability for applications utilizing Weld—the reference implementation of CDI (Contexts and Dependency Injection) for Java EE/Jakarta EE. It aims to deliver detailed insights into Weld's internal operations, with a particular focus on its robust proxy, interceptor, and decorator mechanisms.


## Installation

1. Either download the Release instrumentation jars or build them as described in the Building section below.   
2. In the New Relic Java Agent directory, create a directory named extensions if it does not already exist.   
3. Copy the instrumentation jars into the extensions directory.   
4. Restart the application

## Configuration [optional]

These settings are necessary only if you wish to exclude specific classes and methods from instrumentation. You can configure this via your `newrelic.yml` file under the common section.

```yaml
# newrelic.yml
common:
  # ... other common agent settings ...

  # Custom instrumentation settings for Weld
  weld: 
    ignore_traces_enabled: true # Set to true to enable dynamic trace ignoring (default: false)
    ignored_trace_patterns:
      # List of fully qualified class or method name patterns to ignore from tracing.
      # Uses wildcard matching where '*' matches any sequence of characters.
      # Examples:
      # - "com.nr.labs.weld.PauseService:pause*" # Ignores all 'pause' methods in PauseService
      # - "com.nr.labs.weld.MyService:doSomething" # Ignores a specific method
      # - "com.nr.labs.weld.MyService2:*" # Ignores all methods in MyService2
      # - "org.jboss.weld.bean.proxy.*" # Ignores all methods in Weld's proxy package
      # - "org.jboss.weld.bean.proxy.CombinedInterceptorAndDecoratorStackMethodHandler:invoke" # Ignores a specific internal proxy handler method
      - "com.example.noisy.internal.Class:fastMethod"
      - "com.example.AnotherProxyClass:*"
```

## Building

To build the Weld instrumenation jars requires that Gradle is installed.   
   
Set the environment variable NEW_RELIC_EXTENSIONS_DIR to a local directory.  If building on the same machine as the application use the extensions directory of the New Relic Java Agent.   
To build one of the modules (e.g weld-core-4.0) use the following command.   
gradle weld-core-4.0:clean weld-core-4.0:install .  
   
To build all of the instrumenation jars, use the following command.    
gradle clean install

## Support

New Relic has open-sourced this project. This project is provided AS-IS WITHOUT WARRANTY OR DEDICATED SUPPORT. Issues and contributions should be reported to the project here on GitHub.

>We encourage you to bring your experiences and questions to the [Explorers Hub](https://discuss.newrelic.com) where our community members collaborate on solutions and new ideas.

## Contributing

We encourage your contributions to improve [Project Name]! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project. If you have any questions, or to execute our corporate CLA, required if your contribution is on behalf of a company, please drop us an email at opensource@newrelic.com.

**A note about vulnerabilities**

As noted in our [security policy](../../security/policy), New Relic is committed to the privacy and security of our customers and their data. We believe that providing coordinated disclosure by security researchers and engaging with the security community are important means to achieve our security goals.

If you believe you have found a security vulnerability in this project or any of New Relic's products or websites, we welcome and greatly appreciate you reporting it to New Relic through [HackerOne](https://hackerone.com/newrelic).

## License

Weld  Instrumentation is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.

