var io = require("buffer"), 
fs = require("fs"), 
sys = require("sys"), 
net = require("net")

var Schema = require('protobuf_for_node').Schema
var schema = new Schema(fs.readFileSync("./RemoteActor.desc"))
	
var GenericMessage = schema['com.vasilrem.remote.protocol.GenericMessageProtocol']
var Node = schema['com.vasilrem.remote.protocol.NodeProtocol']
var Locator = schema['com.vasilrem.remote.protocol.LocatorProtocol']
var NamedSend = schema['com.vasilrem.remote.protocol.NameSendProtocol']

/**
* Creates proxy to Scala RemoteActor. Communication is done via Actor's server socket.
* @param hostname actor's host
* @param port actor's port
* @param actorName actor's name alias (converted to Symbol in Scala)
*/
exports.createActorProxy = function(hostname, port, actorName){
    
    console.log('Connecting to ' + hostname + ':' + port)
	
    var client = net.createConnection(port, hostname)
    var node = GenericMessage.serialize({
        fqcn: 'com.vasilrem.remote.protocol.RemoteActorProtocol$NodeProtocol',
        message: Node.serialize({
            host:hostname,
            port: '' + port
        })
    })
		
    /**
    * Asynchronously sends message to the actor
    * @param fqcn FQCN of the message's Java protocol stub
    * @param message message serialized into protobuf
    */
    net.Stream.prototype.send = function(fcqn, message){
        console.log("Sending message: " + message)
        var namedSend = GenericMessage.serialize({
            fqcn: 'com.vasilrem.remote.protocol.RemoteActorProtocol$NameSendProtocol',
            message: NamedSend.serialize({
                sender:   {
                    node:{
                        host:hostname,
                        port: '' + port
                    },
                    serviceName: 'nodejs'
                },
                receiver: {
                    node:{
                        host:hostname,
                        port: '' + port
                    },
                    serviceName: actorName
                },
                message: GenericMessage.serialize({
                    fqcn: fcqn,
                    message: message
                })
            })
        })
        client.writeInt(namedSend.length)
        client.write(namedSend)
    }
	
    /**
    * Reads one message from the buffer
    * @param buffer data received from the remote actor
    * @param handler handler for received messages
    */	
    net.Stream.prototype.readMessage = function(buffer, handler){
        var offset = 4
        var messageSize = this.byteArrayToInt(buffer.slice(0, offset))
        var message = GenericMessage.parse(new io.Buffer(
            NamedSend.parse(new io.Buffer(
                GenericMessage.parse(buffer.slice(offset, messageSize + offset)
                    ).message)
            ).message))
        handler[message.fqcn](new io.Buffer(message.message))
        if(buffer.length > messageSize + offset){
            this.readMessage(buffer.slice(messageSize + offset, buffer.length), handler)
        }
    }
		
    var lostBytes = new io.Buffer([])

    /**
    * Receives response from the actor
    * @param handler handler is a FQCN->function mapping
    */
    net.Stream.prototype.receive = function(handler){
        client.addListener("data", function(data){
            if(data.length < 4){
                // lost bytes should be prepended to the next received message
                lostBytes = data
                console.log('Received message is to short!')
                return
            }
            var buffer = new io.Buffer(data.length + 1)
            lostBytes.copy(buffer, 0, 0 ,lostBytes.length)
            data.copy(buffer, lostBytes.length, 0, data.length)
            lostBytes = new io.Buffer([])
            this.readMessage(buffer, handler)
        })
    }

    /**
    * Writes 4-byte integer to the stream
    * @param value integer
    */
    net.Stream.prototype.writeInt = function(value){
        var bytes =new Array(4)
        bytes[0] = value >> 24
        bytes[1] = value >> 16
        bytes[2] = value >> 8
        bytes[3] = value
        this.write(new io.Buffer(bytes))
    }

    /**
    * Converts 4-byte array to integer
    * @param array 4-byte array
    */
    net.Stream.prototype.byteArrayToInt = function(array){
        if(array.length > 4) return NaN
        var value = array[0]
        for(var i = 1; i<array.length; i++){
            value = value*256 + array[i]
        }
        return value
    }

    client.addListener("connect", function(){
        console.log('Successfully connected to ' + hostname + ':' + port)
        client.writeInt(node.length)
        client.write(node)
        console.log('Sent greeting to the actor node.')
        client.emit('ready')
    })
	
    return client
}

