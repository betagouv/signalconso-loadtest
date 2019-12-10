## SignalConso - performance test suite

Project uses [sbt plugin][sbtplugindoc] of [gatling][gatlingdoc].
It contains basic simulation from gatling quick start bundle.

[sbtplugindoc]: https://gatling.io/docs/current/extensions/sbt_plugin/
[gatlingdoc]: https://gatling.io/docs/current/advanced_tutorial/

### Run

All tests:
```
sbt "gatling:test"
```

Single test:
```
sbt "gatling:testOnly signalconso.BasicSimulation"
```

Report:
```
sbt "gatling:lastReport"
```
