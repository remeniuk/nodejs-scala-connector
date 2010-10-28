* **[Home](http://github.com/remeniuk/nodejs-scala-connector/wiki)**
* **[Actor Proxy API](http://github.com/remeniuk/nodejs-scala-connector/wiki/Actor-Proxy-API)**
* **[[Example|http://github.com/remeniuk/nodejs-scala-connector/wiki/Example]]**

Asynchronous nature of [[node.js|http://nodejs.org/]] and it's capabilities in handling huge number of connections (with a great potential for building COMET [[web-apps that doesn't suck|http://news.ycombinator.com/item?id=1088699]]) is extremely appealing. However, being a standalone solution (web-server, TCP-server etc), connecting node.js to another ecosystems is not that straightforward (though there're known options of extending node.js functionalities via [[C++ addons|http://nodejs.org/api.html#addons-326]]).

The most popular approach of bridging node.js to JVM (Ruby, etc) is setting up a message-bus (e.g. lightweight  [[Redis Pub/Sub|http://code.google.com/p/redis/wiki/PublishSubscribe]]) between system components. The benefit of this approach is that you can have [[durable subscription|http://www.eaipatterns.com/DurableSubscription.html]] and [[guaranteed delivery|http://www.eaipatterns.com/GuaranteedMessaging.html]] almost for free (the messages sent from node.js to JVM and vise versa aren't lost even if the system crashes). On the other hand, without a careful configuration and in-life monitoring, messaging bus can turn into a bottleneck and additional source of the headaches.

**nodejs-scala-connector** is an alternative approach for linking node.js to Scala (JVM) - no mediators are involved and system components communicate to each other directly.

[[Scala Remote Actors|http://www.scala-lang.org/api/current/scala/actors/remote/RemoteActor$.html]] are accessible via TCP - multiple Actors can be registered on one node. Actors protocol is fairly simple, and the biggest problem is that remote actors marshal messages with Java Serialization facility (in order to achieve the best performance), and therefore non-Java applications can't access remote actors directly.
[[Protocol-buffers|http://code.google.com/p/protobuf/]] is an extensible alternative to native serialization that can be understood by a variety of languages: protobuf drivers exist for Java, Python, C++, **[[node.js|http://code.google.com/p/protobuf-for-node/]]** etc.
**nodejs-scala-connector** modifies standard remote actor in order to serialize messages into protobuf. On the node.js side, [[net.Stream|http://nodejs.org/api.html#net-stream-225]] is used to asynchronously communicate with actors via TCP.
###Sample code of an actor proxy


    // Create new proxy for the remote actor
    var actorProxy = actors.createActorProxy('10.6.122.29', 12345, 'server')

    // Define handler to receive message from the actor asynchronously. Handler is a function that
    // maps FQCN of the protocol stub used by the actor and function that handles serialized response
    actorProxy.receive({
        'com.vasilrem.remote.protocol.RemoteActorProtocol$StringMessage': function(data){
            console.log('Remote actor responded with: ' + StringMessage.parse(data).message)
            actorProxy.end()
            actorProxy.destroy()
        }
    })

    // When proxy is loaded, start sending messages
    actorProxy.on('ready', function(){
        actorProxy.send(
            'com.vasilrem.remote.protocol.RemoteActorProtocol$StringMessage',
            StringMessage.serialize({
                message: 'Message from node.js!'
            }))
    })
