# PG SSL SAMPLE

## Getting started

### 1- Create Postgres SSL docker image

```
cd ./docker && ./create.sh
```

### 2- Run unit test

Using gradle 4.5:
```
gradle test
```


### 3- Tests

**shouldSucceedToInitFromEventLoop**

This test calls Postgres Driver from a verticle running in the event loop.
This is the normal use case and it works well.
The test assert that this use case succeed.

**shouldFailToInitFromWorker**

This test call Postgres Driver from a verticle running in a worker.
this test fail because SSL connection inside worker is failing
The driver is returning an error: 

`io.vertx.core.VertxException: SSL handshake failed`

And the bus call fail with a timeout error.

**shouldFailRandomlyToInitFromWorkerUsingBus** 

This test call Postgres Driver from a verticle running in an event loop but the caller is a worker calling it from the bus.
The test also simulate other driver call while this verticle is calling the driver.
In our production environnement, this use case fail randomly (1% of the time): SSL Handshake failed.

In this test case i was not able to reproduce it because it is random. 
But i keep it to show you how we are using it.