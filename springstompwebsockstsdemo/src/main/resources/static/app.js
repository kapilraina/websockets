var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#chats").html("");
}

function connect() {
    var socket = new SockJS('/wsock');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/publicchat', function (chatmessage) {
            console.log(JSON.stringify(chatmessage));
            showChat(JSON.parse(chatmessage.body));
        });

        stompClient.subscribe('/app/chat.participants', function (participants) {
            console.log(JSON.stringify(participants));
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function sendChat() {
    stompClient.send("/app/sendchat", {}, JSON.stringify({'username': $("#name").val(),'messageText': $("#chatbox").val()}));
}

function showChat(message) {
    
   $("#chats")
   .append(
      "<tr><td>[" + message.username + " ]</td><td>"+message.messageText +"</td><td>"+message.timestamp+"</td></tr>"
    );
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#send" ).click(function() { sendChat(); });
});