
var username;
var clientWebSocket;
//connect();
fetchinitialdata();
$("#connect").prop('disabled', false);
$("#disconnect").prop('disabled', true);
function connect() {


    var cws = new WebSocket("ws://localhost:8080/ws/chat");
    console.log(JSON.stringify(cws));
    clientWebSocket =  cws;
    cws.onopen = function () {
        // console.log("clientWebSocket.onopen", clientWebSocket);
        //console.log("clientWebSocket.readyState", clientWebSocket.);
        // fetchinitialdata();
        events("Opening connection");
        $("#connect").prop('disabled', true);
        $("#disconnect").prop('disabled', false);
        sendChat("Joined", "JOIN")
        populateActiveUsers();
    }
    cws.onclose = function (closeEvent) {
        // console.log("clientWebSocket.onclose", clientWebSocket, error);
        events("Closing connection : "+ JSON.stringify(closeEvent));
        $("#connect").prop('disabled', false);
        $("#disconnect").prop('disabled', true);
    }
    cws.onerror = function (error) {
        //  console.log("clientWebSocket.onerror", clientWebSocket, error);
        events("An error occured : "+  JSON.stringify(error));
    }
    cws.onmessage = function (data) {
        // console.log("clientWebSocket.onmessage", clientWebSocket, error);
        message(data.data);
    }
}
    function events(responseEvent) {
        document.querySelector(".events").innerHTML += responseEvent + "<br>";
        console.log(responseEvent);
    }


    function message(message) {

        let messageObj = JSON.parse(message);
        console.log(message);

        if (messageObj['username'] === username) {
            $('.messagescontainer').append(
                "<br/><div class='messagerow ownmessage'>" +
                "<div class='column left'><label class='username'>[" + messageObj['username'] + "] </label></div>" +
                "<div class='column middle'><label class='message'>" + messageObj['message'] + "</label></div>" +
                "<div class='column right'><label class='timestamp'>" + messageObj['timestamp'] + "</label></div>" +
                "</div>");
        }
        else {
            $('.messagescontainer').append(
                "<br/><div class='messagerow othersmessage'>" +
                "<div class='column left'><label class='username'>[" + messageObj['username'] + "] </label></div>" +
                "<div class='column middle'><label class='message'>" + messageObj['message'] + "</label></div>" +
                "<div class='column right'><label class='timestamp'>" + messageObj['timestamp'] + "</label></div>" +
                "</div>");
        }

        $(".messagescontainer").scrollTop($(".messagescontainer")[0].scrollHeight + Number(30));

        checkActiveUserList(messageObj);
    }




function disconnect() {
    console.log("Who Clicked Disconnect ?");
    sendChat("Left", "LEAVE");
    $('.activeuserscontainer').empty();
    //clientWebSocket.close(1000);
    clientWebSocket.onclose({});// Hack
}

function sendChat(message, chattype) {
    if(clientWebSocket !== 'undefined' && clientWebSocket.readyState === 1) {

        //clientWebSocket.send(JSON.stringify({'username': $("#username").val(),'messageText': $("#chatmessage").val()}));
        if (message != 'undefined' && message.trim() !== "") {
            clientWebSocket.send(JSON.stringify({'username': username, message, 'type': chattype}));
            $("#chatmessage").val("");
        }
    }
    else {
        console.log("Socket Not Ready");
        connect();
    }

    // $("#messages").scrollTop($("#messages")[0].scrollHeight+Number(30));
}

function fetchinitialdata() {

    $.get("/chat/initialdata", function (data) {

        username = data['currentUsername'];
        //randomvector =  data.randomvector;
        console.log("Logged in User : " + username);
        console.log("All Logged in Users  : " + JSON.stringify(data.activeUsers));
        $("#usersection").text("Welcome " + username + " !");

    });
}


function populateActiveUsers() {

    $.get("/chat/initialdata", function (data) {

        $('.activeuserscontainer').empty();
        $('.activeuserscontainer').append("<div id='au_" + username + "'>" + username + "(you)</div>");
        $.each(data.activeUsers, function (pos, val) {
            let auid = "au_" + val;
           // console.log(auid + "  " + $('#'+auid).length );
            if (username !== val && $('#'+auid).length === 0) {

                $('.activeuserscontainer').append(
                    "<div id='" + auid  + "'>" + val + "</div>"
                );
            }
        });

    });


}

function checkActiveUserList(messageObj)
{
    let u = messageObj['username'];
    let auid = "au_" + u;
   // console.log(auid + "  " + $('#'+auid).length );
    if (u !== username && messageObj['type'] === 'JOIN' && $('#'+auid).length === 0)
      {
          
        $('.activeuserscontainer').append(
            "<div id='" + auid  + "'>" + u + "</div>"
        );
        $('#'+auid).fadeIn("slow");
      }
      if (u !== username && messageObj['type'] === 'LEAVE')
      {
          $('#'+auid).remove();
      }

      

}



$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#connect").click(function () { connect(); });
    $("#disconnect").click(function () { disconnect(); });
    $("#send").click(function () { sendChat($("#chatmessage").val(), "CHAT"); });

    $('#chatmessage').on("keypress", function (e) {
        if (e.keyCode == 13) {
            sendChat($("#chatmessage").val(), "CHAT");
            return false; // prevent the button click from happening
        }
    });
});