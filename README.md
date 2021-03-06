# replion

The REPL + Datomic ions

Datomic and Datomic Ions are a powerful database and platform for creating cloud applications. However, ions suffer from the same challenge as other cloud-based technologies - [temporal distance](https://www.youtube.com/watch?v=jh4hMAvygjk&feature=youtu.be&t=1). Simply stated, the biggest pain point with ions is the waiting around you do for your changes to be deployed. This is compounded in early-stage development when you are prone to making mistakes due to your lack of familiarity with the architecture, APIs, and so on. This results in most of your time spent pushing and deploying rather than coding. This is extremely frustrating and counter-productive.

What if, instead, you could deploy a simple, working system to the cloud and then securely REPL in to your system, develop as if you were on your local system, and then complete the push and deploy cycle after you've figured out all of the painful aspects of your initial product? 

This is the goal of replion, to give you step-by-step instructions and examples of how to configure your Datomic system for cloud-based REPL development.

Note that for the entirety of this project I will use my stack/application name, "replion." You will use your stack/application name instead.

These instructions are for connecting a REPL to a solo instance of Datomic Cloud. As time and resources become available, I may provide instructions for a production instance in the future. Alternatively, REPL-driven deployment could be done in a solo instance and then pushed to a production instance as desired.

## Recommended Prereading
While not required, I strongly recommend understanding the basics of Datomic Cloud and Datomic Ions by going through the [Datomic Tutorial](https://docs.datomic.com/cloud/tutorial/client.html) and the [Datomic Ions Tutorial](https://docs.datomic.com/cloud/ions/ions-tutorial.html), including cloning, running, and carefully inspecting the [ion starter repo](https://github.com/datomic/ion-starter).

If you are already familiar with Datomic Cloud and Ions, feel free to move on to the next section.

## Enabling the REPL
The next sections will describe changes needed to your system to enable REPL connectivity.

Prior to any of the following steps, set up a Datomic Solo instance _exactly_ as described [here](https://docs.datomic.com/cloud/setting-up.html). This includes setting up the bastion for SSH access as described [here](https://docs.datomic.com/cloud/getting-started/configure-access.html#authorize-gateway).

### System Modifications
 1. Once you create your system, go to the [EC2 console](https://console.aws.amazon.com/ec2/v2/home?#Instances:sort=desc:instanceId) and view your system instances.
 1. Select the _compute_ instance from the instance list (e.g. replion) and then in the window below click on the security group link.
 1. Add 3 entries to the security group's inbound rules that are the same as the existing 3 rules except you want to set the port range to your REPL port. For example, if I were to use port 3001 for my REPL, I'd enter 3001 for the port range. My dialog is shown below.

![Correctly Configured Rules](public/resources/rules.png "REPL Rules")

### Code Modifications
You now need to modify your code to host an nrepl server. If you are using this project as your template the changes are already in place.
 1. Add `nrepl {:mvn/version "0.6.0"}` to the :deps map in your deps.edn file.
 1. In _a namespace that will be loaded by your Datomic system_, make the following changes:
    1. Add `[nrepl.server :refer [start-server]]` to your requires.
    1. Add `(defonce server (start-server :bind "0.0.0.0" :port 3001))` in your ns. Note that these options can vary if you wish.
 1. For reference, here is where I make these changes in this project and when I modified the Datomic/ion-starter project:
    1. In this project, I put these changes in the `replion.core` ns.
    1. If you are modifying the ion-starter project, I'd put the server definition right [here](https://github.com/Datomic/ion-starter/blob/master/src/datomic/ion/starter.clj#L11) and the requires in the usual place.
 1. Complete a push/deploy cycle to get your changes into your Datomic system.

Note that to ensure that your repl server is started, it must be in a ns that Datomic will load. Any ns that is referenced directly or indirectly by your lambdas in your ion-config.edn file will meet this requirement.

### Set up SSH tunnel through your bastion
You are now ready to connect to your system's REPL server. Do the following:
 1. From the EC2 instances panel, select your bastion (e.g. replion-bastion). Click the connect button. It will provide an example ssh connection string. Copy this down. It will be something along the lines of `ssh -i "keyname.pem" root@ec2-1-2-3-4.compute-1.amazonaws.com`.
 1. Select your compute instance (e.g. replion) and note its private ip as listed in the Description tab below the instance list. 
 1. Using the above two items, construct a connection command like so: `ssh -i ~/.ssh/$(yourkey).pem -L 3001:$(replion-private-ip):3001 ec2-user@$(public dns)`. The params `yourkey` and `public dns` will be from the first bullet and `replion-private-ip` will be from the second. Assuming the connection string shown above and a private ip of 4.3.2.1 your connection command will be `ssh -i ~/.ssh/keyname.pem -L 3001:4.3.2.1:3001 ec2-user@ec2-1-2-3-4.compute-1.amazonaws.com`.
 1. Execute this command in your project's terminal.

### Make the REPL connection
 1. From your favorite IDE, set up a remote REPL connection to localhost:3001. An example using Cursive is shown below.
 1. Connect!

![Cursive nREPL Setup](public/resources/nrepl_cursive_config.png "Cursive nREPL Setup")

### Details/Notes
You now have two ways to connect to your Datomic system:
 1. Using the standard socks proxy method as documented by Cognitect. This runs your code locally with a direct connection to your Datomic instance.
 1. Using a directly connected REPL. Your code is local, but you are running on your deployed instance. If you use this method, you do not need to run the socks proxy script or the datomic-access script. Just run the above ssh command to enable port forwarding.

## Time to Feel the Power!
Now that you've done all this, it's time to do something awesome with it. One challenge I face whenever learning APIs such as this is learning the little details of things like the correct form of function arguments. Am I given a map or a string containing a map? Are the keys strings or keywords? Json or edn? What keys are present? If I must do a different deploy and check every time I am working through each of these issues it can be a total time-suck. Let's see how our replion instance can help with this problem.

First, let's execute an existing lambda.

If you are using this project as your code, you can invoke the "hello" lambda like so:

`aws lambda invoke --function-name repl-ion-$(Group Name)-hello  --payload '' /dev/stdout`

You'll need to provide the correct Group Name parameter.

Now, it's time to leverage the awesome. Navigate to the replion.lambdas ns and view the code:

``` 
(ns replion.lambdas
  (:require [replion.core :as core]))

(defn hello [_] "{\"hello\":\"world\"}")
```

Now, change the body of the hello function so that we can get some useful information about what a basic lambda argument looks like:

```
(ns replion.lambdas
  (:require [replion.core :as core]
            [clojure.pprint :as pp]))

;Change your lambda to look like this. Also be sure to add the pprint require.
(defn hello [args]
  (format
    "{\"args\":%s}"
    (with-out-str (pp/pprint args))))
```

Reload this ns into your REPL. You should see something like this in your REPL output: "Loading src/replion/lambdas.clj... done"

Now, invoke the lambda again using the same line as above.

`aws lambda invoke --function-name repl-ion-$(Group Name)-hello  --payload '' /dev/stdout`

You should now see something like this:

```
{"args":{:context
 {:clientContext nil,
  :identity {:identityId "", :identityPoolId ""},
  :functionVersion "$LATEST",
  :memoryLimitInMB 256,
  :invokedFunctionArn
  "arn:aws:lambda:us-east-1:XXXXXXXXXXXX:function:repl-ion-Compute-AWESOMEGROUP-hello",
  :logGroupName "/aws/lambda/repl-ion-Compute-AWESOMEGROUP-hello",
  :logStreamName
  "2019/12/05/[$LATEST]...",
  :awsRequestId "...",
  :functionName "repl-ion-Compute-AWESOMEGROUP-hello",
  :remainingTimeInMillis 59924},
 :input "{}"}
}
```

If your mind wasn't just blown by what happened then you aren't paying attention. This is amazing. We just changed the body of a lambda with no long cycle deployment! Gone are the days of waiting around trying to figure out what's going on. Just connect and develop!

If you want to "commit" this version of the hello lambda, just do a "slow deploy" cycle (i.e. push then deploy as usual).

### Suggested Workflow
Now that we've done all this, I would suggest the following workflow for maximum productivity:
 1. Stand up a _minimal_ system whenever you are greenfielding a project.
 1. Either stub out all of your ions (modification required to `resources/datomic/ion-config.edn` and the appropriate ns) or just create a single 'Work in progress' ion. Deploy these ions.
 1. Using the REPL, develop against the above stubs or WIP until you like what you've got.
 1. Do a "slow deploy" (i.e. push & deploy).
 
As needed, whenever your dependencies change (e.g. you need to add another library) you will also need to do a slow deploy. Tip: Add the dependencies first without doing any coding with them, then add your logic at the REPL. I've encountered conflicts with jar versions when doing a deploy with code changes and these are difficult to debug. Instead, minimize the changes to the deployment cycle by first pushing the deps changes, making the code changes (in the REPL, so it works), and then pushing the code changes.

## Interactive Datomic Development
To this point, I've demonstrated how to connect with a REPL and how to do interactive development with this REPL connection. However, we want to do Datomic since we're connecting to a Datomic instance. Let's work through a simple example of how to develop Datomic queries and lambdas interactively.

The data and examples I am using are lifted from my talk [Datascript and Datomic: Data Modeling for Heroes](https://youtu.be/tV4pHW_WOrY). You may want to watch it to learn a little more about data modeling in these two awesome DBs.

I've put all schemas, data, and queries in the `replion.spiderman-db` ns. Queries and data center around Peter Parker, his family and social relationships, and his evolving status as he gains his powers.

We're going to create a lambda in the ns `replion.spiderman-lambdas`. This lambda should, for a given date, tell us the status of Peter Parker. By running a previous query, we know that his status transitions from :kid (on 2000-01-01) to :bitten (on 2001-01-01) to :spider-man (on 2001-01-05). Dates are made up. When the lambda is run we expect to get the correct status for the date.

Rather than understand the valid arguments, input, output, etc. for a lambda we are just going to start with a non-broken function that looks like this:

``` 
(defn parker-status
  [{:keys [date]}]
  (let [db (d/db (core/connection))]
    (spiderman/parker-status-query db)))
```

Note that the date is not used so the query will always return the latest status of Peter Parker. Also, the return type (a Clojure set) is almost certainly incompatible with lambda.

Let's try it out by locating the lambda name in the AWS Lambda console and invoking it:

`aws lambda invoke --function-name repl-ion-Compute-YOURGROUPNAME-parker-status  --payload '' /dev/stdout`

This returns a nasty error:
``` 
{"errorMessage":"No implementation of method: :->bbuf of protocol: #'datomic.ion.lambda.dispatcher/ToBbuf found for class: java.util.HashSet","errorType":"datomic.ion.lambda.handler.exceptions.Incorrect","stackTrace":["datomic.ion.lambda.handler$throw_anomaly.invokeStatic(handler.clj:24)","datomic.ion.lambda.handler$throw_anomaly.invoke(handler.clj:20)","datomic.ion.lambda.handler.Handler.on_anomaly(handler.clj:171)","datomic.ion.lambda.handler.Handler.handle_request(handler.clj:196)","datomic.ion.lambda.handler$fn__3841$G__3766__3846.invoke(handler.clj:67)","datomic.ion.lambda.handler$fn__3841$G__3765__3852.invoke(handler.clj:67)","clojure.lang.Var.invoke(Var.java:399)","datomic.ion.lambda.handler.Thunk.handleRequest(Thunk.java:35)"]}{
    "StatusCode": 200,
    "FunctionError": "Unhandled",
    "ExecutedVersion": "$LATEST"
```

Normally you would now guess what you did wrong. Was it the input, the output, something else? Once you have an idea as to the problem you re-push and re-deploy, rinse and repeat, until you figured it all out.

We're going to debug it interactively. The message said something about not understanding a HashSet. This tips me off that my return type is wrong. Recalling that I need to return a JSON string, I first need to identify what I am actually returning. At the REPL I am going to eval the inner form of my lambda code. This gives:

``` 
(let [db (d/db (core/connection))]
    (spiderman/parker-status-query db))
=> #{[:spider-man #inst"2001-01-05T00:00:00.000-00:00"]}
```

Time to modify and reload our code as follows:

``` 
(defn parker-status
  [{:keys [date]}]
  (let [db (d/db (core/connection))
        [status as-of-date] (first (spiderman/parker-status-query db))]
    (format "{\"%s\": \"%s\"}" (name status) as-of-date)))
```

Now, when I call the lambda (same aws lambda command as before) I get:
``` 
{"spider-man": "Fri Jan 05 00:00:00 UTC 2001"}{
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
}
```

Awesome! I still need to incorporate my input date, though.

Let's try modifying and hot-reloading our code like so (Reminder - under the old regime this would take minutes):

``` 
(defn parker-status
  [{:keys [date]}]
  (let [db (d/db (core/connection))
        as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query as-of-db))]
    (format
      "{\"%s\": \"%s\"}"
      (name status)
      as-of-date)))
```

We'll now try it like so:

`aws lambda invoke --function-name repl-ion-Compute-YOURGROUPNAME-parker-status  --payload '"2010-01-01"' /dev/stdout`

Ouch! We get this error:

```
{"errorMessage":"datomic.ion.lambda.handler.exceptions.Incorrect","errorType":"datomic.ion.lambda.handler.exceptions.Incorrect","stackTrace":["datomic.ion.lambda.handler$throw_anomaly.invokeStatic(handler.clj:24)","datomic.ion.lambda.handler$throw_anomaly.invoke(handler.clj:20)","datomic.ion.lambda.handler.Handler.on_anomaly(handler.clj:171)","datomic.ion.lambda.handler.Handler.handle_request(handler.clj:196)","datomic.ion.lambda.handler$fn__3841$G__3766__3846.invoke(handler.clj:67)","datomic.ion.lambda.handler$fn__3841$G__3765__3852.invoke(handler.clj:67)","clojure.lang.Var.invoke(Var.java:399)","datomic.ion.lambda.handler.Thunk.handleRequest(Thunk.java:35)"]}{
    "StatusCode": 200,
    "FunctionError": "Unhandled",
    "ExecutedVersion": "$LATEST"
}
```

Well, I already fixed my output so maybe I am not handling my input right. Let's set it as a passthrough with some hot-reload magic:

``` 
(defn parker-status
  [args]
  (let [db (d/db (core/connection))
        ;as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query db))]
    (format
      "{\"args\":\"%s\",\n\"%s\": \"%s\"}"
      (str args)
      (name status)
      as-of-date)))
```

This gives this useful output:
``` 
{"args":"{:context
 {:clientContext nil,
  :identity {:identityId "", :identityPoolId ""},
  :functionVersion "$LATEST",
  :memoryLimitInMB 256,
  :invokedFunctionArn
  "arn:aws:lambda:us-east-1:XXXXXXXXXXX:function:repl-ion-Compute-YOURGROUPNAME-parker-status",
  :logGroupName
  "/aws/lambda/repl-ion-Compute-YOURGROUPNAME-parker-status",
  :logStreamName
  "2019/12/06/[$LATEST]...",
  :awsRequestId "...",
  :functionName "repl-ion-Compute-YOURGROUPNAME-parker-status",
  :remainingTimeInMillis 59999},
 :input "\"2010-01-01\""}
",
"spider-man": "Fri Jan 05 00:00:00 UTC 2001"}
```

I could go surfing through the logs since that is reported in the output, but what's even better is I can see my arguments. It's not a date key (why would it be?), it's an input key. Furthermore, it's double escaped since it is a JSON string so I need to take this into account. Super useful.

Now let's try this:
``` 
(defn parker-status
  [{:keys [input]}]
  (let [date (.parse (SimpleDateFormat. "yyyy-MM-dd") (cs/replace input #"\"" ""))
        db (d/db (core/connection))
        as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query as-of-db))]
    (format
      "{\"%s\": \"%s\"}"
      (name status)
      as-of-date)))
```

Finally, let's try a few invocations:

`aws lambda invoke --function-name repl-ion-Compute-YOURGROUPNAME-parker-status  --payload '"2000-01-01"' /dev/stdout`

`aws lambda invoke --function-name repl-ion-Compute-YOURGROUPNAME-parker-status  --payload '"2001-01-01"' /dev/stdout`

`aws lambda invoke --function-name repl-ion-Compute-YOURGROUPNAME-parker-status  --payload '"2006-01-01"' /dev/stdout`

These return `{"kid": "Sat Jan 01 00:00:00 UTC 2000"}`, `{"bitten": "Mon Jan 01 00:00:00 UTC 2001"}`, and `{"spider-man": "Fri Jan 05 00:00:00 UTC 2001"}`, respectively. Awesome, it works!

At this point everything looks right, so it might a be a good time for a "slow deploy." However, since I am slow deploying already, this is also an opportune time to add some dependencies to make working with json a little less painful. I'll now add `cheshire {:mvn/version "5.9.0"}` to my deps.edn files :deps map and then slow deploy. Note that I add the dependency (1 in this case, but it could be any number) _before_ requiring or using any of them in my project (no code changes - just deps). The reason for this is that each slow deploy should have minimal deltas so that when things go wrong it is easier to diagnose the issue. I've run into issues in which a require was added to code and the deploy failed due to versioning issues related to the Jackson libraries (surprise!). By including but not using the library I was able to manually require libraries one-by-one in the REPL to diagnose the failure mode rather than having the deploy simply fail. This prevents painful log spelunking and other painful debugging processes.

Now that my slow deploy succeeded, I am going to make a final modification to the `parker-status` function after adding `[cheshire.core :as ch]` to my requires:

``` 
(defn parker-status
  [{:keys [input]}]
  (let [date (.parse (SimpleDateFormat. "yyyy-MM-dd") (ch/parse-string input))
        db (d/db (core/connection))
        as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query as-of-db))]
    (ch/encode
      {status as-of-date})))
```

To test this in the REPL I execute this form `(def input "\"2010-01-01\"")` and then place my cursor in the penultimate position in my parker-status function and evaluate the let form, as follows:

```
(let [date (.parse (SimpleDateFormat. "yyyy-MM-dd") (ch/parse-string input))
        db (d/db (core/connection))
        as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query as-of-db))]
    (ch/encode
      {status as-of-date}))
=> "{\"spider-man\":\"2001-01-05T00:00:00Z\"}"
```

Looks good! Let's evaluate the ns then try it with our aws command line:

`aws lambda invoke --function-name repl-ion-Compute-YOURGROUPNAME-parker-status  --payload '"2006-01-01"' /dev/stdout`

Result: `{"spider-man":"2001-01-05T00:00:00Z"}`

Huzzah!!!

This concludes our example of interactive Datomic lambda development. I was able to stub out, debug, and develop a lambda interactively. At every new attempt to understand what's going on I was able to do immediate tries and get immediate feedback as opposed to minutes-long deployments at every single code change otherwise. The only slow loop deploys were when I seeded a feature (e.g. create a new lambda), added a library (changed the deps.edn file), or decided to slow deploy a completed feature. Everything else was immediate and interactive.

## Interactive REST API Development
For our final example I'll show how to create an amazing web API interactively. This example will start with simple "OK" handler via a lambda and the API gateway and eventually create a full on Swagger API with multiple endpoints all using a _single_ lambda.

First, there's a little prework:

Add this entry to your `ion-config.edn` file in the `:lambdas` section:
```
:handler
{:fn          replion.web/handler-proxy
 :description "Our single web handler"}
```

Examine the `replion.web/handler-proxy` function and the `replion.web/handler` function that it proxies, shown here: 

``` 
(ns replion.web
  (:require [datomic.ion.lambda.api-gateway :as apigw]))

(defn handler
  [request]
  {:status 200
   :headers {}
   :body "OK"})

(def handler-proxy
  (apigw/ionize handler))
```

This ns and the changes to ion-config.edn parallel what is described [here](https://docs.datomic.com/cloud/ions/ions-tutorial.html#lambda-proxy) in the Datomic ions tutorial.

Once you've got your :hander lambda and the two functions defined above in place (already done if using this project), do a slow deploy. Once the deploy is complete you should be able to see your lambda in the AWS Lambda console. It will look something like `repl-ion-Compute-YOURGROUPNAME-handler`.

Now, follow the directions _exactly_ as described in the sections starting with "API Gateway Web Services" in the [Ions Tutorial](https://docs.datomic.com/cloud/ions/ions-tutorial.html) to create, connect, and deploy your API. Note that with the release of the new HTTP API things may change. For now, I'm going to choose the legacy REST API option and follow all of the directions as described in the Datomic ions tutorial. One variant I do on the defaults is that I enable CORS.

As described in the tuturials, you should now be able to go to your invoke URL, something like `https://abc123uandme.execute-api.us-east-1.amazonaws.com/dev`. If you paste this into a browser window, you'll get something like `{"message":"Missing Authentication Token"}`. Append _anything_ onto the end of this path to route to your endpoint. For example, `https://abc123uandme.execute-api.us-east-1.amazonaws.com/dev/handler` or `https://abc123uandme.execute-api.us-east-1.amazonaws.com/dev/datomic`. You should see "OK". Good times!

Now for our first interactive change. Let's understand how requests work for this API. Having to decode this given the nearly non-existent documentation is awful, awful, awful and will take just about forever. Here's what we're going to do to get instant information - change your handler to what is shown below and reload the ns:

```
(defn handler
  [request]
  {:status  200
   :headers {}
   :body    (with-out-str (pp/pprint request))})
```

Finally, let's append some query params onto our request for maximum information learning. Here's your new request. Just paste this into a browser - no need for curl.

`https://abc123uandme.execute-api.us-east-1.amazonaws.com/dev/handler?repl=rocks`

Holy cow! You now know everything there is to know about decoding your requests! Inspect that output to learn a few things:

 * There's a `:query-string "repl=rocks"` entry that we can use.
 * There's a `:uri "/handler"` entry with the path.
 * There's a `:datomic.ion.edn.api-gateway/data` key that has all those params, only a little more Clojure-y.
 
Now it is time to make a brief aside to have a little discussion and make a few observations regarding the awesomeness that is now at our fingertips.
 * It is common in the serverless/FaaS/lambda world to create a single lambda for each endpoint and then wire the lambda in to AWS API Gateway. This uses Gateway for routing and isolates the logic of each endpoint at the lambda level. You can then use API Gateway to build Swagger/OpenAPI documentation and so on.
 * A typical Clojure web app, on the other hand, is a single handler function that takes a request and returns a response. This handler can delegate to a routing library, such as [reitit](https://metosin.github.io/reitit/), to route requests and then wire in the individual handler logic at the edges of the router. Libraries like Swagger can be use to create nice, documented APIs.
 * Both of the above are acceptable strategies to separate the concerns of routing and logic.
 * What isn't immediately obvious, however, is that there is no reason you can't use a single handler as in point 2 above that is dispatched by a single lambda. This would effectively replace your immutant, webkit, jetty, etc. server with a lambda. This greatly minimizes the complexity of dealing with API Gateway and all of the associated moving parts to create a web app. Furthermore, since you are dispatching a single lambda it will likely be called more and remain hot. Autoscaling can be used to handle as much load as possible. Finally, this single handler is just a function that takes a request and returns a response. Once debugged, it need not be hosted as a Datomic ion (Despite being extremely convenient). It could also moved to any other compatible "driver" mechanism (Plain lambda, PaaS, etc.).
 
So, given the above, we are going to extend our single handler function above into a full-fledged web backend with routing and a great UI. Let's do it!

The first thing we should do is a single slow deploy to add in all of the libraries we want. So, we're going to import a few with our deps file and do a slow deploy:
 * `metosin/reitit             {:mvn/version "0.3.10"}`
 * `metosin/ring-http-response {:mvn/version "0.9.1"}`
 * `hiccup                     {:mvn/version "1.0.5"}`

This deploy succeeded, but now we are about to learn the value of separating our deploys from our code implementations. After this deploy I added, one at a time, the following requires to my `replion.web` ns, each time reloading the ns as the line is added:

 * `[reitit.ring :as ring]`
 * `[reitit.swagger :as swagger]`
 * `[reitit.swagger-ui :as swagger-ui]`
 
When I reload the last ns, something bad happens:

``` 
Loading src/replion/web.clj... 
Syntax error (ClassNotFoundException) compiling new at (core.clj:79:38).
com.fasterxml.jackson.core.exc.InputCoercionException
```

Had I added those requires and an initial implementation before doing my slow deploy the deploy would have failed and I would have pulled my hair out for hours trying sifting through logs trying to figure out what happend. Instead, I have some useful info and I also recall that when I deployed I had this dependency conflict: `com.fasterxml.jackson.core/jackson-core #:mvn{:version "2.9.8"}`. Googling [com.fasterxml.jackson.core.exc.InputCoercionException](http://fasterxml.github.io/jackson-core/javadoc/2.10/com/fasterxml/jackson/core/exc/InputCoercionException.html) shows that this class was added in version 2.10. I need to downgrade my jackson version. This demonstrates the value of the REPL. This is a local vs. cloud environment difference that was most easily debugged in the cloud on the system with the problem. The solution is to add the older version of jackson to my deps:

 * `com.fasterxml.jackson.core/jackson-core     {:mvn/version "2.9.8"}`
 * `com.fasterxml.jackson.core/jackson-databind {:mvn/version "2.9.8"}`

I now comment out the offending import,`[reitit.swagger-ui :as swagger-ui]`, and do another slow deploy.

Now I can successfully add all of my requires. Time to make a better handler.

With the power of reitit and some other useful nses from that project, I now create the following handler:

```
(def router
  (ring/router
    [["/api" {:swagger {:tags    ["basic api"]
                        :summary "This is the api"}}
      ["/time" {:get (fn [_] (ok {:time (str (Date.))}))}]
      ["/memory" {:get (fn [_] (ok {:runtime-stats (runtime-data)}))}]]
     ["" {:no-doc true
          :swagger {:basePath "/dev"}}
      ["/swagger.json" {:get (swagger/create-swagger-handler)}]
      ["/api-docs*" {:get (swagger-ui/create-swagger-ui-handler {:url "/dev/swagger.json"})}]]]
    {:data {:coercion   reitit.coercion.spec/coercion
            :middleware [params/wrap-params
                         middleware/wrap-format]}}))

(def handler
  (ring/ring-handler
    router
    (constantly (not-found "Not found"))))
```

This is a pretty simple example that doesn't do anything to exciting, but it does demonstrate some important concepts:
 * I am able to make a single handler using only one HTTP API gateway lambda. No need to mess with that any further.
 * I can use reitit routing to all the logic I need for my entire API.
 * I can create a nice Swagger UI with almost no effort. No need to mess with the AWS stuff.
 
Again, this was all done interactively.

Check out `replion.web` for the full example, including the progressive steps performed along the way.

## Conclusion
I hope that this project demonstrates the value of a REPL-enabled workflow with Datomic cloud. Once you know how to do it, it is pretty easy to connect a REPL and do all of your coding live on your Cloud instance. This saves a tremendous amount of time in figuring out all of the integration details. This gives you immediate access to develop your Datomic instance, lambdas, and REST APIs live and interactive.

## TODOs
This is a work in progress, but is very powerful already. Some additional things I would like to do or see done:

 * Streamline the creation of the API gateway endpoints. If the one-lambda model were used then perhaps this would minimize the moving parts such that it would be feasible to just create one preconfigured endpoint per system automatically.
 * Get an example of this working on all deployment topologies.

## Suggestions for the Datomic Team
These are a few ideas that the Datomic Team might want to consider for future development. Datomic is, of course, their product and not mine so they can do whatever they want with these suggestions.

 * Add a startup/init ns or hook along the lines of integrant init. This would enable control of stateful items such as connections as well as provide a more solid mount point for starting the nrepl server and so on.
 * If the above ideas are good ones, consider adding port fowarding over the bastion as a default option. Perhaps setting the port as a configuration option in the CloudFormation template. This would also necessitate ensuring the concept works for other topologies as well.

## Misc
 * I find it useful to capture all the commands that I can't remember as local executable scripts. For example, I've commited my _push_ file. It has the clojure push command and I've a+x'd it. Not included are one-liner files for executing each of the lambda's I've discussed above such as the parker-status invocation. 

## License

Copyright © 2019 Mark Bastian

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
