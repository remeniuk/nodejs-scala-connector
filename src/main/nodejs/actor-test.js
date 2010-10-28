var actors = require("./ActorClient"), io = require("buffer"), fs = require("fs")
var Schema = require('protobuf_for_node').Schema

var schema = new Schema(fs.readFileSync("../../main/nodejs/RemoteActor.desc"))
var StringMessage = schema['com.vasilrem.remote.protocol.StringMessage']

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
