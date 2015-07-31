var FayePG = function(){};

FayePG.prototype.PInvoke = function(method, data, callbackOK, callbackError){
	if(data == null || data === undefined)
		data = [];
	else if(!Array.isArray(data))
	  data = [data];

	cordova.exec(callbackOK, callbackError, 'FayePG', method, data);
};

FayePG.prototype.init = function(address, authToken){
	var args = [address, authToken];
	window.plugins.FayePG.PInvoke("init", args, function(msg){console.log(msg)}, function(error){console.log(error)});
};

FayePG.prototype.disconnect = function(successCallback){
		  window.plugins.FayePG.PInvoke("disconnect", null, successCallback, function(error){console.log(error)});
};

FayePG.prototype.subscribe = function(channel, commandCallback, successCallback){
  var args = [channel, commandCallback];
	window.plugins.FayePG.PInvoke("subscribe", args, successCallback, function(error){console.log(error)});
};

FayePG.prototype.sendMessage = function(channel, data){
	var args = [channel, data];
	window.plugins.FayePG.PInvoke("sendMessage", args, function(msg){console.log(msg)}, function(error){console.log(error)});
};

FayePG.prototype.setNotifTexts = function(title, text, ticker){
	var args = [title, text, ticker];
	window.plugins.FayePG.PInvoke("setNotifTexts", args, function(msg){console.log(msg)}, function(error){console.log(error)});
};

cordova.addConstructor(function() {
  if (!window.Cordova) {
	  window.Cordova = cordova;
	};
 if(!window.plugins) window.plugins = {};
 window.plugins.FayePG = new FayePG();																						                       });

